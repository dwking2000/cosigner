#!/bin/bash

# Running the image should trigger the test script, dumping output to console.

cd /opt/functional/cosigner-client/target/
CLIENTLIB=cosigner-client-java-0.0.1-SNAPSHOT.jar
USERKEY=deadbeef

echo "Cosigner functional tests"
echo ""

echo "Waiting for cosigner to start..."
cat < /dev/tcp/${COSIGNER_PORT_8443_TCP_ADDR}/8443
CATRES=$?
echo ${CATRES}
while [ ${CATRES} -ne 0 ]; do
  sleep 5
  cat < /dev/tcp/${COSIGNER_PORT_8443_TCP_ADDR}/8443
  CATRES=$?
done

#========================SETUP===============================
CURLURL="http://${GETH_PORT_8101_TCP_ADDR}:${GETH_PORT_8101_TCP_PORT}"
export GETH_CONTRACT_ACCOUNT=$(curl -s -H 'Content-Type: application/json' -X POST -d '{"jsonrpc": "2.0", "id": "curl", "method": "eth_coinbase", "params": [] }' ${CURLURL} | jq '.result' | sed 's/^"//g' | sed 's/"$//g' | sed 's/^0x//g')

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
curl -s --user bitcoinrpc:changeit -H 'Content-Type: application/json' -X POST -d '{"jsonrpc": "2.0", "id": "curl", "method": "generate", "params": [101] }' ${CURLURL}
curl -s --user bitcoinrpc:changeit -H 'Content-Type: application/json' -X POST -d "{\"jsonrpc\": \"2.0\", \"id\": \"curl\", \"method\": \"sendtoaddress\", \"params\": [\"${ADDRESS}\", 5] }" \
${CURLURL}
curl -s --user bitcoinrpc:changeit -H 'Content-Type: application/json' -X POST -d '{"jsonrpc": "2.0", "id": "curl", "method": "generate", "params": [10] }' ${CURLURL}

echo ""
echo "Generating a second address..."
ADDRESS2=$(java -jar ${CLIENTLIB} getNewAddress BTC ${USERKEY} | tail -n 1)
echo ${ADDRESS2}

echo ""
echo "Checking balances..."
ADD1BALANCE=$(java -jar ${CLIENTLIB} getBalance BTC ${ADDRESS} | tail -n 1)
ADD2BALANCE=$(java -jar ${CLIENTLIB} getBalance BTC ${ADDRESS2} | tail -n 1)
echo "${ADDRESS}: ${ADD1BALANCE}"
echo "${ADDRESS2}: ${ADD2BALANCE}"

echo ""
echo "Preparing a transaction to second address..."
TX=$(java -jar ${CLIENTLIB} prepareTransaction BTC ${ADDRESS} ${ADDRESS2} 3 ${USERKEY} | tail -n 1)
echo ${TX}

echo ""
echo "Signing the TX..."
SIGNEDTX=$(java -jar ${CLIENTLIB} approveTransaction BTC ${TX} ${ADDRESS} | tail -n 1)
echo ${SIGNEDTX}

echo ""
echo "Submitting the TX..."
TXID=$(java -jar ${CLIENTLIB} broadcastTransaction BTC ${SIGNEDTX} | tail -n 1)
echo ${TXID}

echo ""
echo "Generating confirmations..."
curl -s --user bitcoinrpc:changeit -H 'Content-Type: application/json' -X POST -d '{"jsonrpc": "2.0", "id": "curl", "method": "generate", "params": [10] }' ${CURLURL}

echo ""
echo "Checking balances..."
ADD1BALANCE=$(java -jar ${CLIENTLIB} getBalance BTC ${ADDRESS} | tail -n 1)
ADD2BALANCE=$(java -jar ${CLIENTLIB} getBalance BTC ${ADDRESS2} | tail -n 1)
echo "${ADDRESS}: ${ADD1BALANCE}"
echo "${ADDRESS2}: ${ADD2BALANCE}"

