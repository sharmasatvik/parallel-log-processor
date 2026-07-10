package com.logprocessor.chunker;

import com.logprocessor.model.FileChunk;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

/**
 * Splits a file into byte-range chunks that align to line boundaries,
 * so no chunk starts or ends in the middle of a line.
 * <p>
 * Alignment is necessary because naive equal-size splitting would cut
 * lines in half at chunk boundaries, corrupting line/word counts.
 * </p>
 */
public class FileChunker {

    /**
     * Splits the given file into approximate number of chunks.
     * The actual chunk boundaries are adjusted to the nearest newline
     * so each chunk contains only whole lines.
     *
     * @param filePath  Path to the file to split.
     * @param numChunks Desired number of chunks (actual count may be lower
     *                  if the file is smaller than expected).
     * @return List of file chunk objects representing byte ranges.
     */
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
                    // Last chunk always goes to the end of the file,
                    // avoids leaving a tiny leftover chunk due to rounding.
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

    /**
     * Moves forward from the given position until the next newline character
     * is found, so the chunk boundary lands cleanly at the end of a line.
     *
     * @param raf      Open file handle to read from.
     * @param position Tentative byte offset to start searching from.
     * @param fileSize Total size of the file, used as a fallback boundary
     * @return byte offset of the first position after the next newline,
     * or fileSize if no newline is found before EOF.
     */
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