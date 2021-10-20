package com.iexec.blockchain.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.*;

/**
 * Execute {@link Runnable}s as they arrive.
 * Some {@link Runnable}s can have higher priority than others, so they can be treated before the others.
 */
@Slf4j
@Service
public class QueueService {
    // We'd like to give more chance to the high priority tasks to execute first.
    private static final long POLL_TIMEOUT_IN_MILLISECONDS = 10;

    private final ExecutorService executorService;
    private final BlockingQueue<Runnable> lowPriorityQueue  = new LinkedBlockingQueue<>();
    private final BlockingQueue<Runnable> highPriorityQueue = new LinkedBlockingQueue<>();

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
        if (taskExecutor != null && !taskExecutor.isDone()) return;
        
        taskExecutor = CompletableFuture.runAsync(this::executeTasks);
    }

    /**
     * Execute {@link Runnable}s as they come.
     * At each occurrence, the following rules are applied:
     * <ul>
     *     <li>If there is at least one priority {@link Runnable}, execute the first one;</li>
     *     <li>Otherwise, execute the first non-priority {@link Runnable}.</li>
     * </ul>
     */
    void executeTasks() {
        while (Thread.currentThread().isAlive()) {
            executeFirstTask();
        }
    }

    /**
     * Execute a single task with the following rules:
     * <ul>
     *     <li>If there is at least one priority {@link Runnable}, execute the first one;</li>
     *     <li>Otherwise, execute the first non-priority {@link Runnable}.</li>
     * </ul>
     */
    void executeFirstTask() {
        try {
            // Wait until a new runnable is available.
            Runnable runnable = getExecutableRunnable();
            CompletableFuture.runAsync(runnable, executorService);
        } catch (InterruptedException e) {
            log.error("Task thread got interrupted.", e);
            Thread.currentThread().interrupt();
        } catch (RuntimeException e) {
            log.error("An error occurred while waiting for new tasks.", e);
        }
    }

    /**
     * Retrieve and return the first runnable.
     * If there is at least one priority runnable, return the first one or wait a few milliseconds to get one.
     * If there is no priority runnable and at least one non-priority runnable, return the first one.
     * If there is no runnable at all, back to first step.
     */
    private Runnable getExecutableRunnable() throws InterruptedException {
        while (Thread.currentThread().isAlive()) {
            // Try to take from high priority queue before looking at low priority queue.
            Runnable runnable = highPriorityQueue.poll(POLL_TIMEOUT_IN_MILLISECONDS, TimeUnit.MILLISECONDS);
            if (runnable != null) {
                return runnable;
            }
            runnable = lowPriorityQueue.poll();
            if (runnable != null) {
                return runnable;
            }
        }

        // Little hack to make this method always return non-null object.
        // This avoids some warnings such as "Argument 'xxx' can be null".
        return () -> {};
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
        if (priority) {
            highPriorityQueue.add(runnable);
        } else {
            lowPriorityQueue.add(runnable);
        }
    }
}

