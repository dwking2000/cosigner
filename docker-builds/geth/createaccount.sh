#!/usr/bin/expect -f

spawn geth --port 30301 --rpc --rpcport 8101 --rpcaddr 0.0.0.0 console
expect ">"
send "personal.newAccount('')\r"
expect ">"
exit
