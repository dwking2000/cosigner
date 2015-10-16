#!/bin/bash

geth --genesis ../geth/private_genesis.json --password <(echo -n heimdal) --unlock primary --rpc --rpcport 8101 --rpcaddr 0.0.0.0

