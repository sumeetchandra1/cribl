package com.sumeet.cribl.logretriever.tests.unit;

import old.logfilters.LogFilter;
import old.repository.LargeFileStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LargeFileStrategyTest {

    private LargeFileStrategy strategy;
    private LogFilter mockFilter;

    @BeforeEach
    void setUp() {
        strategy = new LargeFileStrategy();
        mockFilter = mock(LogFilter.class);
    }

    @Test
    void retrieveLogsAsync_Success(@TempDir Path tempDir) throws Exception {
        Path logFile = tempDir.resolve("test.log");
        List<String> logLines = Arrays.asList(
                "2023-09-03 10:00:00 INFO Log entry 1",
                "Additional info",
                "2023-09-03 10:01:00 ERROR Log entry 2",
                "Stack trace"
        );
        Files.write(logFile, logLines);

        when(mockFilter.apply(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<List<String>> result = strategy.retrieveLogsAsync(logFile, mockFilter).get();

        assertEquals(2, result.size());
        assertEquals(2, result.get(0).size());
        assertEquals(2, result.get(1).size());
        assertEquals("2023-09-03 10:00:00 INFO Log entry 1", result.get(0).get(0));
        assertEquals("2023-09-03 10:01:00 ERROR Log entry 2", result.get(1).get(0));
    }
}