/*
 * Copyright 2023-2023 IEXEC BLOCKCHAIN TECH
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

package com.iexec.blockchain.tool;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Web3jServiceTests {
    private final ChainConfig chainConfig = ChainConfig
            .builder()
            .id(134)
            .isSidechain(true)
            .nodeAddress("https://bellecour.iex.ec")
            .hubAddress("0x3eca1B216A7DF1C7689aEb259fFB83ADFB894E7f")
            .blockTime(5)
            .gasPriceMultiplier(1.0f)
            .gasPriceCap(22_000_000_000L)
            .build();

    @Test
    void shouldCreateInstance() {
        assertThat(new Web3jService(chainConfig)).isNotNull();
    }
}
