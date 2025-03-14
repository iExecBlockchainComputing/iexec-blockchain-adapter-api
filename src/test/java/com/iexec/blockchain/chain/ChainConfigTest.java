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
    private static final boolean DEFAULT_IS_SIDECHAIN = true;
    private static final String DEFAULT_NODE_ADDRESS = "http://localhost:8545";
    private static final String DEFAULT_HUB_ADDRESS = "0xBF6B2B07e47326B7c8bfCb4A5460bef9f0Fd2002";
    private static final Duration DEFAULT_BLOCK_TIME = Duration.ofSeconds(1);
    private static final float DEFAULT_GAS_PRICE_MULTIPLIER = 0.1f;
    private static final long DEFAULT_GAS_PRICE_CAP = 22_000_000_000L;
    private static final int DEFAULT_MAX_ALLOWED_TX_PER_BLOCK = 1;

    private Set<ConstraintViolation<ChainConfig>> validate(ChainConfig chainConfig) {
        try (final ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            return factory.getValidator().validate(chainConfig);
        }
    }

    // region Valid data
    static Stream<Arguments> validData() {
        return Stream.of(
                Arguments.of(100, true, "http://localhost:8545", "0xBF6B2B07e47326B7c8bfCb4A5460bef9f0Fd2002", "PT0.1S", 1.0f, 11_000_000_000L, 1),
                Arguments.of(42, true, "https://localhost:8545", "0x0000000000000000000000000000000000000001", "PT10S", 1.0f, 22_000_000_000L, 2),
                Arguments.of(10, true, "https://www.classic-url.com", "0xBF6B2B07e47326B7c8bfCb4A5460bef9f0Fd2002", "PT20S", 1.0f, 22_000_000_000L, 2),
                Arguments.of(1, true, "http://ibaa.iex.ec:443/test?validation=should:be@OK", "0xBF6B2B07e47326B7c8bfCb4A5460bef9f0Fd2002", "PT5S", 1.0f, 0L, 1),
                Arguments.of(DEFAULT_CHAIN_ID, DEFAULT_IS_SIDECHAIN, DEFAULT_NODE_ADDRESS, DEFAULT_HUB_ADDRESS, DEFAULT_BLOCK_TIME, DEFAULT_GAS_PRICE_MULTIPLIER, DEFAULT_GAS_PRICE_CAP, DEFAULT_MAX_ALLOWED_TX_PER_BLOCK)
        );
    }

    @ParameterizedTest
    @MethodSource("validData")
    void shouldValidate(Integer chainId,
                        boolean sidechain,
                        String nodeAddress,
                        String hubAddress,
                        Duration blockTime,
                        float gasPriceMultiplier,
                        long gasPriceCap,
                        int maxAllowedTxPerBlock) {
        final ChainConfig chainConfig = ChainConfig.builder()
                .id(chainId)
                .sidechain(sidechain)
                .nodeAddress(nodeAddress)
                .hubAddress(hubAddress)
                .blockTime(blockTime)
                .gasPriceMultiplier(gasPriceMultiplier)
                .gasPriceCap(gasPriceCap)
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
                .sidechain(DEFAULT_IS_SIDECHAIN)
                .nodeAddress(DEFAULT_NODE_ADDRESS)
                .hubAddress(DEFAULT_HUB_ADDRESS)
                .blockTime(DEFAULT_BLOCK_TIME)
                .gasPriceMultiplier(DEFAULT_GAS_PRICE_MULTIPLIER)
                .gasPriceCap(DEFAULT_GAS_PRICE_CAP)
                .maxAllowedTxPerBlock(DEFAULT_MAX_ALLOWED_TX_PER_BLOCK)
                .build();

        log.info("{}", chainConfig);
        assertThat(validate(chainConfig))
                .extracting(v -> v.getPropertyPath().toString())
                .containsExactly("id");
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
                .sidechain(DEFAULT_IS_SIDECHAIN)
                .nodeAddress(nodeAddress)
                .hubAddress(DEFAULT_HUB_ADDRESS)
                .blockTime(DEFAULT_BLOCK_TIME)
                .gasPriceMultiplier(DEFAULT_GAS_PRICE_MULTIPLIER)
                .gasPriceCap(DEFAULT_GAS_PRICE_CAP)
                .maxAllowedTxPerBlock(DEFAULT_MAX_ALLOWED_TX_PER_BLOCK)
                .build();

        log.info("{}", chainConfig);
        assertThat(validate(chainConfig))
                .extracting(v -> v.getPropertyPath().toString())
                .containsExactly("nodeAddress");
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
                .sidechain(DEFAULT_IS_SIDECHAIN)
                .nodeAddress(DEFAULT_NODE_ADDRESS)
                .hubAddress(DEFAULT_HUB_ADDRESS)
                .blockTime(blockTime)
                .gasPriceMultiplier(DEFAULT_GAS_PRICE_MULTIPLIER)
                .gasPriceCap(DEFAULT_GAS_PRICE_CAP)
                .maxAllowedTxPerBlock(DEFAULT_MAX_ALLOWED_TX_PER_BLOCK)
                .build();

        log.info("{}", chainConfig);
        assertThat(validate(chainConfig))
                .extracting(v -> v.getPropertyPath().toString())
                .containsExactly("blockTime");
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
                .sidechain(DEFAULT_IS_SIDECHAIN)
                .nodeAddress(DEFAULT_NODE_ADDRESS)
                .hubAddress(hubAddress)
                .blockTime(DEFAULT_BLOCK_TIME)
                .gasPriceMultiplier(DEFAULT_GAS_PRICE_MULTIPLIER)
                .gasPriceCap(DEFAULT_GAS_PRICE_CAP)
                .maxAllowedTxPerBlock(DEFAULT_MAX_ALLOWED_TX_PER_BLOCK)
                .build();

        log.info("{}", chainConfig);
        assertThat(validate(chainConfig))
                .extracting(v -> v.getPropertyPath().toString())
                .containsExactly("hubAddress");
    }
    // endregion
}
