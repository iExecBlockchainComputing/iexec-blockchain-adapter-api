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

package com.iexec.blockchain.command.task.finalize;


import com.iexec.blockchain.command.generic.CommandBlockchain;
import com.iexec.blockchain.tool.IexecHubService;
import com.iexec.common.chain.ChainTask;
import com.iexec.common.chain.ChainTaskStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

import static com.iexec.common.utils.DateTimeUtils.now;

@Slf4j
@Service
public class TaskFinalizeBlockchainService implements CommandBlockchain<TaskFinalizeArgs> {

    private final IexecHubService iexecHubService;

    public TaskFinalizeBlockchainService(IexecHubService iexecHubService) {
        this.iexecHubService = iexecHubService;
    }

    @Override
    public boolean canSendBlockchainCommand(TaskFinalizeArgs args) {
        String chainTaskId = args.getChainTaskId();

        Optional<ChainTask> optional = iexecHubService.getChainTask(chainTaskId);
        if (!optional.isPresent()) {
            return false;
        }
        ChainTask chainTask = optional.get();

        boolean isChainTaskStatusRevealing = chainTask.getStatus().equals(ChainTaskStatus.REVEALING);
        boolean isFinalDeadlineInFuture = now() < chainTask.getFinalDeadline();
        boolean hasEnoughRevealors = (chainTask.getRevealCounter() == chainTask.getWinnerCounter())
                || (chainTask.getRevealCounter() > 0 && chainTask.getRevealDeadline() <= now());

        boolean ret = isChainTaskStatusRevealing && isFinalDeadlineInFuture && hasEnoughRevealors;
        if (ret) {
            log.info("Finalizable onchain [chainTaskId:{}]", chainTaskId);
        } else {
            log.warn("Can't finalize [chainTaskId:{}, " +
                            "isChainTaskStatusRevealing:{}, " +
                            "isFinalDeadlineInFuture:{}, " +
                            "hasEnoughRevealors:{}]",
                    chainTaskId,
                    isChainTaskStatusRevealing,
                    isFinalDeadlineInFuture,
                    hasEnoughRevealors);
        }
        return ret;
    }


    @Override
    public CompletionStage<TransactionReceipt> sendBlockchainCommand(TaskFinalizeArgs args) {
        return iexecHubService.finalize(args.getChainTaskId(),
                args.getResultLink(),
                args.getCallbackData());
    }

}
