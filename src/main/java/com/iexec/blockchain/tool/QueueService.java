package com.iexec.blockchain.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * Execute {@link Runnable}s as they arrive.
 * Some {@link Runnable}s can have higher priority than others, so they can be treated before the others.
 */
@Slf4j
@Service
public class QueueService {
    private final ExecutorService executorService;
    private final PriorityBlockingQueue<WaitingTask> queue = new PriorityBlockingQueue<>();

    private CompletableFuture<Void> taskExecutor;

    public QueueService() {
        executorService = Executors.newFixedThreadPool(1);
    }

    /**
     * Scheduled method execution.
     * If the {@link QueueService#taskExecutor} isn't running, start a new thread.
     * Otherwise, do nothing.
     */
    @Scheduled(fixedRate = 30000)
    private void startAsyncTasksExecution() {
        if (taskExecutor != null && !taskExecutor.isDone()) {
            return;
        }
        
        taskExecutor = CompletableFuture.runAsync(this::executeTasks);
    }

    /**
     * Execute {@link Runnable}s as they come.
     * At each occurrence, execute the first task in the queue or wait for a new one to appear.
     * The first task is the task with the highest priority that is in the queue for the longer time.
     */
    void executeTasks() {
        while (Thread.currentThread().isAlive()) {
            executeFirstTask();
        }
    }

    /**
     * Execute the first task in the queue or wait for a new one to appear.
     * The first task is the task with the highest priority that is in the queue for the longer time.
     */
    void executeFirstTask() {
        try {
            // Wait until a new task is available.
            Runnable runnable = queue.take().getRunnable();
            CompletableFuture.runAsync(runnable, executorService);
        } catch (InterruptedException e) {
            log.error("Task thread got interrupted.", e);
            Thread.currentThread().interrupt();
        } catch (RuntimeException e) {
            log.error("An error occurred while waiting for new tasks.", e);
        }
    }

    /**
     * Add a {@link Runnable} to the low priority or the high priority queue, depending on {@code priority}.
     * If it's not priority, it will be executed once:
     * <ul>
     *     <li>All low priority {@link Runnable}s inserted before this one have completed;</li>
     *     <li>All high priority {@link Runnable}s inserted before the execution of this one have completed.</li>
     * </ul>
     *
     * If it's priority, it will be executed once
     * all high priority {@link Runnable}s inserted before this one have completed.
     *
     * @param runnable {@link Runnable} to execute.
     * @param priority Whether this {@link Runnable} is priority.
     */
    public void addExecutionToQueue(Runnable runnable, boolean priority) {
        queue.add(new WaitingTask(runnable, priority));
    }

    /**
     * Represent a task that could wait in a {@link java.util.Queue}.
     * It contains its timestamp creation, its priority and its {@link Runnable}.
     */
    private static class WaitingTask implements Comparable<WaitingTask> {
        private final Runnable runnable;
        private final boolean priority;
        private final long time;

        public WaitingTask(Runnable runnable, boolean priority) {
            this.runnable = runnable;
            this.priority = priority;
            this.time = System.nanoTime();
        }

        public Runnable getRunnable() {
            return runnable;
        }

        @Override
        public int compareTo(WaitingTask other) {
            if (other == null) {
                return -1;
            }
            if (this.priority && !other.priority) {
                return -1;
            }
            if (!this.priority && other.priority) {
                return 1;
            }
            return Long.compare(this.time, other.time);
        }
    }
}

