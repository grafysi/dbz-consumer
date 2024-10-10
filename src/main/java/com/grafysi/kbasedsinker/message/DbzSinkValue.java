package com.grafysi.kbasedsinker.message;

import lombok.Data;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.reflect.AvroName;
import org.apache.avro.specific.SpecificRecord;
import org.apache.avro.specific.SpecificRecordBase;

@Data
public class DbzSinkValue {

    private GenericRecord before;

    private GenericRecord after;

    private RecordSource source;

    private GenericRecord transaction;

    @AvroName("operation")
    private GenericRecord operation;

    @AvroName("ts_ms")
    private Long transactionMs;

    @AvroName("ts_us")
    private Long transactionUs;

    @AvroName("ts_ns")
    private Long transactionNs;
}

















