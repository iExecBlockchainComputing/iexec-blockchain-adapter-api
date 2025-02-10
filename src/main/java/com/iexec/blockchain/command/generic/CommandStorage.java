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
import com.mongodb.client.result.UpdateResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
public class CommandStorage {

    private static final String STATUS_FIELD_NAME = "status";
    private final MongoTemplate mongoTemplate;

    public CommandStorage(final MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Locally set status to received and store blockchain command arguments for
     * future use.
     *
     * @param args input arguments for the blockchain command
     * @return true on successful update
     */
    public boolean updateToReceived(final CommandArgs args) {
        final Command command = new Command();
        command.setStatus(CommandStatus.RECEIVED);
        command.setChainObjectId(args.getChainObjectId());
        command.setCommandName(args.getCommandName());
        command.setArgs(args);
        command.setCreationDate(Instant.now());

        try {
            mongoTemplate.insert(command);
            return true;
        } catch (Exception e) {
            log.error("Failed to submit command to queue [chainObjectId:{}, args:{}]",
                    args.getChainObjectId(), args, e);
            return false;
        }
    }

    /**
     * Locally set status to processing just before sending the blockchain command
     *
     * @param chainObjectId blockchain object ID on which the blockchain command
     *                      is performed
     * @return true on successful update
     */
    public boolean updateToProcessing(final String chainObjectId, final CommandName commandName) {
        final Criteria criteria = createUpdateCriteria(chainObjectId, commandName, CommandStatus.RECEIVED);
        final Update update = new Update();
        update.set(STATUS_FIELD_NAME, CommandStatus.PROCESSING);
        update.set("processingDate", Instant.now());
        final UpdateResult result = mongoTemplate.updateFirst(Query.query(criteria), update, Command.class);
        return result.getModifiedCount() != 0;
    }

    /**
     * Locally set status both to success or failure, when blockchain command
     * is completed.
     *
     * @param chainObjectId blockchain object ID on which the blockchain command
     *                      is performed
     * @param receipt       blockchain receipt
     */
    public boolean updateToFinal(final String chainObjectId,
                                 final CommandName commandName,
                                 final TransactionReceipt receipt) {
        final CommandStatus finalStatus = receipt != null && receipt.isStatusOK() ? CommandStatus.SUCCESS : CommandStatus.FAILURE;
        log.info("Command final status with transaction receipt [chainObjectId]:{}, command:{}, status:{}, receipt:{}]",
                chainObjectId, commandName.name(), finalStatus, receipt);
        final Criteria criteria = createUpdateCriteria(chainObjectId, commandName, CommandStatus.PROCESSING);
        final Update update = new Update();
        update.set(STATUS_FIELD_NAME, finalStatus);
        update.set("transactionReceipt", receipt);
        update.set("finalDate", Instant.now());
        final UpdateResult result = mongoTemplate.updateFirst(Query.query(criteria), update, Command.class);
        return result.getModifiedCount() != 0;
    }

    /**
     * Creates a criteria, the rule to lookup for a specific entry in the Mongo collection.
     *
     * @param chainObjectId On-chain ID of the object to look for in collection
     * @param commandName   Name of the command applied to the on-chain object
     * @param status        Currently expected status for the on-chain object
     * @return A {@code Criteria} instance to use in {@code MongoTemplate} operations
     */
    private Criteria createUpdateCriteria(final String chainObjectId, final CommandName commandName, final CommandStatus status) {
        return Criteria.where("chainObjectId").is(chainObjectId)
                .and("commandName").is(commandName)
                .and(STATUS_FIELD_NAME).is(status);
    }

    /**
     * Get status for the initialize task process (which is async)
     *
     * @param chainObjectId blockchain object ID on which the blockchain command
     *                      is performed
     */
    public Optional<CommandStatus> getStatusForCommand(final String chainObjectId, final CommandName commandName) {
        final Criteria criteria = Criteria.where("chainObjectId").is(chainObjectId)
                .and("commandName").is(commandName);
        final Command command = mongoTemplate.findOne(Query.query(criteria), Command.class);
        return Optional.ofNullable(command)
                .map(Command::getStatus);
    }

}
