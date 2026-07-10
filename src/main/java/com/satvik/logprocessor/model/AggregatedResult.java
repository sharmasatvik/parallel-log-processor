package com.satvik.logprocessor.model;

import java.util.Map;

/**
 * Final combined result after merging all chunk results together.
 *
 * @param totalLines  Total number of lines across the whole file.
 * @param totalErrors Total number of ERROR-matching lines across the whole file.
 * @param topWords    Top N most frequent words across the whole file.
 */
public record AggregatedResult(
        long totalLines,
        long totalErrors,
        Map<String, Long> topWords
) {
}