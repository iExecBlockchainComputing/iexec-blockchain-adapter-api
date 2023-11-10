# iExec Blockchain Adapter API

## Overview

The Blockchain Adapter API enables interacting with iExec smart contracts plus doing other Ethereum things.
The Blockchain Adapter API accepts incoming requests asking for submitting transactions to iExec smart contracts.
Incoming requests are locally stored in a database.
Transactions related to these requests are being asynchronously sent to a blockchain node.
At any time, the caller can retrieve the processing status for his request.

The iExec Blockchain Adapter API is available as an OCI image
on [Docker Hub](https://hub.docker.com/r/iexechub/iexec-blockchain-adapter-api/tags).

To run properly, the iExec Blockchain Adapter API requires:

* A blockchain node. iExec smart contracts must be deployed on the blockchain network.
* A MongoDB instance to persist its data.
* An Ethereum wallet to interact with smart contracts on the blockchain network.

## Configuration

| Environment variable                                    | Description                                                                                            | Type             | Default value                                |
|---------------------------------------------------------|--------------------------------------------------------------------------------------------------------|------------------|----------------------------------------------|
| `IEXEC_BLOCKCHAIN_ADAPTER_API_PORT`                     | Server HTTP port of the Blockchain Adapter API.                                                        | Positive integer | `13010`                                      |
| `IEXEC_BLOCKCHAIN_ADAPTER_API_USERNAME`                 | Login username of the server.                                                                          | String           | `admin`                                      |
| `IEXEC_BLOCKCHAIN_ADAPTER_API_PASSWORD`                 | Login password of the server.                                                                          | String           | `whatever`                                   |
| `IEXEC_BLOCKCHAIN_ADAPTER_API_MONGO_HOST`               | Mongo server host. Cannot be set with URI.                                                             | String           | `localhost`                                  |
| `IEXEC_BLOCKCHAIN_ADAPTER_API_MONGO_PORT`               | Mongo server port. Cannot be set with URI.                                                             | Positive integer | `13012`                                      |
| `IEXEC_BLOCKCHAIN_ADAPTER_API_CHAIN_ID`                 | Chain ID of the blockchain network to connect.                                                         | Positive integer | `134`                                        |
| `IEXEC_BLOCKCHAIN_ADAPTER_API_IS_SIDECHAIN`             | Define whether iExec on-chain protocol is built on top of token (`false`) or native currency (`true`). | Boolean          | `true`                                       |
| `IEXEC_BLOCKCHAIN_ADAPTER_API_NODE_ADDRESS`             | URL to connect to the blockchain network.                                                              | URL              | `https://bellecour.iex.ec`                   |
| `IEXEC_BLOCKCHAIN_ADAPTER_API_BLOCK_TIME`               | Duration between consecutive blocks on the blockchain network, in seconds.                             | Positive integer | `5`                                          |
| `IEXEC_BLOCKCHAIN_ADAPTER_API_HUB_ADDRESS`              | Proxy contract address to interact with the iExec on-chain protocol.                                   | Ethereum Address | `0x3eca1B216A7DF1C7689aEb259fFB83ADFB894E7f` |
| `IEXEC_BLOCKCHAIN_ADAPTER_API_GAS_PRICE_MULTIPLIER`     | Transactions will be sent with `networkGasPrice * gasPriceMultiplier`.                                 | Float            | `1.0`                                        |
| `IEXEC_BLOCKCHAIN_ADAPTER_API_GAS_PRICE_CAP`            | In Wei, will be used for transactions if `networkGasPrice * gasPriceMultiplier > gasPriceCap`.         | Positive integer | `22000000000`                                |
| `IEXEC_BLOCKCHAIN_ADAPTER_API_MAX_ALLOWED_TX_PER_BLOCK` | Max number of transactions per block, it can either be `1` or `2`.                                     | Positive integer | `1`                                          |
| `IEXEC_BLOCKCHAIN_ADAPTER_API_WALLET_PATH`              | Path to the wallet of the server.                                                                      | String           | `src/main/resources/wallet.json`             |
| `IEXEC_BLOCKCHAIN_ADAPTER_API_WALLET_PASSWORD`          | Password to unlock the wallet of the server.                                                           | String           | `whatever`                                   |

## Health checks

A health endpoint (`/actuator/health`) is enabled by default and can be accessed on the *
*IEXEC_BLOCKCHAIN_ADAPTER_API_PORT**.
This endpoint allows to define health checks in an orchestrator or
a [compose file](https://github.com/compose-spec/compose-spec/blob/master/spec.md#healthcheck).
No default strategy has been implemented in the [Dockerfile](Dockerfile) at the moment.

## Running in development mode

`./gradlew docker`

## CI/CD build

`docker image build -f Dockerfile .`

## License

This repository code is released under the [Apache License 2.0](LICENSE).
