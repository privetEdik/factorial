package org.example.my.performer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.my.config.AppConfig;
import org.example.my.consumer.OutLine;
import org.example.my.producer.InputLine;

import java.math.BigInteger;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@RequiredArgsConstructor
@Slf4j
public class FactorialWorker implements Runnable {

    private final BlockingQueue<InputLine> inputQueue;
    private final BlockingQueue<OutLine> outputQueue;
    private final Semaphore tokenLimiter;
    private final boolean limiterEnabled;
    private final AtomicBoolean stopFlag;
    private static final int START_POSITION_FACTORIAL = 2;

    @Override
    public void run() {
        try {
            while (true) {
                InputLine input = inputQueue.poll(AppConfig.POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                if (input == null) {
                    if (stopFlag.get() && inputQueue.isEmpty()) return;
                    continue;
                }
                //хочу отключать лимитер для быстрого тестирования
                if (limiterEnabled) {
                    tokenLimiter.acquire();
                }
                String factorialForOut = formatterStringFactorial(input);
                outputQueue.put(new OutLine(input.index(), factorialForOut));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.info("Thread is interrupted : " + e.getMessage());
        }
    }

    private String formatterStringFactorial(InputLine inputLine) {
        String result;
        try {
            int number = Integer.parseInt(inputLine.rawLine());
            result = String.format("%s = %s", number, calculateFactorial(number));
            //  если пустая строка или символ(не число), то его выведем
        } catch (NumberFormatException e) {
            result = inputLine.rawLine();
        }
        return result;
    }

    private BigInteger calculateFactorial(int n) {
        BigInteger result = BigInteger.ONE;
        for (int i = START_POSITION_FACTORIAL; i <= n; i++) {
            result = result.multiply(BigInteger.valueOf(i));
        }
        return result;
    }
}

