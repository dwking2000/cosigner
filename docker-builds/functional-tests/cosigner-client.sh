#!/bin/bash

# Running the image should trigger the test script, dumping output to console.

cd /opt/functional/cosigner-client/target/
CLIENTLIB=cosigner-client-java-0.0.1-SNAPSHOT.jar
USERKEY=deadbeef

echo "Cosigner functional tests"
echo ""

#========================SETUP===============================
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

#========================BITCOIN===============================
echo ""
echo "Testing Bitcoind (BTC)"
echo ""
echo "Generating address..."
ADDRESS=$(java -jar ${CLIENTLIB} getNewAddress BTC ${USERKEY} | tail -n 1)
echo ${ADDRESS}

echo ""
echo "Funding the account with 5 BTC"

CURLURL="http://${BITCOIND_PORT_18332_TCP_ADDR}:${BITCOIND_PORT_18332_TCP_PORT}"
curl -s --user bitcoinrpc:changeit -H 'Content-Type: application/json' -X POST -d '{"jsonrpc": "2.0", "id": "curl", "method": "generate", "params": [101] }' ${CURLURL} ${OUTPUTSINK}
curl -s --user bitcoinrpc:changeit -H 'Content-Type: application/json' -X POST -d "{\"jsonrpc\": \"2.0\", \"id\": \"curl\", \"method\": \"sendtoaddress\", \"params\": [\"${ADDRESS}\", 5] }" \
${CURLURL} ${OUTPUTSINK}
curl -s --user bitcoinrpc:changeit -H 'Content-Type: application/json' -X POST -d '{"jsonrpc": "2.0", "id": "curl", "method": "generate", "params": [10] }' ${CURLURL} ${OUTPUTSINK}

echo ""
echo "Generating a second address..."
ADDRESS2=$(java -jar ${CLIENTLIB} getNewAddress BTC ${USERKEY} | tail -n 1)
echo ${ADDRESS2}

echo ""
echo "Checking balances..."
ADD1BALANCE=$(java -jar ${CLIENTLIB} getBalance BTC ${ADDRESS} tail -n 1)
ADD2BALANCE=$(java -jar ${CLIENTLIB} getBalance BTC ${ADDRESS2} tail -n 1)
echo "${ADDRESS}: ${ADD1BALANCE}"
echo "${ADDRESS2}: ${ADD2BALANCE}"

echo ""
echo "Preparing a transaction to second address..."
TX=$(java -jar ${CLIENTLIB} prepareTransaction BTC ${ADDRESS} ${ADDRESS2} 3 ${USERKEY} | tail -n 1)
echo ${TX}

echo ""
echo "Signing the TX (Configuration could mean nothing changes)..."
SIGNEDTX=$(java -jar ${CLIENTLIB} approveTransaction BTC ${TX} | tail -n 1)
echo ${SIGNEDTX}

echo ""
echo "Submitting the TX..."
TXID=$(java -jar ${CLIENTLIB} sendTransaction BTC ${SIGNEDTX} | tail -n 1)
echo ${TXID}

echo ""
echo "Generating confirmations..."
curl -s --user bitcoinrpc:changeit -H 'Content-Type: application/json' -X POST -d '{"jsonrpc": "2.0", "id": "curl", "method": "generate", "params": [10] }' ${CURLURL} ${OUTPUTSINK}

echo ""
echo "Checking balances..."
ADD1BALANCE=$(java -jar ${CLIENTLIB} getBalance BTC ${ADDRESS} | tail -n 1)
ADD2BALANCE=$(java -jar ${CLIENTLIB} getBalance BTC ${ADDRESS2} | tail -n 1)
echo "${ADDRESS}: ${ADD1BALANCE}"
echo "${ADDRESS2}: ${ADD2BALANCE}"

#========================ETHEREUM===============================
echo ""
echo "Testing Ethereum (ETH)"
echo "Not done yet...."

