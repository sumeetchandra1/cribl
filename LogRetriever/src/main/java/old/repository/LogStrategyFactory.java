package old.repository;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class LogStrategyFactory {
    private static final long SMALL_FILE_LIMIT = 10 * 1024 * 1024; // 10 MB
    private static final long MEDIUM_FILE_LIMIT = 100 * 1024 * 1024; // 100 MB

    public LogRetrievalStrategy getStrategy(Path filePath) throws IOException {
        long fileSize = Files.size(filePath);
        if (fileSize <= SMALL_FILE_LIMIT) {
            return new SmallFileStrategy();
        } else if (fileSize <= MEDIUM_FILE_LIMIT) {
            return new MediumFileStrategy();
        } else {
            return new LargeFileStrategy();
        }
    }
}