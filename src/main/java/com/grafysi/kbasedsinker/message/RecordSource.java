package com.grafysi.kbasedsinker.message;

import lombok.Data;
import org.apache.avro.reflect.AvroName;

import java.util.List;

@Data
public class RecordSource {

    private String version;

    private String connector;

    private String name;

    @AvroName("ts_ms")
    private Long transactionMs;

    private Boolean snapshot;

    @AvroName("db")
    private String dbName;

    private List<String> sequence;

    @AvroName("ts_us")
    private Long transactionUs;

    @AvroName("ts_ns")
    private Long transactionNs;

    @AvroName("schema")
    private String dbSchema;

    private String table;

    @AvroName("txId")
    private Long transactionId;

    private Long lsn;

    @AvroName("xmin")
    private Long xMin;
}









































