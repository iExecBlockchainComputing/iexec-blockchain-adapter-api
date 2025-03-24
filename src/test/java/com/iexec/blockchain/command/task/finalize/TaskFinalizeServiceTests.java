/*
 * Copyright 2024-2025 IEXEC BLOCKCHAIN TECH
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
import com.iexec.blockchain.command.generic.CommandName;
import com.iexec.blockchain.command.generic.CommandStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.util.Optional;
import java.util.stream.Stream;

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
    private CommandStorage updaterService;
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

    @ParameterizedTest
    @MethodSource("provideTaskFinalizeBadParameters")
    void shouldNotFinalizeTaskWithBadParameters(final String chainTaskId, final String resultLink) {
        assertThat(taskFinalizeService.start(chainTaskId, resultLink, EMPTY_ADDRESS)).isEmpty();
    }

    private static Stream<Arguments> provideTaskFinalizeBadParameters() {
        return Stream.of(
                Arguments.of("not-a-task", RESULT_LINK),
                Arguments.of(CHAIN_TASK_ID, null)
        );
    }

    @Test
    void shouldNotFinalizeTaskSinceCannotOnChain() {
        when(blockchainService.canSendBlockchainCommand(args)).thenReturn(false);

        final String chainTaskId = taskFinalizeService.start(CHAIN_TASK_ID, RESULT_LINK, EMPTY_ADDRESS);

        assertThat(chainTaskId).isEmpty();
        verifyNoInteractions(queueService);
    }

    @Test
    void shouldNotFinalizeTaskSinceCannotUpdate() {
        when(blockchainService.canSendBlockchainCommand(args)).thenReturn(true);
        when(updaterService.updateToReceived(args)).thenReturn(false);

        final String chainTaskId = taskFinalizeService.start(CHAIN_TASK_ID, RESULT_LINK, EMPTY_ADDRESS);

        assertThat(chainTaskId).isEmpty();
        verifyNoInteractions(queueService);
    }
    // endregion

    // region triggerBlockchainCommand
    @Test
    void triggerFinalizeTask() throws Exception {
        final TransactionReceipt receipt = mock(TransactionReceipt.class);
        when(updaterService.updateToProcessing(args)).thenReturn(true);
        when(blockchainService.sendBlockchainCommand(args)).thenReturn(receipt);

        taskFinalizeService.triggerBlockchainCommand(args);
        verify(updaterService).updateToFinal(args, receipt);
    }

    @Test
    void shouldNotTriggerFinalizeTaskSinceCannotUpdate() {
        final TransactionReceipt receipt = mock(TransactionReceipt.class);
        when(updaterService.updateToProcessing(args)).thenReturn(false);

        taskFinalizeService.triggerBlockchainCommand(args);
        verify(updaterService, never()).updateToFinal(args, receipt);
        verifyNoInteractions(blockchainService);
    }

    @Test
    void shouldNotTriggerFinalizeTaskSinceReceiptIsNull() throws Exception {
        when(updaterService.updateToProcessing(args)).thenReturn(true);
        when(blockchainService.sendBlockchainCommand(args)).thenReturn(null);

        taskFinalizeService.triggerBlockchainCommand(args);
        verify(updaterService).updateToFinal(args, null);
    }
    // endregion

    // region getStatusForCommand
    @Test
    void shouldGetStatusForFinalizeTaskRequest() {
        when(updaterService.getStatusForCommand(CHAIN_TASK_ID, CommandName.TASK_FINALIZE)).thenReturn(Optional.of(CommandStatus.PROCESSING));
        assertThat(taskFinalizeService.getStatusForCommand(CHAIN_TASK_ID, CommandName.TASK_FINALIZE)).contains(CommandStatus.PROCESSING);
    }

    @Test
    void shouldNotGetStatusForFinalizeTaskRequestSinceNoRequest() {
        when(updaterService.getStatusForCommand(CHAIN_TASK_ID, CommandName.TASK_FINALIZE)).thenReturn(Optional.empty());
        assertThat(taskFinalizeService.getStatusForCommand(CHAIN_TASK_ID, CommandName.TASK_FINALIZE)).isEmpty();
    }
    // endregion

}
