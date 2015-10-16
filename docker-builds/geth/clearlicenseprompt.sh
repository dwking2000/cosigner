#!/usr/bin/expect -f

spawn geth --genesis ../geth/private_genesis.json console
expect "Do you accept this agreement? \\\[y/N\\\]"
send "Y\r"
expect ">"
exit

