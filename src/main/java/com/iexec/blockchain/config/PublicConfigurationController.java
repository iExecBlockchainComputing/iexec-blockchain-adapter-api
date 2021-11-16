/*
 * Copyright 2021 IEXEC BLOCKCHAIN TECH
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/config")
public class PublicConfigurationController {
    private final ChainConfig chainConfig;

    public PublicConfigurationController(ChainConfig chainConfig) {
        this.chainConfig = chainConfig;
    }

    /**
     * Unauthenticated endpoint.
     */
    @GetMapping("/chain")
    public ResponseEntity<PublicChainConfig> getPublicChainConfig() {
        final Integer blockTime = chainConfig.getBlockTime();
        final PublicChainConfig publicChainConfig = PublicChainConfig
                .builder()
                .blockTime(Duration.ofSeconds(blockTime))
                .build();
        return ResponseEntity.ok(publicChainConfig);
    }
}
