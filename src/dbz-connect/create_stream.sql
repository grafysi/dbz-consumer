CREATE STREAM admissions_stream WITH (
    KAFKA_TOPIC = 'mimic4demo.mimiciv_hosp.admissions',
    VALUE_FORMAT = 'AVRO',
    KEY_FORMAT = 'AVRO'
);


CREATE TABLE admissions_table WITH (
                                  KAFKA_TOPIC = 'mimic4demo.mimiciv_hosp.admissions',
                                  VALUE_FORMAT = 'AVRO',
                                  KEY_FORMAT = 'AVRO'
                                  );