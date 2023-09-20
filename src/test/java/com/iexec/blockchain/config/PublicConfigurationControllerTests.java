/*
 * Copyright 2023 IEXEC BLOCKCHAIN TECH
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

package com.iexec.blockchain.config;

import com.iexec.blockchain.tool.ChainConfig;
import com.iexec.common.config.PublicChainConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.time.Duration;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class PublicConfigurationControllerTests {

    private static final int ID = 65535;
    private static final String NODE_ADDRESS = "http://localhost:8545";
    private static final String HUB_ADDRESS = "0xC129e7917b7c7DeDfAa5Fff1FB18d5D7050fE8ca";
    private static final int BLOCK_TIME = 5;
    private static final boolean IS_SIDECHAIN = true;

    ChainConfig chainConfig = ChainConfig.builder()
            .id(ID)
            .nodeAddress(NODE_ADDRESS)
            .hubAddress(HUB_ADDRESS)
            .blockTime(BLOCK_TIME)
            .isSidechain(IS_SIDECHAIN)
            .build();

    PublicConfigurationController controller;

    @BeforeEach
    void init() {
        controller = new PublicConfigurationController(chainConfig);
    }

    @Test
    void shouldReturnConfig() {
        PublicChainConfig expectedConfig = PublicChainConfig
                .builder()
                .sidechain(IS_SIDECHAIN)
                .chainId(ID)
                .chainNodeUrl(NODE_ADDRESS)
                .iexecHubContractAddress(HUB_ADDRESS)
                .blockTime(Duration.ofSeconds(BLOCK_TIME))
                .build();
        ResponseEntity<PublicChainConfig> response = controller.getPublicChainConfig();
        assertThat(response.getBody())
                .isNotNull()
                .isEqualTo(expectedConfig);
    }

}
