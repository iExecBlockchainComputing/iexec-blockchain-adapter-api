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

package com.iexec.blockchain.command.task.finalize;

import com.iexec.blockchain.api.CommandStatus;
import com.iexec.blockchain.chain.QueueService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.util.Optional;

import static com.iexec.commons.poco.utils.BytesUtils.EMPTY_ADDRESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskFinalizeServiceTests {

    private static final String CHAIN_TASK_ID =
            "0xe90fc4654b5ea32ad8689091e7610cad7ee5c8b9b1a6e39401b57d90343bfcaa";
    private static final String RESULT_LINK = "/ipfs/QmeQHGKFAkEkA5tm3kuXqBM9zz9JorkvCsAJ2bzAAh6NX4";

    @InjectMocks
    private TaskFinalizeService taskFinalizeService;
    @Mock
    private TaskFinalizeBlockchainService blockchainService;
    @Mock
    private TaskFinalizeStorageService updaterService;
    @Mock
    private QueueService queueService;

    private final TaskFinalizeArgs args = new TaskFinalizeArgs(CHAIN_TASK_ID, RESULT_LINK, EMPTY_ADDRESS);

    // region start
    @Test
    void shouldFinalizeTask() {
        when(blockchainService.canSendBlockchainCommand(args)).thenReturn(true);
        when(updaterService.updateToReceived(args)).thenReturn(true);
        final String chainTaskId = taskFinalizeService.start(CHAIN_TASK_ID, RESULT_LINK, EMPTY_ADDRESS);
        assertThat(chainTaskId).isEqualTo(CHAIN_TASK_ID);
        verify(queueService).addExecutionToQueue(any(Runnable.class), eq(true));
    }

    @Test
    void shouldNotFinalizeTaskSinceCannotOnChain() {
        when(blockchainService.canSendBlockchainCommand(args)).thenReturn(false);

        final String chainTaskId = taskFinalizeService.start(CHAIN_TASK_ID, RESULT_LINK, EMPTY_ADDRESS);

        assertThat(chainTaskId).isEmpty();
        verifyNoInteractions(queueService);
    }

    @Test
    void shouldNotInitializeTaskSinceCannotUpdate() {
        when(blockchainService.canSendBlockchainCommand(args)).thenReturn(true);
        when(updaterService.updateToReceived(args)).thenReturn(false);

        final String chainTaskId = taskFinalizeService.start(CHAIN_TASK_ID, RESULT_LINK, EMPTY_ADDRESS);

        assertThat(chainTaskId).isEmpty();
        verifyNoInteractions(queueService);
    }
    // endregion

    // region triggerBlockchainCommand
    @Test
    void triggerInitializeTask() throws Exception {
        final TransactionReceipt receipt = mock(TransactionReceipt.class);
        when(updaterService.updateToProcessing(CHAIN_TASK_ID)).thenReturn(true);
        when(blockchainService.sendBlockchainCommand(args)).thenReturn(receipt);

        taskFinalizeService.triggerBlockchainCommand(args);
        verify(updaterService).updateToFinal(CHAIN_TASK_ID, receipt);
    }

    @Test
    void shouldNotTriggerInitializeTaskSinceCannotUpdate() {
        final TransactionReceipt receipt = mock(TransactionReceipt.class);
        when(updaterService.updateToProcessing(CHAIN_TASK_ID)).thenReturn(false);

        taskFinalizeService.triggerBlockchainCommand(args);
        verify(updaterService, never()).updateToFinal(CHAIN_TASK_ID, receipt);
        verifyNoInteractions(blockchainService);
    }

    @Test
    void shouldNotTriggerInitializeTaskSinceReceiptIsNull() throws Exception {
        when(updaterService.updateToProcessing(CHAIN_TASK_ID)).thenReturn(true);
        when(blockchainService.sendBlockchainCommand(args)).thenReturn(null);

        taskFinalizeService.triggerBlockchainCommand(args);
        verify(updaterService).updateToFinal(CHAIN_TASK_ID, null);
    }
    // endregion

    // region getStatusForCommand
    @Test
    void shouldGetStatusForInitializeTaskRequest() {
        when(updaterService.getStatusForCommand(CHAIN_TASK_ID)).thenReturn(Optional.of(CommandStatus.PROCESSING));
        assertThat(taskFinalizeService.getStatusForCommand(CHAIN_TASK_ID)).contains(CommandStatus.PROCESSING);
    }

    @Test
    void shouldNotGetStatusForInitializeTaskRequestSinceNoRequest() {
        when(updaterService.getStatusForCommand(CHAIN_TASK_ID)).thenReturn(Optional.empty());
        assertThat(taskFinalizeService.getStatusForCommand(CHAIN_TASK_ID)).isEmpty();
    }
    // endregion

}
