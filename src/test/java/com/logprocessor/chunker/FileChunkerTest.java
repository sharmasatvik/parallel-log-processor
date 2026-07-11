package com.logprocessor.chunker;

import com.logprocessor.model.FileChunk;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FileChunkerTest {

    @Test
    void splitsFileIntoRequestedNumberOfChunks(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("sample.log");
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            content.append("line ").append(i).append(" log content\n");
        }
        Files.writeString(file, content.toString());

        FileChunker chunker = new FileChunker();
        List<FileChunk> chunks = chunker.splitIntoChunks(file.toString(), 4);

        assertEquals(4, chunks.size());
    }

    @Test
    void chunksCoverEntireFileWithNoGapsOrOverlaps(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("sample.log");
        Files.writeString(file, "a\nb\nc\nd\ne\nf\ng\nh\n");

        FileChunker chunker = new FileChunker();
        List<FileChunk> chunks = chunker.splitIntoChunks(file.toString(), 3);

        long fileSize = Files.size(file);

        // first chunk must start at byte 0
        assertEquals(0, chunks.getFirst().startByte());
        // last chunk must end at EOF
        assertEquals(fileSize, chunks.getLast().endByte());

        // each chunk's end must exactly equal the next chunk's start (no gaps/overlaps)
        for (int i = 0; i < chunks.size() - 1; i++) {
            assertEquals(chunks.get(i).endByte(), chunks.get(i + 1).startByte());
        }
    }

    @Test
    void chunkBoundariesAlignToLineBoundaries(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("sample.log");
        Files.writeString(file, "line1\nline2\nline3\nline4\nline5\nline6\n");

        FileChunker chunker = new FileChunker();
        List<FileChunk> chunks = chunker.splitIntoChunks(file.toString(), 3);

        byte[] allBytes = Files.readAllBytes(file);

        for (FileChunk chunk : chunks) {
            // every chunk boundary (except EOF) must immediately follow a newline character
            if (chunk.endByte() < allBytes.length) {
                assertEquals('\n', (char) allBytes[(int) chunk.endByte() - 1],
                        "Chunk boundary at " + chunk.endByte() + " does not align to a line end");
            }
        }
    }

    @Test
    void handlesFileSmallerThanRequestedChunkCount(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("tiny.log");
        Files.writeString(file, "only one line\n");

        FileChunker chunker = new FileChunker();
        List<FileChunk> chunks = chunker.splitIntoChunks(file.toString(), 10);

        assertFalse(chunks.isEmpty());
        assertTrue(chunks.size() <= 10);
    }
}