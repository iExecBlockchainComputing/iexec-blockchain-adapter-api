/*
 * Copyright 2023-2025 IEXEC BLOCKCHAIN TECH
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

import com.iexec.commons.poco.chain.*;
import com.iexec.commons.poco.contract.generated.IexecHubContract;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.gas.DefaultGasProvider;

import java.io.IOException;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IexecHubServiceTests {
    private final String chainDealId = "0x1";
    private final String chainTaskId = "0x2";
    private final String resultDigest = "0x3";
    private final String enclaveChallenge = "0x4";
    private final ChainConfig chainConfig = ChainConfig
            .builder()
            .hubAddress("0xC129e7917b7c7DeDfAa5Fff1FB18d5D7050fE8ca")
            .build();
    @Mock
    private SignerService signerService;
    @Mock
    private IexecHubContract iexecHubContract;
    @Mock
    private Web3jService web3jService;
    @Mock
    private TransactionReceipt receipt;
    private IexecHubService iexecHubService;

    @BeforeEach
    void init() {
        final Credentials credentials = createEthereumCredentials();
        when(signerService.getCredentials()).thenReturn(credentials);
        when(web3jService.getContractGasProvider()).thenReturn(new DefaultGasProvider());
        iexecHubService = spy(new IexecHubService(signerService, web3jService, chainConfig));
    }

    @SneakyThrows
    private Credentials createEthereumCredentials() {
        final ECKeyPair ecKeyPair = Keys.createEcKeyPair();
        return Credentials.create(ecKeyPair);
    }

    private void mockTransaction() throws IOException {
        when(signerService.getNonce()).thenReturn(BigInteger.ONE);
        when(signerService.signAndSendTransaction(any(), any(), any(), any(), any()))
                .thenReturn("txHash");
        when(web3jService.getTransactionReceipt("txHash")).thenReturn(receipt);
    }

    // region initializeTask

    @Test
    void shouldInitializeTask() throws Exception {
        mockTransaction();
        assertThat(iexecHubService.initializeTask(chainDealId, 0))
                .isEqualTo(receipt);
    }

    @Test
    void shouldNotInitializeTask() throws IOException {
        when(signerService.signAndSendTransaction(any(), any(), any(), any(), any()))
                .thenThrow(IOException.class);
        assertThat(iexecHubService.initializeTask(chainDealId, 0))
                .isNull();
    }

    // endregion

    @Test
    void shouldNotContribute() throws IOException {
        when(signerService.signAndSendTransaction(any(), any(), any(), any(), any()))
                .thenThrow(IOException.class);
        assertThat(iexecHubService.contribute(chainTaskId, resultDigest,
                "workerpoolSignature", enclaveChallenge, "enclaveSignature"))
                .isNull();
    }

    @Test
    void shouldNotReveal() throws IOException {
        when(signerService.signAndSendTransaction(any(), any(), any(), any(), any()))
                .thenThrow(IOException.class);
        assertThat(iexecHubService.reveal(chainTaskId, resultDigest))
                .isNull();
    }

    // region finalizeTask

    @Test
    void shouldFinalizeTask() throws IOException {
        mockTransaction();
        assertThat(iexecHubService.finalizeTask(chainTaskId, "resultLink", "callbackData"))
                .isEqualTo(receipt);
    }

    @Test
    void shouldNotFinalizeTask() throws IOException {
        when(signerService.signAndSendTransaction(any(), any(), any(), any(), any()))
                .thenThrow(IOException.class);
        assertThat(iexecHubService.finalizeTask(chainTaskId, "resultLink", "callbackData"))
                .isNull();
    }

    // endregion

    // region isTaskInUnsetStatusOnChain

    @Test
    void shouldBeUnsetWhenNoTask() {
        assertThat(iexecHubService.isTaskInUnsetStatusOnChain("chainTaskId"))
                .isTrue();
    }

    @Test
    void shouldNotBeUnset() {
        doReturn(Optional.of(ChainTask.builder().status(ChainTaskStatus.ACTIVE).build()))
                .when(iexecHubService).getChainTask("chainTaskId");
        assertThat(iexecHubService.isTaskInUnsetStatusOnChain("chainTaskId"))
                .isFalse();
    }

    // endregion

    // region isBeforeContributionDeadline

    @Test
    void shouldNotBeBeforeContributionDeadline() {
        assertThat(iexecHubService.isBeforeContributionDeadline("dealId"))
                .isFalse();
    }

    @Test
    void shouldBeBeforeContributionDeadline() {
        final BigInteger startTime = BigInteger.valueOf(Instant.now().getEpochSecond());
        final ChainCategory category = ChainCategory.builder().id(0).maxExecutionTime(1000L).build();
        doReturn(Optional.of(ChainDeal.builder().chainCategory(category).startTime(startTime).build()))
                .when(iexecHubService).getChainDeal("dealId");
        assertThat(iexecHubService.isBeforeContributionDeadline("dealId"))
                .isTrue();
    }

    // endregion
}
