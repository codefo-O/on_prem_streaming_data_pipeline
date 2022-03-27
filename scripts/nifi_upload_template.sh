#!/bin/sh

proccessId=$(curl -k X POST http://localhost:8080/nifi-api/flow/process-groups/root/status?recursive=true | jq '.processGroupStatus.id' | tr -d '"')
curl -k -F template=@templates/nifi_bus_demo.xml -X POST http://localhost:8080/nifi-api/process-groups/$proccessId/templates/upload
tempateId=$(curl -v -X GET http://localhost:8080/nifi-api/resources | jq '.' | grep '"/templates/' | awk '{print $2}' | sed 's/\"//g'| sed 's/\/templates//g' | tr -d '/,')
curl -s http://localhost:8080/nifi-api/process-groups/$proccessId/template-instance -H 'Content-Type: application/json' -d "{\"templateId\": \""$tempateId"\",\"originX\": 100.0,\"originY\": 100.0,\"disconnectedNodeAcknowledged\": false}" > /dev/null
connectioNID=$(curl -v -X GET http://localhost:8080/nifi-api/flow/process-groups/$proccessId/controller-services | jq '.controllerServices'  | grep -m 1 'id' | grep -oP '(?<=\": \").*?(?=")')

echo -e \ 
echo "Finished importing NiFi Template"


