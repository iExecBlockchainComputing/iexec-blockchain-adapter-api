package com.iexec.blockchain.tool;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class QueueServiceTest {
    @InjectMocks
    @Spy
    private QueueService queueService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // region executeFirstTask
    @Test
    void shouldRunHighPriorityRunnable() {
        final List<Long> highPriorityTimestamps = new ArrayList<>();
        final Runnable highPriorityRunnable = () -> highPriorityTimestamps.add(System.nanoTime());

        queueService.addExecutionToQueue(highPriorityRunnable, true);

        // Start execution thread.
        // It won't stop itself, so we have to do it.
        final CompletableFuture<Void> tasksExecutionFuture = CompletableFuture.runAsync(queueService::executeTasks);
        Awaitility
                .await()
                .atMost(5, TimeUnit.SECONDS)
                .until(() -> highPriorityTimestamps.size() == 1);
        tasksExecutionFuture.cancel(true);

        // We simply ensure the execution has completed.
        assertThat(highPriorityTimestamps.size()).isOne();
    }

    @Test
    void shouldRunLowPriorityRunnable() {
        final List<Long> lowPriorityTimestamps = new ArrayList<>();
        final Runnable lowPriorityRunnable = () -> lowPriorityTimestamps.add(System.nanoTime());

        queueService.addExecutionToQueue(lowPriorityRunnable, false);

        // Start execution thread.
        // It won't stop itself, so we have to do it.
        final CompletableFuture<Void> tasksExecutionFuture = CompletableFuture.runAsync(queueService::executeTasks);
        Awaitility
                .await()
                .atMost(5, TimeUnit.SECONDS)
                .until(() -> lowPriorityTimestamps.size() == 1);
        tasksExecutionFuture.cancel(true);

        // We simply ensure the execution has completed.
        assertThat(lowPriorityTimestamps .size()).isOne();
    }

    @Test
    void shouldRunHighPriorityBeforeLowPriority() {
        final List<Long> highPriorityTimestamps = new ArrayList<>();
        final List<Long> lowPriorityTimestamps = new ArrayList<>();

        final Runnable highPriorityRunnable = () -> highPriorityTimestamps.add(System.nanoTime());
        final Runnable lowPriorityRunnable = () -> lowPriorityTimestamps.add(System.nanoTime());

        // Add a low priority and a high priority tasks to queue.
        // The queueService should select the high priority task before the low priority.
        queueService.addExecutionToQueue(lowPriorityRunnable, false);
        queueService.addExecutionToQueue(highPriorityRunnable, true);

        // Start execution thread.
        // It won't stop itself, so we have to do it.
        final CompletableFuture<Void> tasksExecutionFuture = CompletableFuture.runAsync(queueService::executeTasks);
        Awaitility
                .await()
                .atMost(5, TimeUnit.SECONDS)
                .until(() -> highPriorityTimestamps.size() == 1 && lowPriorityTimestamps.size() == 1);
        tasksExecutionFuture.cancel(true);

        // We have executed a single task per priority so we that's what we should now have.
        assertThat(highPriorityTimestamps.size()).isOne();
        assertThat(lowPriorityTimestamps .size()).isOne();

        // The high priority task should have completed before the low priority one has started.
        assertThat(highPriorityTimestamps.get(0)).isLessThan(lowPriorityTimestamps.get(0));
    }

    @Test
    void shouldExecuteInOrder() {
        final ConcurrentLinkedQueue<Integer> executionOrder = new ConcurrentLinkedQueue<>();

        queueService.addExecutionToQueue(() -> executionOrder.add(4), false);
        queueService.addExecutionToQueue(() -> executionOrder.add(5), false);
        queueService.addExecutionToQueue(() -> executionOrder.add(6), false);

        queueService.addExecutionToQueue(() -> executionOrder.add(1), true);
        queueService.addExecutionToQueue(() -> executionOrder.add(2), true);
        queueService.addExecutionToQueue(() -> executionOrder.add(3), true);

        final CompletableFuture<Void> executionFuture = CompletableFuture.runAsync(queueService::executeTasks);
        Awaitility
                .await()
                .atMost(1, TimeUnit.SECONDS)
                .until(() -> executionOrder.size() == 6);
        executionFuture.cancel(true);

        System.out.println(executionOrder);
        // Tasks should have been executed in the right order
        // so that each element of `executionOrder` should be greater than its precedent.
        for (int i = 1; i < 6; i++) {
            assertThat(new ArrayList<>(executionOrder).get(i)).isGreaterThan(new ArrayList<>(executionOrder).get(i - 1));
        }
    }
    // endregion
}