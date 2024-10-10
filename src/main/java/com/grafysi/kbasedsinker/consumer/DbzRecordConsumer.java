package com.grafysi.kbasedsinker.consumer;

import com.grafysi.kbasedsinker.processor.RecordProcessor;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.CommitFailedException;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class DbzRecordConsumer implements RecordConsumer {

    private static final Logger log = LoggerFactory.getLogger(DbzRecordConsumer.class);

    private final KafkaConsumer<GenericRecord, GenericRecord> consumer;

    private final String topic;

    private final RecordProcessor processor;

    private final CountDownLatch shutdownLatch;

    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    private final long pollIntervalMs;

    private final boolean consumeAtBeginning;


    public DbzRecordConsumer(String topic, Properties properties,
                             RecordProcessor processor, CountDownLatch shutdownLatch,
                             long pollIntervalMs, boolean consumeAtBeginning) {
        this.topic = topic;
        this.consumer = new KafkaConsumer<>(properties);
        this.processor = processor;
        this.shutdownLatch = shutdownLatch;
        this.pollIntervalMs = pollIntervalMs;
        this.consumeAtBeginning = consumeAtBeginning;
    }

    @Override
    public void run() {
        try {
            isRunning.set(true);
            consumer.subscribe(Collections.singleton(topic));
            resolveInitialOffset();
            while (true) {
                var records = consumer.poll(Duration.ofMillis(pollIntervalMs));
                processor.process(records);
                doCommitSync();
            }
        } catch (WakeupException e) {
            // ignore as we're closing
        } catch (Exception e) {
            log.error("Unexpected error", e);
        } finally {
            cleanup();
        }
    }

    private void doCommitSync() {
        try {
            consumer.commitSync();
        } catch (WakeupException e) {
            doCommitSync();
            throw e;
        } catch (CommitFailedException e) {
            log.debug("Commit failed", e);
        }
    }

    private void resolveInitialOffset() {
        if (consumeAtBeginning) {
            log.info("Assigned partitions: {}", consumer.assignment());
            consumer.seekToBeginning(consumer.assignment());
            log.info("Seek to first offset");
        }
    }


    private void cleanup() {
        try {
            doCommitSync();
        } finally {
            consumer.close();
            shutdownLatch.countDown();
            isRunning.set(false);
        }
    }

    @Override
    public void shutdown() {
        if (!isRunning.get()) {
            return;
        }
        consumer.wakeup();
    }

    public KafkaConsumer<?, ?> internal() {
        return consumer;
    }

    public static Builder create() {
        return new Builder();
    }


    // builder for DbzRecordConsumer
    public static class Builder {

        private Properties properties;

        private String topic;

        private RecordProcessor processor;

        private CountDownLatch shutdownLatch;

        private long pollIntervalMs;

        private boolean consumeAtBeginning = false;


        private Builder() {

        }

        public Builder setConsumeAtBeginning(boolean isSet) {
            this.consumeAtBeginning = isSet;
            return this;
        }

        public Builder setTopic(String topic) {
            this.topic = topic;
            return this;
        }

        public Builder setProcessor(RecordProcessor processor) {
            this.processor = processor;
            return this;
        }

        public Builder setPollIntervalMs(long ms) {
            this.pollIntervalMs = ms;
            return this;
        }

        public Builder setShutdownLatch(CountDownLatch latch) {
            this.shutdownLatch = latch;
            return this;
        }

        public Builder setProperties(Properties properties) {
            this.properties = properties;
            return this;
        }

        public Builder addProperty(String key, String value) {
            ensurePropertiesExists();
            properties.put(key, value);
            return this;
        }

        public Builder addProperties(Properties properties) {
            ensurePropertiesExists();
            this.properties.putAll(properties);
            return this;
        }

        private void ensurePropertiesExists() {
            if (properties == null) {
                this.properties = new Properties();
            }
        }

        public DbzRecordConsumer build() {
            return new DbzRecordConsumer(topic, properties, processor,
                    shutdownLatch, pollIntervalMs, consumeAtBeginning);
        }

    }

}



