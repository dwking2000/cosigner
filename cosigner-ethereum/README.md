# cosigner-ethereum

## Overview

An ethereum implementation for cosigner-api  

## Current State

Mostly-stable. 

# Warning #
cosigner-ethereum tracks addresses based on the contract creation account and the specific contract it uses. Until the contract is finalized, any software updates may break cosigner's ability to access accounts from previous versions. So you should only be using this on a test network. We will attempt to address the changing contracts issue once the library is stable.

## Building

### Requirements

- Java 1.8
- Maven 3

### Compiling

`mvn install` should work.

## Usage

See [Usage.md](https://github.com/EMAXio/cosigner/blob/master/cosigner-ethereum/Usage.md) for usage information
