package com.sumeet.cribl.logretriever.repository;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class LogFileRetrievalStrategyFactory {

    private static final long IN_MEMORY_FILE_LIMIT = 15 * 1024 * 1024; // 15 MB

    public LogFileReadStrategy getAppropriateStrategy(long fileSize) throws IOException {

        if (fileSize <= IN_MEMORY_FILE_LIMIT) {
            return new InMemoryFileStrategy();
        } else {
            return new ThreadedChunkFileStrategy();
        }
    }

}
