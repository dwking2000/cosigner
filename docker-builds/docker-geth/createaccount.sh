#!/bin/bash

geth --datadir=/opt/emax/ETHTest --verbosity 6 --maxpeers 0 --port 30301 --rpc --rpcport 8101 --genesis ../geth/private_genesis.json console <<EOF
Y
personal.newAccount('')

EOF
