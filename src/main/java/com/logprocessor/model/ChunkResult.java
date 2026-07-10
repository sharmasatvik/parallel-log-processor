package com.logprocessor.model;

import java.util.Map;

/**
 * Holds the analysis output for a single chunk of the log file. Multiple chunk results
 * are later combined into one aggregated result.
 *
 * @param chunkIndex    Index of the chunk this result belongs to.
 * @param lineCount     Number of lines found in this chunk.
 * @param errorCount    Number of lines matching the ERROR pattern.
 * @param wordFrequency Frequency count of individual words in this chunk.
 */
public record ChunkResult(
        int chunkIndex,
        long lineCount,
        long errorCount,
        Map<String, Long> wordFrequency
) {
}