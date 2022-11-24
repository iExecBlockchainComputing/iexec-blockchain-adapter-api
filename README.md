# iexec-blockchain-adapter-api

## Overview

A blockchain adapter API for iExec Smart-Contracts and other Ethereum things.

The iExec Blockchain Adapter API is available as an OCI image on [dockerhub](https://hub.docker.com/r/iexechub/iexec-blockchain-adapter-api/tags).

To run properly, the iExec Blockchain Adapter API requires:
* A reachable blockchain node URL hosting iExec smart contracts.
* A reachable MongoDB instance to persist its data.
* A reachable broker to match iExec orders.
* An Ethereum wallet to interact with Smart Contracts on the blockchain network.

## Configuration

| Environment variables | Description | Values | Default value |
| --- | --- | --- | --- |
| IEXEC_BLOCKCHAIN_ADAPTER_API_PORT | Server HTTP port of the blockchain adapter API. | Positive integer | `13010` |
| IEXEC_BLOCKCHAIN_ADAPTER_API_USERNAME | Login username of the server. | String | `admin` |
| IEXEC_BLOCKCHAIN_ADAPTER_API_PASSWORD | Login password of the server. | String | `whatever` |
| IEXEC_BLOCKCHAIN_ADAPTER_API_MONGO_HOST | Mongo server host. Cannot be set with URI. | String | `localhost` |
| IEXEC_BLOCKCHAIN_ADAPTER_API_MONGO_PORT | Mongo server port. Cannot be set with URI. | Positive integer | `13012` |
| IEXEC_BLOCKCHAIN_ADAPTER_API_CHAIN_ID | Chain ID of the blockchain network to connect. | Positive integer | `65535` |
| IEXEC_BLOCKCHAIN_ADAPTER_API_NODE_ADDRESS | URL to connect to the blockchain network. | URL | `http://localhost:8545` |
| IEXEC_BLOCKCHAIN_ADAPTER_API_BLOCK_TIME | In seconds, parameter of the targeted blockchain. | Positive integer | `1` |
| IEXEC_BLOCKCHAIN_ADAPTER_API_HUB_ADDRESS | Proxy contract address to interact with the iExec on-chain protocol. | Ethereum Address | `0xBF6B2B07e47326B7c8bfCb4A5460bef9f0Fd2002` |
| IEXEC_BLOCKCHAIN_ADAPTER_API_IS_SIDECHAIN | Define if iExec on-chain protocol is built on top of token (`false`) or native currency (`true`). | Boolean | `false` |
| IEXEC_BLOCKCHAIN_ADAPTER_API_GAS_PRICE_MULTIPLIER | Transactions will be sent with `networkGasPrice * gasPriceMultiplier`. | Float | `1.0` |
| IEXEC_BLOCKCHAIN_ADAPTER_API_GAS_PRICE_CAP | In Wei, will be used for transactions if `networkGasPrice * gasPriceMultiplier > gasPriceCap`. | Positive integer | `22000000000` |
| IEXEC_BLOCKCHAIN_ADAPTER_API_BROKER_URL | URL of the broker to interact with when matching iExec orders. | URL | `http://localhost:3000` |
| IEXEC_BLOCKCHAIN_ADAPTER_API_WALLET_PATH | Path to the wallet of the server. | String | `src/main/resources/wallet.json` |
| IEXEC_BLOCKCHAIN_ADAPTER_API_WALLET_PASSWORD | Password to unlock the wallet of the server. | String | `whatever` |

## Running in development mode

`./gradlew docker`

# CI/CD build

`docker image build -f docker/Dockerfile .`
