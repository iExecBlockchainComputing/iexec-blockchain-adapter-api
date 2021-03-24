package com.iexec.blockchain.tool;

import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class QueueService {

    private final ExecutorService executorService;

    public QueueService() {
        executorService = Executors.newFixedThreadPool(1);
    }

    public void runAsync(Runnable runnable) {
        CompletableFuture.runAsync(runnable, executorService);
    }
}