#========================ETHEREUM===============================
echo ""
echo "Testing Ethereum (ETH)"

CURLURL="http://${GETH_PORT_8101_TCP_ADDR}:${GETH_PORT_8101_TCP_PORT}"

waitBlock () {
  echo ""
  CURRBLOCK=$(curl -s -H 'Content-Type: application/json' -X POST -d '{"jsonrpc": "2.0", "id": "curl", "method": "eth_blockNumber", "params": []}'  ${CURLURL} | jq '.result' | sed 's/^"//g' | sed 's/"$//g' | sed 's/^0x//g')
  BLOCK=$(curl -s -H 'Content-Type: application/json' -X POST -d '{"jsonrpc": "2.0", "id": "curl", "method": "eth_blockNumber", "params": []}'  ${CURLURL} | jq '.result' | sed 's/^"//g' | sed 's/"$//g' | sed 's/^0x//g')
  while [ ${CURRBLOCK} == ${BLOCK} ]; do
    echo "Waiting for block generation..."
    sleep 5
    BLOCK=$(curl -s -H 'Content-Type: application/json' -X POST -d '{"jsonrpc": "2.0", "id": "curl", "method": "eth_blockNumber", "params": []}'  ${CURLURL} | jq '.result' | sed 's/^"//g' | sed 's/"$//g' | sed 's/^0x//g')
    echo "Starting block: " ${CURRBLOCK}
    echo "New block number: " ${BLOCK}
  done
  return 0
}

echo ""
echo "Getting coinbase"
COINBASE=$(curl -s -H 'Content-Type: application/json' -X POST -d '{"jsonrpc": "2.0", "id": "curl", "method": "eth_coinbase", "params": [] }' ${CURLURL} | jq '.result' | sed 's/^"//g' | sed 's/"$//g' | sed 's/^0x//g')
echo ${COINBASE}

CONTRACT_ACCOUNT=e8a78b476ae1403b7fd39b662545ae608aced7c7
echo ""
echo "Contract account should be: "${CONTRACT_ACCOUNT}

echo ""
echo "Waiting 10 blocks for coinbase to generate funds before funding..."
waitBlock
waitBlock
waitBlock
waitBlock
waitBlock
waitBlock
waitBlock
waitBlock
waitBlock
waitBlock

echo ""
echo "Funding contract account"
TX=$(java -jar ${CLIENTLIB} prepareTransaction ETH ${COINBASE} ${CONTRACT_ACCOUNT} 5 | tail -n 1)
echo ${TX}
SIGNEDTX=$(java -jar ${CLIENTLIB} approveTransaction ETH ${TX} ${COINBASE} | tail -n 1)
echo ${SIGNEDTX}
TXID=$(java -jar ${CLIENTLIB} broadcastTransaction ETH ${SIGNEDTX} | tail -n 1)
echo ${TXID}

echo ""
echo "Waiting 10 blocks for coinbase to generate funds before starting..."
waitBlock
waitBlock
waitBlock
waitBlock
waitBlock
waitBlock
waitBlock
waitBlock
waitBlock
waitBlock

echo ""
echo "Checking balances"
CBBALANCE=$(java -jar ${CLIENTLIB} getBalance ETH ${COINBASE} | tail -n 1)
CABALANCE=$(java -jar ${CLIENTLIB} getBalance ETH ${CONTRACT_ACCOUNT} | tail -n 1)
echo ${COINBASE}": "${CBBALANCE}
echo ${CONTRACT_ACCOUNT}": "${CABALANCE}

echo ""
echo "Generating address"
ADDRESS=$(java -jar ${CLIENTLIB} getNewAddress ETH ${USERKEY} | tail -n 1)
echo ${ADDRESS}

waitBlock
waitBlock
waitBlock
waitBlock
waitBlock

