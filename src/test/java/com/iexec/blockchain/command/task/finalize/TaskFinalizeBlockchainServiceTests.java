/*
 * Copyright 2022-2024 IEXEC BLOCKCHAIN TECH
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

import com.iexec.blockchain.tool.IexecHubService;
import com.iexec.commons.poco.chain.ChainTask;
import com.iexec.commons.poco.chain.ChainTaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(OutputCaptureExtension.class)
class TaskFinalizeBlockchainServiceTests {

    private static final String CHAIN_TASK_ID = "chainTaskId";
    private static final long TIME_INTERVAL_IN_MS = 100L;

    @Mock
    private IexecHubService iexecHubService;
    @InjectMocks
    private TaskFinalizeBlockchainService taskFinalizeBlockchainService;

    @BeforeEach
    public void init() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void canNotSendCommandWhenNoTask(CapturedOutput output) {
        TaskFinalizeArgs args = new TaskFinalizeArgs(CHAIN_TASK_ID, "resultLink", "callbackData");
        when(iexecHubService.getChainTask(CHAIN_TASK_ID)).thenReturn(Optional.empty());
        assertThat(taskFinalizeBlockchainService.canSendBlockchainCommand(args)).isFalse();
        assertThat(output.getOut()).contains("blockchain read");
    }

    @Test
    void canNotSendCommandWhenTaskNotRevealing(CapturedOutput output) {
        TaskFinalizeArgs args = new TaskFinalizeArgs(CHAIN_TASK_ID, "resultLink", "callbackData");
        ChainTask chainTask = ChainTask.builder().status(ChainTaskStatus.ACTIVE).build();
        when(iexecHubService.getChainTask(CHAIN_TASK_ID)).thenReturn(Optional.of(chainTask));
        assertThat(taskFinalizeBlockchainService.canSendBlockchainCommand(args)).isFalse();
        assertThat(output.getOut()).contains("task is not revealing");
    }

    @Test
    void canNotSendCommandWhenTaskFinalDeadlineReached(CapturedOutput output) {
        TaskFinalizeArgs args = new TaskFinalizeArgs(CHAIN_TASK_ID, "resultLink", "callbackData");
        ChainTask chainTask = ChainTask.builder()
                .status(ChainTaskStatus.REVEALING)
                .finalDeadline(Instant.now().toEpochMilli())
                .build();
        when(iexecHubService.getChainTask(CHAIN_TASK_ID)).thenReturn(Optional.of(chainTask));
        assertThat(taskFinalizeBlockchainService.canSendBlockchainCommand(args)).isFalse();
        assertThat(output.getOut()).contains("after final deadline");
    }

    @Test
    void canNotSendCommandWhenNotEnoughReveals(CapturedOutput output) {
        TaskFinalizeArgs args = new TaskFinalizeArgs(CHAIN_TASK_ID, "resultLink", "callbackData");
        ChainTask chainTask = ChainTask.builder()
                .status(ChainTaskStatus.REVEALING)
                .revealCounter(1)
                .winnerCounter(2)
                .revealDeadline(Instant.now().plus(TIME_INTERVAL_IN_MS, ChronoUnit.MILLIS).toEpochMilli())
                .finalDeadline(Instant.now().plus(TIME_INTERVAL_IN_MS, ChronoUnit.MILLIS).toEpochMilli())
                .build();
        when(iexecHubService.getChainTask(CHAIN_TASK_ID)).thenReturn(Optional.of(chainTask));
        assertThat(taskFinalizeBlockchainService.canSendBlockchainCommand(args)).isFalse();
        assertThat(output.getOut()).contains("not enough revealers");
    }

    @Test
    void canSendCommandWhenRevealDeadlineReached() {
        TaskFinalizeArgs args = new TaskFinalizeArgs(CHAIN_TASK_ID, "resultLink", "callbackData");
        ChainTask chainTask = ChainTask.builder()
                .status(ChainTaskStatus.REVEALING)
                .revealCounter(1)
                .winnerCounter(2)
                .revealDeadline(Instant.now().minus(TIME_INTERVAL_IN_MS, ChronoUnit.MILLIS).toEpochMilli())
                .finalDeadline(Instant.now().plus(TIME_INTERVAL_IN_MS, ChronoUnit.MILLIS).toEpochMilli())
                .build();
        when(iexecHubService.getChainTask(CHAIN_TASK_ID)).thenReturn(Optional.of(chainTask));
        assertThat(taskFinalizeBlockchainService.canSendBlockchainCommand(args)).isTrue();
    }

    @Test
    void canSendBlockchainCommandWhenAllWinnersRevealed() {
        TaskFinalizeArgs args = new TaskFinalizeArgs(CHAIN_TASK_ID, "resultLink", "callbackData");
        ChainTask chainTask = ChainTask.builder()
                .status(ChainTaskStatus.REVEALING)
                .revealCounter(1)
                .winnerCounter(1)
                .revealDeadline(Instant.now().plus(TIME_INTERVAL_IN_MS, ChronoUnit.MILLIS).toEpochMilli())
                .finalDeadline(Instant.now().plus(TIME_INTERVAL_IN_MS, ChronoUnit.MILLIS).toEpochMilli())
                .build();
        when(iexecHubService.getChainTask(CHAIN_TASK_ID)).thenReturn(Optional.of(chainTask));
        assertThat(taskFinalizeBlockchainService.canSendBlockchainCommand(args)).isTrue();
    }

}
