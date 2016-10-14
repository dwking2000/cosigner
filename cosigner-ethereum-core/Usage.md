# General usage

## Network requirements

Cosigner-ethereum requires a geth node to access the ethereum network. In order to create multi-sig contracts it also needs an unlocked address with enough funds to submit the creation transaction. 

When configuring the geth node, make sure the RPC server is available. Since an address needs to be unlocked for singing and contract creation, make sure the port is only accessible by the cosigner server.

## Running the library directly

Once that's working, run the JAR directly and it will provide you with command line access to the interface. The CLI funcionality is a little limited in terms of inputs, use the library as an import to get the most out of it.

To see the usage use:
```bash
java -jar cosigner-ethereum-0.0.1-SNAPSHOT.jar
```

### Account basics

Creating a new regular address:
```bash
java -jar cosigner-ethereum-0.0.1-SNAPSHOT.jar getNewAddress <random hex string without leading 0x>
```

Check the account balance
```bash
java -jar cosigner-ethereum-0.0.1-SNAPSHOT.jar getBalance <address>
```

### Transaction basics

Creating a transaction:
```bash
java -jar cosigner-ethereum-0.0.1-SNAPSHOT.jar createTransaction <from> <to> <amount in Ether>
```

Sign it as appropriate. If the key is stored in geth, don't pass in the hex string for the account. If it's a cosigner account, enter the account string and cosigner will figure out which key to use.
```bash
java -jar cosigner-ethereum-0.0.1-SNAPSHOT.jar signTransaction <tx string> <signer> <optional account hex>
```

And finally
```bash
java -jar cosigner-ethereum-0.0.1-SNAPSHOT.jar sendTransaction <signed tx string>
```

### Multi-sig basics

Cosigner's multi-sig contract works much like bitcoin's multi-sig. Everyone signs the transaction before it's sent. The biggest difference is that the last address to sign the transaction is the one who pays gas fees.

When creating the multi-sig address, the configuration has a "contract" address. This is the address that will pay for the contract creation. Make sure it's unlocked and funded so cosigner can create it. 

Because the multi-sig account is actually a contract, it needs to be processed and confirmed by the network. This means it can take some time before the address can withdraw funds. 

Create a transaction from the multisig account and sign it using the multi-sig address and user key. Cosigner will figure out which component addresses need to sign it. Sign it using the multi-sig address and no user key to sign it with any unlocked wallet/geth keys that cosigner knows about.
