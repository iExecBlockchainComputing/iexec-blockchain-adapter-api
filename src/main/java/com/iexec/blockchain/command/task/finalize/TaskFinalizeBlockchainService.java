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

package com.iexec.blockchain.command.task.finalize;

import com.iexec.blockchain.chain.IexecHubService;
import com.iexec.blockchain.command.generic.CommandBlockchain;
import com.iexec.commons.poco.chain.ChainTask;
import com.iexec.commons.poco.chain.ChainTaskStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.time.Instant;

@Slf4j
@Service
public class TaskFinalizeBlockchainService implements CommandBlockchain<TaskFinalizeArgs> {

    private final IexecHubService iexecHubService;

    public TaskFinalizeBlockchainService(final IexecHubService iexecHubService) {
        this.iexecHubService = iexecHubService;
    }

    @Override
    public boolean canSendBlockchainCommand(final TaskFinalizeArgs args) {
        final String chainTaskId = args.getChainTaskId();
        final ChainTask chainTask = iexecHubService.getChainTask(chainTaskId).orElse(null);
        if (chainTask == null) {
            logError(chainTaskId, args, "blockchain read");
            return false;
        }
        if (chainTask.getStatus() != ChainTaskStatus.REVEALING) {
            logError(chainTaskId, args, "task is not revealing");
            return false;
        }
        final long now = Instant.now().toEpochMilli();
        if (now >= chainTask.getFinalDeadline()) {
            logError(chainTaskId, args, "after final deadline");
            return false;
        }
        final boolean hasEnoughRevealers = chainTask.getRevealCounter() == chainTask.getWinnerCounter()
                || (chainTask.getRevealCounter() > 0 && chainTask.getRevealDeadline() <= now);
        if (!hasEnoughRevealers) {
            logError(chainTaskId, args, "not enough revealers");
            return false;
        }
        return true;
    }

    private void logError(final String chainTaskId, final TaskFinalizeArgs args, final String error) {
        log.error("Finalize task blockchain call is likely to revert ({}) [chainTaskId:{}, args:{}]",
                error, chainTaskId, args);
    }

    @Override
    public TransactionReceipt sendBlockchainCommand(final TaskFinalizeArgs args) throws Exception {
        return iexecHubService.finalizeTask(args.getChainTaskId(),
                args.getResultLink(),
                args.getCallbackData());
    }

}
