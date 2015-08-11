# heimdal-ethereum

### Ownership & License

## Overview

An ethereum implementation for heimdal-api  

## Current State

Unstable
- Only stubbed out right now

## Testing recommendations

You'll need an ethereum network. Create a local/private copy for testing with no peers. Delete the data directory to reset the network.

For now, create the following genesis block file as private_genesis.json and load it:
```json
{
    "nonce": "0xdeadbeefdeadbeef",
    "timestamp": "0x0", 
    "parentHash": "0x0000000000000000000000000000000000000000000000000000000000000000", 
    "extraData": "0x686f727365", 
    "gasLimit": "0x8000000", 
    "difficulty": "0x400", 
    "mixhash": "0x0000000000000000000000000000000000000000000000000000000000000000", 
    "coinbase": "0x3333333333333333333333333333333333333333",
    "alloc": {
   }
}
```

```bash
geth --datadir="D:\ETHTest" --verbosity "6" --maxpeers "0" --port 30301 --rpc --rpcport 8101 --genesis ./private_genesis.json console
```

Coin creation is done through mining. In another window create a base account
```bash
geth --datadir="D:\ETHTest" --port 30301 --rpcport 8101 account new
```

If you want a second console to make it easier to see data use:
```bash
geth --datadir="D:\ETHTest" --maxpeers "0" --port 30301 --rpcport 8101 attach
```

If you get errors when you try to mine about no coinbase, set the --etherbase on your geth console command and pass it the public address where your mining proceeds should go. This should just work though if you use the command above to create a new account. 

In order to start mining in the console run: 
```js
miner.start(#threads)
```
And to stop:
```bash
miner.stop()
```

Recommend that you use at most [# cpu's - 1] to avoid a slow/non-responsive console. And for a private network 1 is probably sufficient, difficulty is not high.

Once that's working, run the JAR directly and it will provide you with command line access to the interface. 
```bash
java -jar heimdal-ethereum-0.0.1-SNAPSHOT.jar getNewAddress <random hex string without leading 0x>
```

Check the account balance (before and after your transaction above)
```bash
java -jar heimdal-ethereum-0.0.1-SNAPSHOT.jar getBalance <address>
```

### Multi-sig test

TODO

### Notes
