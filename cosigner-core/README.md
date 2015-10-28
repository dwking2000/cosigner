# cosigner-core

## Overview

cosigner's core coordinator and external facing RPC server.  

## Current State
Unstable 
- Authorization is not implemented yet.
- Validation is not implemented yet.

## Building

### Requirements

- Java 1.8
- Maven 3

### Compiling

`mvn install` should work.

### Vagrant

There is a vagrant file in the root project path intended to provide a build environment that's known to work. To make use of it you will need vagrant installed. To build using this method run the following:
- `vagrant up`
- `vagrant ssh`
- `cd /vagrant`
- `mvn install`
- `exit`
- `vagrant destroy`

The result will be a compiled and packaged cosigner suite in the ./target directory.

## Using cosigner-core

For instructions on getting the server running see [Usage.md](https://github.com/EMAXio/cosigner/blob/master/cosigner-core/Usage.md)

cosigner provides REST endpoints under the /rs/ path, and web socket endpoints under the /ws/ path.

All endpoints expect the same generic parameter object for input. It has the following definition, only the parameters being used by the function being called need to be filled in:
```json
[{"currencySymbol": "BTC",
  "userKey": "<random hex string>",
  "account":["<addresses for given currency>"],
  "callback": "",
  "receivingAccount": [{"recipientAddress": "", "amount": ""}],
  "transactionData": ""}]
```
