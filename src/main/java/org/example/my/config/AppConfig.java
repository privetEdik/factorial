package org.example.my.config;

import org.example.my.producer.InputLine;

public record AppConfig(
        String inputFilePath,
        String outputFilePath,
        Integer tokensPerSecond,
        Integer tokenIntervalMillis,
        InputLine eofMark) {

    public static AppConfig defaultConfig() {
        return new AppConfig(
                "input.txt",
                "output.txt",
                100,
                1000,
                new InputLine(-1, "__EOF__")
        );
    }
}

