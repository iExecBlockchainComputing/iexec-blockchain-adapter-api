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

package com.iexec.blockchain.command.generic;

import com.iexec.blockchain.api.CommandStatus;
import com.iexec.blockchain.chain.QueueService;
import lombok.extern.slf4j.Slf4j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.util.Optional;

@Slf4j
public abstract class CommandEngine<C extends Command<A>, A extends CommandArgs> {

    private static final int MAX_ATTEMPTS = 5;

    private final CommandBlockchain<A> blockchainService;
    private final CommandStorage<C, A> updaterService;
    private final QueueService queueService;

    protected CommandEngine(
            CommandBlockchain<A> blockchainService,
            CommandStorage<C, A> updaterService,
            QueueService queueService
    ) {
        this.blockchainService = blockchainService;
        this.updaterService = updaterService;
        this.queueService = queueService;
    }

    /**
     * Start blockchain command. Request is synchronously updated to
     * received, then rest of the workflow is done asynchronously.
     *
     * @param args input arguments for the blockchain command
     * @return blockchain object ID if successful
     */
    public String startBlockchainCommand(A args, boolean isPriority) {
        final String chainObjectId = args.getChainObjectId();
        final String messageDetails = String.format("[chainObjectId:%s, commandArgs:%s]", chainObjectId, args);
        if (!blockchainService.canSendBlockchainCommand(args)) {
            log.error("Starting blockchain command failed (failing on-chain checks) {}", messageDetails);
            return "";
        }

        if (!updaterService.updateToReceived(args)) {
            log.error("Starting blockchain command failed (failing update to received) {}", messageDetails);
            return "";
        }
        log.info("Received command {}", messageDetails);

        final Runnable runnable = () -> triggerBlockchainCommand(args);
        queueService.addExecutionToQueue(runnable, isPriority);

        return chainObjectId;
    }

    /**
     * Trigger blockchain command process by :
     * - firing the corresponding blockchain transaction
     * - performing local updates
     *
     * @param args input arguments for the blockchain command
     */
    public void triggerBlockchainCommand(A args) {
        String chainObjectId = args.getChainObjectId();
        if (!updaterService.updateToProcessing(chainObjectId)) {
            log.error("Triggering blockchain command failed (failing update" +
                            " to processing) [chainObjectId:{}, commandArgs:{}]",
                    chainObjectId, args);
            return;
        }
        int attempt = 0;
        log.info("Processing command [chainObjectId:{}, commandArgs:{}]",
                chainObjectId, args);
        TransactionReceipt receipt = null;
        while (attempt < MAX_ATTEMPTS && receipt == null) {
            attempt++;
            try {
                receipt = blockchainService.sendBlockchainCommand(args);
            } catch (Exception e) {
                log.error("Something wrong happened while triggering command [chainObjectId:{}, commandArgs:{}, attempt:{}]",
                        chainObjectId, args, attempt, e);
            }
        }
        if (receipt == null) {
            log.error("Triggering blockchain command failed " +
                            "(received null receipt after blockchain send) " +
                            "[chainObjectId:{}, commandArgs:{}, attempt:{}]",
                    chainObjectId, args, attempt);
        }
        updaterService.updateToFinal(chainObjectId, receipt);
    }

    /**
     * Get current status for the async blockchain command.
     *
     * @param chainObjectId blockchain object ID
     * @return status
     */
    public Optional<CommandStatus> getStatusForCommand(String chainObjectId) {
        return updaterService.getStatusForCommand(chainObjectId);
    }

}
