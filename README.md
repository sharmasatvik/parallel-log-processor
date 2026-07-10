# Parallel Log Processor

A Java multithreading project that processes large log files
in parallel. Splits a log file into chunks, analyzes each chunk
concurrently, and aggregates the results - extracting line counts,
error counts, and word frequency statistics.

Built to explore practical concurrency patterns in Java 21: thread
pools, `CompletableFuture` composition, and the tradeoffs between
sequential and parallel processing on I/O + CPU bound work.

## What it does

Given a log file, the tool:

1. Splits the file into N chunks, aligned to line boundaries so no line
   is split across two chunks.
2. Analyzes each chunk to extract:
    - Line count
    - Error count (lines containing `ERROR`, case-insensitive)
    - Word frequency
3. Runs this twice - once sequentially (baseline) and once in parallel
   across a fixed thread pool - and prints both timings so the speedup
   is visible directly.
4. Aggregates all chunk results into one combined summary.

## Why

Most "hello world" multithreading demos are too trivial to expose real
concurrency issues. This project uses a problem - parallel file
processing - that surfaces genuine practical concerns:

- Splitting work into independent units without corrupting data at the
  boundaries (chunk alignment to line breaks)
- Bounded thread pools instead of unbounded thread creation
- Composing multiple async results with `CompletableFuture.allOf`
- Merging independently-computed partial results correctly
  (`ConcurrentHashMap`-free aggregation, since merging happens only
  after all futures are joined)

## Tech stack

- Java 21 (records, `RandomAccessFile`, `CompletableFuture`)
- Maven
- JUnit 5

## Project structure

```
src/main/java/com/logprocessor/

|---- Main.java                      # CLI entry point
|---- model/
|   |---- FileChunk.java             # byte-range slice of the file
|   |---- ChunkResult.java           # per-chunk analysis output
|   |---- AggregatedResult.java      # combined final result
|---- chunker/
|   |---- FileChunker.java           # splits file into line-aligned chunks
|---- analyzer/
|   |---- ChunkAnalyzer.java         # analyzes a single chunk (sequential)
|---- processor/
|   |---- LogProcessor.java          # parallel orchestration via CompletableFuture
|---- aggregator/
|   |---- ResultAggregator.java      # merges chunk results into one summary
|---- util/
|---- SampleLogGenerator.java        # generates synthetic log files for testing
```

## Usage

Build:

```bash
mvn clean package
```

Generate a sample log file (optional, for testing):

```bash
java -cp target/classes com.logprocessor.util.SampleLogGenerator sample.log 500000
```

Run:

```bash
java -jar target/parallel-log-processor-1.0-SNAPSHOT.jar sample.log [numChunks] [threadPoolSize]
```

- `numChunks` (optional, default `8`) - number of chunks to split the file into
- `threadPoolSize` (optional, default `4`) - number of worker threads for parallel processing

Example output:

```
=== Sequential Baseline ===
Total lines:  500000
Total errors: 124821
Top words:
the: 892341
...
Elapsed time: 842 ms
=== Parallel (4 threads, 8 chunks) ===
Total lines:  500000
Total errors: 124821
Top words:
the: 892341
...
Elapsed time: 231 ms
```

## Design notes

- **Chunk alignment**: chunks are byte ranges adjusted to the nearest
  newline so no line is split across two chunks, which would otherwise
  corrupt line/word counts at boundaries.
- **Fixed thread pool**: uses `Executors.newFixedThreadPool` for
  predictable, bounded resource usage rather than unbounded thread
  creation.
- **`CompletableFuture.allOf`**: chunk analysis tasks are submitted
  independently and combined with `allOf`, expressing "wait for the
  whole batch" as a single composed future rather than joining each
  one sequentially in a loop.
- **Aggregation happens single-threaded**: `ResultAggregator` only runs
  after all chunk futures are joined, so no synchronization is needed
  when merging word frequency maps.

## Possible future improvements

- Replace `System.currentTimeMillis()` timing with JMH for rigorous
  micro-benchmarking (JIT warmup, GC noise currently affect results).
- Make thread pool externally injectable for better testability.
- Add configurable output formats (JSON, CSV) for aggregated results.
- Stream large chunk results instead of loading full chunk content into
  memory at once.

## Running tests

```bash
mvn test
```

## License

MIT