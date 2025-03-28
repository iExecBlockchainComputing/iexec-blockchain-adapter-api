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

import com.iexec.blockchain.chain.QueueService;
import com.iexec.blockchain.command.generic.CommandEngine;
import com.iexec.blockchain.command.generic.CommandStorage;
import com.iexec.commons.poco.chain.ChainUtils;
import org.springframework.stereotype.Service;

import static com.iexec.blockchain.chain.IexecHubService.isByte32;

@Service
public class TaskInitializeService extends CommandEngine<TaskInitializeArgs> {

    public TaskInitializeService(
            final TaskInitializeBlockchainService blockchainService,
            final CommandStorage updaterService,
            final QueueService queueService) {
        super(blockchainService, updaterService, queueService);
    }

    public String start(final String chainDealId, final int taskIndex) {
        if (!isByte32(chainDealId) || taskIndex < 0) {
            return "";
        }
        final String chainTaskId = ChainUtils.generateChainTaskId(chainDealId, taskIndex);
        return startBlockchainCommand(
                new TaskInitializeArgs(chainTaskId, chainDealId, taskIndex),
                false);
    }

}
