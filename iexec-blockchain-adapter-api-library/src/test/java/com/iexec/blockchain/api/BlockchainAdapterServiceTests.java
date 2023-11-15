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

import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class BlockchainAdapterServiceTests {

    static final String CHAIN_TASK_ID = "CHAIN_TASK_ID";
    static final int PERIOD = 10;
    static final int MAX_ATTEMPTS = 3;

    @Mock
    private BlockchainAdapterApiClient blockchainAdapterClient;
    @InjectMocks
    private BlockchainAdapterService blockchainAdapterService;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
    }

    // region isCommandCompleted
    @Test
    void isCommandCompletedTrueWhenSuccess() {
        when(blockchainAdapterClient.getStatusForInitializeTaskRequest(CHAIN_TASK_ID))
                .thenReturn(CommandStatus.RECEIVED)
                .thenReturn(CommandStatus.PROCESSING)
                .thenReturn(CommandStatus.SUCCESS);

        boolean commandCompleted = blockchainAdapterService.isCommandCompleted(
                blockchainAdapterClient::getStatusForInitializeTaskRequest, CHAIN_TASK_ID, PERIOD, MAX_ATTEMPTS);
        assertTrue(commandCompleted);
    }

    @Test
    void isCommandCompletedFalseWhenFailure() {
        when(blockchainAdapterClient.getStatusForInitializeTaskRequest(CHAIN_TASK_ID))
                .thenReturn(CommandStatus.RECEIVED)
                .thenReturn(CommandStatus.PROCESSING)
                .thenReturn(CommandStatus.FAILURE);

        boolean commandCompleted = blockchainAdapterService.isCommandCompleted(
                blockchainAdapterClient::getStatusForInitializeTaskRequest, CHAIN_TASK_ID, PERIOD, MAX_ATTEMPTS);
        assertFalse(commandCompleted);
    }

    @Test
    void isCommandCompletedFalseWhenMaxAttempts() {
        when(blockchainAdapterClient.getStatusForInitializeTaskRequest(CHAIN_TASK_ID))
                .thenReturn(CommandStatus.PROCESSING);
        boolean commandCompleted = blockchainAdapterService.isCommandCompleted(
                blockchainAdapterClient::getStatusForInitializeTaskRequest, CHAIN_TASK_ID, PERIOD, MAX_ATTEMPTS);
        assertFalse(commandCompleted);
    }

    @Test
    void isCommandCompletedFalseWhenFeignException() {
        when(blockchainAdapterClient.getStatusForFinalizeTaskRequest(CHAIN_TASK_ID))
                .thenThrow(FeignException.class);
        boolean commandCompleted = blockchainAdapterService.isCommandCompleted(
                blockchainAdapterClient::getStatusForFinalizeTaskRequest, CHAIN_TASK_ID, PERIOD, MAX_ATTEMPTS);
        assertFalse(commandCompleted);
    }

    @Test
    void isCommandCompletedFalseWhenInterrupted() throws InterruptedException {
        when(blockchainAdapterClient.getStatusForFinalizeTaskRequest(CHAIN_TASK_ID))
                .thenReturn(CommandStatus.PROCESSING);
        ExecutorService service = Executors.newSingleThreadExecutor();
        Future<?> future = service.submit(() ->
                blockchainAdapterService.isCommandCompleted(blockchainAdapterClient::getStatusForFinalizeTaskRequest,
                        CHAIN_TASK_ID, 5000L, MAX_ATTEMPTS));
        Thread.sleep(1000L);
        future.cancel(true);
        assertThrows(CancellationException.class, future::get);
    }
    // endregion
}
