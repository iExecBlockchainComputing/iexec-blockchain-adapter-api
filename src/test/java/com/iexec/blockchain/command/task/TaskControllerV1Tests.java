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

package com.iexec.blockchain.command.task;

import com.iexec.blockchain.api.CommandStatus;
import com.iexec.blockchain.command.task.finalize.TaskFinalizeService;
import com.iexec.blockchain.command.task.initialize.TaskInitializeService;
import com.iexec.common.chain.adapter.args.TaskFinalizeArgs;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskControllerV1Tests {

    private static final String CHAIN_DEAL_ID = "0x1";
    private static final int TASK_INDEX = 0;
    private static final String CHAIN_TASK_ID = "0x2";

    @Mock
    private TaskInitializeService taskInitializeService;
    @Mock
    private TaskFinalizeService taskFinalizeService;
    @InjectMocks
    private TaskControllerV1 taskController;

    // region requestInitializeTask
    @Test
    void shouldNotifyInitializeCommandSubmissionFailure() {
        when(taskInitializeService.start(CHAIN_DEAL_ID, TASK_INDEX)).thenReturn("");
        assertThat(taskController.requestInitializeTask(CHAIN_DEAL_ID, TASK_INDEX))
                .isEqualTo(ResponseEntity.badRequest().build());
    }

    @Test
    void shouldNotifyInitializeCommandSubmissionSuccess() {
        when(taskInitializeService.start(CHAIN_DEAL_ID, TASK_INDEX)).thenReturn(CHAIN_TASK_ID);
        assertThat(taskController.requestInitializeTask(CHAIN_DEAL_ID, TASK_INDEX))
                .isEqualTo(ResponseEntity.ok(CHAIN_TASK_ID));
    }
    // endregion

    // region getStatusForInitializeTaskRequest
    @ParameterizedTest
    @EnumSource(value = CommandStatus.class)
    void shouldReturnInitializeCommandStatusWhenAvailable(CommandStatus status) {
        when(taskInitializeService.getStatusForCommand(CHAIN_TASK_ID)).thenReturn(Optional.of(status));
        assertThat(taskController.getStatusForInitializeTaskRequest(CHAIN_TASK_ID))
                .isEqualTo(ResponseEntity.ok(status));
    }

    @Test
    void shouldNotReturnInitializeCommandStatusWhenEmpty() {
        when(taskInitializeService.getStatusForCommand(CHAIN_TASK_ID)).thenReturn(Optional.empty());
        assertThat(taskController.getStatusForInitializeTaskRequest(CHAIN_TASK_ID))
                .isEqualTo(ResponseEntity.notFound().build());
    }
    // endregion

    // region requestFinalizeTask
    @Test
    void shouldNotifyFinalizeCommandSubmissionFailure() {
        when(taskFinalizeService.start(CHAIN_TASK_ID, null, null)).thenReturn("");
        assertThat(taskController.requestFinalizeTask(CHAIN_TASK_ID, TaskFinalizeArgs.builder().build()))
                .isEqualTo(ResponseEntity.badRequest().build());
    }

    @Test
    void shouldNotifyFinalizeCommandSubmissionSuccess() {
        when(taskFinalizeService.start(CHAIN_TASK_ID, null, null)).thenReturn(CHAIN_TASK_ID);
        assertThat(taskController.requestFinalizeTask(CHAIN_TASK_ID, TaskFinalizeArgs.builder().build()))
                .isEqualTo(ResponseEntity.ok(CHAIN_TASK_ID));
    }
    // endregion

    // region getStatusForFinalizeTaskRequest
    @ParameterizedTest
    @EnumSource(value = CommandStatus.class)
    void shouldReturnFinalizeCommandStatusWhenAvailable(CommandStatus status) {
        when(taskFinalizeService.getStatusForCommand(CHAIN_TASK_ID)).thenReturn(Optional.of(status));
        assertThat(taskController.getStatusForFinalizeTaskRequest(CHAIN_TASK_ID))
                .isEqualTo(ResponseEntity.ok(status));
    }

    @Test
    void shouldNotReturnFinalizeCommandStatusWhenEmpty() {
        when(taskFinalizeService.getStatusForCommand(CHAIN_TASK_ID)).thenReturn(Optional.empty());
        assertThat(taskController.getStatusForFinalizeTaskRequest(CHAIN_TASK_ID))
                .isEqualTo(ResponseEntity.notFound().build());
    }
    // endregion
}
