package com.grafysi.kbasedsinker.processor;

import com.grafysi.kbasedsinker.message.DbzSinkValue;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;

import java.util.List;

public interface RecordProcessor {

    void process(ConsumerRecords<GenericRecord, GenericRecord> records);
}
