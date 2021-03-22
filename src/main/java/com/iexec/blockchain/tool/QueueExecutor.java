package com.iexec.blockchain.tool;

import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class QueueExecutor {

    private final ExecutorService executorService;

    public QueueExecutor() {
        executorService = Executors.newFixedThreadPool(1);
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }
}
