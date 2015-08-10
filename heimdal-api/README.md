# heimdal-api

### Ownership & License

## Overview

Interfaces and utilities that a crypto-currency library needs to implement.

## Design Goals

### For internal review, should be removed when an RFC is created or this is released publicly. 

#### Naming of components is open to suggestion. I'm aware that the initial ones are heavily Linux/C++ based. 

### General functionality
The Heimdal main application will provide authenticated RPC access to each api-based plugin it's been loaded with. This may be ZMQ-based, but there's no reason it couldn't support various interfaces. This is a problem for another day. The main application will provide a list of currency plugins it has loaded and will interact with the chosen plugin using the interfaces defined here in api.

For example. Say a third party application (user) requests wallet information for a bitcoin account. Heimdal asks its bitcoin plugin, which implements api interfaces, for that information. Heimdal-bitcoin knows how to talk to the bitcoin network and how to perform transactions on it. The user can turn around and ask similar information about ethereum and get the results back in the same format despite it being a different network/currency.

A nice result of this that we can re-use the existing bitcoin and ethereum work by breaking out the currency specific logic from their respective projects and implementing them as a library here. This will also allow us to get one currency functional, get the RPC and authentication portion integrated, and then add other currencies quickly. The bitcoin logic is pretty much done for us and so shouldn't take much time to port over to a library.

### Multi-sig and validation
Each api plugin can obviously only sign with the keys it has available to it. This means a 2nd copy of the application can be running which is never told about the keys from the other. If this 2nd application's keys are properly included in a mult-sig address, transactions would have to be sent to it for signing, allowing for separation of keys. In addition, the 2nd application can be independently queried to verify balances, ensuring the network is in working order before signing.

Extra cold-storage keys can be provided in the multi-sig addresses for disaster recovery.

### Recovery
Recovery should only consist of reloading the currency's network daemon (bitcoind and geth for example) with their private keys.  If a library implements deterministic addresses properly we should be able to recover everything by requesting specific account names through the RPC interfaces. 

For example, Heimdal's server has exploded. Once everyone has stopped roasting s'mores, the server is replaced and we reload the private keys and start everything up. The business logic application starts requesting balances for user accounts. Heimdal's api plugins do something like use the user account string along with the private keys as a seed to regenerate all the private keys associated to that user. 

There's also no reason the currency's wallets couldn't be backed up occasionally as an added failsafe.   

### Auditing
This has been mentioned in discussions to I'll add it here. Because each piece of this can be loaded separately, we can have experts in the currency of choice audit the library. As well as experts in cryptology or any other relevant field. We can then provide source code & binary checksums, etc... 

On top of code audits on the libraries, we can have the main Heimdal application log every call to every api plugin as well as the results. Nothing in this design results in private data being passed around, only public keys and balances. So the logs wouldn't be a direct security risk, and it would allow us to replay everything that's happened in case of an audit or discrepancy.