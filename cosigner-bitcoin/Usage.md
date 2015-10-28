# General usage

## Network requirements

Cosigner-bitcoin requires a bitcoind node to access the bitoin network. 

When configuring the bitcoind node, make sure the RPC server is available. Since an address needs to be unlocked for singing, make sure the port is only accessible by the cosigner server.

## Running the library directly

Once that's working, run the JAR directly and it will provide you with command line access to the interface. The CLI funcionality is a little limited in terms of inputs, use the library as an import to get the most out of it.

To see the usage use:
```bash
java -jar cosigner-bitcoin-0.0.1-SNAPSHOT.jar
```

### Account basics

Creating a new regular address:
```bash
java -jar cosigner-bitcoin-0.0.1-SNAPSHOT.jar getNewAddress <random hex string without leading 0x>
```

Check the account balance
```bash
java -jar cosigner-bitcoin-0.0.1-SNAPSHOT.jar getBalance <address>
```

### Transaction basics

Creating a transaction:
```bash
java -jar cosigner-bitcoin-0.0.1-SNAPSHOT.jar createTransaction <from> <to> <amount in BTC>
```

Sign it as appropriate. If the key is stored in bitcoind, don't pass in the hex string for the account. If it's a cosigner account, enter the account string and cosigner will figure out which key to use.
```bash
java -jar cosigner-bitcoin-0.0.1-SNAPSHOT.jar signTransaction <tx string> <signer> <optional account hex>
```

And finally
```bash
java -jar cosigner-bitcoin-0.0.1-SNAPSHOT.jar sendTransaction <signed tx string>
```
