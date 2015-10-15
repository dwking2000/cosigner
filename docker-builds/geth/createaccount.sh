#!/usr/bin/expect -f

spawn /opt/emax/bin/geth --testnet --rpc --rpcport 8101 --rpcaddr 0.0.0.0 console
expect ">"
send "personal.newAccount('heimdal')\r"
expect ">"
exit
