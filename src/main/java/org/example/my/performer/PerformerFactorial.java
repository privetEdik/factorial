package org.example.my.performer;

import org.example.my.config.AppConfig;
import org.example.my.consumer.OutLine;
import org.example.my.producer.InputLine;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;

public class PerformerFactorial {

    private final BlockingQueue<InputLine> inputQueue;
    private final BlockingQueue<OutLine> outputQueue;
    private final Semaphore tokenLimiter;
    private final AppConfig config;

    public PerformerFactorial(BlockingQueue<InputLine> inputQueue, BlockingQueue<OutLine> outputQueue,
                              Semaphore tokenLimiter, AppConfig config) {
        this.inputQueue = inputQueue;
        this.outputQueue = outputQueue;
        this.tokenLimiter = tokenLimiter;
        this.config = config;
    }

    public void startWorkers(ExecutorService executor, int poolSize) {
        for (int i = 0; i < poolSize; i++) {
            executor.submit(new FactorialWorker(inputQueue, outputQueue, tokenLimiter, config));
        }
    }
}
