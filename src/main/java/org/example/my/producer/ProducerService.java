package org.example.my.producer;

import org.example.my.config.AppConfig;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.concurrent.BlockingQueue;


public class ProducerService implements Runnable {

    private final BlockingQueue<InputLine> inputQueue;
    private final AppConfig  config;

    public ProducerService(BlockingQueue<InputLine> inputQueue, AppConfig config) {
        this.inputQueue = inputQueue;
        this.config = config;
    }

    @Override
    public void run() {
        try (BufferedReader reader = new BufferedReader(new FileReader(config.inputFilePath()))) {
            String line;
            int index = 0;
            while ((line = reader.readLine()) != null) {
                inputQueue.put(new InputLine(index++, line.trim()));
            }

            inputQueue.put(config.eofMark());
        } catch (Exception e) {
            System.err.println("Error reading input file: " + e.getMessage());
        }
    }
}

