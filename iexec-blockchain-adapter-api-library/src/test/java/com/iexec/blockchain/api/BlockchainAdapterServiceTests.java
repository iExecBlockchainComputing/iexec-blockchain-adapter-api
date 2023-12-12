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
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class BlockchainAdapterServiceTests {

    static final String CHAIN_DEAL_ID = "CHAIN_DEAL_ID";
    static final int TASK_INDEX = 0;
    static final String CHAIN_TASK_ID = "CHAIN_TASK_ID";
    static final String LINK = "link";
    static final String CALLBACK = "callback";
    static final Duration PERIOD = Duration.ofMillis(10);
    static final int MAX_ATTEMPTS = 3;

    @Mock
    private BlockchainAdapterApiClient blockchainAdapterClient;
    private BlockchainAdapterService blockchainAdapterService;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        blockchainAdapterService = new BlockchainAdapterService(blockchainAdapterClient, PERIOD, MAX_ATTEMPTS);
    }

    // region initialize

    @Test
    void requestInitialize() {
        when(blockchainAdapterClient.requestInitializeTask(CHAIN_DEAL_ID, TASK_INDEX))
                .thenReturn(CHAIN_TASK_ID);
        assertTrue(blockchainAdapterService.requestInitialize(CHAIN_DEAL_ID, TASK_INDEX).isPresent());
    }

    @Test
    void requestInitializeFailedSinceException() {
        when(blockchainAdapterClient.requestInitializeTask(CHAIN_DEAL_ID, TASK_INDEX))
                .thenThrow(RuntimeException.class);
        assertTrue(blockchainAdapterService.requestInitialize(CHAIN_DEAL_ID, TASK_INDEX).isEmpty());
    }

    @Test
    void requestInitializeFailedSinceNot200() {
        when(blockchainAdapterClient.requestInitializeTask(CHAIN_DEAL_ID, TASK_INDEX))
                .thenThrow(FeignException.BadRequest.class);
        assertTrue(blockchainAdapterService.requestInitialize(CHAIN_DEAL_ID, TASK_INDEX).isEmpty());
    }

    @Test
    void requestInitializeFailedSinceNoBody() {
        when(blockchainAdapterClient.requestInitializeTask(CHAIN_DEAL_ID, TASK_INDEX))
                .thenReturn("");
        assertTrue(blockchainAdapterService.requestInitialize(CHAIN_DEAL_ID, TASK_INDEX).isEmpty());
    }

    @Test
    void isInitialized() {
        when(blockchainAdapterClient.getStatusForInitializeTaskRequest(CHAIN_TASK_ID))
                .thenReturn(CommandStatus.SUCCESS);
        assertEquals(Optional.of(true), blockchainAdapterService.isInitialized(CHAIN_TASK_ID));
    }

    // endregion

    // region finalize

    @Test
    void requestFinalize() {
        when(blockchainAdapterClient.requestFinalizeTask(CHAIN_TASK_ID, new TaskFinalizeArgs(LINK, CALLBACK)))
                .thenReturn(CHAIN_TASK_ID);

        assertTrue(blockchainAdapterService.requestFinalize(CHAIN_TASK_ID, LINK, CALLBACK).isPresent());
    }

    @Test
    void requestFinalizeFailedSinceNot200() {
        when(blockchainAdapterClient.requestFinalizeTask(CHAIN_TASK_ID, new TaskFinalizeArgs(LINK, CALLBACK)))
                .thenThrow(FeignException.BadRequest.class);
        assertTrue(blockchainAdapterService.requestFinalize(CHAIN_TASK_ID, LINK, CALLBACK).isEmpty());
    }

    @Test
    void requestFinalizeFailedSinceNoBody() {
        when(blockchainAdapterClient.requestFinalizeTask(CHAIN_TASK_ID, new TaskFinalizeArgs(LINK, CALLBACK)))
                .thenReturn("");
        assertTrue(blockchainAdapterService.requestFinalize(CHAIN_TASK_ID, LINK, CALLBACK).isEmpty());
    }

    @Test
    void isFinalized() {
        when(blockchainAdapterClient.getStatusForFinalizeTaskRequest(CHAIN_TASK_ID))
                .thenReturn(CommandStatus.SUCCESS);
        assertEquals(Optional.of(true), blockchainAdapterService.isFinalized(CHAIN_TASK_ID));
    }

    // endregion

    // region isCommandCompleted

    @Test
    void isCommandCompletedTrueWhenSuccess() {
        when(blockchainAdapterClient.getStatusForInitializeTaskRequest(CHAIN_TASK_ID))
                .thenReturn(CommandStatus.RECEIVED)
                .thenReturn(CommandStatus.PROCESSING)
                .thenReturn(CommandStatus.SUCCESS);

        Optional<Boolean> commandCompleted = blockchainAdapterService.isCommandCompleted(
                blockchainAdapterClient::getStatusForInitializeTaskRequest, CHAIN_TASK_ID, MAX_ATTEMPTS);
        assertEquals(Optional.of(true), commandCompleted);
    }

    @Test
    void isCommandCompletedFalseWhenFailure() {
        when(blockchainAdapterClient.getStatusForInitializeTaskRequest(CHAIN_TASK_ID))
                .thenReturn(CommandStatus.RECEIVED)
                .thenReturn(CommandStatus.PROCESSING)
                .thenReturn(CommandStatus.FAILURE);

        Optional<Boolean> commandCompleted = blockchainAdapterService.isCommandCompleted(
                blockchainAdapterClient::getStatusForInitializeTaskRequest, CHAIN_TASK_ID, MAX_ATTEMPTS);
        assertEquals(Optional.of(false), commandCompleted);
    }

    @Test
    void isCommandCompletedFalseWhenMaxAttempts() {
        when(blockchainAdapterClient.getStatusForInitializeTaskRequest(CHAIN_TASK_ID))
                .thenReturn(CommandStatus.PROCESSING);
        Optional<Boolean> commandCompleted = blockchainAdapterService.isCommandCompleted(
                blockchainAdapterClient::getStatusForInitializeTaskRequest, CHAIN_TASK_ID, MAX_ATTEMPTS);
        assertEquals(Optional.empty(), commandCompleted);
    }

    @Test
    void isCommandCompletedFalseWhenFeignException() {
        when(blockchainAdapterClient.getStatusForFinalizeTaskRequest(CHAIN_TASK_ID))
                .thenThrow(FeignException.class);
        Optional<Boolean> commandCompleted = blockchainAdapterService.isCommandCompleted(
                blockchainAdapterClient::getStatusForFinalizeTaskRequest, CHAIN_TASK_ID, MAX_ATTEMPTS);
        assertEquals(Optional.empty(), commandCompleted);
    }

    @Test
    void isCommandCompletedFalseWhenInterrupted() throws InterruptedException {
        blockchainAdapterService = new BlockchainAdapterService(blockchainAdapterClient, Duration.ofSeconds(5), MAX_ATTEMPTS);

        when(blockchainAdapterClient.getStatusForFinalizeTaskRequest(CHAIN_TASK_ID))
                .thenReturn(CommandStatus.PROCESSING);
        ExecutorService service = Executors.newSingleThreadExecutor();
        Future<?> future = service.submit(() ->
                blockchainAdapterService.isCommandCompleted(blockchainAdapterClient::getStatusForFinalizeTaskRequest,
                        CHAIN_TASK_ID, MAX_ATTEMPTS));
        Thread.sleep(1000L);
        future.cancel(true);
        assertThrows(CancellationException.class, future::get);
    }

    // endregion
}
