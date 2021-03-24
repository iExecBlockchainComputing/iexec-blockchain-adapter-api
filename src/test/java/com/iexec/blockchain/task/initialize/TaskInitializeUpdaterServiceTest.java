package com.iexec.blockchain.task.initialize;

import com.iexec.blockchain.tool.Status;
import com.iexec.common.chain.ChainUtils;
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

class TaskInitializeUpdaterServiceTest {

    public static final String CHAIN_DEAL_ID = "0x000000000000000000000000000000000000000000000000000000000000dea1";
    public static final int TASK_INDEX = 0;
    public static final String CHAIN_TASK_ID = ChainUtils.generateChainTaskId(CHAIN_DEAL_ID, TASK_INDEX);
    @InjectMocks
    private TaskInitializeUpdaterService updaterService;
    @Mock
    private TaskInitializeRepository repository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldSetReceived() {
        when(repository.findByChainTaskId(CHAIN_TASK_ID))
                .thenReturn(Optional.empty());

        boolean isSet = updaterService.setReceived(CHAIN_DEAL_ID, TASK_INDEX, CHAIN_TASK_ID);

        Assertions.assertTrue(isSet);
        ArgumentCaptor<TaskInitialize> taskInitializeCaptor =
                ArgumentCaptor.forClass(TaskInitialize.class);
        verify(repository, times(1))
                .save(taskInitializeCaptor.capture());
        TaskInitialize initializeCaptorValue = taskInitializeCaptor.getValue();
        Assertions.assertEquals(Status.RECEIVED, initializeCaptorValue.getStatus());
        Assertions.assertEquals(CHAIN_DEAL_ID, initializeCaptorValue.getChainDealId());
        Assertions.assertEquals(TASK_INDEX, initializeCaptorValue.getTaskIndex());
        Assertions.assertEquals(CHAIN_TASK_ID, initializeCaptorValue.getChainTaskId());
        Assertions.assertNotNull(initializeCaptorValue.getCreationDate());
    }

    @Test
    void shouldNotSetReceivedSinceEmptyDeal() {
        when(repository.findByChainTaskId(CHAIN_TASK_ID))
                .thenReturn(Optional.empty());

        boolean isSet = updaterService.setReceived("", TASK_INDEX, CHAIN_TASK_ID);

        Assertions.assertFalse(isSet);
        verify(repository, times(0)).save(any());
    }

    @Test
    void shouldNotSetReceivedSinceEmptyIndex() {
        when(repository.findByChainTaskId(CHAIN_TASK_ID))
                .thenReturn(Optional.empty());

        boolean isSet = updaterService.setReceived(CHAIN_DEAL_ID, -1, CHAIN_TASK_ID);

        Assertions.assertFalse(isSet);
        verify(repository, times(0)).save(any());
    }

    @Test
    void shouldNotSetReceivedSinceEmptyTask() {
        when(repository.findByChainTaskId(CHAIN_TASK_ID))
                .thenReturn(Optional.empty());

        boolean isSet = updaterService.setReceived(CHAIN_DEAL_ID, TASK_INDEX, "");

        Assertions.assertFalse(isSet);
        verify(repository, times(0)).save(any());
    }

    @Test
    void shouldSetProcessing() {
        TaskInitialize taskInitialize = TaskInitialize.builder()
                .status(Status.RECEIVED).build();
        when(repository.findByChainTaskId(CHAIN_TASK_ID))
                .thenReturn(Optional.of(taskInitialize));

        boolean isSet = updaterService.setProcessing(CHAIN_TASK_ID);

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
    void shouldNotSetProcessingSinceNullStatus() {
        TaskInitialize taskInitialize = TaskInitialize.builder()
                .status(null).build();
        when(repository.findByChainTaskId(CHAIN_TASK_ID))
                .thenReturn(Optional.of(taskInitialize));

        boolean isSet = updaterService.setProcessing(CHAIN_TASK_ID);

        Assertions.assertFalse(isSet);
        verify(repository, times(0)).save(any());
    }

    @Test
    void shouldNotSetProcessingSinceBadStatus() {
        TaskInitialize taskInitialize = TaskInitialize.builder()
                .status(Status.PROCESSING).build();
        when(repository.findByChainTaskId(CHAIN_TASK_ID))
                .thenReturn(Optional.of(taskInitialize));

        boolean isSet = updaterService.setProcessing(CHAIN_TASK_ID);

        Assertions.assertFalse(isSet);
        verify(repository, times(0)).save(any());
    }

    @Test
    void shouldSetFinalSuccess() {
        TransactionReceipt receipt = mock(TransactionReceipt.class);
        when(receipt.getStatus()).thenReturn("0x1");
        TaskInitialize taskInitialize = TaskInitialize.builder()
                .status(Status.PROCESSING).build();
        when(repository.findByChainTaskId(CHAIN_TASK_ID))
                .thenReturn(Optional.of(taskInitialize));

        updaterService.setFinal(CHAIN_TASK_ID, receipt);

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
        TaskInitialize taskInitialize = TaskInitialize.builder()
                .status(Status.PROCESSING).build();
        when(repository.findByChainTaskId(CHAIN_TASK_ID))
                .thenReturn(Optional.of(taskInitialize));

        updaterService.setFinal(CHAIN_TASK_ID, receipt);

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
    void shouldNotSetFinalSinceNullStatus() {
        TransactionReceipt receipt = mock(TransactionReceipt.class);
        TaskInitialize taskInitialize = TaskInitialize.builder()
                .status(null).build();
        when(repository.findByChainTaskId(CHAIN_TASK_ID))
                .thenReturn(Optional.of(taskInitialize));

        updaterService.setFinal(CHAIN_TASK_ID, receipt);

        verify(repository, times(0)).save(any());
    }

    @Test
    void shouldNotSetFinalSinceBadStatus() {
        TransactionReceipt receipt = mock(TransactionReceipt.class);
        TaskInitialize taskInitialize = TaskInitialize.builder()
                .status(Status.RECEIVED).build();
        when(repository.findByChainTaskId(CHAIN_TASK_ID))
                .thenReturn(Optional.of(taskInitialize));

        updaterService.setFinal(CHAIN_TASK_ID, receipt);

        verify(repository, times(0)).save(any());
    }
}