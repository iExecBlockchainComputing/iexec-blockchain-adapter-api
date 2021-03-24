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


import com.iexec.blockchain.tool.Status;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
public class TaskInitializeUpdaterService {

    private final TaskInitializeRepository taskInitializeRepository;

    public TaskInitializeUpdaterService(TaskInitializeRepository taskInitializeRepository) {
        this.taskInitializeRepository = taskInitializeRepository;
    }

    public boolean setReceived(String chainDealId, int taskIndex, String chainTaskId) {
        if (!StringUtils.hasText(chainDealId) || taskIndex < 0) {
            return false;
        }

        if (taskInitializeRepository.findByChainTaskId(chainTaskId).isPresent()) {
            log.warn("Task already locally created for initialize task" +
                    " [chainTaskId:{}]", chainTaskId);
            return false;
        }

        TaskInitialize taskInitialize = TaskInitialize.builder()
                .status(Status.RECEIVED)
                .chainTaskId(chainTaskId)
                .chainDealId(chainDealId)
                .taskIndex(taskIndex)
                .creationDate(Instant.now())
                .build();
        taskInitializeRepository.save(taskInitialize);
        log.info("Locally created task for initialize task [chainTaskId:{}]",
                chainTaskId);
        return true;
    }

    public boolean setProcessing(String chainTaskId) {
        Optional<TaskInitialize> localTask = taskInitializeRepository.findByChainTaskId(chainTaskId)
                .filter(taskInitialize -> taskInitialize.getStatus() != null)
                .filter(taskInitialize -> taskInitialize.getStatus() == Status.RECEIVED);
        if (localTask.isEmpty()) {
            log.error("Cannot set processing state for initialize task " +
                    "[chainTaskId:{}]", chainTaskId);
            return false;
        }
        TaskInitialize taskInitialize = localTask.get();
        taskInitialize.setStatus(Status.PROCESSING);
        taskInitialize.setProcessingDate(Instant.now());
        taskInitializeRepository.save(taskInitialize);
        log.info("Processing initialize task [chainTaskId:{}]", chainTaskId);
        return true;
    }

    public void setFinal(String chainTaskId, TransactionReceipt receipt) {
        Optional<TaskInitialize> localTask = taskInitializeRepository.findByChainTaskId(chainTaskId)
                .filter(taskInitialize -> taskInitialize.getStatus() != null)
                .filter(taskInitialize -> taskInitialize.getStatus() == Status.PROCESSING);
        if (localTask.isEmpty()) {
            log.error("Cannot set final state for initialize task " +
                    "[chainTaskId:{}]", chainTaskId);
            return;
        }
        TaskInitialize taskInitialize = localTask.get();

        Status status;
        if (StringUtils.hasText(receipt.getStatus())
                && receipt.getStatus().equals("0x1")) {
            status = Status.SUCCESS;
            log.info("Initialized task [chainTaskId:{}, receipt:{}]",
                    chainTaskId, receipt.toString());
        } else {
            status = Status.FAILURE;
            log.info("Failure after initialize task transaction " +
                    "[chainTaskId:{}, receipt:{}]", chainTaskId, receipt.toString());
        }
        taskInitialize.setTransactionReceipt(receipt);
        taskInitialize.setStatus(status);
        taskInitialize.setProcessingDate(Instant.now());
        taskInitializeRepository.save(taskInitialize);
    }


}
