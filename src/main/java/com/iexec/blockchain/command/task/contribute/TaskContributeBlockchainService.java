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

package com.iexec.blockchain.command.task.contribute;


import com.iexec.blockchain.command.generic.CommandBlockchain;
import com.iexec.blockchain.tool.CredentialsService;
import com.iexec.blockchain.tool.IexecHubService;
import com.iexec.common.chain.ChainTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

@Slf4j
@Service
public class TaskContributeBlockchainService implements CommandBlockchain<TaskContributeArgs> {

    private final IexecHubService iexecHubService;
    private final CredentialsService credentialsService;

    public TaskContributeBlockchainService(IexecHubService iexecHubService,
                                           CredentialsService credentialsService) {
        this.iexecHubService = iexecHubService;
        this.credentialsService = credentialsService;
    }

    @Override
    public boolean canSendBlockchainCommand(TaskContributeArgs args) {
        String chainTaskId = args.getChainTaskId();
        String workerWallet = credentialsService.getCredentials().getAddress();
        Optional<ChainTask> optionalChainTask = iexecHubService.getChainTask(chainTaskId);
        if (optionalChainTask.isEmpty()) {
            return false;
        }
        ChainTask chainTask = optionalChainTask.get();

        if (!iexecHubService.hasEnoughStakeToContribute(chainTask.getDealid(), workerWallet)) {
            return false;
        }

        if (!iexecHubService.isChainTaskActive(chainTask.getStatus())) {
            return false;
        }

        if (!iexecHubService.isBeforeContributionDeadlineToContribute(chainTask)) {
            return false;
        }

        return iexecHubService.isContributionUnsetToContribute(chainTaskId, workerWallet);
    }

    @Override
    public CompletionStage<TransactionReceipt> sendBlockchainCommand(TaskContributeArgs args) {
        return iexecHubService.contribute(args.getChainTaskId(),
                args.getResultDigest(),
                args.getWorkerpoolSignature(),
                args.getEnclaveChallenge(),
                args.getEnclaveChallenge());
    }

}
