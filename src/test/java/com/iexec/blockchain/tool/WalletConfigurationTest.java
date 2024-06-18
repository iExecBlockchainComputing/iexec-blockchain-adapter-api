/*
 * Copyright 2024 IEXEC BLOCKCHAIN TECH
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

import com.iexec.commons.poco.chain.SignerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.web3j.crypto.WalletUtils;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

class WalletConfigurationTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner();
    @TempDir
    private File tempWalletDir;

    @Test
    void shouldCreateBeans() throws Exception {
        final String tempWalletName = WalletUtils.generateFullNewWalletFile("changeit", tempWalletDir);
        final String tempWalletPath = tempWalletDir.getAbsolutePath() + File.separator + tempWalletName;
        runner.withBean(ChainConfig.class, 65535, "http://localhost:8545", 5, "0xC129e7917b7c7DeDfAa5Fff1FB18d5D7050fE8ca", true, 1.0f, 0L, 2)
                .withBean(IexecHubService.class)
                .withBean(WalletConfiguration.class, tempWalletPath, "changeit")
                .withBean(Web3jService.class)
                .run(context -> assertThat(context)
                        .hasSingleBean(ChainConfig.class)
                        .hasSingleBean(IexecHubService.class)
                        .hasSingleBean(SignerService.class)
                        .hasSingleBean(WalletConfiguration.class)
                        .hasSingleBean(Web3jService.class));
    }
}
