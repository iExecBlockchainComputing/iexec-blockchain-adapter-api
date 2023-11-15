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

import lombok.extern.slf4j.Slf4j;

import java.util.function.Function;

@Slf4j
public class BlockchainAdapterService {
    /**
     * Verify if a command sent to the adapter is completed on-chain.
     *
     * @param getCommandStatusFunction method for fetching the current command status from the adapter
     * @param chainTaskId              ID of the task
     * @param period                   period in ms between consecutive checks
     * @param maxAttempts              maximum number of attempts
     * @return true if the tx is mined, false if reverted or empty for other cases.
     * (too long since still RECEIVED or PROCESSING, adapter error)
     */
    boolean isCommandCompleted(
            Function<String, CommandStatus> getCommandStatusFunction,
            String chainTaskId, long period, int maxAttempts) {
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            try {
                CommandStatus status = getCommandStatusFunction.apply(chainTaskId);
                if (CommandStatus.SUCCESS == status || CommandStatus.FAILURE == status) {
                    return CommandStatus.SUCCESS == status;
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
                return false;
            }
        }
        log.error("Reached max retry while waiting command completion [chainTaskId:{}, maxAttempts:{}]",
                chainTaskId, maxAttempts);
        return false;
    }
}
