#!/usr/bin/expect -f

spawn /opt/emax/bin/geth --testnet console
expect "Do you accept this agreement? \\\[y/N\\\]"
send "Y\r"
expect ">"
exit

