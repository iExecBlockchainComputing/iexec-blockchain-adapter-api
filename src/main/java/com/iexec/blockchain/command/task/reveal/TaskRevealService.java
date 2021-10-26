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


import com.iexec.blockchain.command.generic.CommandEngine;
import com.iexec.blockchain.tool.QueueService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static com.iexec.blockchain.tool.IexecHubService.isByte32;

@Slf4j
@Service
public class TaskRevealService extends CommandEngine<TaskReveal, TaskRevealArgs> {

    public TaskRevealService(
            TaskRevealBlockchainService blockchainService,
            TaskRevealStorageService storageService,
            QueueService queueService) {
        super(blockchainService, storageService, queueService);
    }

    public String start(String chainTaskId,
                        com.iexec.common.chain.adapter.args.TaskRevealArgs args) {
        if (!isByte32(chainTaskId)
                || args == null
                || !isByte32(args.getResultDigest())) {
            log.error("At least one bad args [chainTaskId:{}, args:{}]", chainTaskId, args);
            return "";
        }
        return startBlockchainCommand(new TaskRevealArgs(chainTaskId,
                args.getResultDigest()),
                false);
    }

}