echo ""
echo "Generating second address"
ADDRESS2=$(java -jar ${CLIENTLIB} getNewAddress ETH ${USERKEY} | tail -n 1)
echo ${ADDRESS2}

waitBlock
waitBlock
waitBlock
waitBlock
waitBlock

echo ""
echo "Checking balances..."
ADD1BALANCE=$(java -jar ${CLIENTLIB} getBalance ETH ${ADDRESS} | tail -n 1)
ADD2BALANCE=$(java -jar ${CLIENTLIB} getBalance ETH ${ADDRESS2} | tail -n 1)
echo "${ADDRESS}: ${ADD1BALANCE}"
echo "${ADDRESS2}: ${ADD2BALANCE}"

echo ""
echo "Funding address"
TX=$(java -jar ${CLIENTLIB} prepareTransaction ETH ${COINBASE} ${ADDRESS} 50 | tail -n 1)
echo ${TX}
SIGNEDTX=$(java -jar ${CLIENTLIB} approveTransaction ETH ${TX} ${COINBASE} | tail -n 1)
echo ${SIGNEDTX}
TXID=$(java -jar ${CLIENTLIB} broadcastTransaction ETH ${SIGNEDTX} | tail -n 1)
echo ${TXID}

waitBlock
waitBlock
waitBlock
waitBlock
waitBlock
waitBlock
waitBlock
waitBlock
waitBlock
waitBlock
waitBlock
waitBlock

echo ""
echo "Checking balances..."
ADD1BALANCE=$(java -jar ${CLIENTLIB} getBalance ETH ${ADDRESS} | tail -n 1)
ADD2BALANCE=$(java -jar ${CLIENTLIB} getBalance ETH ${ADDRESS2} | tail -n 1)
echo "${ADDRESS}: ${ADD1BALANCE}"
echo "${ADDRESS2}: ${ADD2BALANCE}"

echo ""
echo "Generate tx between addresses"
TX=$(java -jar ${CLIENTLIB} prepareTransaction ETH ${ADDRESS} ${ADDRESS2} 5 ${USERKEY} | tail -n 1)
echo ${TX}

echo ""
echo "Sign tx"
SIGNEDTX=$(java -jar ${CLIENTLIB} approveTransaction ETH ${TX} ${ADDRESS} | tail -n 1)
echo ${SIGNEDTX}

echo ""
echo "Submit tx"
TXID=$(java -jar ${CLIENTLIB} broadcastTransaction ETH ${SIGNEDTX} | tail -n 1)
echo ${TXID}

waitBlock
waitBlock
waitBlock
waitBlock
waitBlock
waitBlock
waitBlock
waitBlock
waitBlock
waitBlock
waitBlock
waitBlock

echo ""
echo "Checking balances..."
ADD1BALANCE=$(java -jar ${CLIENTLIB} getBalance ETH ${ADDRESS} | tail -n 1)
ADD2BALANCE=$(java -jar ${CLIENTLIB} getBalance ETH ${ADDRESS2} | tail -n 1)
echo "${ADDRESS}: ${ADD1BALANCE}"
echo "${ADDRESS2}: ${ADD2BALANCE}"

#========================FIAT===============================
echo ""
echo "Testing Fiat (EUR)"

CURLURL="http://${GETH_PORT_8101_TCP_ADDR}:${GETH_PORT_8101_TCP_PORT}"

waitBlock () {
  echo ""
  CURRBLOCK=$(curl -s -H 'Content-Type: application/json' -X POST -d '{"jsonrpc": "2.0", "id": "curl", "method": "eth_blockNumber", "params": []}'  ${CURLURL} | jq '.result' | sed 's/^"//g' | sed 's/"$//g' | sed 's/^0x//g')
  BLOCK=$(curl -s -H 'Content-Type: application/json' -X POST -d '{"jsonrpc": "2.0", "id": "curl", "method": "eth_blockNumber", "params": []}'  ${CURLURL} | jq '.result' | sed 's/^"//g' | sed 's/"$//g' | sed 's/^0x//g')
  while [ ${CURRBLOCK} == ${BLOCK} ]; do
    echo "Waiting for block generation..."
    sleep 5
    BLOCK=$(curl -s -H 'Content-Type: application/json' -X POST -d '{"jsonrpc": "2.0", "id": "curl", "method": "eth_blockNumber", "params": []}'  ${CURLURL} | jq '.result' | sed 's/^"//g' | sed 's/"$//g' | sed 's/^0x//g')
    echo "Starting block: " ${CURRBLOCK}
    echo "New block number: " ${BLOCK}
  done
  return 0
}

