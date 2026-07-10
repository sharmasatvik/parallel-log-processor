package com.logprocessor.util;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

/**
 * Generates a synthetic log file for testing and benchmarking the
 * parallel log processor against realistically sized input.
 */
public class SampleLogGenerator {

    private static final String[] LEVELS = {"INFO", "DEBUG", "WARN", "ERROR"};
    private static final String[] MESSAGES = {
            "user login successful",
            "connection timeout while calling downstream service",
            "cache miss for key",
            "request completed in reasonable time",
            "failed to parse request body",
            "retrying operation after transient failure",
            "database connection pool exhausted",
            "scheduled job completed successfully"
    };

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("Usage: SampleLogGenerator <outputPath> <numLines>");
            return;
        }

        String outputPath = args[0];
        int numLines = Integer.parseInt(args[1]);

        generate(outputPath, numLines);
        System.out.println("Generated " + numLines + " lines at " + outputPath);
    }

    public static void generate(String outputPath, int numLines) throws IOException {
        Random random = new Random(42); // fixed seed for reproducible test data

        try (BufferedWriter writer = Files.newBufferedWriter(Path.of(outputPath))) {
            for (int i = 0; i < numLines; i++) {
                String level = LEVELS[random.nextInt(LEVELS.length)];
                String message = MESSAGES[random.nextInt(MESSAGES.length)];
                writer.write(String.format("%s [thread-%d] %s%n", level, random.nextInt(20), message));
            }
        }
    }
}