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
        // создание очереди на чтение, очереди на запись, флага остановки
        BlockingQueue<InputLine> inputQueue = new LinkedBlockingQueue<>();
        BlockingQueue<OutLine> outputQueue = new LinkedBlockingQueue<>();
        // создание флагов остановки
        AtomicBoolean stopWorkersFlag = new AtomicBoolean(false);
        AtomicBoolean stopWriterFlag = new AtomicBoolean(false);
        // создание потоков
        ExecutorService reader = Executors.newSingleThreadExecutor();
        ExecutorService workers = Executors.newFixedThreadPool(poolSize);
        ExecutorService writer = Executors.newSingleThreadExecutor();
        // создание ограничителей: лимит разрешений
        Semaphore rateLimiter = new Semaphore(AppConfig.INITIAL_SEMAPHORE_PERMIT);
        // создание ограничителей: наполнитель разрешений
        ScheduledExecutorService tokenIntervalFiller = Executors.newSingleThreadScheduledExecutor();
        // конфигурация наполнителя
        tokenIntervalFiller.scheduleAtFixedRate(
                () -> rateLimiter.release(AppConfig.SEMAPHORE_PERMITS),
                AppConfig.SEMAPHORE_INITIAL_DELAY,
                AppConfig.SEMAPHORE_INTERVAL_MILLIS,
                TimeUnit.MILLISECONDS
        );
        reader.execute(new ProducerService(inputQueue));
        new FactorialManager().submitWorkers(
                inputQueue, outputQueue, rateLimiter,
                workers, poolSize, limiterEnabled, stopWorkersFlag
        );
        writer.execute(new ConsumerService(outputQueue, stopWriterFlag));
        // Последовательное завершение
        shutdownWithCompletableFuture(reader, workers, writer, tokenIntervalFiller,
                inputQueue, outputQueue,
                stopWorkersFlag, stopWriterFlag);

    }

    private static void shutdownWithCompletableFuture(
            ExecutorService reader,
            ExecutorService workers,
            ExecutorService writer,
            ScheduledExecutorService tokenIntervalFiller,
            BlockingQueue<InputLine> inputQueue,
            BlockingQueue<OutLine> outputQueue,
            AtomicBoolean stopWorkersFlag,
            AtomicBoolean stopWriterFlag
    ) {
        CompletableFuture<?> shutdownSequence = CompletableFuture
                .runAsync(() -> shutdownExecutor("Reader", reader))
                .thenRunAsync(() -> awaitQueueDrain("Input Queue", inputQueue))
                .thenRunAsync(() -> {
                    stopWorkersFlag.set(true);
                    shutdownExecutor("Workers", workers);
                })
                .thenRunAsync(() -> shutdownExecutor("Token Interval Filler", tokenIntervalFiller))
                .thenRunAsync(() -> awaitQueueDrain("Output Queue", outputQueue))
                .thenRunAsync(() -> {
                    stopWriterFlag.set(true);
                    shutdownExecutor("Writer", writer);
                });

        try {
            shutdownSequence.get(AppConfig.SHUTDOWN_TIMEOUT_SEC, TimeUnit.SECONDS); // Таймаут на завершение
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Shutdown interrupted: ", e);
        } catch (ExecutionException | TimeoutException e) {
            log.error(e.getMessage());
        }
    }

    private static void shutdownExecutor(String name, ExecutorService service) {
        log.info("Shutting down {}...", name);
        service.shutdown();
        try {
            if (!service.awaitTermination(AppConfig.EXECUTOR_SHUTDOWN_TIMEOUT_SEC, TimeUnit.SECONDS)) {
                log.warn("Forcing {} shutdown", name);
                service.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            service.shutdownNow();
        }
    }

    private static void awaitQueueDrain(String queueName, BlockingQueue<?> queue) {

        while (!queue.isEmpty()) {
            try {
                TimeUnit.MILLISECONDS.sleep(AppConfig.WAITING_TIME_FOR_FULL_QUEUE_MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        log.debug("{} drained", queueName);

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
}
