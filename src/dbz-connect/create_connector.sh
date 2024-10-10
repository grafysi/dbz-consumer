curl -i -X POST -H "Accept:application/json" -H "Content-Type:application/json" localhost:8083/connectors/ -d @pg_avro.json


curl -i -X POST -H "Accept:application/json" -H "Content-Type:application/json" dbzconnect-api.hzp.local:8000/connectors/ -d @pg_json_reroute.json

curl -i -X POST -H "Accept:application/json" -H "Content-Type:application/json" dbzconnect-api.hzp.local:8000/connectors/ -d @pg_avro_apicurio.json

curl -i -X POST -H "Accept:application/json" -H "Content-Type:application/json" dbzconnect-api.hzp.local:8000/connectors/ -d @pg_avro_apicurio_reroute.json

curl -i -X POST -H "Accept:application/json" -H "Content-Type:application/json" dbzconnect-api.hzp.local:8000/connectors/ -d @pg_avro_apicurio_iheader.json

curl -i -X POST -H "Accept:application/json" -H "Content-Type:application/json" dbzconnect-api.hzp.local:8000/connectors/ -d @pg_avro_tables_select.json
