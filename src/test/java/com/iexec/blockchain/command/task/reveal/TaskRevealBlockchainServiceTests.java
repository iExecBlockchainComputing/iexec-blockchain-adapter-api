/*
 * Copyright 2022 IEXEC BLOCKCHAIN TECH
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

package com.iexec.blockchain.command.task.reveal;

import com.iexec.blockchain.tool.CredentialsService;
import com.iexec.blockchain.tool.IexecHubService;
import com.iexec.common.chain.ChainTask;
import com.iexec.common.chain.ChainTaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Keys;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Optional;

import static com.iexec.common.utils.DateTimeUtils.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(OutputCaptureExtension.class)
class TaskRevealBlockchainServiceTests {

    private static final String CHAIN_TASK_ID = "chainTaskId";

    @Mock
    private CredentialsService credentialsService;
    @Mock
    private IexecHubService iexecHubService;
    @InjectMocks
    private TaskRevealBlockchainService taskRevealBlockchainService;

    @BeforeEach
    public void init() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void canNotSendBlockchainCommandWhenNoTask(CapturedOutput output) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException {
        TaskRevealArgs args = new TaskRevealArgs(CHAIN_TASK_ID, "resultDigest");
        Credentials credentials = Credentials.create(Keys.createEcKeyPair());
        when(credentialsService.getCredentials()).thenReturn(credentials);
        when(iexecHubService.getChainTask(CHAIN_TASK_ID)).thenReturn(Optional.empty());
        assertThat(taskRevealBlockchainService.canSendBlockchainCommand(args)).isFalse();
        assertThat(output.getOut()).contains("blockchain read");
    }

    @Test
    void canNotSendBlockchainCommandWhenTaskNotRevealing(CapturedOutput output) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException {
        TaskRevealArgs args = new TaskRevealArgs(CHAIN_TASK_ID, "resultDigest");
        ChainTask chainTask = ChainTask.builder().status(ChainTaskStatus.ACTIVE).build();
        Credentials credentials = Credentials.create(Keys.createEcKeyPair());
        when(credentialsService.getCredentials()).thenReturn(credentials);
        when(iexecHubService.getChainTask(CHAIN_TASK_ID)).thenReturn(Optional.of(chainTask));
        assertThat(taskRevealBlockchainService.canSendBlockchainCommand(args)).isFalse();
        assertThat(output.getOut()).contains("task is not revealing");
    }

    @Test
    void canNotSendCommandWhenTaskRevealDeadlineReached(CapturedOutput output) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException {
        TaskRevealArgs args = new TaskRevealArgs("chainTaskId", "resultDigest");
        ChainTask chainTask = ChainTask.builder().status(ChainTaskStatus.REVEALING).finalDeadline(now()).build();
        Credentials credentials = Credentials.create(Keys.createEcKeyPair());
        when(credentialsService.getCredentials()).thenReturn(credentials);
        when(iexecHubService.getChainTask(CHAIN_TASK_ID)).thenReturn(Optional.of(chainTask));
        assertThat(taskRevealBlockchainService.canSendBlockchainCommand(args)).isFalse();
        assertThat(output.getOut()).contains("after reveal deadline");
    }

}
