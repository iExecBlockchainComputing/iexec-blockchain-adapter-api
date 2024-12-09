/*
 * Copyright 2021-2024 IEXEC BLOCKCHAIN TECH
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

package com.iexec.blockchain.command.task.initialize;

import com.iexec.blockchain.api.CommandStatus;
import com.iexec.blockchain.chain.QueueService;
import com.iexec.commons.poco.chain.ChainUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskInitializeServiceTests {

    public static final String CHAIN_DEAL_ID =
            "0x000000000000000000000000000000000000000000000000000000000000dea1";
    public static final int TASK_INDEX = 0;
    public static final String CHAIN_TASK_ID =
            ChainUtils.generateChainTaskId(CHAIN_DEAL_ID, TASK_INDEX);

    @InjectMocks
    private TaskInitializeService taskInitializeService;
    @Mock
    private TaskInitializeBlockchainService blockchainCheckerService;
    @Mock
    private TaskInitializeStorageService updaterService;
    @Mock
    private QueueService queueService;

    private final TaskInitializeArgs args = new TaskInitializeArgs(CHAIN_TASK_ID, CHAIN_DEAL_ID, TASK_INDEX);

    // region start
    @Test
    void shouldInitializeTask() {
        when(blockchainCheckerService.canSendBlockchainCommand(args)).thenReturn(true);
        when(updaterService.updateToReceived(args)).thenReturn(true);

        final String chainTaskId = taskInitializeService.start(CHAIN_DEAL_ID, TASK_INDEX);

        assertThat(chainTaskId).isEqualTo(CHAIN_TASK_ID);
        verify(queueService).addExecutionToQueue(any(Runnable.class), eq(false));
    }

    @Test
    void shouldNotInitializeTaskSinceCannotOnChain() {
        when(blockchainCheckerService.canSendBlockchainCommand(args)).thenReturn(false);

        final String chainTaskId = taskInitializeService.start(CHAIN_DEAL_ID, TASK_INDEX);

        assertThat(chainTaskId).isEmpty();
        verifyNoInteractions(queueService);
    }

    @Test
    void shouldNotInitializeTaskSinceCannotUpdate() {
        when(blockchainCheckerService.canSendBlockchainCommand(args)).thenReturn(true);
        when(updaterService.updateToReceived(args)).thenReturn(false);

        final String chainTaskId = taskInitializeService.start(CHAIN_DEAL_ID, TASK_INDEX);

        assertThat(chainTaskId).isEmpty();
        verifyNoInteractions(queueService);
    }
    // endregion

    // region triggerBlockchainCommand
    @Test
    void triggerInitializeTask() throws Exception {
        final TransactionReceipt receipt = mock(TransactionReceipt.class);
        when(updaterService.updateToProcessing(CHAIN_TASK_ID)).thenReturn(true);
        when(blockchainCheckerService.sendBlockchainCommand(args)).thenReturn(receipt);

        taskInitializeService.triggerBlockchainCommand(args);
        verify(updaterService).updateToFinal(CHAIN_TASK_ID, receipt);
    }

    @Test
    void shouldNotTriggerInitializeTaskSinceCannotUpdate() {
        final TransactionReceipt receipt = mock(TransactionReceipt.class);
        when(updaterService.updateToProcessing(CHAIN_TASK_ID)).thenReturn(false);

        taskInitializeService.triggerBlockchainCommand(args);
        verify(updaterService, never()).updateToFinal(CHAIN_TASK_ID, receipt);
        verifyNoInteractions(blockchainCheckerService);
    }

    @Test
    void shouldNotTriggerInitializeTaskSinceReceiptIsNull() throws Exception {
        when(updaterService.updateToProcessing(CHAIN_TASK_ID)).thenReturn(true);
        when(blockchainCheckerService.sendBlockchainCommand(args)).thenReturn(null);

        taskInitializeService.triggerBlockchainCommand(args);
        verify(updaterService).updateToFinal(CHAIN_TASK_ID, null);
    }
    // endregion

    // region getStatusForCommand
    @Test
    void shouldGetStatusForInitializeTaskRequest() {
        when(updaterService.getStatusForCommand(CHAIN_TASK_ID)).thenReturn(Optional.of(CommandStatus.PROCESSING));
        assertThat(taskInitializeService.getStatusForCommand(CHAIN_TASK_ID)).contains(CommandStatus.PROCESSING);
    }

    @Test
    void shouldNotGetStatusForInitializeTaskRequestSinceNoRequest() {
        when(updaterService.getStatusForCommand(CHAIN_TASK_ID)).thenReturn(Optional.empty());
        assertThat(taskInitializeService.getStatusForCommand(CHAIN_TASK_ID)).isEmpty();
    }
    // endregion

}
