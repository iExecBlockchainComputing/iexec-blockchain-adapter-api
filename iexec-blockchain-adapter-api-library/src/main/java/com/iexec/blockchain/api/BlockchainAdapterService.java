/*
 * Copyright 2021-2023 IEXEC BLOCKCHAIN TECH
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

package com.iexec.blockchain.api;

import com.iexec.common.chain.adapter.args.TaskFinalizeArgs;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;
import java.util.function.Function;

@Slf4j
public class BlockchainAdapterService {

    private final BlockchainAdapterApiClient apiClient;
    private final long period;
    private final int maxAttempts;

    public BlockchainAdapterService(BlockchainAdapterApiClient apiClient, long period, int maxAttempts) {
        this.apiClient = apiClient;
        this.period = period;
        this.maxAttempts = maxAttempts;
    }

    // region initialize

    /**
     * Request on-chain initialization of the task.
     *
     * @param chainDealId ID of the deal
     * @param taskIndex   index of the task in the deal
     * @return chain task ID is initialization is properly requested
     */
    public Optional<String> requestInitialize(String chainDealId, int taskIndex) {
        try {
            String chainTaskId = apiClient.requestInitializeTask(chainDealId, taskIndex);
            if (!StringUtils.isEmpty(chainTaskId)) {
                log.info("Requested initialize [chainTaskId:{}, chainDealId:{}, taskIndex:{}]",
                        chainTaskId, chainDealId, taskIndex);
                return Optional.of(chainTaskId);
            }
        } catch (Exception e) {
            log.error("Failed to requestInitialize [chainDealId:{}, taskIndex:{}]",
                    chainDealId, taskIndex, e);
        }
        return Optional.empty();
    }

    /**
     * Verify if the initialize task command is completed on-chain.
     *
     * @param chainTaskId ID of the task
     * @return an optional which will be:
     * <ul>
     * <li>true if the tx is mined
     * <li>false if reverted
     * <li>empty for other cases (max attempts reached while still in RECEIVED or PROCESSING state, adapter error)
     * </ul>
     */
    public Optional<Boolean> isInitialized(String chainTaskId) {
        return isCommandCompleted(apiClient::getStatusForInitializeTaskRequest,
                chainTaskId, period, maxAttempts);
    }

    // endregion

    // region finalize

    /**
     * Request on-chain finalization of the task.
     *
     * @param chainTaskId  ID of the deal
     * @param resultLink   link of the result to be published on-chain
     * @param callbackData optional data for on-chain callback
     * @return chain task ID is initialization is properly requested
     */
    public Optional<String> requestFinalize(String chainTaskId,
                                            String resultLink,
                                            String callbackData) {
        try {
            String finalizeResponse = apiClient.requestFinalizeTask(chainTaskId,
                    new TaskFinalizeArgs(resultLink, callbackData));
            if (!StringUtils.isEmpty(finalizeResponse)) {
                log.info("Requested finalize [chainTaskId:{}, resultLink:{}, callbackData:{}]",
                        chainTaskId, resultLink, callbackData);
                return Optional.of(chainTaskId);
            }
        } catch (Exception e) {
            log.error("Failed to requestFinalize [chainTaskId:{}, resultLink:{}, callbackData:{}]",
                    chainTaskId, resultLink, callbackData, e);
        }
        return Optional.empty();
    }

    /**
     * Verify if the finalize task command is completed on-chain.
     *
     * @param chainTaskId ID of the task
     * @return an optional which will be
     * <ul>
     * <li>true if the tx is mined
     * <li>false if reverted
     * <li>empty for other cases (max attempts reached while still in RECEIVED or PROCESSING state, adapter error)
     * </ul>
     */
    public Optional<Boolean> isFinalized(String chainTaskId) {
        return isCommandCompleted(apiClient::getStatusForFinalizeTaskRequest,
                chainTaskId, period, maxAttempts);
    }

    // endregion

    /**
     * Verify if a command sent to the adapter is completed on-chain.
     *
     * @param getCommandStatusFunction method for fetching the current command status from the adapter
     * @param chainTaskId              ID of the task
     * @param period                   period in ms between consecutive checks
     * @param maxAttempts              maximum number of attempts
     * @return an optional which will be:
     * <ul>
     * <li>true if the tx is mined
     * <li>false if reverted
     * <li>empty for other cases (max attempts reached while still in RECEIVED or PROCESSING state, adapter error)
     * </ul>
     */
    Optional<Boolean> isCommandCompleted(
            Function<String, CommandStatus> getCommandStatusFunction,
            String chainTaskId, long period, int maxAttempts) {
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            try {
                CommandStatus status = getCommandStatusFunction.apply(chainTaskId);
                if (CommandStatus.SUCCESS == status || CommandStatus.FAILURE == status) {
                    return Optional.of(CommandStatus.SUCCESS == status);
                }
                // RECEIVED, PROCESSING
                log.warn("Waiting command completion [chainTaskId:{}, status:{}, period:{}ms, attempt:{}, maxAttempts:{}]",
                        chainTaskId, status, period, attempt, maxAttempts);
            } catch (Exception e) {
                log.error("Unexpected error while waiting command completion [chainTaskId:{}, period:{}ms, attempt:{}, maxAttempts:{}]",
                        chainTaskId, period, attempt, maxAttempts, e);
            }

            try {
                Thread.sleep(period);
            } catch (InterruptedException e) {
                log.error("Polling on blockchain command was interrupted", e);
                Thread.currentThread().interrupt();
                return Optional.empty();
            }
        }
        log.error("Reached max retry while waiting command completion [chainTaskId:{}, maxAttempts:{}]",
                chainTaskId, maxAttempts);
        return Optional.empty();
    }
}
