package com.iexec.blockchain.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.concurrent.*;

/**
 * Execute {@link Runnable}s as they arrive.
 * Some {@link Runnable}s can have higher priority than others, so they can be treated before the others.
 */
@Slf4j
@Service
public class QueueService {
    private static final long POLL_TIMEOUT_IN_MILLISECONDS = 100;

    private final ExecutorService executorService;
    private final BlockingQueue<Runnable> lowPriorityQueue  = new LinkedBlockingQueue<>();
    private final BlockingQueue<Runnable> highPriorityQueue = new LinkedBlockingQueue<>();

    public QueueService() {
        executorService = Executors.newFixedThreadPool(1);
    }

    @PostConstruct
    private void startAsyncTasksExecution() {
        CompletableFuture.runAsync(this::executeTasks);
    }

    /**
     * Execute {@link Runnable}s as they come.
     * At each occurrence, the following rules are applied:
     * <ul>
     *     <li>If there is at least one priority {@link Runnable}, execute the first one;</li>
     *     <li>Otherwise, execute the first non-priority {@link Runnable}.</li>
     * </ul>
     */
    private void executeTasks() {
        while (Thread.currentThread().isAlive()) {
            Runnable runnable = null;
            try {
                // Wait until a new runnable is available.
                // Try to take into high priority queue for a few millis
                // before looking at low priority queue for the same period.
                while (runnable == null) {
                    runnable = highPriorityQueue.poll(POLL_TIMEOUT_IN_MILLISECONDS, TimeUnit.MILLISECONDS);
                    if (runnable == null) {
                        runnable = lowPriorityQueue.poll(POLL_TIMEOUT_IN_MILLISECONDS, TimeUnit.MILLISECONDS);
                    }
                }
                CompletableFuture.runAsync(runnable, executorService);
            } catch (InterruptedException e) {
                log.error("Task thread got interrupted.", e);
                Thread.currentThread().interrupt();
            } catch (RuntimeException e) {
                log.error("An error occurred while waiting for new tasks.", e);
            }
        }
    }

    /**
     * Add a {@link Runnable} to the tail of the low priority queue.
     * It will be executed once:
     * <ul>
     *     <li>All low priority {@link Runnable}s inserted before this one have completed;</li>
     *     <li>All high priority {@link Runnable}s inserted before the execution of this one have completed.</li>
     * </ul>
     *
     * @param runnable {@link Runnable} to execute.
     */
    public void addExecutionToQueue(Runnable runnable) {
        addExecutionToQueue(runnable, false);
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

