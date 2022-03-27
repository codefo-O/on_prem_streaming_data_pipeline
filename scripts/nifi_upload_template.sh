#!/bin/sh

#gets processID for the new process group
proccessId=$(curl -k X POST http://localhost:8080/nifi-api/flow/process-groups/root/status?recursive=true | jq '.processGroupStatus.id' | tr -d '"')
#uploads to sample template to the process group with id $processid
curl -k -F template=@templates/nifi_bus_demo.xml -X POST http://localhost:8080/nifi-api/process-groups/$proccessId/templates/upload
#Onces template has been uploaded gets the templateID 
templateId=$(curl -v -X GET http://localhost:8080/nifi-api/resources | jq '.' | grep '"/templates/' | awk '{print $2}' | sed 's/\"//g'| sed 's/\/templates//g' | tr -d '/,')
#Adds the template to current workspace
curl -s http://localhost:8080/nifi-api/process-groups/$proccessId/template-instance -H 'Content-Type: application/json' -d "{\"templateId\": \""$templateId"\",\"originX\": 100.0,\"originY\": 100.0,\"disconnectedNodeAcknowledged\": false}" > /dev/null
#Gets the connectionID for the MySQL Conenction String for testing setting password for future automation
#connectionID=$(curl -v -X GET http://localhost:8080/nifi-api/flow/process-groups/$proccessId/controller-services | jq '.controllerServices'  | grep -m 1 'id' | grep -oP '(?<=\": \").*?(?=")')

echo -e \ 
echo "Finished importing NiFi Template"


