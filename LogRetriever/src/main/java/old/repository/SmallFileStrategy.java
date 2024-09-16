package old.repository;

import old.exception.LogRetrievalException;
import old.logfilters.LogFilter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class SmallFileStrategy extends AbstractLogRetrievalStrategy {
    @Override
    public CompletableFuture<List<List<String>>> retrieveLogsAsync(Path filePath, LogFilter filter) throws LogRetrievalException {
        return CompletableFuture.supplyAsync(() -> {
            try {
                validateFile(filePath);
                List<String> allLines = Files.readAllLines(filePath);
                Collections.reverse(allLines);
                return groupIntoLogEntries(allLines, filter);
            } catch (IOException e) {
                throw new CompletionException(new LogRetrievalException("Error reading log file", e));
            }
        });
    }

    private List<List<String>> groupIntoLogEntries(List<String> lines, LogFilter filter) {
        List<List<String>> entries = new ArrayList<>();
        List<String> currentEntry = new ArrayList<>();

        for (String line : lines) {
            if (isNewLogEntry(line) && !currentEntry.isEmpty()) {
                List<String> filteredEntry = filter.apply(currentEntry);
                if (!filteredEntry.isEmpty()) {
                    entries.add(filteredEntry);
                }
                currentEntry = new ArrayList<>();
            }
            currentEntry.add(line);
        }

        if (!currentEntry.isEmpty()) {
            List<String> filteredEntry = filter.apply(currentEntry);
            if (!filteredEntry.isEmpty()) {
                entries.add(filteredEntry);
            }
        }

        return entries;
    }
}
