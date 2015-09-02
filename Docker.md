# Docker

## Overview

Instructions for running heimdal in docker with regtest/private networks.

## Building and running
```bash
docker-compose up
```

## Funding accounts
Connect to the relevant docker image using exec bash, and find the wallet's coinbase account where the mining proceeds are going. Once unlocked you should be able to send funds from these by using the heimdal interface.

### Geth
```bash
docker exec -it geth bash
geth attach
personal.unlock(eth.coinbase, '')
eth.coinbase // Make note of what this value is
Ctrl+D until you're back out of docker
```

### Bitcoin
```bash
docker exec -it bitcoind bash
bitcoin-cli -regtest walletinfo // Should show you one account with a balance, make note of this address
Ctrl+D until you're back out of docker
```

