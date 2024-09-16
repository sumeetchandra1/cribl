package com.sumeet.cribl.logretriever.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ThreadedChunkFileStrategy implements LogFileReadStrategy  {

    private static final int BUFFER_SIZE = 8192; // 8KB buffer
    private static final int QUEUE_CAPACITY = 1000;

    private static final Logger logger = LoggerFactory.getLogger(ThreadedChunkFileStrategy.class);

    @Override
    public InputStream readFile(Path filePath, String keywords, Integer limit) throws IOException {
        return new ThreadedFilteredFileInputStream(filePath, keywords, limit);
    }

    private static class ThreadedFilteredFileInputStream extends InputStream {
        private final BlockingQueue<String> lineQueue;
        private final Thread backgroundThread;
        private final Thread streamingThread;
        private final PipedOutputStream pos;
        private final PipedInputStream pis;
        private final AtomicBoolean error;
        private volatile Exception backgroundException;
        private volatile Exception streamingException;

        public ThreadedFilteredFileInputStream(Path filePath, String keywords, Integer limit) throws IOException {
            this.lineQueue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
            this.pos = new PipedOutputStream();
            this.pis = new PipedInputStream(pos);
            this.error = new AtomicBoolean(false);

            this.backgroundThread = new Thread(() -> {
                try {
                    processFileInBackground(filePath, keywords, limit);
                } catch (Exception e) {
                    handleBackgroundException(e);
                }
            });

            this.streamingThread = new Thread(() -> {
                try {
                    streamToClient();
                } catch (Exception e) {
                    handleStreamingException(e);
                }
            });

            this.backgroundThread.start();
            this.streamingThread.start();
        }

        private void processFileInBackground(Path filePath, String keywords, Integer limit) {
            try (RandomAccessFile file = new RandomAccessFile(filePath.toFile(), "r")) {
                long filePointer = file.length();
                StringBuilder lineBuilder = new StringBuilder();
                int matchedLines = 0;
                byte[] chunk = new byte[BUFFER_SIZE];

                while (filePointer > 0 && (limit == null || matchedLines < limit) && !error.get()) {
                    long startPoint = Math.max(filePointer - BUFFER_SIZE, 0);
                    int bytesToRead = (int) (filePointer - startPoint);
                    file.seek(startPoint);
                    file.readFully(chunk, 0, bytesToRead);

                    // Log the read chunk for debugging purposes
                    logger.info("Read {} bytes from file", bytesToRead);

                    for (int i = bytesToRead - 1; i >= 0; i--) {
                        if (chunk[i] == '\n') {
                            String line = lineBuilder.reverse().toString();
                            lineBuilder.setLength(0);
                            if (keywords == null || line.contains(keywords)) {
                                try {
                                    // Try to offer the line to the queue, wait for up to 1 second
                                    if (!lineQueue.offer(line, 1, TimeUnit.SECONDS)) {
                                        // Handle the case where the queue is full and offer times out
                                        handleBackgroundException(new InterruptedException("Queue is full or timed out while offering"));
                                        return;
                                    }
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();  // Restore the interrupt status
                                    handleBackgroundException(e);        // Handle the exception and stop processing
                                    return;
                                }
                                matchedLines++;
                                if (limit != null && matchedLines >= limit) break;
                            }
                        } else if (chunk[i] != '\r') {
                            lineBuilder.append((char) chunk[i]);
                        }
                    }

                    filePointer = startPoint;
                }

                // Handle the last line
                if (lineBuilder.length() > 0 && !error.get()) {
                    String line = lineBuilder.reverse().toString();
                    if (keywords == null || line.contains(keywords)) {
                        try {
                            if (!lineQueue.offer(line, 1, TimeUnit.SECONDS)) {
                                handleBackgroundException(new InterruptedException("Queue is full or timed out while offering"));
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            handleBackgroundException(e);
                        }
                    }
                }
            } catch (IOException e) {
                handleBackgroundException(e);
            } finally {
                try {
                    logger.info("Background file processing complete.");
                    lineQueue.put(""); // Signal end of stream
                } catch (InterruptedException e) {
                    handleBackgroundException(e);
                }
            }
        }

        private void streamToClient() {
            try {
                while (!error.get()) {
                    String line = lineQueue.take();
                    if (line.isEmpty()) {
                        logger.info("End of stream reached.");
                        break; // End of stream
                    }
                    logger.debug("Streaming line: {}", line);
                    byte[] lineBytes = (line + System.lineSeparator()).getBytes(StandardCharsets.UTF_8);
                    pos.write(lineBytes);
                    pos.flush();
                }
            } catch (IOException | InterruptedException e) {
                handleStreamingException(e);
            } finally {
                try {
                    pos.close();
                } catch (IOException e) {
                    handleStreamingException(e);
                }
            }
        }

        private void handleBackgroundException(Exception e) {
            logger.error("Error in background processing: {}", e.getMessage(), e);
            error.set(true);
            backgroundException = e;
            lineQueue.clear();
            try {
                lineQueue.put(""); // Signal end of stream
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }

        private void handleStreamingException(Exception e) {
            logger.error("Error in streaming process: {}", e.getMessage(), e);
            error.set(true);
            streamingException = e;
        }

        @Override
        public int read() throws IOException {
            if (error.get()) {
                throwAppropriateException();
            }
            int result = pis.read();
            if (result == -1 && error.get()) {
                throwAppropriateException();
            }
            return result;
        }

        private void throwAppropriateException() throws IOException {
            if (backgroundException != null) {
                throw new IOException("Error in background processing", backgroundException);
            }
            if (streamingException != null) {
                throw new IOException("Error in streaming", streamingException);
            }
        }

        @Override
        public void close() throws IOException {
            try {
                backgroundThread.interrupt();
                streamingThread.interrupt();
            } catch (Exception e) {
                logger.error("Error while interrupting threads: {}", e.getMessage());
            } finally {
                pis.close();
                pos.close();
                if (error.get()) {
                    throwAppropriateException();
                }
            }
        }
    }
}
