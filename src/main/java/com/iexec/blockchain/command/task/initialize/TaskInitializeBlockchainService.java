/*
 * Copyright 2020-2025 IEXEC BLOCKCHAIN TECH
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
import com.iexec.blockchain.command.generic.CommandBlockchain;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.exceptions.TransactionException;

import java.io.IOException;

@Slf4j
@Service
public class TaskInitializeBlockchainService implements CommandBlockchain<TaskInitializeArgs> {

    private final IexecHubService iexecHubService;

    public TaskInitializeBlockchainService(final IexecHubService iexecHubService) {
        this.iexecHubService = iexecHubService;
    }

    @Override
    public boolean canSendBlockchainCommand(final TaskInitializeArgs args) {
        String chainTaskId = args.getChainTaskId();
        if (!iexecHubService.hasEnoughGas()) {
            logError(chainTaskId, args, "task is not revealing");
            return false;
        }
        if (!iexecHubService.isTaskInUnsetStatusOnChain(args.getChainTaskId())) {
            logError(chainTaskId, args, "task is not unset");
            return false;
        }
        if (!iexecHubService.isBeforeContributionDeadline(args.getChainDealId())) {
            logError(chainTaskId, args, "after contribution deadline");
            return false;
        }
        return true;
    }

    private void logError(final String chainTaskId, final TaskInitializeArgs args, final String error) {
        log.error("Initialize task blockchain call is likely to revert ({}) [chainTaskId:{}, args:{}]",
                error, chainTaskId, args);
    }

    @Override
    public TransactionReceipt sendBlockchainCommand(final TaskInitializeArgs args) throws IOException, TransactionException {
        return iexecHubService.initializeTask(args.getChainDealId(), args.getTaskIndex());
    }

}