echo ""
echo "Getting coinbase"
COINBASE=$(curl -s -H 'Content-Type: application/json' -X POST -d '{"jsonrpc": "2.0", "id": "curl", "method": "eth_coinbase", "params": [] }' ${CURLURL} | jq '.result' | sed 's/^"//g' | sed 's/"$//g' | sed 's/^0x//g')
echo ${COINBASE}

CONTRACT_ACCOUNT=e8a78b476ae1403b7fd39b662545ae608aced7c7
echo ""
echo "Contract account should be: "${CONTRACT_ACCOUNT}

echo ""
echo "Waiting 10 blocks for coinbase to generate funds before funding..."
waitBlock
waitBlock
waitBlock
waitBlock
waitBlock
waitBlock
waitBlock
waitBlock
waitBlock
waitBlock

echo ""
echo "Funding contract account"
TX=$(java -jar ${CLIENTLIB} prepareTransaction ETH ${COINBASE} ${CONTRACT_ACCOUNT} 5 | tail -n 1)
echo ${TX}
SIGNEDTX=$(java -jar ${CLIENTLIB} approveTransaction ETH ${TX} ${COINBASE} | tail -n 1)
echo ${SIGNEDTX}
TXID=$(java -jar ${CLIENTLIB} broadcastTransaction ETH ${SIGNEDTX} | tail -n 1)
echo ${TXID}

echo ""
echo "Waiting 10 blocks for coinbase to generate funds before starting..."
waitBlock
waitBlock
waitBlock
waitBlock
waitBlock
waitBlock
waitBlock
waitBlock
waitBlock
waitBlock

echo ""
echo "Checking balances"
CBBALANCE=$(java -jar ${CLIENTLIB} getBalance ETH ${COINBASE} | tail -n 1)
CABALANCE=$(java -jar ${CLIENTLIB} getBalance ETH ${CONTRACT_ACCOUNT} | tail -n 1)
echo ${COINBASE}": "${CBBALANCE}
echo ${CONTRACT_ACCOUNT}": "${CABALANCE}

## Generate Tokens
ADMINLIB=/opt/emax/lib/cosigner-fiat-0.0.1-SNAPSHOT.jar
TOKENTX=$(java -jar ${ADMINLIB} generateTokens 10000 | tail -n 1)
echo "Generated transaction: "${TOKENTX}
SIGNEDTX=$(java -jar ${CLIENTLIB} approveTransaction EUR ${TOKENTX} ${COINBASE} | tail -n 1)
echo "Signed: "${SIGNEDTX}
TXID=$(java -jar ${CLIENTLIB} broadcastTransaction EUR ${SIGNEDTX} | tail -n 1)
echo "TxID: "${TXID}

waitBlock
waitBlock
waitBlock
waitBlock
waitBlock
waitBlock
waitBlock
waitBlock
waitBlock
waitBlock
waitBlock

echo ""
echo "Checking balances..."
ADD1BALANCE=$(java -jar ${CLIENTLIB} getBalance EUR ${COINBASE} | tail -n 1)
echo ${COINBASE}": "${ADD1BALANCE}

## Generate an address
echo ""
echo "Creating address"
FIATADDRESS=$(java -jar ${CLIENTLIB} getNewAddress EUR ${USERKEY} | tail -n 1)
echo ${FIATADDRESS}

