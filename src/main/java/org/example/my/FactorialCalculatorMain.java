package org.example.my;

import org.example.my.config.AppConfig;
import org.example.my.consumer.ConsumerService;
import org.example.my.consumer.OutLine;
import org.example.my.performer.PerformerFactorial;
import org.example.my.producer.InputLine;
import org.example.my.producer.ProducerService;
import org.example.my.valid.ValidPoolSize;

import java.util.concurrent.*;

public class FactorialCalculatorMain {
    public static void main(String[] args) throws InterruptedException {

        int poolSize = ValidPoolSize.findPoolSize(args);

        AppConfig config = AppConfig.defaultConfig();

        BlockingQueue<InputLine> inputQueue = new LinkedBlockingQueue<>();
        BlockingQueue<OutLine> outputQueue = new LinkedBlockingQueue<>();
        Semaphore tokenLimiter = new Semaphore(0);

        ExecutorService reader = Executors.newSingleThreadExecutor();
        reader.submit(new ProducerService(inputQueue, config));
        reader.shutdown();

        ExecutorService writer = Executors.newSingleThreadExecutor();
        writer.submit(new ConsumerService(outputQueue, config));

        ScheduledExecutorService limiter = Executors.newScheduledThreadPool(1);
        limiter.scheduleAtFixedRate(
                () -> tokenLimiter.release(config.tokensPerSecond()),
                0,
                config.tokenIntervalMillis(),
                TimeUnit.MILLISECONDS
        );

        ExecutorService workers = Executors.newFixedThreadPool(poolSize);
        PerformerFactorial performer = new PerformerFactorial(inputQueue, outputQueue, tokenLimiter, config);
        performer.startWorkers(workers, poolSize);

        reader.shutdown();
        while (!reader.awaitTermination(1, TimeUnit.SECONDS)) {
            System.out.println("Waiting for reader thread...");
        }
        for (int i = 0; i < poolSize; i++) {
            inputQueue.put(config.eofMark());
        }

        workers.shutdown();
        while (!workers.awaitTermination(1, TimeUnit.SECONDS)) {
            System.out.println("Waiting for worker threads to finish...");
        }

        outputQueue.put(new OutLine(config.eofMark().index(), config.eofMark().rawLine()));

        writer.shutdown();
        while (!writer.awaitTermination(1, TimeUnit.SECONDS)) {
            System.out.println("Waiting for writer thread...");
        }

        limiter.shutdownNow();

        System.out.println("All operations completed. Application exits cleanly.");


    }
}
