# Changelog

All notable changes to this project will be documented in this file.

## [[NEXT]](https://github.com/iExecBlockchainComputing/iexec-blockchain-adapter-api/releases/tag/vNEXT) 2024

### New Features

- Replace `CredentialsService` with `SignerService`. (#143)

### Quality

- Use `Instant` instead of `DateTimeUtils`. (#138)
- Configure Gradle JVM Test Suite Plugin. (#139)

### Dependency Upgrades

- Upgrade to Gradle 8.7. (#140)
- Upgrade to `eclipse-temurin:11.0.22_7-jre-focal`. (#141)
- Upgrade to Spring Boot 2.7.18. (#142)

## [[8.4.0]](https://github.com/iExecBlockchainComputing/iexec-blockchain-adapter-api/releases/tag/v8.4.0) 2024-02-29

### New Features

- Label REST API with `v1` version. (#132)

### Bug Fixes

- Add retry mechanism and set command status to `FAILURE` after all attempts failed. (#134)

### Quality

- Remove `/tasks/{chainTaskId}` endpoint, the adapter must only call `initialize` and `finalize` **PoCo** methods. (#130)
- Remove `/broker/orders/match` endpoint, `matchOrders` must be done through the **Market API**. (#131)
- Remove dead code in `IexecHubService` and `CommandStorage`. (#133)

### Dependency Upgrades

- Upgrade to `iexec-common` 8.4.0. (#135)

## [[8.3.0]](https://github.com/iExecBlockchainComputing/iexec-blockchain-adapter-api/releases/tag/v8.3.0) 2024-01-10

### New Features

- Send up to 2 blockchain transactions per block.
  With a big enough latency, the nonce is properly computed in web3j library against the pending block. (#111)
- Add `BlockchainAdapterService` class to implement interactions with REST API. (#117 #118 #119 126)
- Expose version through prometheus endpoint and through VersionController. (#122 #123)

### Bug Fixes

- Remove `contribute` and `reveal` endpoints. (#110)
- Fix web security depreciation warning. (#112)

### Quality

- Add and use a non-root user in the dockerfile. (#113)
- Reindent `build.gradle` file. (#114)
- Standardisation of the dockerfile and its location in regard to other java components. (#115)
- Rename `Status` to `CommandStatus` in library. (#117)
- Remove `com.iexec.blockchain.command.task.contribute` package. (#124)
- Remove `com.iexec.blockchain.command.task.reveal` package. (#125)

### Dependency Upgrades

- Upgrade to `eclipse-temurin:11.0.21_9-jre-focal`. (#121)
- Upgrade to Spring Boot 2.7.17. (#120)
- Upgrade to Spring Dependency Management Plugin 1.1.4. (#120)
- Upgrade to Spring Doc OpenAPI 1.7.0. (#122)
- Upgrade to `jenkins-library` 2.7.4. (#116)
- Upgrade to `iexec-commons-poco` 3.2.0. (#127)
- Upgrade to `iexec-common` 8.3.1. (#127)

## [[8.2.0]](https://github.com/iExecBlockchainComputing/iexec-blockchain-adapter-api/releases/tag/v8.2.0) 2023-09-28

### Bug Fixes

- Fix and harmonize `Dockerfile entrypoint` in all Spring Boot applications. (#102)

### Quality

- Remove `nexus.intra.iex.ec` repository. (#96)
- Upgrade to Gradle 8.2.1 with up-to-date plugins. (#100)
- Clean TODOs. (#104)
- `ChainConfig` instance is immutable and validated. Application will fail to start if chain config parameters violate
  constraints. (#105)
- Remove `SignerService` class only used in integration tests. (#107)

### Dependency Upgrades

- Upgrade to `eclipse-temurin` 11.0.20. (#98)
- Upgrade to Spring Boot 2.7.14. (#99)
- Upgrade to Spring Dependency Management Plugin 1.1.3. (#99)
- Upgrade to `testcontainers` 1.19.0. (#101)
- Upgrade to `jenkins-library` 2.7.3. (#103)
- Upgrade to `iexec-common` 8.3.0. (#106)
- Upgrade to `iexec-common-poco` 3.1.0. (#106)

## [[8.1.1]](https://github.com/iExecBlockchainComputing/iexec-blockchain-adapter-api/releases/tag/v8.1.1) 2023-06-23

### Dependency Upgrades

- Upgrade to `iexec-common` 8.2.1. (#94)
- Upgrade to `iexec-commons-poco` 3.0.4. (#94)

## [[8.1.0]](https://github.com/iExecBlockchainComputing/iexec-blockchain-adapter-api/releases/tag/v8.1.0) 2023-06-07

### New Features

- Enable Prometheus actuator. (#79)
- Rework `QueueService` with a thread pool based on a `PriorityBlockingQueue`. (#84)
- Do not use `broker` to match orders on chain. (#87 #88)

### Bug Fixes

- Fix security rule to access Swagger API. (#79)

### Quality

- Use `testcontainers` in integration tests. (#89)
- Remove `com.iexec.blockchain.dataset` package and update `feign` client endpoints. (#91)

### Dependency Upgrades

- Upgrade to `feign` 11.10. (#80)
- Upgrade to `iexec-common` 8.2.0. (#83 #85 #90)
- Upgrade to `iexec-commons-poco` 3.0.2. (#83 #85 #86 #90)

## [[8.0.1]](https://github.com/iExecBlockchainComputing/iexec-blockchain-adapter-api/releases/tag/v8.0.1) 2023-04-06

### Quality

* Run integration tests on `poco-chain@native-v5.4.2-5s`. (#81)
* Connect by default to iExec Bellecour blockchain. (#81)

## [[8.0.0]](https://github.com/iExecBlockchainComputing/iexec-blockchain-adapter-api/releases/tag/v8.0.0) 2023-03-03

### New Features

* Expose a `BrokerClient` in library.
* Add iExec banner at startup.
* Show application version on banner.

### Quality

* Improve code quality.
* Use new TEE classes in tests.

### Dependency Upgrades

* Replace the deprecated `openjdk` Docker base image with `eclipse-temurin` and upgrade to Java 11.0.18 patch.
* Upgrade to Spring Boot 2.6.14.
* Upgrade to Gradle 7.6.
* Upgrade OkHttp to 4.9.0.
* Upgrade to `iexec-common` 7.0.0.
* Upgrade to `jenkins-library` 2.4.0.

## [[7.3.0]](https://github.com/iExecBlockchainComputing/iexec-blockchain-adapter-api/releases/tag/v7.3.0) 2023-01-18

* Add endpoint to allow health checks.

## [[7.2.0]](https://github.com/iExecBlockchainComputing/iexec-blockchain-adapter-api/releases/tag/v7.2.0) 2023-01-09

* Increments jenkins-library up to version 2.2.3. Enable SonarCloud analyses on branches and pull requests.
* Improve thread management in some tests.

## [[7.1.2]](https://github.com/iExecBlockchainComputing/iexec-blockchain-adapter-api/releases/tag/v7.1.2) 2022-11-29

* Update build workflow to 2.1.4, update documentation in README and add CHANGELOG.

## [[7.1.1]](https://github.com/iExecBlockchainComputing/iexec-blockchain-adapter-api/releases/tag/v7.1.1) 2022-07-01

* Hotfix integration tests.

## [[7.1.0]](https://github.com/iExecBlockchainComputing/iexec-blockchain-adapter-api/releases/tag/v7.1.0) 2022-07-01

* Add OpenFeign client library in dedicated iexec-blockchain-adapter-api-library jar.
* Use Spring Boot 2.6.2.
* Use Java 11.0.15.

## [[7.0.0]](https://github.com/iExecBlockchainComputing/iexec-blockchain-adapter-api/releases/tag/v7.0.0) 2021-12-14

* Highly improved throughput of the iExec protocol.

## [[0.2.0]](https://github.com/iExecBlockchainComputing/iexec-blockchain-adapter-api/releases/tag/v0.2.0) 2021-11-25

* Expose public chain configuration.
* Enable local import of iexec-common.
* Update build pipeline.

## [[0.1.1]](https://github.com/iExecBlockchainComputing/iexec-blockchain-adapter-api/releases/tag/0.1.1) 2021-11-10

* Send transactions synchronously to easily fix nonce conflicts.

## [[0.1.0]](https://github.com/iExecBlockchainComputing/iexec-blockchain-adapter-api/releases/tag/0.1.0) 2021-10-26

* First version.
