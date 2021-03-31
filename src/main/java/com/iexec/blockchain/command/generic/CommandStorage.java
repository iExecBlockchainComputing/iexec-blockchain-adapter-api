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

package com.iexec.blockchain.command.generic;


import com.iexec.blockchain.tool.Status;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.time.Instant;
import java.util.Optional;

@Slf4j
public abstract class CommandStorage<C extends Command<A>, A extends CommandArgs>
        implements CommandFactory<C> {

    private final CommandRepository<C> commandRepository;

    protected CommandStorage(CommandRepository<C> commandRepository) {
        this.commandRepository = commandRepository;
    }

    /**
     * Locally set status to received and store blockchain command arguments for
     * future use.
     *
     * @param args input arguments for the blockchain command
     * @return true on successful update
     */
    public boolean updateToReceived(A args) {
        String chainObjectId = args.getChainObjectId();

        if (commandRepository.findByChainObjectId(chainObjectId).isPresent()) {
            return false;
        }

        C command = this.newCommandInstance();
        command.setStatus(Status.RECEIVED);
        command.setChainObjectId(chainObjectId);
        command.setArgs(args);
        command.setCreationDate(Instant.now());

        commandRepository.save(command);
        return true;
    }

    /**
     * Locally set status to processing just before sending the blockchain command
     *
     * @param chainObjectId blockchain object ID on which the blockchain command
     *                      is performed
     * @return true on successful update
     */
    public boolean updateToProcessing(String chainObjectId) {
        Optional<C> localCommand = commandRepository
                .findByChainObjectId(chainObjectId)
                .filter(command -> command.getStatus() != null)
                .filter(command -> command.getStatus() == Status.RECEIVED);
        if (localCommand.isEmpty()) {
            return false;
        }
        C command = localCommand.get();
        command.setStatus(Status.PROCESSING);
        command.setProcessingDate(Instant.now());
        commandRepository.save(command);
        return true;
    }

    /**
     * Locally set status both to success or failure, when blockchain command
     * is completed.
     *
     * @param chainObjectId blockchain object ID on which the blockchain command
     *                      is performed
     * @param receipt       blockchain receipt
     */
    public void updateToFinal(String chainObjectId,
                              @NonNull TransactionReceipt receipt) {
        Optional<C> localCommand = commandRepository
                .findByChainObjectId(chainObjectId)
                .filter(command -> command.getStatus() != null)
                .filter(command -> command.getStatus() == Status.PROCESSING);
        if (localCommand.isEmpty()) {
            return;
        }
        C command = localCommand.get();

        Status status;
        if (StringUtils.hasText(receipt.getStatus())
                && receipt.getStatus().equals("0x1")) {
            status = Status.SUCCESS;
            log.info("Success command with transaction receipt " +
                            "[chainObjectId:{}, command:{}, receipt:{}]",
                    chainObjectId,
                    command.getClass().getSimpleName(),
                    receipt.toString());
        } else {
            status = Status.FAILURE;
            log.info("Failure after transaction sent [chainObjectId:{}, " +
                            "command:{}, receipt:{}]", chainObjectId,
                    command.getClass().getSimpleName(), receipt.toString());
        }
        command.setStatus(status);
        command.setTransactionReceipt(receipt);
        command.setFinalDate(Instant.now());
        commandRepository.save(command);
    }

    /**
     * Get status for the initialize task process (which is async)
     *
     * @param chainObjectId blockchain object ID on which the blockchain command
     *                      is performed
     */
    public Optional<Status> getStatusForCommand(String chainObjectId) {
        Status status = commandRepository.findByChainObjectId(chainObjectId)
                .map(Command::getStatus)
                .orElse(null);
        if (status != null) {
            return Optional.of(status);
        }
        return Optional.empty();
    }

}
