package org.example.my;

import lombok.extern.slf4j.Slf4j;
import org.example.my.config.AppConfig;
import org.example.my.consumer.ConsumerService;
import org.example.my.consumer.OutLine;
import org.example.my.performer.FactorialManager;
import org.example.my.producer.InputLine;
import org.example.my.producer.ProducerService;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class FactorialCalculatorMain {
    public static void main(String[] args) {
        //  получение данных из консоли: пул потоков, возможность выключать работу лимитера(не по тз)
        int poolSize = findPoolSize(args);
        boolean limiterEnabled = findLimiterFlag(args);
        //создание очереди на чтение, очереди на запись, флага остановки
        BlockingQueue<InputLine> inputQueue = new LinkedBlockingQueue<>();
        BlockingQueue<OutLine> outputQueue = new LinkedBlockingQueue<>();
        AtomicBoolean stopFlag = new AtomicBoolean(false);
        // создание потоков
        ExecutorService reader = Executors.newSingleThreadExecutor();
        ExecutorService writer = Executors.newSingleThreadExecutor();
        ExecutorService workers = Executors.newFixedThreadPool(poolSize);
        // создание ограничителей: лимит разрешений
        Semaphore tokenLimiter = new Semaphore(AppConfig.INITIAL_SEMAPHORE_PERMIT);
        //                       : частота выдачи
        ScheduledExecutorService limiter = Executors.newScheduledThreadPool(AppConfig.CORE_POOL_SIZE_SCHEDULER);
        limiter.scheduleAtFixedRate(
                () -> tokenLimiter.release(AppConfig.SEMAPHORE_PERMITS),
                AppConfig.SEMAPHORE_INITIAL_DELAY,
                AppConfig.SEMAPHORE_INTERVAL_MILLIS,
                TimeUnit.MILLISECONDS);
        // старт выполнения
        reader.submit(new ProducerService(inputQueue));
        FactorialManager manager = new FactorialManager();
        manager.submitWorkers(
                inputQueue,
                outputQueue,
                tokenLimiter,
                workers,
                poolSize,
                limiterEnabled,
                stopFlag);
        writer.submit(new ConsumerService(outputQueue, stopFlag));
        //завершение
        shutdownAndAwait(reader, "reader");
        stopFlag.set(true);
        shutdownAndAwait(workers, "workers");
        shutdownAndAwait(writer, "writer");

        limiter.shutdownNow();
        log.info("All operations completed. Application exits cleanly.");

    }

    private static int findPoolSize(String[] args) {
        int defaultPoolSize = 100;
        if (args.length == 0) {
            log.info("No pool size provided, using default: {}", defaultPoolSize);
            return defaultPoolSize;
        }

        try {
            int parsed = Integer.parseInt(args[0]);
            if (parsed < 1 || parsed > 200) throw new IllegalArgumentException();
            return parsed;
        } catch (Exception e) {
            log.error("Invalid pool size '{}', using default: {}", args[0], defaultPoolSize);
            return defaultPoolSize;
        }
    }

    private static boolean findLimiterFlag(String[] args) {
        if (args.length < 2) {
            return true; // default = enabled
        }
        try {
            return Boolean.parseBoolean(args[1]); // parses "true", "false"
        } catch (Exception e) {
            log.info("Invalid limiter flag. Using default: true");
            return true;
        }
    }

    private static void shutdownAndAwait(ExecutorService service, String name) {
        service.shutdown();
        try {
            while (!service.awaitTermination(AppConfig.AWAIT_TERMINATION, TimeUnit.SECONDS)) {
                log.info("Waiting for {} to finish...", name);
            }
        } catch (InterruptedException e) {
            log.warn("Interrupted while waiting for {} to finish", name);
            Thread.currentThread().interrupt();
        }
    }

}
