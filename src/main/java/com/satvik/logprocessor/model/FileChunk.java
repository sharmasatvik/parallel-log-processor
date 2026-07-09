package com.satvik.logprocessor.model;

/**
 * The file chunk to process.
 *
 * @param startByte  The start byte.
 * @param endByte    The end byte.
 * @param chunkIndex The chunking index.
 */
public record FileChunk(long startByte, long endByte, int chunkIndex) {
}