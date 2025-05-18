package org.example.my.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.my.config.AppConfig;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;

@RequiredArgsConstructor
@Slf4j
public class ProducerService implements Runnable {
    private final BlockingQueue<InputLine> inputQueue;

    @Override
    public void run() {
        try (BufferedReader reader = new BufferedReader(new FileReader(AppConfig.INPUT_FILE_PATH))) {
            String line;
            int index = 0;
            while ((line = reader.readLine()) != null) {
                inputQueue.put(new InputLine(index++, line.trim()));
            }
        } catch (IOException e) {
            log.error("Error reading input file: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Error put to Queue: " + e.getMessage());
        }
    }
}

