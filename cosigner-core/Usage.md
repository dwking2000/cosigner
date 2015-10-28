# General Usage

## Configuration

Default configuration files are generated during a build. 

### core.yml

This configuration file is for the REST server. Which ports it should use, logging configuration, configurable features. 

### cosigner-<currency>.properties

The currency libraries should each have a properties file. These should allow you to configure network access, addresses needed for things like multi-sig, and so on. 

## Running locally

If running currency nodes already, cosigner can make user of them directly. Edit the configuration files as appropriate and run cosigner with:
```bash
java -jar cosigner-core-0.0.1-SNAPSHOT.jar server core.yml
```

## Running with docker-compose

To run cosigner on a private testnet, there is a docker-compose file in the root of the project. 

Build and start the images:
- `docker-compose pull`
- `docker-compose build`
- `docker-compose up -d`

This will get a cosigner server running locally on port 8080. However to make use of the nodes it's necessary to get the addresses that were generated when the images were built and insert them into the docker-compose YML. It's also necessary to generate blocks to move the networks ahead.

List and access the images command line:
- `docker ps`
- `docker exec -it cosigner_<currencynode>_1 /bin/bash`
- Run the currency node's command line tools to list addresses or generate blocks as necessary.

### Geth

There's a gethattach.sh script in the login directory that will attach to the node. To get the account that will generate funds run `eth.coinbase`. To generate blocks run `miner.start()` and to stop generating blocks run `miner.stop()`. Generating blocks is CPU intense, it's not recommended to leave it running indefinitely. 

### Bitcoind

The bitcoind image is running in regtest mode. To get the account that will be getting funds as blocks are generated run `bitcoin-cli -regtest getaddressesbyaccount ""`. To generate blocks run `bitcoin-cli -regtest generate <number of blocks>`
