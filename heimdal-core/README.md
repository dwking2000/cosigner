# heimdal-core

### Ownership & License

## Overview

Heimdal's core coordinator and external facing RPC server.  

## Current State
- Unstable 
- Needs user-facing endpoints
  - [Done] GetCurrencies() 
  - [Done] GetNewAccount(Currency, User)
  - [Done] ListAllAccounts(Currency, User)
  - [Done] GetBalance(Currency, User, [Account]) 
  - [Done] MonitorBalance(Currency, [Accounts], Callback)
    - Needs work on disconnects/exceptions
    - Needs to filter or needs to be broken into instanced copies     
  - [Done] PrepareTransaction(Currency, FromUser, FromAddress, To, Amount)   
    - Needs validation
    - Heimdal servers need to communicate with each other    
  - [Done] ApproveTransaction(Currency, TransactionID)      
  - [Done] SubmitTransaction(TransactionID)
- Authorization and logging is not implemented yet.

*TransactionID may be the raw transaction. TransactionIDs may expire. 

## Testing hints

Run the jar with
```bash
java -jar heimdal-core-0.0.1-SNAPSHOT.jar server core.yml
```

Core.yml should look like the following:
```yml
server:
  applicationConnectors:
    - type: http
      port: 8080      
  adminConnectors:
    - type: http
      port: 8081

clusterLocation: "localhost"
clusterPort: 5555
clusterRPCPort: 8080
```

If running multiple instances on the same server make sure to update the ports to avoid conflicts. When running in this setup, your first node will know about new ones automatically but new ones will not be able to listen on the same port. Use /admin/AddNode to add them manually.  

/admin/AddNode & /admin/ListNodes are administrative endpoints, will be used to add a heimdal node to the cluster. More detail to follow.

/ws/\<Function\> will access the WebSocket version of the RPC <br/>
/rs/\<Function\> will access the REST version of the RPC <br/>
Both versions expect the same input as a JSON object and return JSON. WebSockets won't have callbacks, will simply send the data later.

Generic parameter object has the following definition, you only need to provide the parameters being used by the function being called:
```json
[{"currencySymbol": "BTC",
  "userKey": "<random hex string>",
  "account":["<addresses for given currency>"],
  "callback": "",
  "receivingAccount": "<address for given currency>",
  "amount": "10.0",
  "transactionData": ""}]
```
