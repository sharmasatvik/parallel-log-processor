package com.satvik.logprocessor.processor;

import com.satvik.logprocessor.analyzer.ChunkAnalyzer;
import com.satvik.logprocessor.chunker.FileChunker;
import com.satvik.logprocessor.model.ChunkResult;
import com.satvik.logprocessor.model.FileChunk;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Orchestrates parallel analysis of a log file.
 * <p>
 * The file is first split into chunks (aligned to line boundaries), then
 * each chunk is analyzed concurrently on a dedicated thread pool using
 * CompletableFuture. Results are collected once every chunk has finished
 * processing.
 * <p>
 * This class owns the lifecycle of its own ExecutorService, so callers
 * must invoke {@link #shutdown()} (or use it in a try-with-resources-like
 * pattern) once processing is complete to avoid leaking threads.
 */
public class LogProcessor {

    private final FileChunker fileChunker;
    private final ChunkAnalyzer chunkAnalyzer;
    private final ExecutorService executorService;

    /**
     * Creates a log processor backed by a fixed-size thread pool.
     *
     * @param threadPoolSize Number of worker threads to use for parallel chunk analysis.
     */
    public LogProcessor(int threadPoolSize) {
        this.fileChunker = new FileChunker();
        this.chunkAnalyzer = new ChunkAnalyzer();
        // Fixed thread pool keeps resource usage predictable and bounded,
        // rather than spawning unbounded threads for large files.
        this.executorService = Executors.newFixedThreadPool(threadPoolSize);
    }

    /**
     * Splits the given file into chunks and analyzes all chunks concurrently.
     *
     * @param filePath  Path to the log file to process.
     * @param numChunks Number of chunks to split the file into (roughly maps
     *                  to the degree of parallelism achievable).
     * @return List of chunk result objects, one per chunk, in chunk order.
     */
    public List<ChunkResult> processInParallel(String filePath, int numChunks) throws IOException {
        List<FileChunk> chunks = fileChunker.splitIntoChunks(filePath, numChunks);

        // Kick off one async task per chunk. Each task runs analyze() on the
        // shared executor's worker threads instead of blocking the calling thread.
        List<CompletableFuture<ChunkResult>> futures = chunks.stream()
                .map(chunk -> CompletableFuture.supplyAsync(
                        () -> analyzeChunkUnchecked(filePath, chunk),
                        executorService))
                .toList();

        // allOf() gives us a single future that completes only once every
        // individual chunk future has completed. We block here (join) because
        // this method's contract is synchronous from the caller's perspective;
        // the parallelism already happened while waiting.
        CompletableFuture<Void> allDone = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
        );

        allDone.join();

        // Every future is guaranteed complete at this point, so join() on
        // each individual future here will not block further.
        return futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }

    /**
     * Wraps the checked IOException from ChunkAnalyzer.analyze() into an
     * unchecked exception, since CompletableFuture.supplyAsync requires a
     * Supplier, which cannot declare checked exceptions.
     */
    private ChunkResult analyzeChunkUnchecked(String filePath, FileChunk chunk) {
        try {
            return chunkAnalyzer.analyze(filePath, chunk);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to analyze chunk " + chunk.chunkIndex(), e);
        }
    }

    /**
     * Shuts down the internal thread pool. Should be called once this
     * LogProcessor instance is no longer needed.
     */
    public void shutdown() {
        executorService.shutdown();
    }
}