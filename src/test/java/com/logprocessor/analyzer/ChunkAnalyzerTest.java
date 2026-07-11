package com.logprocessor.analyzer;

import com.logprocessor.model.ChunkResult;
import com.logprocessor.model.FileChunk;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChunkAnalyzerTest {

    @Test
    void countsLinesAndErrorsCorrectly(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("sample.log");
        String content = "INFO starting up\nERROR something broke\nINFO all good\nerror lowercase counts too\n";
        Files.writeString(file, content);

        long fileSize = Files.size(file);
        FileChunk wholeFileChunk = new FileChunk(0, fileSize, 0);

        ChunkAnalyzer analyzer = new ChunkAnalyzer();
        ChunkResult result = analyzer.analyze(file.toString(), wholeFileChunk);

        assertEquals(4, result.lineCount());
        assertEquals(2, result.errorCount());
    }

    @Test
    void buildsWordFrequencyMapCaseInsensitively(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("sample.log");
        Files.writeString(file, "Apple apple APPLE banana\n");

        long fileSize = Files.size(file);
        FileChunk wholeFileChunk = new FileChunk(0, fileSize, 0);

        ChunkAnalyzer analyzer = new ChunkAnalyzer();
        ChunkResult result = analyzer.analyze(file.toString(), wholeFileChunk);

        assertEquals(3L, result.wordFrequency().get("apple"));
        assertEquals(1L, result.wordFrequency().get("banana"));
    }
}