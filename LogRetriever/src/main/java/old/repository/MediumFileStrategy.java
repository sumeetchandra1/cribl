package old.repository;

import old.exception.LogRetrievalException;
import old.logfilters.LimitLogFilter;
import old.logfilters.LogFilter;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class MediumFileStrategy extends AbstractLogRetrievalStrategy {
    private static final int CHUNK_SIZE = 1024 * 1024; // 1MB chunks

    @Override
    public CompletableFuture<List<List<String>>> retrieveLogsAsync(Path filePath, LogFilter filter) throws LogRetrievalException {
        return CompletableFuture.supplyAsync(() -> {
            try {
                validateFile(filePath);
                long fileSize = Files.size(filePath);
                int chunks = (int) Math.ceil((double) fileSize / CHUNK_SIZE);
                List<List<String>> filteredLogs = new ArrayList<>();

                for (int i = chunks - 1; i >= 0; i--) {
                    long chunkStart = Math.max(0, i * CHUNK_SIZE);
                    int chunkSize = (int) Math.min(CHUNK_SIZE, fileSize - chunkStart);
                    List<String> chunkLines = readChunk(filePath, chunkStart, chunkSize);
                    List<List<String>> chunkEntries = groupIntoLogEntries(chunkLines, filter);
                    filteredLogs.addAll(chunkEntries);

                    if (filter instanceof LimitLogFilter && ((LimitLogFilter) filter).isLimitReached()) {
                        break;
                    }
                }

                Collections.reverse(filteredLogs);
                return filteredLogs;
            } catch (IOException e) {
                throw new CompletionException(new LogRetrievalException("Error reading log file", e));
            }
        });
    }

    private List<String> readChunk(Path filePath, long start, int size) throws IOException {
        try (RandomAccessFile file = new RandomAccessFile(filePath.toFile(), "r")) {
            file.seek(start);
            byte[] bytes = new byte[size];
            int bytesRead = file.read(bytes);
            if (bytesRead > 0) {
                String chunk = new String(bytes, 0, bytesRead);
                return Arrays.asList(chunk.split("\\R"));
            }
            return Collections.emptyList();
        }
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
