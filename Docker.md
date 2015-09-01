# Docker

## Overview

Instructions for running heimdal in docker with regtest/private networks.

## Building Images
### Bitcoin Image
Go into the docker-builds/bitcoind directory.<br/>
Run 
```bash
docker build -t heimdal-bitcoin ./
```

### Geth Image
Go into the docker-builds/geth directory.<br/>
Run 
```bash
docker build -t heimdal-geth ./
```

### Heimdal Image
Edit the target/heimdal-<currency>.properties files and switch the commented lines for daemonConnectionString to use the docker environment variables before building this docker image.
From this directory run
```bash
docker build -t heimdal ./
```

## Running the Images
### Bitcoin
```bash
docker run --name bitcoind -t -d heimdal-bitcoin
```

### Geth
```bash
docker run --name geth -t -d heimdal-geth
```

### Heimdal
```bash
docker run -t -d -p 127.0.0.1:8080:8080 --name heimdal --link geth:geth --link bitcoind:bitcoind heimdal
```

### Alternate process
Expose the ports to the geth and bitcoind images when running them and run heimdal locally when developing, etc... 

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

## Teardown
```bash
docker kill heimdal
docker kill geth
docker kill bitcoind
```
