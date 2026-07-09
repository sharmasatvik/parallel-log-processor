package com.satvik.logprocessor.model;

/**
 * Represents a byte-range slice of a file to be processed independently.
 *
 * @param startByte  Inclusive starting byte offset of this chunk.
 * @param endByte    Exclusive ending byte offset of this chunk.
 * @param chunkIndex The position of this chunk relative to others (used for ordering results).
 */
public record FileChunk(long startByte, long endByte, int chunkIndex) {
}