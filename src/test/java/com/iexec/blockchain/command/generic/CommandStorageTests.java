/*
 * Copyright 2025 IEXEC BLOCKCHAIN TECH
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
import com.iexec.blockchain.command.task.initialize.TaskInitializeArgs;
import com.iexec.commons.poco.chain.ChainUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

@DataMongoTest
@Testcontainers
class CommandStorageTests {

    public static final String CHAIN_DEAL_ID =
            "0x000000000000000000000000000000000000000000000000000000000000dea1";
    public static final int TASK_INDEX = 0;
    public static final String CHAIN_TASK_ID =
            ChainUtils.generateChainTaskId(CHAIN_DEAL_ID, TASK_INDEX);

    @Container
    private static final MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse(System.getProperty("mongo.image")));

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.host", mongoDBContainer::getHost);
        registry.add("spring.data.mongodb.port", () -> mongoDBContainer.getMappedPort(27017));
    }

    private CommandStorage updaterService;
    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void init() {
        mongoTemplate.findAllAndRemove(new Query(), Command.class);
        updaterService = new CommandStorage(mongoTemplate);
    }

    @Test
    void shouldSetReceived() {
        final TaskInitializeArgs args = getArgs();
        Assertions.assertTrue(updaterService.updateToReceived(args));
        final CommandStatus status = updaterService.getStatusForCommand(args.getChainObjectId(), CommandName.TASK_INITIALIZE).orElseThrow();
        Assertions.assertEquals(CommandStatus.RECEIVED, status);
    }

    @Test
    void shouldNotSetReceivedSinceAlreadyPresent() {
        final TaskInitializeArgs args = getArgs();
        Assertions.assertTrue(updaterService.updateToReceived(args));
        Assertions.assertFalse(updaterService.updateToReceived(args));
    }

    @Test
    void shouldSetProcessing() {
        final TaskInitializeArgs args = getArgs();
        Assertions.assertTrue(updaterService.updateToReceived(args));
        Assertions.assertTrue(updaterService.updateToProcessing(CHAIN_TASK_ID, CommandName.TASK_INITIALIZE));
        final CommandStatus status = updaterService.getStatusForCommand(CHAIN_TASK_ID, CommandName.TASK_INITIALIZE).orElseThrow();
        Assertions.assertEquals(CommandStatus.PROCESSING, status);
    }

    @ParameterizedTest
    @EnumSource(value = CommandStatus.class, names = "RECEIVED", mode = EnumSource.Mode.EXCLUDE)
    void shouldNotSetProcessingSinceBadStatus(final CommandStatus status) {
        final Command command = createCommand(status);
        mongoTemplate.insert(command);
        Assertions.assertFalse(updaterService.updateToProcessing(CHAIN_TASK_ID, CommandName.TASK_INITIALIZE));
    }

    @Test
    void shouldSetFinalSuccess() {
        final TransactionReceipt receipt = new TransactionReceipt();
        receipt.setStatus("0x1");
        final Command command = createCommand(CommandStatus.PROCESSING);
        mongoTemplate.insert(command);

        Assertions.assertTrue(updaterService.updateToFinal(CHAIN_TASK_ID, CommandName.TASK_INITIALIZE, receipt));
        final CommandStatus status = updaterService.getStatusForCommand(CHAIN_TASK_ID, CommandName.TASK_INITIALIZE).orElseThrow();
        Assertions.assertEquals(CommandStatus.SUCCESS, status);
    }

    @Test
    void shouldSetFinalFailure() {
        final TransactionReceipt receipt = new TransactionReceipt();
        receipt.setStatus("0x0");
        final Command command = createCommand(CommandStatus.PROCESSING);
        mongoTemplate.insert(command);

        Assertions.assertTrue(updaterService.updateToFinal(CHAIN_TASK_ID, CommandName.TASK_INITIALIZE, receipt));
        final CommandStatus status = updaterService.getStatusForCommand(CHAIN_TASK_ID, CommandName.TASK_INITIALIZE).orElseThrow();
        Assertions.assertEquals(CommandStatus.FAILURE, status);
    }

    @ParameterizedTest
    @EnumSource(value = CommandStatus.class, names = "PROCESSING", mode = EnumSource.Mode.EXCLUDE)
    void shouldNotSetFinalSinceBadStatus(final CommandStatus status) {
        final TransactionReceipt receipt = new TransactionReceipt();
        final Command taskInitialize = new Command();
        taskInitialize.setStatus(status);
        mongoTemplate.insert(taskInitialize);

        Assertions.assertFalse(updaterService.updateToFinal(CHAIN_TASK_ID, CommandName.TASK_INITIALIZE, receipt));
    }

    private Command createCommand(final CommandStatus status) {
        final Command command = new Command();
        command.setChainObjectId(CHAIN_TASK_ID);
        command.setCommandName(CommandName.TASK_INITIALIZE);
        command.setStatus(status);
        return command;
    }

    private TaskInitializeArgs getArgs() {
        return new TaskInitializeArgs(CHAIN_TASK_ID, CHAIN_DEAL_ID, TASK_INDEX);
    }
}
