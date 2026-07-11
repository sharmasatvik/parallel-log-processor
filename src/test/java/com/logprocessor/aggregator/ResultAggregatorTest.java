package com.logprocessor.aggregator;

import com.logprocessor.model.AggregatedResult;
import com.logprocessor.model.ChunkResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResultAggregatorTest {

    @Test
    void sumsLineAndErrorCountsAcrossChunks() {
        ChunkResult chunk1 = new ChunkResult(0, 10, 2, Map.of("error", 2L));
        ChunkResult chunk2 = new ChunkResult(1, 15, 3, Map.of("error", 3L));

        ResultAggregator aggregator = new ResultAggregator();
        AggregatedResult result = aggregator.aggregate(List.of(chunk1, chunk2));

        assertEquals(25, result.totalLines());
        assertEquals(5, result.totalErrors());
    }

    @Test
    void mergesWordFrequenciesAcrossChunks() {
        ChunkResult chunk1 = new ChunkResult(0, 5, 0, Map.of("apple", 3L, "banana", 1L));
        ChunkResult chunk2 = new ChunkResult(1, 5, 0, Map.of("apple", 2L, "cherry", 4L));

        ResultAggregator aggregator = new ResultAggregator();
        AggregatedResult result = aggregator.aggregate(List.of(chunk1, chunk2));

        assertEquals(5L, result.topWords().get("apple"));
        assertEquals(1L, result.topWords().get("banana"));
        assertEquals(4L, result.topWords().get("cherry"));
    }

    @Test
    void respectsTopWordLimit() {
        ChunkResult chunk = new ChunkResult(0, 5, 0,
                Map.of("a", 5L, "b", 4L, "c", 3L, "d", 2L, "e", 1L));

        ResultAggregator aggregator = new ResultAggregator();
        AggregatedResult result = aggregator.aggregate(List.of(chunk), 2);

        assertEquals(2, result.topWords().size());
        assertEquals(5L, result.topWords().get("a"));
        assertEquals(4L, result.topWords().get("b"));
    }
}