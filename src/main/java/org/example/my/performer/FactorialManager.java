package org.example.my.performer;

import org.example.my.consumer.OutLine;
import org.example.my.producer.InputLine;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

public class FactorialManager {
    public void submitWorkers(BlockingQueue<InputLine> inputQueue,
                              BlockingQueue<OutLine> outputQueue,
                              Semaphore tokenLimiter,
                              ExecutorService executor,
                              int poolSize,
                              boolean limiterEnabled,
                              AtomicBoolean stopFlag) {
        for (int i = 0; i < poolSize; i++) {
            executor.submit(new FactorialWorker(inputQueue, outputQueue, tokenLimiter, limiterEnabled, stopFlag));
        }
    }
}
