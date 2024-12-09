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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskInitializeTest {

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

    @Test
    void shouldInitializeTask() {
        TaskInitializeArgs args = getArgs();
        when(blockchainCheckerService.canSendBlockchainCommand(args)).thenReturn(true);
        when(updaterService.updateToReceived(args)).thenReturn(true);

        String chainTaskId = taskInitializeService.start(CHAIN_DEAL_ID, TASK_INDEX);

        Assertions.assertEquals(CHAIN_TASK_ID, chainTaskId);
        verify(queueService).addExecutionToQueue(any(), eq(false));
    }

    @Test
    void shouldNotInitializeTaskSinceCannotOnChain() {
        TaskInitializeArgs args = getArgs();
        when(blockchainCheckerService.canSendBlockchainCommand(args)).thenReturn(false);

        String chainTaskId = taskInitializeService.start(CHAIN_DEAL_ID, TASK_INDEX);

        Assertions.assertTrue(chainTaskId.isEmpty());
        verifyNoInteractions(queueService);
    }

    @Test
    void shouldNotInitializeTaskSinceCannotUpdate() {
        TaskInitializeArgs args = getArgs();
        when(blockchainCheckerService.canSendBlockchainCommand(args)).thenReturn(true);
        when(updaterService.updateToReceived(args)).thenReturn(false);

        String chainTaskId = taskInitializeService.start(CHAIN_DEAL_ID, TASK_INDEX);

        Assertions.assertTrue(chainTaskId.isEmpty());
        verify(queueService, never()).addExecutionToQueue(any(), anyBoolean());
    }

    @Test
    void triggerInitializeTask() throws Exception {
        TransactionReceipt receipt = mock(TransactionReceipt.class);
        TaskInitializeArgs args = getArgs();
        when(updaterService.updateToProcessing(CHAIN_TASK_ID)).thenReturn(true);
        when(blockchainCheckerService.sendBlockchainCommand(args))
                .thenReturn(receipt);

        taskInitializeService.triggerBlockchainCommand(args);
        verify(updaterService, times(1))
                .updateToFinal(CHAIN_TASK_ID, receipt);
    }

    @Test
    void shouldNotTriggerInitializeTaskSinceCannotUpdate() {
        TransactionReceipt receipt = mock(TransactionReceipt.class);
        TaskInitializeArgs args = getArgs();
        when(updaterService.updateToProcessing(CHAIN_TASK_ID)).thenReturn(false);

        taskInitializeService.triggerBlockchainCommand(args);
        verify(updaterService, never()).updateToFinal(CHAIN_TASK_ID, receipt);
        verifyNoInteractions(blockchainCheckerService);
    }

    @Test
    void shouldNotTriggerInitializeTaskSinceReceiptIsNull() throws Exception {
        TaskInitializeArgs args = getArgs();
        when(updaterService.updateToProcessing(CHAIN_TASK_ID)).thenReturn(true);
        when(blockchainCheckerService.sendBlockchainCommand(args))
                .thenReturn(null);

        taskInitializeService.triggerBlockchainCommand(args);
        verify(updaterService)
                .updateToFinal(CHAIN_TASK_ID, null);
    }

    @Test
    void shouldGetStatusForInitializeTaskRequest() {
        when(updaterService.getStatusForCommand(CHAIN_TASK_ID))
                .thenReturn(Optional.of(CommandStatus.PROCESSING));

        Assertions.assertEquals(Optional.of(CommandStatus.PROCESSING),
                taskInitializeService.getStatusForCommand(CHAIN_TASK_ID));
    }

    @Test
    void shouldNotGetStatusForInitializeTaskRequestSinceNoRequest() {
        when(updaterService.getStatusForCommand(CHAIN_TASK_ID))
                .thenReturn(Optional.empty());

        Assertions.assertEquals(Optional.empty(),
                taskInitializeService.getStatusForCommand(CHAIN_TASK_ID));
    }

    private TaskInitializeArgs getArgs() {
        return new TaskInitializeArgs(CHAIN_TASK_ID, CHAIN_DEAL_ID, TASK_INDEX);
    }

}