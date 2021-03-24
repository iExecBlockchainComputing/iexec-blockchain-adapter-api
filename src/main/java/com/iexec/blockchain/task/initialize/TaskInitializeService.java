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

package com.iexec.blockchain.task.initialize;


import com.iexec.blockchain.tool.IexecHubService;
import com.iexec.blockchain.tool.QueueService;
import com.iexec.blockchain.tool.Status;
import com.iexec.common.chain.ChainUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class TaskInitializeService {

    private final TaskInitializeBlockchainCheckerService blockchainCheckerService;
    private final IexecHubService iexecHubService;
    private final TaskInitializeUpdaterService updaterService;
    private final TaskInitializeRepository repository;
    private final QueueService queueService;

    public TaskInitializeService(
            TaskInitializeBlockchainCheckerService blockchainCheckerService,
            IexecHubService iexecHubService,
            TaskInitializeUpdaterService updaterService,
            TaskInitializeRepository repository,
            QueueService queueService) {
        this.blockchainCheckerService = blockchainCheckerService;
        this.iexecHubService = iexecHubService;
        this.updaterService = updaterService;
        this.repository = repository;
        this.queueService = queueService;
    }

    /**
     * Start initialize task process. Request is synchronously updated to
     * received, then rest of the workflow is done asynchronously
     *
     * @param chainDealId blockchain ID of the deal
     * @param taskIndex   index of the task int the bag
     * @return blockchain ID of the task
     */
    public String initializeTask(String chainDealId, int taskIndex) {
        String chainTaskId = ChainUtils.generateChainTaskId(chainDealId,
                taskIndex);

        if (!blockchainCheckerService.canInitializeTask(chainDealId, taskIndex,
                chainTaskId)) {
            log.error("Cannot initialize task " +
                            "[chainDealId:{}, taskIndex:{}, chainTaskId:{}]",
                    chainDealId, taskIndex, chainTaskId);
            return "";
        }

        if (!updaterService.setReceived(chainDealId, taskIndex, chainTaskId)) {
            log.error("Cannot set received for initialize task " +
                            "[chainDealId:{}, taskIndex:{}, chainTaskId:{}]",
                    chainDealId, taskIndex, chainTaskId);
            return "";
        }

        Runnable runnable = () ->
                triggerInitializeTask(chainDealId, taskIndex, chainTaskId);
        queueService.runAsync(runnable);
        return chainTaskId;
    }

    /**
     * Trigger initialize task process by :
     * - firing the corresponding blockchain transaction
     * - performing local updates
     *
     * @param chainDealId blockchain ID of the deal
     * @param taskIndex   index of the task int the bag
     * @param chainTaskId blockchain ID of the task
     */
    void triggerInitializeTask(String chainDealId,
                               int taskIndex,
                               String chainTaskId) {
        if (!updaterService.setProcessing(chainTaskId)) {
            log.error("Cannot set processing for triggerInitializeTask " +
                    "[chainTaskId:{}]", chainTaskId);
            return;
        }
        iexecHubService.initializeTask(chainDealId, taskIndex)
                .thenAccept(receipt -> {
                    if (receipt == null) {
                        log.error("Cannot set final for triggerInitializeTask " +
                                "[chainTaskId:{}]", chainTaskId);
                        return;
                    }
                    updaterService.setFinal(chainTaskId, receipt);
                });
    }

    /**
     * Get status for the initialize task process (which is async)
     *
     * @param chainTaskId blockchain ID of the task
     * @return status of the initialize task process
     */
    public Optional<Status> getStatusForInitializeTaskRequest(String chainTaskId) {
        Status status = repository.findByChainTaskId(chainTaskId)
                .map(TaskInitialize::getStatus)
                .orElse(null);
        if (status != null) {
            return Optional.of(status);
        }
        return Optional.empty();
    }
}
