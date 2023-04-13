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

package com.iexec.blockchain.command.task.initialize;

import com.iexec.blockchain.tool.Status;
import com.iexec.commons.poco.chain.ChainUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.util.Optional;

import static org.mockito.Mockito.*;

class TaskInitializeStorageTest {

    public static final String CHAIN_DEAL_ID =
            "0x000000000000000000000000000000000000000000000000000000000000dea1";
    public static final int TASK_INDEX = 0;
    public static final String CHAIN_TASK_ID =
            ChainUtils.generateChainTaskId(CHAIN_DEAL_ID, TASK_INDEX);

    @InjectMocks
    private TaskInitializeStorageService updaterService;
    @Mock
    private TaskInitializeRepository repository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldSetReceived() {
        TaskInitializeArgs args = getArgs();
        when(repository.findByChainObjectId(CHAIN_TASK_ID))
                .thenReturn(Optional.empty());

        boolean isSet = updaterService.updateToReceived(args);

        Assertions.assertTrue(isSet);
        ArgumentCaptor<TaskInitialize> taskInitializeCaptor =
                ArgumentCaptor.forClass(TaskInitialize.class);
        verify(repository, times(1))
                .save(taskInitializeCaptor.capture());
        TaskInitialize initializeCaptorValue = taskInitializeCaptor.getValue();
        Assertions.assertEquals(Status.RECEIVED, initializeCaptorValue.getStatus());
        Assertions.assertEquals(CHAIN_TASK_ID, initializeCaptorValue.getChainObjectId());
        Assertions.assertEquals(args, initializeCaptorValue.getArgs());
        Assertions.assertNotNull(initializeCaptorValue.getCreationDate());
    }


    @Test
    void shouldNotSetReceivedSinceAlreadyPresent() {
        TaskInitializeArgs args = getArgs();
        when(repository.findByChainObjectId(CHAIN_TASK_ID))
                .thenReturn(Optional.of(mock(TaskInitialize.class)));

        boolean isSet = updaterService.updateToReceived(args);

        Assertions.assertFalse(isSet);
        verify(repository, times(0)).save(any());
    }

    @Test
    void shouldSetProcessing() {
        TaskInitialize taskInitialize = new TaskInitialize();
        taskInitialize.setStatus(Status.RECEIVED);
        when(repository.findByChainObjectId(CHAIN_TASK_ID))
                .thenReturn(Optional.of(taskInitialize));

        boolean isSet = updaterService.updateToProcessing(CHAIN_TASK_ID);

        Assertions.assertTrue(isSet);
        ArgumentCaptor<TaskInitialize> taskInitializeCaptor =
                ArgumentCaptor.forClass(TaskInitialize.class);
        verify(repository, times(1))
                .save(taskInitializeCaptor.capture());
        TaskInitialize initializeCaptorValue = taskInitializeCaptor.getValue();
        Assertions.assertEquals(Status.PROCESSING, initializeCaptorValue.getStatus());
        Assertions.assertNotNull(initializeCaptorValue.getProcessingDate());
    }

    @Test
    void shouldNotSetProcessingSinceBadStatus() {
        TaskInitialize taskInitialize = new TaskInitialize();
        taskInitialize.setStatus(Status.PROCESSING);
        when(repository.findByChainObjectId(CHAIN_TASK_ID))
                .thenReturn(Optional.of(taskInitialize));

        boolean isSet = updaterService.updateToProcessing(CHAIN_TASK_ID);

        Assertions.assertFalse(isSet);
        verify(repository, times(0)).save(any());
    }

    @Test
    void shouldSetFinalSuccess() {
        TransactionReceipt receipt = mock(TransactionReceipt.class);
        when(receipt.getStatus()).thenReturn("0x1");
        TaskInitialize taskInitialize = new TaskInitialize();
        taskInitialize.setStatus(Status.PROCESSING);
        when(repository.findByChainObjectId(CHAIN_TASK_ID))
                .thenReturn(Optional.of(taskInitialize));

        updaterService.updateToFinal(CHAIN_TASK_ID, receipt);

        ArgumentCaptor<TaskInitialize> taskInitializeCaptor =
                ArgumentCaptor.forClass(TaskInitialize.class);
        verify(repository, times(1))
                .save(taskInitializeCaptor.capture());
        TaskInitialize initializeCaptorValue = taskInitializeCaptor.getValue();
        Assertions.assertEquals(Status.SUCCESS, initializeCaptorValue.getStatus());
        Assertions.assertEquals(receipt, initializeCaptorValue.getTransactionReceipt());
        Assertions.assertNotNull(initializeCaptorValue.getFinalDate());
    }

    @Test
    void shouldSetFinalFailure() {
        TransactionReceipt receipt = mock(TransactionReceipt.class);
        when(receipt.getStatus()).thenReturn("0x0");
        TaskInitialize taskInitialize = new TaskInitialize();
        taskInitialize.setStatus(Status.PROCESSING);
        when(repository.findByChainObjectId(CHAIN_TASK_ID))
                .thenReturn(Optional.of(taskInitialize));

        updaterService.updateToFinal(CHAIN_TASK_ID, receipt);

        ArgumentCaptor<TaskInitialize> taskInitializeCaptor =
                ArgumentCaptor.forClass(TaskInitialize.class);
        verify(repository, times(1))
                .save(taskInitializeCaptor.capture());
        TaskInitialize initializeCaptorValue = taskInitializeCaptor.getValue();
        Assertions.assertEquals(Status.FAILURE, initializeCaptorValue.getStatus());
        Assertions.assertEquals(receipt, initializeCaptorValue.getTransactionReceipt());
        Assertions.assertNotNull(initializeCaptorValue.getFinalDate());
    }

    @Test
    void shouldNotSetFinalSinceBadStatus() {
        TransactionReceipt receipt = mock(TransactionReceipt.class);
        TaskInitialize taskInitialize = new TaskInitialize();
        taskInitialize.setStatus(Status.RECEIVED);
        when(repository.findByChainObjectId(CHAIN_TASK_ID))
                .thenReturn(Optional.of(taskInitialize));

        updaterService.updateToFinal(CHAIN_TASK_ID, receipt);

        verify(repository, times(0)).save(any());
    }

    private TaskInitializeArgs getArgs() {
        return new TaskInitializeArgs(CHAIN_TASK_ID, CHAIN_DEAL_ID, TASK_INDEX);
    }
}