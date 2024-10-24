/*
 * Copyright 2023-2024 IEXEC BLOCKCHAIN TECH
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
import com.iexec.commons.poco.contract.generated.IexecHubContract;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;

import java.io.IOException;
import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IexecHubServiceTests {
    private final String chainTaskId = "0xefb041df2b4c7bac206cd852f289ff632a90c92bda289acdf5abbc0d8014461b";
    private final String enclaveChallenge = "0x123";
    private final String resultDigest = "0x456";

    private final ChainConfig chainConfig = ChainConfig
            .builder()
            .hubAddress("0xC129e7917b7c7DeDfAa5Fff1FB18d5D7050fE8ca")
            .gasPriceCap(20_000_000_000L)
            .build();
    @Mock
    private SignerService signerService;
    @Mock
    private IexecHubContract iexecHubContract;
    @Mock
    private Web3jService web3jService;
    private IexecHubService iexecHubService;

    @BeforeEach
    void init() {
        Credentials credentials = createEthereumCredentials();
        when(signerService.getCredentials()).thenReturn(credentials);
        iexecHubService = spy(new IexecHubService(signerService, web3jService, chainConfig));
        ReflectionTestUtils.setField(iexecHubService, "iexecHubContract", iexecHubContract);
    }

    @SneakyThrows
    private Credentials createEthereumCredentials() {
        ECKeyPair ecKeyPair = Keys.createEcKeyPair();
        return Credentials.create(ecKeyPair);
    }

    @Test
    void shouldNotInitializeTask() throws Exception {
        when(signerService.getNonce()).thenReturn(BigInteger.ONE);
        when(signerService.signAndSendTransaction(any(), any(), any(), any())).thenThrow(IOException.class);
        assertThatThrownBy(() -> iexecHubService.initializeTask(chainTaskId, 0))
                .isInstanceOf(IOException.class);
    }

    @Test
    void shouldNotContribute() throws Exception {
        when(signerService.signAndSendTransaction(any(), any(), any(), any())).thenThrow(IOException.class);
        assertThatThrownBy(() -> iexecHubService.contribute(chainTaskId, resultDigest,
                "workerpoolSignature", enclaveChallenge, "enclaveSignature"))
                .isInstanceOf(Exception.class);
    }

    @Test
    void shouldNotReveal() throws Exception {
        when(signerService.signAndSendTransaction(any(), any(), any(), any())).thenThrow(IOException.class);
        assertThatThrownBy(() -> iexecHubService.reveal(chainTaskId, resultDigest))
                .isInstanceOf(Exception.class);
    }

    @Test
    void shouldNotFinalizeTask() throws Exception {
        when(signerService.signAndSendTransaction(any(), any(), any(), any())).thenThrow(IOException.class);
        assertThatThrownBy(() -> iexecHubService.finalizeTask(chainTaskId, "resultLink", "callbackData"))
                .isInstanceOf(IOException.class);
    }
}
