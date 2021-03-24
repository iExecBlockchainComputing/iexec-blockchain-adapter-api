package com.iexec.blockchain.task.initialize;

import com.iexec.blockchain.tool.IexecHubService;
import com.iexec.blockchain.tool.QueueService;
import com.iexec.blockchain.tool.Status;
import com.iexec.common.chain.ChainUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TaskInitializeServiceTest {

    public static final String CHAIN_DEAL_ID = "0x000000000000000000000000000000000000000000000000000000000000dea1";
    public static final int TASK_INDEX = 0;
    public static final String CHAIN_TASK_ID = ChainUtils.generateChainTaskId(CHAIN_DEAL_ID, TASK_INDEX);
    @InjectMocks
    private TaskInitializeService taskInitializeService;
    @Mock
    private TaskInitializeBlockchainCheckerService blockchainCheckerService;
    @Mock
    private IexecHubService iexecHubService;
    @Mock
    private TaskInitializeUpdaterService updaterService;
    @Mock
    private TaskInitializeRepository repository;
    @Mock
    private QueueService queueService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldInitializeTask() {
        when(blockchainCheckerService.canInitializeTask(CHAIN_DEAL_ID, TASK_INDEX, CHAIN_TASK_ID))
                .thenReturn(true);
        when(updaterService.setReceived(CHAIN_DEAL_ID, TASK_INDEX, CHAIN_TASK_ID))
                .thenReturn(true);

        String chainTaskId = taskInitializeService.initializeTask(CHAIN_DEAL_ID, TASK_INDEX);

        Assertions.assertEquals(CHAIN_TASK_ID, chainTaskId);
        verify(queueService, times(1)).runAsync(any());
    }

    @Test
    void shouldNotInitializeTaskSinceCannotOnChain() {
        when(blockchainCheckerService.canInitializeTask(CHAIN_DEAL_ID, TASK_INDEX, CHAIN_TASK_ID))
                .thenReturn(false);
        when(updaterService.setReceived(CHAIN_DEAL_ID, TASK_INDEX, CHAIN_TASK_ID))
                .thenReturn(true);

        String chainTaskId = taskInitializeService.initializeTask(CHAIN_DEAL_ID, TASK_INDEX);

        Assertions.assertTrue(chainTaskId.isEmpty());
        verify(queueService, times(0)).runAsync(any());
    }

    @Test
    void shouldNotInitializeTaskSinceCannotUpdate() {
        when(blockchainCheckerService.canInitializeTask(CHAIN_DEAL_ID, TASK_INDEX, CHAIN_TASK_ID))
                .thenReturn(true);
        when(updaterService.setReceived(CHAIN_DEAL_ID, TASK_INDEX, CHAIN_TASK_ID))
                .thenReturn(false);

        String chainTaskId = taskInitializeService.initializeTask(CHAIN_DEAL_ID, TASK_INDEX);

        Assertions.assertTrue(chainTaskId.isEmpty());
        verify(queueService, times(0)).runAsync(any());
    }

    @Test
    void triggerInitializeTask() {
        TransactionReceipt receipt = mock(TransactionReceipt.class);
        when(updaterService.setProcessing(CHAIN_TASK_ID)).thenReturn(true);
        when(iexecHubService.initializeTask(CHAIN_DEAL_ID, TASK_INDEX))
                .thenReturn(CompletableFuture.completedFuture(receipt));

        taskInitializeService.triggerInitializeTask(CHAIN_DEAL_ID, TASK_INDEX, CHAIN_TASK_ID);
        verify(updaterService, times(1))
                .setFinal(CHAIN_TASK_ID, receipt);
    }

    @Test
    void shouldNotTriggerInitializeTaskSinceCannotUpdate() {
        TransactionReceipt receipt = mock(TransactionReceipt.class);
        when(updaterService.setProcessing(CHAIN_TASK_ID)).thenReturn(false);
        when(iexecHubService.initializeTask(CHAIN_DEAL_ID, TASK_INDEX))
                .thenReturn(CompletableFuture.completedFuture(receipt));

        taskInitializeService.triggerInitializeTask(CHAIN_DEAL_ID, TASK_INDEX, CHAIN_TASK_ID);
        verify(updaterService, times(0))
                .setFinal(CHAIN_TASK_ID, receipt);
    }

    @Test
    void shouldNotTriggerInitializeTaskSinceReceiptIsNull() {
        when(updaterService.setProcessing(CHAIN_TASK_ID)).thenReturn(true);
        when(iexecHubService.initializeTask(CHAIN_DEAL_ID, TASK_INDEX))
                .thenReturn(CompletableFuture.completedFuture(null));

        taskInitializeService.triggerInitializeTask(CHAIN_DEAL_ID, TASK_INDEX, CHAIN_TASK_ID);
        verify(updaterService, times(0))
                .setFinal(CHAIN_TASK_ID, null);
    }

    @Test
    void shouldGetStatusForInitializeTaskRequest() {
        TaskInitialize taskInitialize = mock(TaskInitialize.class);
        when(taskInitialize.getStatus()).thenReturn(Status.PROCESSING);
        when(repository.findByChainTaskId(CHAIN_TASK_ID))
                .thenReturn(Optional.of(taskInitialize));

        Assertions.assertEquals(Optional.of(Status.PROCESSING),
                taskInitializeService.getStatusForInitializeTaskRequest(CHAIN_TASK_ID));
    }

    @Test
    void shouldNotGetStatusForInitializeTaskRequestSinceNoRequest() {
        when(repository.findByChainTaskId(CHAIN_TASK_ID))
                .thenReturn(Optional.empty());

        Assertions.assertEquals(Optional.empty(),
                taskInitializeService.getStatusForInitializeTaskRequest(CHAIN_TASK_ID));
    }

    @Test
    void shouldNotGetStatusForInitializeTaskRequestSinceNoStatus() {
        TaskInitialize taskInitialize = mock(TaskInitialize.class);
        when(taskInitialize.getStatus()).thenReturn(null);
        when(repository.findByChainTaskId(CHAIN_TASK_ID))
                .thenReturn(Optional.of(taskInitialize));

        Assertions.assertEquals(Optional.empty(),
                taskInitializeService.getStatusForInitializeTaskRequest(CHAIN_TASK_ID));
    }

}