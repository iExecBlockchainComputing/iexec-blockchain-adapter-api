package com.iexec.blockchain.command.task.initialize;

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

class TaskInitializeTest {

    public static final String CHAIN_DEAL_ID =
            "0x000000000000000000000000000000000000000000000000000000000000dea1";
    public static final int TASK_INDEX = 0;
    public static final String CHAIN_TASK_ID =
            ChainUtils.generateChainTaskId(CHAIN_DEAL_ID, TASK_INDEX);

    @InjectMocks
    private TaskInitializeService taskInitializeService;
    @Mock
    private TaskInitializeBlockchainService blockchainCheckerService;
    @Mock
    private TaskInitializeStorageService updaterService;
    @Mock
    private QueueService queueService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldInitializeTask() {
        TaskInitializeArgs args = getArgs();
        when(blockchainCheckerService.canSendBlockchainCommand(args)).thenReturn(true);
        when(updaterService.setReceived(args)).thenReturn(true);

        String chainTaskId = taskInitializeService.start(CHAIN_DEAL_ID, TASK_INDEX);

        Assertions.assertEquals(CHAIN_TASK_ID, chainTaskId);
        verify(queueService, times(1)).runAsync(any());
    }

    @Test
    void shouldNotInitializeTaskSinceCannotOnChain() {
        TaskInitializeArgs args = getArgs();
        when(blockchainCheckerService.canSendBlockchainCommand(args)).thenReturn(false);
        when(updaterService.setReceived(args)).thenReturn(true);

        String chainTaskId = taskInitializeService.start(CHAIN_DEAL_ID, TASK_INDEX);

        Assertions.assertTrue(chainTaskId.isEmpty());
        verify(queueService, times(0)).runAsync(any());
    }

    @Test
    void shouldNotInitializeTaskSinceCannotUpdate() {
        TaskInitializeArgs args = getArgs();
        when(blockchainCheckerService.canSendBlockchainCommand(args)).thenReturn(true);
        when(updaterService.setReceived(args)).thenReturn(false);

        String chainTaskId = taskInitializeService.start(CHAIN_DEAL_ID, TASK_INDEX);

        Assertions.assertTrue(chainTaskId.isEmpty());
        verify(queueService, times(0)).runAsync(any());
    }

    @Test
    void triggerInitializeTask() {
        TransactionReceipt receipt = mock(TransactionReceipt.class);
        TaskInitializeArgs args = getArgs();
        when(updaterService.setProcessing(CHAIN_TASK_ID)).thenReturn(true);
        when(blockchainCheckerService.sendBlockchainCommand(args))
                .thenReturn(CompletableFuture.completedFuture(receipt));

        taskInitializeService.triggerBlockchainCommand(args);
        verify(updaterService, times(1))
                .setFinal(CHAIN_TASK_ID, receipt);
    }

    @Test
    void shouldNotTriggerInitializeTaskSinceCannotUpdate() {
        TransactionReceipt receipt = mock(TransactionReceipt.class);
        TaskInitializeArgs args = getArgs();
        when(updaterService.setProcessing(CHAIN_TASK_ID)).thenReturn(false);
        when(blockchainCheckerService.sendBlockchainCommand(args))
                .thenReturn(CompletableFuture.completedFuture(receipt));

        taskInitializeService.triggerBlockchainCommand(args);
        verify(updaterService, times(0))
                .setFinal(CHAIN_TASK_ID, receipt);
    }

    @Test
    void shouldNotTriggerInitializeTaskSinceReceiptIsNull() {
        TaskInitializeArgs args = getArgs();
        when(updaterService.setProcessing(CHAIN_TASK_ID)).thenReturn(true);
        when(blockchainCheckerService.sendBlockchainCommand(args))
                .thenReturn(CompletableFuture.completedFuture(null));

        taskInitializeService.triggerBlockchainCommand(args);
        verify(updaterService, times(0))
                .setFinal(CHAIN_TASK_ID, null);
    }

    @Test
    void shouldGetStatusForInitializeTaskRequest() {
        TaskInitialize taskInitialize = mock(TaskInitialize.class);
        when(taskInitialize.getStatus()).thenReturn(Status.PROCESSING);
        when(updaterService.getStatusForCommand(CHAIN_TASK_ID))
                .thenReturn(Optional.of(Status.PROCESSING));

        Assertions.assertEquals(Optional.of(Status.PROCESSING),
                taskInitializeService.getStatusForCommand(CHAIN_TASK_ID));
    }

    @Test
    void shouldNotGetStatusForInitializeTaskRequestSinceNoRequest() {
        when(updaterService.getStatusForCommand(CHAIN_TASK_ID))
                .thenReturn(Optional.empty());

        Assertions.assertEquals(Optional.empty(),
                taskInitializeService.getStatusForCommand(CHAIN_TASK_ID));
    }

    private TaskInitializeArgs getArgs() {
        return TaskInitializeArgs.builder()
                .chainDealId(CHAIN_DEAL_ID)
                .taskIndex(TASK_INDEX)
                .chainTaskId(ChainUtils.generateChainTaskId(CHAIN_DEAL_ID, TASK_INDEX))
                .build();
    }

}