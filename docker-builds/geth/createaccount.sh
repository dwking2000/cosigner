#!/bin/bash

geth --genesis ../geth/private_genesis.json --password <(echo -n heimdal) account new

