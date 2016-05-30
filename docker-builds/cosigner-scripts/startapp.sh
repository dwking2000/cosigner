#!/bin/bash

CURLURL="http://${GETH_PORT_8101_TCP_ADDR}:${GETH_PORT_8101_TCP_PORT}"

waitBlock () {
  echo ""
  CURRBLOCK=$(curl -s -H 'Content-Type: application/json' -X POST -d '{"jsonrpc": "2.0", "id": "curl", "method": "eth_blockNumber", "params": []}'  ${CURLURL} | jq '.result' | sed 's/^"//g' | sed 's/"$//g' | sed 's/^0x//g')
  BLOCK=$(curl -s -H 'Content-Type: application/json' -X POST -d '{"jsonrpc": "2.0", "id": "curl", "method": "eth_blockNumber", "params": []}'  ${CURLURL} | jq '.result' | sed 's/^"//g' | sed 's/"$//g' | sed 's/^0x//g')
  while [ ${CURRBLOCK} == ${BLOCK} ]; do
    echo "Waiting for block generation..."
    echo "Starting block: " ${CURRBLOCK}
    sleep 5
    BLOCK=$(curl -s -H 'Content-Type: application/json' -X POST -d '{"jsonrpc": "2.0", "id": "curl", "method": "eth_blockNumber", "params": []}'  ${CURLURL} | jq '.result' | sed 's/^"//g' | sed 's/"$//g' | sed 's/^0x//g')
    echo "Starting block: " ${CURRBLOCK}
    echo "New block number: " ${BLOCK}
  done
  return 0
}

waitBlock;
waitBlock;
waitBlock;

export FIAT_ADMIN_ACCOUNT=$(curl -s -H 'Content-Type: application/json' -X POST -d '{"jsonrpc": "2.0", "id": "curl", "method": "eth_coinbase", "params": [] }' ${CURLURL} | jq '.result' | sed 's/^"//g' | sed 's/"$//g' | sed 's/^0x//g')
#export GETH_CONTRACT_ACCOUNT=$(curl -s -H 'Content-Type: application/json' -X POST -d '{"jsonrpc": "2.0", "id": "curl", "method": "eth_coinbase", "params": [] }' ${CURLURL} | jq '.result' | sed 's/^"//g' | sed 's/"$//g' | sed 's/^0x//g')
export GETH_CONTRACT_KEY=deadbeef

java -server -jar cosigner-core-0.0.1-SNAPSHOT.jar server core.yml

