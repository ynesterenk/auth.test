#!/usr/bin/env bash

set -e

if [ $# -ne 2 ]; then
  echo "Usage: $0 [user-name] [resource-group]"
  echo ''
  echo 'Options:'
  echo '  user-name        User name'
  echo '  resource-group   Azure resource group name'
  exit 1
fi

USER=$1
RESOURCE_GROUP=$2
TEMPLATE_FILE='azure-deploy.json'
BUILD=`date -u +%Y-%m-%dT%H:%M:%SZ`

echo 'Create/Update stack initialized'
for MODULE in 'server' 'basic-authenticator' 'jwt-authorizer'; do
  az storage blob upload --account-name ${STORAGE_ACCOUNT_NAME} --container-name ${USER} \
    --name ${BUILD}/authorization-service-${MODULE}-1.0-SNAPSHOT.jar \
    --file "../authorization-service-${MODULE}/target/authorization-service-${MODULE}-1.0-SNAPSHOT.jar"
done

az deployment group create --resource-group ${RESOURCE_GROUP} --template-file ${TEMPLATE_FILE} \
  --parameters UserName=${USER} Build=${BUILD}
