#!/bin/bash

# Running the image should trigger the test script, dumping output to console.

cd /opt/functional/cosigner-client/target/
CLIENTLIB=cosigner-client-java-0.0.1-SNAPSHOT.jar
USERKEY=deadbeef

echo "Cosigner functional tests"
echo ""

echo "Configuration: "
cat cosigner-client.properties

echo ""
echo ""
echo "Environment vars..."
set

echo "Testing client execution..."
java -jar ${CLIENTLIB}
if [[ $? -ne 0 ]]; then
  echo "Can't run JAR!!!"
  exit 1
fi

echo ""
echo "Listing currencies..."
CURRENCIES=$(java -jar ${CLIENTLIB} listCurrencies | tail -n 1)
echo ${CURRENCIES}

echo ""
echo "Generating address..."
ADDRESS=$(java -jar ${CLIENTLIB} getNewAddress BTC ${USERKEY} | tail -n 1)
echo ${ADDRESS}

echo ""
echo "Sending money.... eventually"
