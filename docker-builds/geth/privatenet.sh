#!/bin/bash

geth --dev --port 30303 --password <(echo -n cosigner) --unlock 0 --rpc --rpcport 8101 --rpcaddr 0.0.0.0 ${GETHOPTS}

