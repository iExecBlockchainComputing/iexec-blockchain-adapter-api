package com.iexec.blockchain.tool;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class QueueServiceTest {
    @InjectMocks
    @Spy
    private QueueService queueService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // region startAsyncActionsExecution
    @Test
    void shouldStartASingleThread() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {
        // Make some `QueueService` members accessible, so we can easily test them.
        final Method startAsyncActionsExecutionMethod = QueueService.class.getDeclaredMethod("startAsyncActionsExecution");
        startAsyncActionsExecutionMethod.setAccessible(true);
        final Field actionExecutorField = QueueService.class.getDeclaredField("actionExecutor");
        actionExecutorField.setAccessible(true);

        // First execution should start a new thread.
        startAsyncActionsExecutionMethod.invoke(queueService);
        waitForNewActionCompletion();
        Mockito.verify(queueService, Mockito.times(1)).executeActions();

        // Second execution should not start a new thread.
        startAsyncActionsExecutionMethod.invoke(queueService);
        waitForNewActionCompletion();
        Mockito.verify(queueService, Mockito.times(1)).executeActions();

        // Interrupt current thread and try to spawn a new one.
        final CompletableFuture<?> actionExecutor = (CompletableFuture<?>) actionExecutorField.get(queueService);
        actionExecutor.cancel(true);
        assertThat(actionExecutor.isDone()).isTrue();

        startAsyncActionsExecutionMethod.invoke(queueService);
        waitForNewActionCompletion();
        Mockito.verify(queueService, Mockito.times(2)).executeActions();
    }

    private void waitForNewActionCompletion() {
        final AtomicBoolean actionExecuted = new AtomicBoolean(false);
        final Runnable action = () -> actionExecuted.set(true);
        queueService.addExecutionToQueue(action, true);

        Awaitility
                .await()
                .atMost(200, TimeUnit.MILLISECONDS)
                .until(actionExecuted::get);
    }
    // endregion

    // region executeActions
    @Test
    void shouldRunHighPriorityRunnable() {
        final List<Long> highPriorityTimestamps = new ArrayList<>();
        final Runnable highPriorityRunnable = () -> highPriorityTimestamps.add(System.nanoTime());

        queueService.addExecutionToQueue(highPriorityRunnable, true);

        // Start execution thread.
        // It won't stop itself, so we have to do it.
        final CompletableFuture<Void> tasksExecutionFuture = CompletableFuture.runAsync(queueService::executeActions);
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
        final CompletableFuture<Void> tasksExecutionFuture = CompletableFuture.runAsync(queueService::executeActions);
        Awaitility
                .await()
                .atMost(5, TimeUnit.SECONDS)
                .until(() -> lowPriorityTimestamps.size() == 1);
        tasksExecutionFuture.cancel(true);

        // We simply ensure the execution has completed.
        assertThat(lowPriorityTimestamps.size()).isOne();
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
        final CompletableFuture<Void> tasksExecutionFuture = CompletableFuture.runAsync(queueService::executeActions);
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
    void shouldExecuteInOrder() throws NoSuchFieldException {
        final int taskNumberPerPriority = 3;
        final int totalTasksNumber = taskNumberPerPriority * 2;

        // We'll keep track of the execution order of the tasks.
        final ArrayList<Integer> executionOrder = new ArrayList<>();
        // We'll also keep track of the remaining tasks in queue after each task execution.
        final ArrayList<Integer> remainingTasksInQueue = new ArrayList<>();

        // Make `QueueService::queue` accessible so that we could check it is emptied in the right way,
        // i.e. it should be emptied one by one, before each new task execution.
        final Field queueField = QueueService.class.getDeclaredField("queue");
        queueField.setAccessible(true);

        // Create a bunch of tasks.
        final Function<Integer, Runnable> runnableCreator = i -> () -> {
            try {
                // Wait a bit of time to emulate a real function.
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            try {
                // Get how many tasks are still in queue after this one.
                remainingTasksInQueue.add(((Queue<?>) queueField.get(queueService)).size());
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            executionOrder.add(i);
        };

        for (int i = 0; i < taskNumberPerPriority; i++) {
            queueService.addExecutionToQueue(runnableCreator.apply(taskNumberPerPriority + i), false);
        }
        for (int i = 0; i < taskNumberPerPriority; i++) {
            queueService.addExecutionToQueue(runnableCreator.apply(i), true);
        }

        // Start execution thread.
        // It won't stop itself, so we have to do it.
        final CompletableFuture<Void> executionFuture = CompletableFuture.runAsync(queueService::executeActions);
        Awaitility
                .await()
                .atMost(5, TimeUnit.SECONDS)
                .until(() -> executionOrder.size() == totalTasksNumber);
        executionFuture.cancel(true);

        // Tasks should have been executed in the right order.
        // This should look like [0, 1, 2, 3, 4, 5].
        final List<Integer> expectedExecutionOrder = IntStream
                .range(0, totalTasksNumber)
                .boxed()
                .collect(Collectors.toList());
        assertThat(executionOrder).isEqualTo(expectedExecutionOrder);

        // After each task execution, one less task should be in the queue.
        // This should look like [5, 4, 3, 2, 1, 0].
        final List<Integer> expectedRemainingTasksInQueue = IntStream
                .range(0, totalTasksNumber)
                .mapToObj(i -> totalTasksNumber - i - 1)
                .collect(Collectors.toList());
        assertThat(remainingTasksInQueue).isEqualTo(expectedRemainingTasksInQueue);
    }
    // endregion
}