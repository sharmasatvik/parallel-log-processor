package com.satvik.logprocessor.aggregator;

import com.satvik.logprocessor.model.AggregatedResult;
import com.satvik.logprocessor.model.ChunkResult;

import java.util.*;

/**
 * Merges the per-chunk analysis results produced by parallel processing
 * into a single combined result for the whole file.
 * <p>
 * Chunks may complete in any order (threads race independently), so this
 * aggregator does not rely on ordering, it simply sums counts and merges
 * word frequency maps across all chunks.
 */
public class ResultAggregator {

    // Default number of top words to retain in the final aggregated result
    private static final int DEFAULT_TOP_WORD_LIMIT = 20;

    /**
     * Aggregates a list of ChunkResults into one AggregatedResult, keeping
     * only the top N most frequent words overall.
     *
     * @param chunkResults Results from each processed chunk, in any order.
     *
     * @return A single combined result.
     */
    public AggregatedResult aggregate(List<ChunkResult> chunkResults) {
        return aggregate(chunkResults, DEFAULT_TOP_WORD_LIMIT);
    }

    /**
     * Aggregates a list of ChunkResults into one AggregatedResult.
     *
     * @param chunkResults Results from each processed chunk, in any order.
     * @param topWordLimit How many of the most frequent words to retain
     *                     in the final result (avoids returning a huge
     *                     map for large files with many unique words).
     *
     * @return A single combined result.
     */
    public AggregatedResult aggregate(final List<ChunkResult> chunkResults
            , final int topWordLimit) {
        long totalLines = 0;
        long totalErrors = 0;

        // Combined word frequency map across every chunk. Using a plain
        // HashMap here since this method runs single-threaded, after all
        // parallel chunk processing has already completed and joined.
        Map<String, Long> combinedWordFreq = new HashMap<>();

        for (ChunkResult result : chunkResults) {
            totalLines += result.lineCount();
            totalErrors += result.errorCount();

            // merge() with Long::sum adds counts together when the same
            // word appears in multiple chunks, rather than overwriting
            result.wordFrequency().forEach((word, count) ->
                    combinedWordFreq.merge(word, count, Long::sum));
        }

        Map<String, Long> topWords = extractTopWords(combinedWordFreq, topWordLimit);

        return new AggregatedResult(totalLines, totalErrors, topWords);
    }

    /**
     * Picks the top N entries from the word frequency map by count,
     * descending. Uses a LinkedHashMap for the result so iteration order
     * reflects rank (most frequent first) rather than hash order.
     *
     * @param wordFreq Combined word frequency map across all chunks.
     * @param limit    Max number of entries to retain.
     *
     * @return A rank-ordered map of the top words and their counts.
     */
    private Map<String, Long> extractTopWords(final Map<String, Long> wordFreq
            , final int limit) {
        Map<String, Long> topWords = new LinkedHashMap<>();

        wordFreq.entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, Long>>comparingLong(Map.Entry::getValue).reversed())
                .limit(limit)
                .forEach(entry -> topWords.put(entry.getKey(), entry.getValue()));

        return topWords;
    }
}