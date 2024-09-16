package com.sumeet.cribl.logretriever.repository;

import org.springframework.beans.factory.annotation.Value;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class InMemoryFileStrategy implements LogFileReadStrategy {

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY = 1000; // 1 second

    /**
     * @param filename
     * @param keywords
     * @param limit
     * @return
     * @throws IOException
     */
    @Override
    public InputStream readFile(Path filePath, String keywords, Integer limit) throws IOException {
        List<String> lines = retryOperation(() -> Files.readAllLines(filePath, StandardCharsets.UTF_8));
        Collections.reverse(lines);

        List<String> filteredLines = lines.stream()
                .filter(line -> keywords == null || line.contains(keywords))
                .limit(limit != null ? limit : Long.MAX_VALUE)
                .collect(Collectors.toList());

        String result = String.join(System.lineSeparator(), filteredLines) + System.lineSeparator();
        return new ByteArrayInputStream(result.getBytes(StandardCharsets.UTF_8));
    }

    private <T> T retryOperation(IOSupplier<T> operation) throws IOException {
        IOException lastException = null;
        for (int i = 0; i < MAX_RETRIES; i++) {
            try {
                return operation.get();
            } catch (IOException e) {
                lastException = e;
                try {
                    Thread.sleep(RETRY_DELAY);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Operation interrupted", ie);
                }
            }
        }
        throw new IOException("Operation failed after " + MAX_RETRIES + " attempts", lastException);
    }

    @FunctionalInterface
    private interface IOSupplier<T> {
        T get() throws IOException;
    }
}
