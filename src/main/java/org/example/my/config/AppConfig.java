package org.example.my.config;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AppConfig {
    public static final String INPUT_FILE_PATH = "input.txt";
    public static final String OUTPUT_FILE_PATH = "output.txt";
    public static final int SEMAPHORE_PERMITS = 100;
    public static final int SEMAPHORE_INTERVAL_MILLIS = 1000;
    public static final int SEMAPHORE_INITIAL_DELAY = 0;
    public static final int INITIAL_SEMAPHORE_PERMIT = 0;
    public static final int CORE_POOL_SIZE_SCHEDULER = 1;
    public static final long POLL_TIMEOUT_MS = 100;
    public static final long AWAIT_TERMINATION = 1;

}

