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

package com.iexec.blockchain.command.generic;

import com.iexec.blockchain.api.CommandStatus;
import com.iexec.blockchain.chain.QueueService;
import lombok.extern.slf4j.Slf4j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.util.Optional;

@Slf4j
public abstract class CommandEngine<A extends CommandArgs> {

    private static final int MAX_ATTEMPTS = 5;

    private final CommandBlockchain<A> blockchainService;
    private final CommandStorage updaterService;
    private final QueueService queueService;

    protected CommandEngine(
            CommandBlockchain<A> blockchainService,
            CommandStorage updaterService,
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
    public String startBlockchainCommand(final A args, final boolean isPriority) {
        final String messageDetails = String.format("chainObjectId:%s, commandArgs:%s", args.getChainObjectId(), args);
        if (!blockchainService.canSendBlockchainCommand(args)) {
            log.error("Starting blockchain command failed (failing on-chain checks) [{}]", messageDetails);
            return "";
        }

        if (!updaterService.updateToReceived(args)) {
            log.error("Starting blockchain command failed (failing update to received) [{}]", messageDetails);
            return "";
        }
        log.info("Received command {}", messageDetails);

        final Runnable runnable = () -> triggerBlockchainCommand(args);
        queueService.addExecutionToQueue(runnable, isPriority);

        return args.getChainObjectId();
    }

    /**
     * Trigger blockchain command process by :
     * - firing the corresponding blockchain transaction
     * - performing local updates
     *
     * @param args input arguments for the blockchain command
     */
    public void triggerBlockchainCommand(final A args) {
        final String messageDetails = String.format("chainObjectId:%s, commandArgs:%s", args.getChainObjectId(), args);
        if (!updaterService.updateToProcessing(args)) {
            log.error("Triggering blockchain command failed (failing update to processing) [{}]", messageDetails);
            return;
        }
        int attempt = 0;
        log.info("Processing command [{}]", messageDetails);
        TransactionReceipt receipt = null;
        while (attempt < MAX_ATTEMPTS && receipt == null) {
            attempt++;
            try {
                receipt = blockchainService.sendBlockchainCommand(args);
            } catch (Exception e) {
                log.error("Something wrong happened while triggering command [{}, attempt:{}]",
                        messageDetails, attempt, e);
            }
        }
        if (receipt == null) {
            log.error("Triggering blockchain command failed " +
                            "(received null receipt after blockchain send) [{}, attempt:{}]",
                    messageDetails, attempt);
        }
        updaterService.updateToFinal(args, receipt);
    }

    /**
     * Get current status for the async blockchain command.
     *
     * @param chainObjectId on-chain object ID
     * @param commandName   command applied to the on-chain object
     * @return status
     */
    public Optional<CommandStatus> getStatusForCommand(final String chainObjectId, final CommandName commandName) {
        return updaterService.getStatusForCommand(chainObjectId, commandName);
    }

}
