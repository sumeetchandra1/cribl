package old.repository;

import old.exception.LogRetrievalException;
import old.logfilters.LogFilter;

import java.io.BufferedReader;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class LargeFileStrategy extends AbstractLogRetrievalStrategy {
    private static final int BUFFER_SIZE = 1024 * 1024; // 1MB buffer

    @Override
    public CompletableFuture<List<List<String>>> retrieveLogsAsync(Path filePath, LogFilter filter) throws LogRetrievalException {
        return CompletableFuture.supplyAsync(() -> {
            try {
                validateFile(filePath);
                List<List<String>> allLogs = new ArrayList<>();
                try (BufferedReader reader = Files.newBufferedReader(filePath)) {
                    String line;
                    List<String> currentEntry = new ArrayList<>();
                    while ((line = reader.readLine()) != null) {
                        if (isNewLogEntry(line) && !currentEntry.isEmpty()) {
                            List<String> filteredEntry = filter.apply(currentEntry);
                            if (!filteredEntry.isEmpty()) {
                                allLogs.add(filteredEntry);
                            }
                            currentEntry = new ArrayList<>();
                        }
                        currentEntry.add(line);

                        if (allLogs.size() % 1000 == 0) { // Yield every 1000 entries
                            Thread.yield();
                        }
                    }
                    if (!currentEntry.isEmpty()) {
                        List<String> filteredEntry = filter.apply(currentEntry);
                        if (!filteredEntry.isEmpty()) {
                            allLogs.add(filteredEntry);
                        }
                    }
                }
                return allLogs;
            } catch (IOException e) {
                throw new CompletionException(new LogRetrievalException("Error reading log file", e));
            }
        });
    }

    @Override
    protected boolean isNewLogEntry(String line) {
        // This is a simple implementation. Adjust according to your log format.
        return line.matches("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}.*");
    }
}