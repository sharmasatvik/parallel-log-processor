package com.satvik.logprocessor;

import com.satvik.logprocessor.aggregator.ResultAggregator;
import com.satvik.logprocessor.analyzer.ChunkAnalyzer;
import com.satvik.logprocessor.chunker.FileChunker;
import com.satvik.logprocessor.model.AggregatedResult;
import com.satvik.logprocessor.model.ChunkResult;
import com.satvik.logprocessor.model.FileChunk;
import com.satvik.logprocessor.processor.LogProcessor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Entry point for the Parallel Log Processor application.
 * <p>
 * Runs both a sequential baseline and a parallel version of the same
 * analysis over the given log file, printing timing for each so the
 * speedup from parallelization is directly visible.
 */
public class Main {

    private static final int DEFAULT_NUM_CHUNKS = 8;
    private static final int DEFAULT_THREAD_POOL_SIZE = 4;

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java -jar parallel-log-processor.jar <path-to-log-file> [numChunks] [threadPoolSize]");
            return;
        }

        String filePath = args[0];
        int numChunks = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_NUM_CHUNKS;
        int threadPoolSize = args.length > 2 ? Integer.parseInt(args[2]) : DEFAULT_THREAD_POOL_SIZE;

        try {
            runSequentialBaseline(filePath, numChunks);
            runParallelVersion(filePath, numChunks, threadPoolSize);
        } catch (IOException e) {
            System.err.println("Failed to process file: " + e.getMessage());
        }
    }

    /**
     * Runs chunk analysis one chunk at a time on the main thread, as a
     * baseline for comparing against the parallel version.
     */
    private static void runSequentialBaseline(String filePath, int numChunks) throws IOException {
        FileChunker fileChunker = new FileChunker();
        ChunkAnalyzer chunkAnalyzer = new ChunkAnalyzer();
        ResultAggregator aggregator = new ResultAggregator();

        final var chunks = fileChunker.splitIntoChunks(filePath, numChunks);

        long startTime = System.currentTimeMillis();

        List<ChunkResult> results = new ArrayList<>();
        for (FileChunk chunk : chunks) {
            results.add(chunkAnalyzer.analyze(filePath, chunk));
        }

        long elapsedMs = System.currentTimeMillis() - startTime;

        final var aggregated = aggregator.aggregate(results);

        System.out.println("=== Sequential Baseline ===");
        printResult(aggregated, elapsedMs);
    }

    /**
     * Runs chunk analysis concurrently across a thread pool using
     * LogProcessor, then prints the same stats plus elapsed time so it
     * can be directly compared against the sequential run above.
     */
    private static void runParallelVersion(String filePath, int numChunks, int threadPoolSize) throws IOException {
        final var logProcessor = new LogProcessor(threadPoolSize);
        final var aggregator = new ResultAggregator();

        long startTime = System.currentTimeMillis();

        final var results = logProcessor.processInParallel(filePath, numChunks);

        long elapsedMs = System.currentTimeMillis() - startTime;

        // Always shut down the executor once processing is done to avoid
        // leaking non-daemon threads and keeping the JVM alive.
        logProcessor.shutdown();

        final var aggregated = aggregator.aggregate(results);

        System.out.println("=== Parallel (" + threadPoolSize + " threads, " + numChunks + " chunks) ===");
        printResult(aggregated, elapsedMs);
    }

    /**
     * Prints aggregated stats and elapsed time in a consistent format,
     * shared by both the sequential and parallel runs.
     */
    private static void printResult(AggregatedResult result, long elapsedMs) {
        System.out.println("Total lines:  " + result.totalLines());
        System.out.println("Total errors: " + result.totalErrors());
        System.out.println("Top words:");
        for (Map.Entry<String, Long> entry : result.topWords().entrySet()) {
            System.out.println("  " + entry.getKey() + ": " + entry.getValue());
        }
        System.out.println("Elapsed time: " + elapsedMs + " ms");
        System.out.println();
    }
}