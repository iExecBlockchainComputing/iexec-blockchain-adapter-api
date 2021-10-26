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
        if (optionalChainTask.isEmpty()) {
            logError(chainTaskId, args, "task blockchain read");
            return false;
        }
        ChainTask chainTask = optionalChainTask.get();

        if (!iexecHubService.isChainTaskRevealing(chainTask.getStatus())) {
            logError(chainTaskId, args, "task is not revealing");
            return false;
        }
        if (chainTask.getRevealDeadline() < new Date().getTime()) {
            logError(chainTaskId, args, "after reveal deadline");
            return false;
        }
        Optional<ChainContribution> optionalContribution =
                iexecHubService.getChainContribution(chainTaskId, workerWallet);
        if (optionalContribution.isEmpty()) {
            logError(chainTaskId, args, "contribution blockchain read");
            return false;
        }
        ChainContribution chainContribution = optionalContribution.get();
        if (!chainContribution.getStatus().equals(ChainContributionStatus.CONTRIBUTED)) {
            logError(chainTaskId, args, "task is not contributed");
            return false;
        }
        if (!chainContribution.getResultHash().equals(chainTask.getConsensusValue())) {
            logError(chainTaskId, args, "result hash does not match consensus");
            return false;
        }
        if (!chainContribution.getResultHash()
                .equals(ResultUtils.computeResultHash(chainTaskId, resultDigest))) {
            logError(chainTaskId, args, "result hash does not match contribution");
            return false;
        }
        if (!chainContribution.getResultSeal().equals(
                ResultUtils.computeResultSeal(workerWallet, chainTaskId, resultDigest))) {
            logError(chainTaskId, args, "result seal does not match contribution");
            return false;
        }
        return true;
    }

    private void logError(String chainTaskId, TaskRevealArgs args, String error) {
        log.error("Reveal task blockchain call is likely to revert ({}) " +
                "[chainTaskId:{}, args:{}]", error, chainTaskId, args);
    }

    @Override
    public CompletionStage<TransactionReceipt> sendBlockchainCommand(TaskRevealArgs args) {
        return iexecHubService.reveal(args.getChainTaskId(), args.getResultDigest());
    }

}
