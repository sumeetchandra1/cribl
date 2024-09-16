package old.repository;

import old.exception.LogRetrievalException;
import old.logfilters.LogFilter;

import java.io.FileNotFoundException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public abstract class AbstractLogRetrievalStrategy implements LogRetrievalStrategy {

    protected void validateFile(Path filePath) throws LogRetrievalException {
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

    protected boolean isNewLogEntry(String line) {
        // This is a simple implementation. Adjust according to your log format.
        return line.matches("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}.*");
    }

    @Override
    public abstract CompletableFuture<List<List<String>>> retrieveLogsAsync(Path filePath, LogFilter filter) throws LogRetrievalException;

}
