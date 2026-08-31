/*
 * Copyright 2023-2026 IEXEC BLOCKCHAIN TECH
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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@EnableConfigurationProperties(value = ChainConfig.class)
@TestPropertySource(properties = {
        "chain.id=421614",
        "chain.sidechain=false",
        "chain.node-address=https://sepolia-rollup.arbitrum.io/rpc",
        "chain.hub-address=0xB2157BF2fAb286b2A4170E3491Ac39770111Da3E",
        "chain.block-time=PT5S",
        "chain.gas-price-multiplier=1.1",
        "chain.gas-price-cap=22000000000",
        "chain.max-allowed-tx-per-block=1"})
class Web3jServiceTests {
    @Autowired
    private ChainConfig chainConfig;

    @Test
    void checkChainConfig() {
        final ChainConfig expectedChainConfig = ChainConfig
                .builder()
                .id(421614)
                .sidechain(false)
                .nodeAddress("https://sepolia-rollup.arbitrum.io/rpc")
                .hubAddress("0xB2157BF2fAb286b2A4170E3491Ac39770111Da3E")
                .blockTime(Duration.ofSeconds(5))
                .gasPriceMultiplier(1.1f)
                .gasPriceCap(22_000_000_000L)
                .maxAllowedTxPerBlock(1)
                .build();
        assertThat(chainConfig).isEqualTo(expectedChainConfig);
    }

    @Test
    void shouldCreateInstance() {
        assertThat(new Web3jService(chainConfig)).isNotNull();
    }
}
