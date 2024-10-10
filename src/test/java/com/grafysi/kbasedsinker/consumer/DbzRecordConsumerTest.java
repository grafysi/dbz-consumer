package com.grafysi.kbasedsinker.consumer;

import io.apicurio.registry.serde.SerdeConfig;
import io.apicurio.registry.serde.avro.AvroKafkaDeserializer;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.config.SaslConfigs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class DbzRecordConsumerTest {

    private static final Logger log = LoggerFactory.getLogger(DbzRecordConsumerTest.class);

    private Properties props;

    private ExecutorService executor;

    private final AtomicInteger previousBatchSize = new AtomicInteger(-1);

    @BeforeEach
    void initTest() {
        this.props = new Properties();

        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "strimzi-bootstrap.hzp.local:8443");
        props.put(ConsumerConfig.CLIENT_ID_CONFIG, "mimic4demo-source1-avro-consumer");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "mimic4demo-source1-avro-consumer-group");
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, "30000");

        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                AvroKafkaDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                AvroKafkaDeserializer.class.getName());

        props.put(SerdeConfig.REGISTRY_URL, "http://apicurio.hzp.local:8000/apis/registry/v2");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "5");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        props.putIfAbsent(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        props.put(SaslConfigs.SASL_JAAS_CONFIG,
                "org.apache.kafka.common.security.scram.ScramLoginModule " +
                        "required username=\"open-user1\" password=\"abcd1234\";");

        props.put("ssl.truststore.location",
                getResourcePath("strimzi-kafka-truststore.jks"));
        props.put("ssl.truststore.password", "kenobi");

        props.put(SaslConfigs.SASL_MECHANISM, "SCRAM-SHA-512");
        props.put("security.protocol", "SASL_SSL");

        executor = Executors.newCachedThreadPool();
    }

    @AfterEach
    void cleanupTest() throws InterruptedException {
        executor.shutdown();
        if (executor.awaitTermination(5, TimeUnit.SECONDS)) {
            log.info("Executor stopped gracefully.");
        } else {
            log.info("Executor isn't gracefully stopped.");
        }
    }

    private String getResourcePath(String resourceName) {
        return getClass().getClassLoader().getResource(resourceName).getPath();
    }

    @Test
    void testConsumeAndPrint() throws InterruptedException {
        final var shutdownLatch = new CountDownLatch(1);
        final var topic = "dbz.mimic4demo.hosp.all";
        var consumer = DbzRecordConsumer.create()
                .setTopic(topic)
                .setShutdownLatch(shutdownLatch)
                .setProperties(props)
                .setConsumeAtBeginning(true)
                .setPollIntervalMs(600)
                .setProcessor((records) -> {
                    var recordsProcessed = processRecordsByPrinting(records);
                    previousBatchSize.set(recordsProcessed);
                })
                .build();

        executor.submit(consumer);

        sleepMs(Long.MAX_VALUE);

        while (!previousBatchSize.compareAndSet(0, 0)) {
            sleepMs(20);
        }

        consumer.shutdown();
        //log.info("Assigned partitions: {}", consumer.internal().assignment());

        shutdownLatch.await();
    }

    private int processRecordsByPrinting(ConsumerRecords<GenericRecord, GenericRecord> records) {
        var recordCount = 0;
        var recordIter = records.iterator();
        while (recordIter.hasNext()) {
            var record = recordIter.next();

            var headerIter = record.headers().headers("__from_table").iterator();

            if (!headerIter.hasNext()) {
                continue;
            }

            var headerValue = new String(headerIter.next().value(), StandardCharsets.UTF_8);
            if (!headerValue.equals("__dbz_mimic4demo.mimiciv_hosp.patients")
                    && !headerValue.equals("__dbz_mimic4demo.mimiciv_hosp.admissions")) {
                continue;
            }

            log.info("Print record key and value...");
            var key = record.key();
            printGenericRecord(key);

            var value = (GenericRecord) record.value().get("after");
            printGenericRecord(value);
            log.info(value.toString());
            log.info("---------------------------------------");

            recordCount++;
        }

        //log.info("Processed batch of {} records.", recordCount);
        //log.info("---------------------------------------");
        return recordCount;
    }

    private void printGenericRecord(GenericRecord record) {
        var fields = record.getSchema().getFields();
        fields.forEach(field -> {
            log.info("{}: {}", field.name(), record.get(field.name()));
        });
    }

    private void sleepMs(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            log.info("Unexpected interruption", e);
            throw new RuntimeException();
        }
    }

    @Test
    void testResetGroupOffset() {
        final var shutdownLatch = new CountDownLatch(1);
        final var topic = "mimic4demo.mimiciv_hosp.patients";
        var consumer = DbzRecordConsumer.create()
                .setTopic(topic)
                .setShutdownLatch(shutdownLatch)
                .setProperties(props)
                .setConsumeAtBeginning(true)
                .setPollIntervalMs(300)
                .setProcessor((records) -> {
                    var recordsProcessed = processRecordsByPrinting(records);
                    previousBatchSize.set(recordsProcessed);
                })
                .build();

        var internalConsumer = consumer.internal();

        internalConsumer.subscribe(Collections.singleton(topic));
        sleepMs(100);
        internalConsumer.poll(Duration.ofMillis(100));
        internalConsumer.commitSync();
        sleepMs(100);
        //internalConsumer.poll(100);

        var topicInfos = internalConsumer.listTopics();
        log.info("Topic infos: {}", topicInfos);

        var topicPartitions = internalConsumer.assignment();

        log.info("Assignments: {}", topicPartitions);

        topicPartitions.forEach(tp -> internalConsumer.seek(tp, 0));

        internalConsumer.commitSync();
    }

    @Test
    void testPrintRawRecord() throws InterruptedException {
        final var shutdownLatch = new CountDownLatch(1);
        final var topic = "mimic4demo.mimiciv_hosp.admissions";
        var consumer = DbzRecordConsumer.create()
                .setTopic(topic)
                .setShutdownLatch(shutdownLatch)
                .setProperties(props)
                .setConsumeAtBeginning(true)
                .setPollIntervalMs(300)
                .setProcessor((records) -> {
                    var recordsProcessed = printRawRecord(records);
                    previousBatchSize.set(recordsProcessed);
                })
                .build();

        executor.submit(consumer);

        sleepMs(Long.MAX_VALUE);

        while (!previousBatchSize.compareAndSet(0, 0)) {
            sleepMs(20);
        }

        consumer.shutdown();
        //log.info("Assigned partitions: {}", consumer.internal().assignment());

        shutdownLatch.await();
    }

    private int printRawRecord(ConsumerRecords<GenericRecord, GenericRecord> records) {
        var recordCount = 0;
        var recordIter = records.iterator();
        while (recordIter.hasNext()) {
            var record = recordIter.next();
            log.info("Print raw record...");
            log.info("Record key: {}", record.key());
            log.info("Record value: {}", record.value());
            log.info("---------------------------------------");
            recordCount++;
        }

        log.info("Processed batch of {} records.", recordCount);
        return recordCount;
    }
}



































