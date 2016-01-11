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
echo "Fund the account."

CURLURL="http://${BITCOIND_PORT_18332_TCP_ADDR}:${BITCOIND_PORT_18332_TCP_PORT}"
curl --user bitcoinrpc:changeit -H 'Content-Type: application/json' -X POST -d '{"jsonrpc": "2.0", "id": "curl", "method": "generate", "params": [101] }' ${CURLURL}
curl --user bitcoinrpc:changeit -H 'Content-Type: application/json' -X POST -d "{\"jsonrpc\": \"2.0\", \"id\": \"curl\", \"method\": \"sendtoaddress\", \"params\": [\"${ADDRESS}\", 5] }" ${CURLURL}
curl --user bitcoinrpc:changeit -H 'Content-Type: application/json' -X POST -d '{"jsonrpc": "2.0", "id": "curl", "method": "generate", "params": [10] }' ${CURLURL}

echo "Generating a second address..."
ADDRESS2=$(java -jar ${CLIENTLIB} getNewAddress BTC ${USERKEY} | tail -n 1)
echo ${ADDRESS2}

echo "Preparing a transaction to second address..."
TX=$(java -jar ${CLIENTLIB} prepareTransaction BTC ${ADDRESS} ${ADDRESS2} 3 | tail -n 1)
echo ${TX}