echo ""
echo "Checking balances..."
ADD1BALANCE=$(java -jar ${CLIENTLIB} getBalance EUR ${COINBASE} | tail -n 1)
ADD2BALANCE=$(java -jar ${CLIENTLIB} getBalance EUR ${FIATADDRESS} | tail -n 1)
echo ${COINBASE}": "${ADD1BALANCE}
echo ${FIATADDRESS}": "${ADD2BALANCE}

## Transfer it from Admin to Address
echo ""
echo "Transfering 100.00 EUR to "${FIATADDRESS}
FIATTX=$(java -jar ${CLIENTLIB} prepareTransaction EUR ${COINBASE} ${FIATADDRESS} 10000 | tail -n 1)
echo ${FIATTX}
SIGNEDTX=$(java -jar ${CLIENTLIB} approveTransaction EUR ${FIATTX} ${COINBASE} | tail -n 1)
echo ${SIGNEDTX}
TXID=$(java -jar ${CLIENTLIB} broadcastTransaction EUR ${SIGNEDTX} | tail -n 1)
echo ${TXID}

waitBlock
waitBlock
waitBlock
waitBlock
waitBlock
waitBlock
waitBlock
waitBlock
waitBlock
waitBlock
waitBlock

echo ""
echo "Checking balances..."
ADD1BALANCE=$(java -jar ${CLIENTLIB} getBalance EUR ${COINBASE} | tail -n 1)
ADD2BALANCE=$(java -jar ${CLIENTLIB} getBalance EUR ${FIATADDRESS} | tail -n 1)
echo ${COINBASE}": "${ADD1BALANCE}
echo ${FIATADDRESS}": "${ADD2BALANCE}

## Transfer it back
echo ""
echo "Transfering 100.00 EUR to "${COINBASE}
FIATTX=$(java -jar ${CLIENTLIB} prepareTransaction EUR ${FIATADDRESS} ${COINBASE} 10000 ${USERKEY} | tail -n 1)
echo ${FIATTX}
SIGNEDTX=$(java -jar ${CLIENTLIB} approveTransaction EUR ${FIATTX} ${FIATADDRESS} | tail -n 1)
echo ${SIGNEDTX}
TXID=$(java -jar ${CLIENTLIB} broadcastTransaction EUR ${SIGNEDTX} | tail -n 1)
echo ${TXID}

waitBlock
waitBlock
waitBlock
waitBlock
waitBlock
waitBlock
waitBlock
waitBlock
waitBlock
waitBlock
waitBlock

echo ""
echo "Checking balances..."
ADD1BALANCE=$(java -jar ${CLIENTLIB} getBalance EUR ${COINBASE} | tail -n 1)
ADD2BALANCE=$(java -jar ${CLIENTLIB} getBalance EUR ${FIATADDRESS} | tail -n 1)
echo ${COINBASE}": "${ADD1BALANCE}
echo ${FIATADDRESS}": "${ADD2BALANCE}

## Destroy the tokens
TOKENTX=$(java -jar ${ADMINLIB} destroyTokens 10000 | tail -n 1)
echo "Generated transaction: "${TOKENTX}
SIGNEDTX=$(java -jar ${CLIENTLIB} approveTransaction EUR ${TOKENTX} ${COINBASE} | tail -n 1)
echo "Signed: "${SIGNEDTX}
TXID=$(java -jar ${CLIENTLIB} broadcastTransaction EUR ${SIGNEDTX} | tail -n 1)
echo "TxID: "${TXID}

waitBlock
waitBlock
waitBlock
waitBlock
waitBlock
waitBlock
waitBlock
waitBlock
waitBlock
waitBlock
waitBlock

echo ""
echo "Checking balances..."
ADD1BALANCE=$(java -jar ${CLIENTLIB} getBalance EUR ${COINBASE} | tail -n 1)
echo ${COINBASE}": "${ADD1BALANCE}
