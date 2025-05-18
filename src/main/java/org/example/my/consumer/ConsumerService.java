package org.example.my.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.my.config.AppConfig;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@RequiredArgsConstructor
@Slf4j
public class ConsumerService implements Runnable {

    private final BlockingQueue<OutLine> outputQueue;
    private final AtomicBoolean stopFlag;

    @Override
    public void run() {
        int expectedIndex = 0;
        Map<Integer, String> buffer = new TreeMap<>();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(AppConfig.OUTPUT_FILE_PATH))) {
            do {
                OutLine out = outputQueue.poll(AppConfig.POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                if (out != null) {
                    buffer.put(out.index(), out.result());
                }
                while (buffer.containsKey(expectedIndex)) {
                    writer.write(buffer.get(expectedIndex));
                    writer.newLine();
                    buffer.remove(expectedIndex++);
                }
            // если установлен флаг и пуста очередь и пустой буфер - запись закончим
            } while (!(stopFlag.get() && outputQueue.isEmpty() && buffer.isEmpty()));
        } catch (IOException e) {
            log.error("Error writing file: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Writer thread was interrupted. Shutting down..." + e.getMessage());
        }
    }
}
