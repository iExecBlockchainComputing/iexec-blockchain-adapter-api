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
import com.iexec.common.chain.ChainTaskStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.util.Optional;

import static com.iexec.common.utils.DateTimeUtils.now;

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
            logError(chainTaskId, args, "blockchain read");
            return false;
        }
        ChainTask chainTask = optionalChainTask.get();

        if (chainTask.getStatus() != ChainTaskStatus.ACTIVE) {
            logError(chainTaskId, args, "task is not active");
            return false;
        }
        if (now() >= chainTask.getContributionDeadline()) {
            logError(chainTaskId, args, "after contribution deadline");
            return false;
        }
        if (!iexecHubService.hasEnoughStakeToContribute(chainTask.getDealid(), workerWallet)) {
            logError(chainTaskId, args, "stake too low");
            return false;
        }
        if (!iexecHubService.isContributionUnsetToContribute(chainTaskId, workerWallet)) {
            logError(chainTaskId, args, "contribution already set");
            return false;
        }
        return true;
    }

    private void logError(String chainTaskId, TaskContributeArgs args, String error) {
        log.error("Contribute task blockchain call is likely to revert ({}) " +
                "[chainTaskId:{}, args:{}]", error, chainTaskId, args);
    }

    @Override
    public TransactionReceipt sendBlockchainCommand(TaskContributeArgs args) throws Exception {
        return iexecHubService.contribute(args.getChainTaskId(),
                args.getResultDigest(),
                args.getWorkerpoolSignature(),
                args.getEnclaveChallenge(),
                args.getEnclaveChallenge());
    }

}
