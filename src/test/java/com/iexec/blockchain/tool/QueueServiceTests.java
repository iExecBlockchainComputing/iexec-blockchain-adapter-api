package com.iexec.blockchain.tool;

import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Slf4j
class QueueServiceTests {

    private static final int TIMEOUT_DURATION = 5;
    private final QueueService queueService = new QueueService(1);

    // region executeActions
    @Test
    void shouldRunHighPriorityRunnable() {
        final List<Long> highPriorityTimestamps = new ArrayList<>();
        final Runnable highPriorityRunnable = () -> highPriorityTimestamps.add(System.nanoTime());

        queueService.addExecutionToQueue(highPriorityRunnable, true);

        Awaitility
                .await()
                .atMost(TIMEOUT_DURATION, TimeUnit.SECONDS)
                .until(() -> highPriorityTimestamps.size() == 1);

        // We simply ensure the execution has completed.
        assertThat(highPriorityTimestamps.size()).isOne();
    }

    @Test
    void shouldRunLowPriorityRunnable() {
        final List<Long> lowPriorityTimestamps = new ArrayList<>();
        final Runnable lowPriorityRunnable = () -> lowPriorityTimestamps.add(System.nanoTime());

        queueService.addExecutionToQueue(lowPriorityRunnable, false);

        Awaitility
                .await()
                .atMost(TIMEOUT_DURATION, TimeUnit.SECONDS)
                .until(() -> lowPriorityTimestamps.size() == 1);

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
        AtomicBoolean queueReady = new AtomicBoolean(false);
        Future<Void> future = queueService.addExecutionToQueue(() -> waitQueueReady(queueReady), false);
        queueService.addExecutionToQueue(lowPriorityRunnable, false);
        queueService.addExecutionToQueue(highPriorityRunnable, true);
        queueReady.set(true);

        try {
            future.get(TIMEOUT_DURATION, TimeUnit.SECONDS);
        } catch (Exception e) {
            Assertions.fail("Queue was not ready on time");
        }

        Awaitility
                .await()
                .atMost(TIMEOUT_DURATION, TimeUnit.SECONDS)
                .until(() -> highPriorityTimestamps.size() == 1 && lowPriorityTimestamps.size() == 1);

        // We have executed a single task per priority so we that's what we should now have.
        assertThat(highPriorityTimestamps).hasSize(1);
        assertThat(lowPriorityTimestamps).hasSize(1);

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
                // Get how many tasks are still in queue after this one.
                remainingTasksInQueue.add(((Queue<?>) queueField.get(queueService)).size());
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            executionOrder.add(i);
        };

        AtomicBoolean queueReady = new AtomicBoolean(false);
        Future<Void> future = queueService.addExecutionToQueue(() -> waitQueueReady(queueReady), false);
        for (int i = 0; i < taskNumberPerPriority; i++) {
            queueService.addExecutionToQueue(runnableCreator.apply(taskNumberPerPriority + i), false);
        }
        for (int i = 0; i < taskNumberPerPriority; i++) {
            queueService.addExecutionToQueue(runnableCreator.apply(i), true);
        }
        queueReady.set(true);

        try {
            future.get(TIMEOUT_DURATION, TimeUnit.SECONDS);
        } catch (Exception e) {
            Assertions.fail("Queue was not ready on time");
        }

        Awaitility
                .await()
                .atMost(TIMEOUT_DURATION, TimeUnit.SECONDS)
                .until(() -> executionOrder.size() == totalTasksNumber);

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

    private void waitQueueReady(AtomicBoolean queueReady) {
        Awaitility.await()
                .atMost(TIMEOUT_DURATION, TimeUnit.SECONDS)
                .until(queueReady::getPlain);
    }
    // endregion

    //region BlockchainAction
    @Test
    void compareBlockchainActionAgainstNull() {
        QueueService.BlockchainAction lowPriorityAction = new QueueService.BlockchainAction(() -> {}, false);
        QueueService.BlockchainAction highPriorityAction = new QueueService.BlockchainAction(() -> {}, true);
        assertThatThrownBy(() -> lowPriorityAction.compareTo(null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> highPriorityAction.compareTo(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void validateBlockchainActionComparisons() {
        final int hasHigherPriority = -1;
        final int hasLowerPriority = 1;
        QueueService.BlockchainAction action1 = new QueueService.BlockchainAction(() -> {}, false);
        QueueService.BlockchainAction action2 = new QueueService.BlockchainAction(() -> {}, false);
        QueueService.BlockchainAction action3 = new QueueService.BlockchainAction(() -> {}, true);
        QueueService.BlockchainAction action4 = new QueueService.BlockchainAction(() -> {}, true);
        //check action1
        assertThat(action1).isEqualByComparingTo(action1);
        assertThat(action1.compareTo(action2)).isEqualTo(hasHigherPriority);
        assertThat(action1.compareTo(action3)).isEqualTo(hasLowerPriority);
        assertThat(action1.compareTo(action4)).isEqualTo(hasLowerPriority);
        //check action2
        assertThat(action2).isEqualByComparingTo(action2);
        assertThat(action2.compareTo(action1)).isEqualTo(hasLowerPriority);
        assertThat(action2.compareTo(action3)).isEqualTo(hasLowerPriority);
        assertThat(action2.compareTo(action4)).isEqualTo(hasLowerPriority);
        //check action3
        assertThat(action3).isEqualByComparingTo(action3);
        assertThat(action3.compareTo(action1)).isEqualTo(hasHigherPriority);
        assertThat(action3.compareTo(action2)).isEqualTo(hasHigherPriority);
        assertThat(action3.compareTo(action4)).isEqualTo(hasHigherPriority);
        //check action4
        assertThat(action4).isEqualByComparingTo(action4);
        assertThat(action4.compareTo(action1)).isEqualTo(hasHigherPriority);
        assertThat(action4.compareTo(action2)).isEqualTo(hasHigherPriority);
        assertThat(action4.compareTo(action3)).isEqualTo(hasLowerPriority);
    }
    //endregion

    //region TaskWithPriority
    @Test
    void compareTaskWithPriorityAgainstNul() {
        QueueService.BlockchainAction lowPriorityAction = new QueueService.BlockchainAction(() -> {}, false);
        QueueService.BlockchainAction highPriorityAction = new QueueService.BlockchainAction(() -> {}, true);
        assertThatThrownBy(() -> lowPriorityAction.compareTo(null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> highPriorityAction.compareTo(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void validateTaskWithPriorityComparisons() {
        QueueService.BlockchainAction lowPriorityAction = new QueueService.BlockchainAction(() -> {}, false);
        QueueService.TaskWithPriority<Runnable> task1 = new QueueService.TaskWithPriority<>(lowPriorityAction);
        QueueService.TaskWithPriority<Runnable> task2 = new QueueService.TaskWithPriority<>(lowPriorityAction);
        assertThat(task1.compareTo(task2)).isZero();
    }
    //endregion
}
