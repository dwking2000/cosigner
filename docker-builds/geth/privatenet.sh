#!/bin/bash

geth --genesis ../geth/private_genesis.json --password <(echo -n cosigner) --unlock 0 --rpc --rpcport 8101 --rpcaddr 0.0.0.0

