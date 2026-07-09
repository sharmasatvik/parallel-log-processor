package com.satvik.logprocessor.chunker;

import com.satvik.logprocessor.model.FileChunk;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

/**
 * Splits a file into byte-range chunks that align to line boundaries,
 * so no chunk starts or ends in the middle of a line.
 */
public class FileChunker {

    public List<FileChunk> splitIntoChunks(final String filePath
            , final int numChunks) throws IOException {
        List<FileChunk> chunks = new ArrayList<>();

        try (var raf = new RandomAccessFile(filePath, "r")) {
            final long fileSize = raf.length();
            final long approxChunkSize = fileSize / numChunks;

            long start = 0;
            for (int i = 0; i < numChunks; i++) {
                long end;
                if (i == numChunks - 1) {
                    end = fileSize;
                } else {
                    long tentativeEnd = start + approxChunkSize;
                    end = alignToLineBoundary(raf, tentativeEnd, fileSize);
                }

                if (start >= fileSize) break;
                chunks.add(new FileChunk(start, end, i));
                start = end;
            }
        }

        return chunks;
    }

    private long alignToLineBoundary(final RandomAccessFile raf
            , final long position
            , final long fileSize) throws IOException {
        if (position >= fileSize) {
            return fileSize;
        }

        raf.seek(position);
        int b;
        long pos = position;
        while ((b = raf.read()) != -1) {
            pos++;
            if (b == '\n') {
                return pos;
            }
        }
        return fileSize;
    }
}