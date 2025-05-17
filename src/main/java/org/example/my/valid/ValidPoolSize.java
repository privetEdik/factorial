package org.example.my.valid;

public class ValidPoolSize {
    private ValidPoolSize() {
    }

    public static int findPoolSize(String[] args) {
        int defaultPoolSize = 100;
        int poolSize = defaultPoolSize;

        if (args.length == 0) {
            System.out.println("No <worker-pool-size> argument provided. Using default value: " + defaultPoolSize);
            return poolSize;
        }

        try {
            int parsed = Integer.parseInt(args[0]);
            if (parsed >= 1 && parsed <= 200) {
                poolSize = parsed;
            } else {
                System.out.println("Invalid <worker-pool-size> value. Must be between 1 and 200 (inclusive). Using default: " + defaultPoolSize);
            }
        } catch (NumberFormatException e) {
            System.out.println("Failed to parse <worker-pool-size> as an integer. Using default: " + defaultPoolSize);
        }

        return poolSize;
    }
}

