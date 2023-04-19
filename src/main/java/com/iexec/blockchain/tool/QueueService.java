package com.iexec.blockchain.tool;

import lombok.EqualsAndHashCode;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.util.concurrent.*;

/**
 * Execute {@link Runnable}s as they are submitted.
 * <p>
 * The thread pool uses a {@link java.util.concurrent.PriorityBlockingQueue} to execute tasks depending on a priority order.
 * The {@code ThreadPoolExecutor#newTaskFor} method is overridden to wrap the submitted {@link java.lang.Runnable} in a
 * {@link TaskWithPriority<Runnable>} where it will be cast back to a {@link BlockchainAction}.
 * <p>
 * The {@link TaskWithPriority} is a {@link java.lang.Comparable} with a deferred call to {@link BlockchainAction#compareTo(BlockchainAction)}.
 * This enables the thread pool to retrieve tasks from the queue depending on the implemented priority rule.
 * The priority rule is a simple {@code boolean} flag in {@link BlockchainAction}.
 * Tasks with a priority flag defined as {@literal true}, then a lower creation timestamp are sorted first.
 */
@Service
public class QueueService {
    private final PriorityBlockingQueue<Runnable> queue = new PriorityBlockingQueue<>();
    private final ThreadPoolExecutor executorService;

    public QueueService() {
        executorService = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, queue) {
            @Override
            protected <T> RunnableFuture<T> newTaskFor(@NotNull Runnable runnable, T value) {
                return new TaskWithPriority<>(runnable);
            }
        };
    }

    /**
     * Submit a {@link Runnable} to the thread pool.
     *
     * @param runnable {@link Runnable} to submit to the queue.
     * @param priority Whether this {@link Runnable} has a high ({@literal true}) or low ({@literal false}) priority.
     */
    public void addExecutionToQueue(Runnable runnable, boolean priority) {
        executorService.submit(new BlockchainAction(runnable, priority));
    }

    /**
     * Represent an action submitted to the {@link java.util.concurrent.PriorityBlockingQueue}.
     * It contains its timestamp creation, its priority and its {@link Runnable}.
     */
    @EqualsAndHashCode
    static class BlockchainAction implements Comparable<BlockchainAction>, Runnable {
        private final Runnable runnable;
        private final boolean priority;
        private final long time;

        public BlockchainAction(Runnable runnable, boolean priority) {
            this.runnable = runnable;
            this.priority = priority;
            this.time = System.nanoTime();
        }

        @Override
        public void run() {
            runnable.run();
        }

        @Override
        public int compareTo(@NotNull BlockchainAction other) {
            if (this.priority && !other.priority) {
                return -1;
            }
            if (!this.priority && other.priority) {
                return 1;
            }
            return Long.compare(this.time, other.time);
        }
    }

    /**
     * Wrap a {@link BlockchainAction} for a thread pool with a {@code PriorityBlockingQueue<Runnable>}.
     * <p>
     * These class instances are {@code Comparable} and defer the comparison to {@link BlockchainAction#compareTo(BlockchainAction)}.
     */
    @EqualsAndHashCode(callSuper = true)
    static class TaskWithPriority<T> extends FutureTask<T> implements Comparable<TaskWithPriority<?>> {
        private final BlockchainAction action;
        TaskWithPriority(Runnable task) {
            super(task, null);
            this.action = (BlockchainAction) task;
        }

        @Override
        public int compareTo(@NotNull TaskWithPriority other) {
            return this.action.compareTo(other.action);
        }
    }

}

