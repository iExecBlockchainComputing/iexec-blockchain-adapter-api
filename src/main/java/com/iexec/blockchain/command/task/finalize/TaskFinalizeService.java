/*
 * Copyright 2020-2024 IEXEC BLOCKCHAIN TECH
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

import com.iexec.blockchain.chain.QueueService;
import com.iexec.blockchain.command.generic.CommandEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static com.iexec.blockchain.chain.IexecHubService.isByte32;

@Slf4j
@Service
public class TaskFinalizeService extends CommandEngine<TaskFinalize, TaskFinalizeArgs> {

    public TaskFinalizeService(
            TaskFinalizeBlockchainService blockchainService,
            TaskFinalizeStorageService storageService,
            QueueService queueService) {
        super(blockchainService, storageService, queueService);
    }

    public String start(String chainTaskId,
                        com.iexec.common.chain.adapter.args.TaskFinalizeArgs args) {
        if (!isByte32(chainTaskId)
                || args == null) {
            log.error("At least one bad args [chainTaskId:{}, args:{}]", chainTaskId, args);
            return "";
        }
        return startBlockchainCommand(
                new TaskFinalizeArgs(chainTaskId, args.getResultLink(), args.getCallbackData()),
                true);
    }

}
