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
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.exceptions.TransactionException;
import org.web3j.tx.response.PollingTransactionReceiptProcessor;

import java.io.IOException;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
            .blockTime(Duration.ofSeconds(5))
            .hubAddress("0xC129e7917b7c7DeDfAa5Fff1FB18d5D7050fE8ca")
            .build();
    @Mock
    private SignerService signerService;
    @Mock
    private PollingTransactionReceiptProcessor txReceiptProcessor;
    @Mock
    private Web3jService web3jService;
    @Mock
    private TransactionReceipt receipt;
    private IexecHubService iexecHubService;

    @BeforeEach
    void init() {
        final Credentials credentials = createEthereumCredentials();
        when(signerService.getCredentials()).thenReturn(credentials);
        iexecHubService = spy(new IexecHubService(signerService, web3jService, chainConfig));
        ReflectionTestUtils.setField(iexecHubService, "txReceiptProcessor", txReceiptProcessor);
    }

    @SneakyThrows
    private Credentials createEthereumCredentials() {
        final ECKeyPair ecKeyPair = Keys.createEcKeyPair();
        return Credentials.create(ecKeyPair);
    }

    private void mockTransaction() throws IOException, TransactionException {
        when(signerService.getNonce()).thenReturn(BigInteger.ONE);
        when(signerService.estimateGas(any(), any())).thenReturn(BigInteger.valueOf(100_000L));
        when(signerService.signAndSendTransaction(any(), any(), any(), any(), any())).thenReturn("txHash");
        when(txReceiptProcessor.waitForTransactionReceipt("txHash")).thenReturn(receipt);
    }

    // region initializeTask

    @Test
    void shouldInitializeTask() throws IOException, TransactionException {
        mockTransaction();
        assertThat(iexecHubService.initializeTask(chainDealId, 0))
                .isEqualTo(receipt);
    }

    @Test
    void shouldNotInitializeTask() throws IOException {
        when(signerService.estimateGas(any(), any())).thenReturn(BigInteger.valueOf(100_000L));
        when(signerService.signAndSendTransaction(any(), any(), any(), any(), any()))
                .thenThrow(IOException.class);
        assertThatThrownBy(() -> iexecHubService.initializeTask(chainDealId, 0))
                .isInstanceOf(IOException.class);
    }

    // endregion

    @Test
    void shouldNotContribute() throws IOException {
        when(signerService.signAndSendTransaction(any(), any(), any(), any(), any()))
                .thenThrow(IOException.class);
        assertThatThrownBy(() -> iexecHubService.contribute(chainTaskId, resultDigest,
                "workerpoolSignature", enclaveChallenge, "enclaveSignature"))
                .isInstanceOf(IOException.class);
    }

    @Test
    void shouldNotReveal() throws IOException {
        when(signerService.signAndSendTransaction(any(), any(), any(), any(), any()))
                .thenThrow(IOException.class);
        assertThatThrownBy(() -> iexecHubService.reveal(chainTaskId, resultDigest))
                .isInstanceOf(IOException.class);
    }

    // region finalizeTask

    @Test
    void shouldFinalizeTask() throws IOException, TransactionException {
        mockTransaction();
        when(web3jService.sendCall(any(), any(), any())).thenReturn("0x30D40"); // hexadecimal value for 200_000
        assertThat(iexecHubService.finalizeTask(chainTaskId, "resultLink", "callbackData"))
                .isEqualTo(receipt);
    }

    @Test
    void shouldNotFinalizeTask() throws IOException {
        when(signerService.estimateGas(any(), any())).thenReturn(BigInteger.valueOf(100_000L));
        when(web3jService.sendCall(any(), any(), any())).thenReturn("0x30D40"); // hexadecimal value for 200_000
        when(signerService.signAndSendTransaction(any(), any(), any(), any(), any()))
                .thenThrow(IOException.class);
        assertThatThrownBy(() -> iexecHubService.finalizeTask(chainTaskId, "resultLink", "callbackData"))
                .isInstanceOf(IOException.class);
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
        final ChainCategory category = ChainCategory.builder().maxExecutionTime(1000L).build();
        ReflectionTestUtils.setField(iexecHubService, "categories", Map.of(0L, category));
        when(iexecHubService.getChainDeal("dealId"))
                .thenReturn(Optional.of(ChainDeal.builder().chainCategory(category).startTime(startTime).build()));
        assertThat(iexecHubService.isBeforeContributionDeadline("dealId"))
                .isTrue();
    }

    // endregion
}
