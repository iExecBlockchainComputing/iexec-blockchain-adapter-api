/*
 * Copyright 2021-2025 IEXEC BLOCKCHAIN TECH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.iexec.blockchain.chain;

import jakarta.validation.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

@Slf4j
class ChainConfigTest {
    private static final int DEFAULT_CHAIN_ID = 1;
    private static final String DEFAULT_NODE_ADDRESS = "http://localhost:8545";
    private static final String DEFAULT_HUB_ADDRESS = "0xBF6B2B07e47326B7c8bfCb4A5460bef9f0Fd2002";
    private static final Duration DEFAULT_BLOCK_TIME = Duration.ofSeconds(1);

    private Set<ConstraintViolation<ChainConfig>> validate(ChainConfig chainConfig) {
        try (final ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            return factory.getValidator().validate(chainConfig);
        }
    }

    // region Valid data
    static Stream<Arguments> validData() {
        return Stream.of(
                Arguments.of(100, "http://localhost:8545", "0xBF6B2B07e47326B7c8bfCb4A5460bef9f0Fd2002", "PT0.1S", 1.0f, 1),
                Arguments.of(42, "https://localhost:8545", "0x0000000000000000000000000000000000000001", "PT10S", 1.0f, 2),
                Arguments.of(10, "https://www.classic-url.com", "0xBF6B2B07e47326B7c8bfCb4A5460bef9f0Fd2002", "PT20S", 1.0f, 2),
                Arguments.of(1, "http://ibaa.iex.ec:443/test?validation=should:be@OK", "0xBF6B2B07e47326B7c8bfCb4A5460bef9f0Fd2002", "PT5S", 1.0f, 1)
        );
    }

    @ParameterizedTest
    @MethodSource("validData")
    void shouldValidate(Integer chainId,
                        String nodeAddress,
                        String hubAddress,
                        Duration blockTime,
                        float gasPriceMultiplier,
                        int maxAllowedTxPerBlock) {
        final ChainConfig chainConfig = ChainConfig.builder()
                .id(chainId)
                .nodeAddress(nodeAddress)
                .blockTime(blockTime)
                .hubAddress(hubAddress)
                .gasPriceMultiplier(gasPriceMultiplier)
                .maxAllowedTxPerBlock(maxAllowedTxPerBlock)
                .build();

        log.info("{}", chainConfig);
        assertThatCode(() -> validate(chainConfig))
                .doesNotThrowAnyException();
    }
    // endregion

    // region Invalid chain ids
    static Stream<Integer> invalidChainIds() {
        return Stream.of(
                0,      // Chain id should be strictly positive
                -1      // Chain id should be strictly positive
        );
    }

    @ParameterizedTest
    @MethodSource("invalidChainIds")
    void shouldNotValidateChainId(int chainId) {

        final ChainConfig chainConfig = ChainConfig.builder()
                .id(chainId)
                .nodeAddress(DEFAULT_NODE_ADDRESS)
                .blockTime(DEFAULT_BLOCK_TIME)
                .hubAddress(DEFAULT_HUB_ADDRESS)
                .build();

        log.info("{}", chainConfig);
        assertThat(validate(chainConfig))
                .extracting(ConstraintViolation::getMessage)
                .contains("Chain id should be positive");
    }
    // endregion

    // region Invalid node addresses
    static Stream<String> invalidNodeAddresses() {
        return Stream.of(
                null,       // Node address should not be null
                "",         // Node address should be a valid URL
                "12345",    // Node address should be a valid URL
                "0xBF6B2B07e47326B7c8bfCb4A5460bef9f0Fd2002"    // Node address should be a valid URL
        );
    }

    @ParameterizedTest
    @MethodSource("invalidNodeAddresses")
    void shouldNotValidateNodeAddress(String nodeAddress) {
        final ChainConfig chainConfig = ChainConfig.builder()
                .id(DEFAULT_CHAIN_ID)
                .nodeAddress(nodeAddress)
                .blockTime(DEFAULT_BLOCK_TIME)
                .hubAddress(DEFAULT_HUB_ADDRESS)
                .build();

        log.info("{}", chainConfig);
        assertThat(validate(chainConfig))
                .extracting(v -> v.getPropertyPath().toString())
                .contains("nodeAddress");
    }
    // endregion

    // region Invalid block time
    static Stream<Duration> invalidBlockTimes() {
        return Stream.of(
                Duration.ofSeconds(0),    // Block time should be more than 100 milliseconds
                Duration.ofSeconds(-1),    // Block time should be strictly positive
                Duration.ofSeconds(25)   // Block time should be less than 20 seconds
        );
    }

    @ParameterizedTest
    @MethodSource("invalidBlockTimes")
    void shouldNotValidateBlockTime(Duration blockTime) {
        final ChainConfig chainConfig = ChainConfig.builder()
                .id(DEFAULT_CHAIN_ID)
                .nodeAddress(DEFAULT_NODE_ADDRESS)
                .blockTime(blockTime)
                .hubAddress(DEFAULT_HUB_ADDRESS)
                .build();

        log.info("{}", chainConfig);
        assertThat(validate(chainConfig))
                .extracting(v -> v.getPropertyPath().toString())
                .contains("blockTime");
    }
    // endregion

    // region Invalid hub address
    static Stream<String> invalidHubAddresses() {
        return Stream.of(
                null,       // Hub address should not be null
                "0xBF6B2B07e47326B7c8bfCb4A5460bef9f0Fd200211111111111111", // Hub address size should be exactly 40
                "0xBF6B2B07e47326B7c8bfCb4A5460bef9f0Fd200",    // Hub address size should be exactly 40
                "0x0000000000000000000000000000000000000000",    // Hub address should not be zero
                "http://hub.address"   // Hub address should be an Ethereum address
        );
    }

    @ParameterizedTest
    @MethodSource("invalidHubAddresses")
    void shouldNotValidateHubAddress(String hubAddress) {
        final ChainConfig chainConfig = ChainConfig.builder()
                .id(DEFAULT_CHAIN_ID)
                .nodeAddress(DEFAULT_NODE_ADDRESS)
                .blockTime(DEFAULT_BLOCK_TIME)
                .hubAddress(hubAddress)
                .build();

        log.info("{}", chainConfig);
        assertThat(validate(chainConfig))
                .extracting(v -> v.getPropertyPath().toString())
                .contains("hubAddress");
    }
    // endregion
}
