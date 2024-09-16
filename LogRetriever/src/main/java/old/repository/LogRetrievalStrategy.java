package old.repository;

import old.exception.LogRetrievalException;
import old.logfilters.LogFilter;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface LogRetrievalStrategy {
    CompletableFuture<List<List<String>>> retrieveLogsAsync(Path filePath, LogFilter filter) throws LogRetrievalException;
}

