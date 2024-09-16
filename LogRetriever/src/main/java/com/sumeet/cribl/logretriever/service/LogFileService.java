package com.sumeet.cribl.logretriever.service;

import com.sumeet.cribl.logretriever.exception.InvalidFileNameException;
import com.sumeet.cribl.logretriever.exception.LogRetrievalException;
import com.sumeet.cribl.logretriever.repository.InMemoryFileStrategy;
import com.sumeet.cribl.logretriever.repository.LogFileReadStrategy;
import com.sumeet.cribl.logretriever.repository.LogFileRetrievalStrategyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class LogFileService {

    private static final Logger logger = LoggerFactory.getLogger(LogFileService.class);

    private final LogFileRetrievalStrategyFactory logFileRetrievalStrategyFactory;

    @Value("${log.directory}")
    private String logDirectory;

    @Autowired
    public LogFileService(LogFileRetrievalStrategyFactory logFileRetrievalStrategyFactory) {
        this.logFileRetrievalStrategyFactory = logFileRetrievalStrategyFactory;
    }

    public Object getFilteredFile(String filename, String keywords, Integer limit) throws LogRetrievalException {
        try {
            validateFilename(filename);
            long fileSize = getFileSize(filename);

            LogFileReadStrategy strategy = logFileRetrievalStrategyFactory.getAppropriateStrategy(fileSize);

            logger.info("Applying Strategy: " + strategy.getClass() + " for file with size: " + fileSize);
            InputStream fileStream = strategy.readFile(Paths.get(logDirectory, filename), keywords, limit);

            /*Path filePath = Paths.get(logDirectory, filename);
            InputStream fileStream = Files.newInputStream(filePath);
            return new InputStreamResource(fileStream);*/

            if (strategy instanceof InMemoryFileStrategy) {
                return new InputStreamResource(fileStream);
            } else {
                return fileStream;
            }
        } catch (Exception e) {

            logger.error(e.getMessage() + ", " + e.getCause());
            throw new LogRetrievalException("Error accessing file: " + filename, e);
        }
    }

    private void validateFilename(String filename) {

        Path filePath = Paths.get(logDirectory, filename);

        if (filename == null || filename.isEmpty()) {
            throw new LogRetrievalException("Filename issue: ", new InvalidFileNameException("Filename cannot be null or empty"));
        }

        if (filename.split("\\.").length > 2) {
            throw new LogRetrievalException("Filename issue: ", new InvalidFileNameException("Filename cannot contain more than one dot"));
        }

        if (!Files.exists(filePath)) {
            throw new LogRetrievalException("File not found: " + filePath, new FileNotFoundException());
        }

        if (!Files.isRegularFile(filePath)) {
            throw new LogRetrievalException("Not a file: " + filePath, new IllegalArgumentException());
        }

        if (!Files.isReadable(filePath)) {
            throw new LogRetrievalException("Access denied: " + filePath, new AccessDeniedException(filePath.toString()));
        }
    }

    public long getFileSize(String filename) throws IOException {
        Path filePath = Paths.get(logDirectory, filename);
        return Files.size(filePath);
    }
}
