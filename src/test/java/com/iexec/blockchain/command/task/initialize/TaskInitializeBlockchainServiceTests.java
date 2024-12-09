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

import com.iexec.blockchain.chain.IexecHubService;
import com.iexec.commons.poco.chain.ChainUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskInitializeBlockchainServiceTests {

    public static final String CHAIN_DEAL_ID =
            "0x000000000000000000000000000000000000000000000000000000000000dea1";
    public static final int TASK_INDEX = 0;
    public static final String CHAIN_TASK_ID =
            ChainUtils.generateChainTaskId(CHAIN_DEAL_ID, TASK_INDEX);

    @InjectMocks
    private TaskInitializeBlockchainService checkerService;
    @Mock
    private IexecHubService iexecHubService;

    @Test
    void canSendBlockchainCommand() {
        TaskInitializeArgs args = getArgs();
        when(iexecHubService.hasEnoughGas())
                .thenReturn(true);
        when(iexecHubService.isTaskInUnsetStatusOnChain(CHAIN_TASK_ID))
                .thenReturn(true);
        when(iexecHubService.isBeforeContributionDeadline(CHAIN_DEAL_ID))
                .thenReturn(true);

        Assertions.assertTrue(checkerService.canSendBlockchainCommand(args));
    }

    @Test
    void cannotInitializeTaskSinceNotEnoughGas() {
        TaskInitializeArgs args = getArgs();
        when(iexecHubService.hasEnoughGas())
                .thenReturn(false);

        Assertions.assertFalse(checkerService.canSendBlockchainCommand(args));
    }

    @Test
    void cannotInitializeTaskSinceNotUnset() {
        TaskInitializeArgs args = getArgs();
        when(iexecHubService.hasEnoughGas())
                .thenReturn(true);
        when(iexecHubService.isTaskInUnsetStatusOnChain(CHAIN_TASK_ID))
                .thenReturn(false);

        Assertions.assertFalse(checkerService.canSendBlockchainCommand(args));
    }

    @Test
    void cannotInitializeTaskSinceNotBeforeDeadline() {
        TaskInitializeArgs args = getArgs();
        when(iexecHubService.hasEnoughGas())
                .thenReturn(true);
        when(iexecHubService.isTaskInUnsetStatusOnChain(CHAIN_TASK_ID))
                .thenReturn(true);
        when(iexecHubService.isBeforeContributionDeadline(CHAIN_DEAL_ID))
                .thenReturn(false);

        Assertions.assertFalse(checkerService.canSendBlockchainCommand(args));
    }

    private TaskInitializeArgs getArgs() {
        return new TaskInitializeArgs(CHAIN_TASK_ID, CHAIN_DEAL_ID, TASK_INDEX);
    }
}