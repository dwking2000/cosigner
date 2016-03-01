##Overview
Cosigner introduces a best practice implementation for storing, sending and receiving digital financial instruments with the use of blockchain technology as a financial intermediary.

Cosigner is a multi-signature blockchain agnostic gateway, compatible with multiple different blockchains and implementations. Instead of having to implement settlement logic in your application logic, Cosigner maps the Cosigner API with blockchain node RPC's. This abstracts the differences and complexities of each blockchain implementation and makes you application logic cleaner by eliminating edge cases.

##Cosigner Client
There are currently two clients:

cosigner-client-java

cosigner-client-js

These handle the TLS connection and provide object to RPC call conversion methods to make it simple to communicate with Cosigner. Cosigner communicates over TLS with REST and WebSocket endpoints, making it relatively easy to implement a custom client if desired.

##Account structure
[Userkey + Serverkey] => User’s Seed => Generates private keys used for addresses (UserKey Address, shouldn’t hold any balance)

[UserKey Address + Cosigner Node Addresses] => Multi-sig contract/address => Used for transactions

##Signing
Cosigner can sign using ECC secp256k1, create and pass raw transactions to other Cosigner nodes for additional signing, and broadcast signed transactions.

###Multi-Signature
The default setup is for Cosigner to hold 2 out of 4 keys in a 3-of-4 multi-signature address. The other 2 keys are 1) controlled by the user via the userkey, and 2) held offline for emergencies.

###Offline Signing
Cosigner provides APIs to retrieve enough data from a transaction that a client can sign with no direct network/Cosigner connection. This allows for a private key that never leaves the client’s control, while still using Cosigner’s features.

## Transaction monitoring
Cosigner monitors transactions to addresses in its account structure.

### Confirmations

Cosigner always reports the lower balance of either the most recent block, or the balance that has been confirmed. It also provides a WebSocket endpoint that will push updates to clients for the addresses they request, this will alert them to any new transactions as they show up as well as providing periodic balance updates. 

### Multiple Cosigner nodes

Cosigner is designed to be run as a cluster of nodes, this is done to reduce single points of failure in the system, each Cosigner node has its own set of full blockchain nodes, this gives a larger probability of discovering if one of one of your blockchain nodes are on a fork, or if a transaction is being double spended. Each node reconfirms transaction data, balance, etc… before signing. Signing keys can also be spread out over these nodes , so that each one needs to agree before a transaction can be submitted.

### Multiple blockchain's & implementations

Ethereum (Go - Implemented, C++/Parity - coming)

Bitcoin (bitcoind - Implemented, bitcoinj - coming)

## Security
We will eventually release a library for storing keys using hardware security modules.

### Key management

Partially covered above, each Cosigner node should hold only 1 key which is not retrievable through any API. The final key is provided by the ‘userkey’ which is a random number that Cosigner has no persistent knowledge of. It cannot submit transactions on behalf of a user without that information. To gain access to enough keys to submit a transaction someone would have to compromise M-1 cosigner nodes with the appropriate keys, and get the random number which is on the same scale as the keys, which is not stored in Cosigner. 

## Countersigner
Info to come on the light implementation of Cosigner.


## WARNING

Cosigner is in early stages of active development. Until we define RFCs for
Cosigner, the API can and will likely change.

## Code Style

Cosigner will be following Google's
[Java style guide](http://google.github.io/styleguide/javaguide.html).

## Usage

See [cosigner-core](https://github.com/EMAXio/cosigner/blob/master/cosigner-core/README.md) for instructions on building and using cosigner.

or [Braveno Docs](http://docs.braveno.com/#cosigner-core-api) for the API Documentation.

## Ownership and License

Copyright information and contributors are listed in AUTHORS. This project uses
the MPL v2 license, see LICENSE.

Cosigner uses the
[C4.1 (Collective Code Construction Contract)](http://rfc.zeromq.org/spec:22)
process for contributions.

To report an issue, use the
[Cosigner issue tracker](https://github.com/Braveno/cosigner/issues) at
github.com.

