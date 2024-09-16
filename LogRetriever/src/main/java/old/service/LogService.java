package old.service;

import old.exception.LogRetrievalException;
import old.logfilters.LogFilter;
import old.logfilters.LogFilterFactory;
import old.repository.LogStrategyFactory;
import old.repository.LogRetrievalStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Service
public class LogService {
    private final LogStrategyFactory strategyFactory;
    private final LogFilterFactory filterFactory;

    @Value("${log.directory}")
    private String logDirectory;

    public LogService(LogStrategyFactory strategyFactory, LogFilterFactory filterFactory) {
        this.strategyFactory = strategyFactory;
        this.filterFactory = filterFactory;
    }

    @Retryable(
            value = {IOException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public CompletableFuture<List<List<String>>> retrieveLogsAsync(String filename, String keyword, Integer limit) {
        return CompletableFuture.supplyAsync(() -> {
                    try {
                        Path filePath = Paths.get(logDirectory, filename);
                        LogRetrievalStrategy strategy = strategyFactory.getStrategy(filePath);
                        LogFilter filter = filterFactory.createFilter(keyword, limit != null ? limit : -1);
                        return strategy.retrieveLogsAsync(filePath, filter).get();
                    } catch (Exception e) {
                        throw new CompletionException(new LogRetrievalException("Error retrieving logs", e));
                    }
                })
                .exceptionally(this::handleException);
    }

    private List<List<String>> handleException(Throwable ex) {
        LogRetrievalException logEx = (LogRetrievalException) ex.getCause();
        List<String> errorMessage = Collections.singletonList(getErrorMessage(logEx));
        return Collections.singletonList(errorMessage);
    }

    private String getErrorMessage(LogRetrievalException ex) {
        if (ex.getCause() instanceof FileNotFoundException) {
            return "Log file not found. Please check the filename and try again.";
        } else if (ex.getCause() instanceof AccessDeniedException) {
            return "Access denied. Unable to read the log file.";
        } else if (ex.getCause() instanceof IllegalArgumentException) {
            return "Invalid file. The specified path is not a regular file.";
        } else {
            return "An error occurred while retrieving logs: " + ex.getMessage();
        }
    }
}