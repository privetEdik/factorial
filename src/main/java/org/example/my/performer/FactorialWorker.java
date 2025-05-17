package org.example.my.performer;

import org.example.my.config.AppConfig;
import org.example.my.consumer.OutLine;
import org.example.my.producer.InputLine;

import java.math.BigInteger;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;

public class FactorialWorker implements Runnable {

    private final BlockingQueue<InputLine> inputQueue;
    private final BlockingQueue<OutLine> outputQueue;
    private final Semaphore tokenLimiter;
    private final AppConfig config;

    public FactorialWorker(BlockingQueue<InputLine> inputQueue, BlockingQueue<OutLine> outputQueue, Semaphore tokenLimiter, AppConfig config) {
        this.inputQueue = inputQueue;
        this.outputQueue = outputQueue;
        this.tokenLimiter = tokenLimiter;
        this.config = config;
    }

    @Override
    public void run() {
        try {
            while (true) {
                InputLine input = inputQueue.take();

                if (input.equals(config.eofMark())) {
                    inputQueue.put(input); // завершаем и передаем эстафету для другого потока
                    return;
                }

                tokenLimiter.acquire();

                String factorialForOut = formatterStringFactorial(input);

                outputQueue.put(new OutLine(input.index(), factorialForOut));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String formatterStringFactorial(InputLine inputLine) {

        String result;
        try {
            int number = Integer.parseInt(inputLine.rawLine());
            result = number + " = " + calculateFactorial(number);
        } catch (NumberFormatException e) {
            result = inputLine.rawLine();
        }

        return result;
    }

    private BigInteger calculateFactorial(int n) {
        BigInteger result = BigInteger.ONE;
        for (int i = 2; i <= n; i++) {
            result = result.multiply(BigInteger.valueOf(i));
        }
        return result;
    }
}

