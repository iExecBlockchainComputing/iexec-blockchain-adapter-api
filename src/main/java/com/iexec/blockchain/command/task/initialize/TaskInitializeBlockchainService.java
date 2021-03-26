/*
 * Copyright 2020 IEXEC BLOCKCHAIN TECH
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


import com.iexec.blockchain.command.generic.CommandBlockchain;
import com.iexec.blockchain.tool.IexecHubService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.util.concurrent.CompletionStage;

@Slf4j
@Service
public class TaskInitializeBlockchainService implements CommandBlockchain<TaskInitializeArgs> {

    private final IexecHubService iexecHubService;

    public TaskInitializeBlockchainService(IexecHubService iexecHubService) {
        this.iexecHubService = iexecHubService;
    }

    @Override
    public boolean canSendBlockchainCommand(TaskInitializeArgs args) {
        boolean hasEnoughGas =
                iexecHubService.hasEnoughGas();
        boolean isTaskUnsetOnChain =
                iexecHubService.isTaskInUnsetStatusOnChain(args.getChainTaskId());
        boolean isBeforeContributionDeadline =
                iexecHubService.isBeforeContributionDeadline(args.getChainDealId());

        if (!hasEnoughGas || !isTaskUnsetOnChain || !isBeforeContributionDeadline) {
            log.error("Cannot initialize task [chainDealId:{}, taskIndex:{}, chainTaskId:{}, " +
                            "hasEnoughGas:{}, isTaskUnsetOnChain:{}, isBeforeContributionDeadline:{}]",
                    args.getChainDealId(), args.getTaskIndex(), args.getChainTaskId(),
                    hasEnoughGas, isTaskUnsetOnChain, isBeforeContributionDeadline);
            //TODO eventually return cause
            return false;
        }
        return true;
    }

    @Override
    public CompletionStage<TransactionReceipt> sendBlockchainCommand(TaskInitializeArgs args) {
        return iexecHubService.initializeTask(args.getChainDealId(), args.getTaskIndex());
    }

}
