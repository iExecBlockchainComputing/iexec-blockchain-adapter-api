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

package com.iexec.blockchain.chain;

import com.iexec.commons.poco.chain.*;
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
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IexecHubServiceTests {
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
    private RemoteFunctionCall<TransactionReceipt> remoteFunctionCall;
    private IexecHubService iexecHubService;

    @BeforeEach
    void init() {
        final Credentials credentials = createEthereumCredentials();
        when(signerService.getCredentials()).thenReturn(credentials);
        iexecHubService = spy(new IexecHubService(signerService, web3jService, chainConfig));
        ReflectionTestUtils.setField(iexecHubService, "iexecHubContract", iexecHubContract);
    }

    @SneakyThrows
    private Credentials createEthereumCredentials() {
        final ECKeyPair ecKeyPair = Keys.createEcKeyPair();
        return Credentials.create(ecKeyPair);
    }

    // region initializeTask

    @Test
    void shouldInitializeTask() throws Exception {
        final TransactionReceipt receipt = new TransactionReceipt();
        when(iexecHubContract.initialize(any(), any())).thenReturn(remoteFunctionCall);
        when(remoteFunctionCall.send()).thenReturn(receipt);
        assertThat(iexecHubService.initializeTask("chainTaskId", 0))
                .isEqualTo(receipt);
    }

    @Test
    void shouldNotInitializeTask() throws Exception {
        when(iexecHubContract.initialize(any(), any())).thenReturn(remoteFunctionCall);
        when(remoteFunctionCall.send()).thenThrow(Exception.class);
        assertThatThrownBy(() -> iexecHubService.initializeTask("chainTaskId", 0))
                .isInstanceOf(Exception.class);
    }

    // endregion

    @Test
    void shouldNotContribute() throws Exception {
        when(iexecHubContract.contribute(any(), any(), any(), any(), any(), any())).thenReturn(remoteFunctionCall);
        when(remoteFunctionCall.send()).thenThrow(Exception.class);
        assertThatThrownBy(() -> iexecHubService.contribute("chainTaskId", "resultDigest",
                "workerpoolSignature", "enclaveChallenge", "enclaveSignature"))
                .isInstanceOf(Exception.class);
    }

    @Test
    void shouldNotReveal() throws Exception {
        when(iexecHubContract.reveal(any(), any())).thenReturn(remoteFunctionCall);
        when(remoteFunctionCall.send()).thenThrow(Exception.class);
        assertThatThrownBy(() -> iexecHubService.reveal("chainTaskId", "resultDigest"))
                .isInstanceOf(Exception.class);
    }

    // region finalizeTask

    @Test
    void shouldFinalizeTask() throws Exception {
        final TransactionReceipt receipt = new TransactionReceipt();
        when(iexecHubContract.finalize(any(), any(), any())).thenReturn(remoteFunctionCall);
        when(remoteFunctionCall.send()).thenReturn(receipt);
        assertThat(iexecHubService.finalizeTask("chainTaskId", "resultLink", "callbackData"))
                .isEqualTo(receipt);
    }

    @Test
    void shouldNotFinalizeTask() throws Exception {
        when(iexecHubContract.finalize(any(), any(), any())).thenReturn(remoteFunctionCall);
        when(remoteFunctionCall.send()).thenThrow(Exception.class);
        assertThatThrownBy(() -> iexecHubService.finalizeTask("chainTaskId", "resultLink", "callbackData"))
                .isInstanceOf(Exception.class);
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
