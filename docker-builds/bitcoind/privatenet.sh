bitcoind -regtest -server -rpcbind=0.0.0.0 -rpcallowip=172.17.0.0/16 -daemon
sleep 10
bitcoin-cli -regtest generate 101
while true;  do
	bitcoin-cli -regtest generate 1
	sleep 30
done	
