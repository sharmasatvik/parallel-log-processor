package com.satvik.logprocessor.analyzer;

import com.satvik.logprocessor.model.ChunkResult;
import com.satvik.logprocessor.model.FileChunk;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Analyzes a single file chunk sequentially: reads the chunk's byte range,
 * counts lines, counts ERROR-level log lines, and builds a word frequency map.
 * <p>
 * This is the baseline (non-parallel) analyzer.
 * </p>
 */
public class ChunkAnalyzer {

    // Matches sequences of alphabetic characters as "words" for frequency counting
    private static final Pattern WORD_PATTERN = Pattern.compile("[a-zA-Z]+");

    // Case-insensitive match for the literal "ERROR" token in a log line
    private static final Pattern ERROR_PATTERN = Pattern.compile("ERROR", Pattern.CASE_INSENSITIVE);

    /**
     * Reads and analyzes the given chunk of the file.
     *
     * @param filePath Path to the source file.
     * @param chunk    Byte range describing which part of the file to read.
     * @return A chunk result containing line count, error count, and word frequencies.
     */
    public ChunkResult analyze(String filePath, FileChunk chunk) throws IOException {
        long lineCount = 0;
        long errorCount = 0;
        Map<String, Long> wordFreq = new HashMap<>();

        try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
            // Jump directly to this chunk's start offset instead of reading from the beginning
            raf.seek(chunk.startByte());
            long bytesToRead = chunk.endByte() - chunk.startByte();
            byte[] buffer = new byte[(int) bytesToRead];
            raf.readFully(buffer);

            String content = new String(buffer);
            String[] lines = content.split("\n");
            lineCount = lines.length;

            for (String line : lines) {
                if (ERROR_PATTERN.matcher(line).find()) {
                    errorCount++;
                }
                // lowercase before matching so word frequency counting isn't case-sensitive
                var matcher = WORD_PATTERN.matcher(line.toLowerCase());
                while (matcher.find()) {
                    wordFreq.merge(matcher.group(), 1L, Long::sum);
                }
            }
        }

        return new ChunkResult(chunk.chunkIndex(), lineCount, errorCount, wordFreq);
    }
}