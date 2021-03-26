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

package com.iexec.blockchain.command.task.reveal;


import com.iexec.blockchain.command.generic.CommandBlockchain;
import com.iexec.blockchain.tool.CredentialsService;
import com.iexec.blockchain.tool.IexecHubService;
import com.iexec.common.chain.ChainContribution;
import com.iexec.common.chain.ChainContributionStatus;
import com.iexec.common.chain.ChainTask;
import com.iexec.common.worker.result.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.util.Date;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

@Slf4j
@Service
public class TaskRevealBlockchainService implements CommandBlockchain<TaskRevealArgs> {

    private final IexecHubService iexecHubService;
    private final CredentialsService credentialsService;

    public TaskRevealBlockchainService(IexecHubService iexecHubService,
                                       CredentialsService credentialsService) {
        this.iexecHubService = iexecHubService;
        this.credentialsService = credentialsService;
    }

    @Override
    public boolean canSendBlockchainCommand(TaskRevealArgs args) {
        String chainTaskId = args.getChainTaskId();
        String resultDigest = args.getResultDigest();
        String workerWallet = credentialsService.getCredentials().getAddress();
        Optional<ChainTask> optionalChainTask = iexecHubService.getChainTask(chainTaskId);
        if (!optionalChainTask.isPresent()) {
            log.error("Task couldn't be retrieved [chainTaskId:{}]", chainTaskId);
            return false;
        }
        ChainTask chainTask = optionalChainTask.get();

        boolean isChainTaskRevealing = iexecHubService.isChainTaskRevealing(chainTask.getStatus());
        boolean isRevealDeadlineReached = chainTask.getRevealDeadline() < new Date().getTime();

        Optional<ChainContribution> optionalContribution = iexecHubService.getChainContribution(chainTaskId, workerWallet);
        if (!optionalContribution.isPresent()) {
            log.error("Contribution couldn't be retrieved [chainTaskId:{}]", chainTaskId);
            return false;
        }
        ChainContribution chainContribution = optionalContribution.get();
        boolean isChainContributionStatusContributed = chainContribution.getStatus().equals(ChainContributionStatus.CONTRIBUTED);
        boolean isContributionResultHashConsensusValue = chainContribution.getResultHash().equals(chainTask.getConsensusValue());

        boolean isContributionResultHashCorrect = false;
        boolean isContributionResultSealCorrect = false;

        if (!resultDigest.isEmpty()) {//TODO
            isContributionResultHashCorrect = chainContribution.getResultHash().equals(ResultUtils.computeResultHash(chainTaskId, resultDigest));

            isContributionResultSealCorrect = chainContribution.getResultSeal().equals(
                    ResultUtils.computeResultSeal(workerWallet, chainTaskId, resultDigest)
            );
        }

        boolean ret = isChainTaskRevealing && !isRevealDeadlineReached &&
                isChainContributionStatusContributed && isContributionResultHashConsensusValue &&
                isContributionResultHashCorrect && isContributionResultSealCorrect;

        if (ret) {
            log.info("All the conditions are valid for the reveal to happen [chainTaskId:{}]", chainTaskId);
        } else {
            log.warn("One or more conditions are not met for the reveal to happen [chainTaskId:{}, " +
                            "isChainTaskRevealing:{}, isRevealDeadlineReached:{}, " +
                            "isChainContributionStatusContributed:{}, isContributionResultHashConsensusValue:{}, " +
                            "isContributionResultHashCorrect:{}, isContributionResultSealCorrect:{}]", chainTaskId,
                    isChainTaskRevealing, isRevealDeadlineReached,
                    isChainContributionStatusContributed, isContributionResultHashConsensusValue,
                    isContributionResultHashCorrect, isContributionResultSealCorrect);
        }

        return ret;
    }

    @Override
    public CompletionStage<TransactionReceipt> sendBlockchainCommand(TaskRevealArgs args) {
        return iexecHubService.reveal(args.getChainTaskId(), args.getResultDigest());
    }

}
