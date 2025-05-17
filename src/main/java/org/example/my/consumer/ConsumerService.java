package org.example.my.consumer;

import org.example.my.config.AppConfig;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;

public class ConsumerService implements Runnable {

    private final BlockingQueue<OutLine> outputQueue;
    private final AppConfig config;

    public ConsumerService(BlockingQueue<OutLine> outputQueue, AppConfig config) {
        this.outputQueue = outputQueue;
        this.config = config;
    }

    @Override
    public void run() {
        int expectedIndex = 0;
        Map<Integer, String> buffer = new TreeMap<>();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(config.outputFilePath()))) {
            while (true) {
                OutLine out = outputQueue.take();

                if (out.index() == config.eofMark().index() && buffer.isEmpty()) {
                    break;
                }

                buffer.put(out.index(), out.result());

                while (buffer.containsKey(expectedIndex)) {
                    writer.write(buffer.get(expectedIndex));
                    writer.newLine();
                    buffer.remove(expectedIndex++);
                }
            }
        } catch (Exception e) {
            System.err.println("Error writing to file: " + e.getMessage());
        }
    }
}
