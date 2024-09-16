package com.sumeet.cribl.logretriever.tests.integration;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "log.directory=${java.io.tmpdir}/logs"
})
class LogRetrievalIntegrationTest {

   /* @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private LogService logService;

    @Autowired
    private LogController logController;

    @TempDir
    Path logDir;

    @BeforeEach
    void setUp() throws Exception {
        Path logFile = logDir.resolve("test.log");
        List<String> logLines = Arrays.asList(
                "2023-09-03 10:00:00 INFO Log entry 1",
                "Additional info",
                "2023-09-03 10:01:00 ERROR Log entry 2",
                "Stack trace"
        );
        Files.write(logFile, logLines);
        System.setProperty("log.directory", logDir.toString());
    }

    @Test
    void testLogRetrieval() {
        String url = "http://localhost:" + port + "/api/logs?filename=test.log";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        String content = response.getBody();
        assertNotNull(content);
        assertTrue(content.contains("2023-09-03 10:00:00 INFO Log entry 1"));
        assertTrue(content.contains("2023-09-03 10:01:00 ERROR Log entry 2"));
    }

    @Test
    void testLogRetrievalWithKeyword() {
        String url = "http://localhost:" + port + "/api/logs?filename=test.log&keyword=ERROR";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        String content = response.getBody();
        assertNotNull(content);
        assertFalse(content.contains("2023-09-03 10:00:00 INFO Log entry 1"));
        assertTrue(content.contains("2023-09-03 10:01:00 ERROR Log entry 2"));
    }

    @Test
    void testLogRetrievalNonExistentFile() {
        String url = "http://localhost:" + port + "/api/logs?filename=nonexistent.log";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        String content = response.getBody();
        assertNotNull(content);
        assertTrue(content.contains("Log file not found"));
    }*/
}