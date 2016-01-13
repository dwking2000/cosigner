#!/bin/bash

CURLURL="http://${GETH_PORT_8101_TCP_ADDR}:${GETH_PORT_8101_TCP_PORT}"
export GETH_CONTRACT_ACCOUNT=$(curl -s -H 'Content-Type: application/json' -X POST -d '{"jsonrpc": "2.0", "id": "curl", "method": "eth_coinbase", "params": [] }' ${CURLURL} | jq '.result' | sed 's/^"//g' | sed 's/"$//g' | sed 's/^0x//g')

set

java -server -jar cosigner-core-0.0.1-SNAPSHOT.jar server core.yml

