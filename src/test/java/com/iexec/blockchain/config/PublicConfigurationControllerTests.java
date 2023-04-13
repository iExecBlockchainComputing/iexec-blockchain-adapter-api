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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import java.time.Duration;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

class PublicConfigurationControllerTests {

    @Mock
    ChainConfig chainConfig;

    @InjectMocks
    PublicConfigurationController controller;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldReturnConfig() {
        int blockTime = 5;
        PublicChainConfig expectedConfig = PublicChainConfig
                .builder()
                .sidechain(true)
                .chainId(65535)
                .chainNodeUrl("http://localhost:8545")
                .iexecHubContractAddress("0xC129e7917b7c7DeDfAa5Fff1FB18d5D7050fE8ca")
                .blockTime(Duration.ofSeconds(blockTime))
                .build();
        when(chainConfig.getChainId()).thenReturn(expectedConfig.getChainId());
        when(chainConfig.isSidechain()).thenReturn(expectedConfig.isSidechain());
        when(chainConfig.getNodeAddress()).thenReturn(expectedConfig.getChainNodeUrl());
        when(chainConfig.getHubAddress()).thenReturn(expectedConfig.getIexecHubContractAddress());
        when(chainConfig.getBlockTime()).thenReturn(blockTime);
        ResponseEntity<PublicChainConfig> response = controller.getPublicChainConfig();
        assertThat(response.getBody())
                .isNotNull()
                .isEqualTo(expectedConfig);
    }

}
