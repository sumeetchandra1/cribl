package com.sumeet.cribl.logretriever.tests.unit;

class LogControllerTest {

   /* @Mock
    private LogService logService;

    private LogController logController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        logController = new LogController(logService);
    }

    @Test
    void getLogs_Success() throws Exception {
        String filename = "test.log";
        String keyword = "error";
        Integer limit = 10;
        List<List<String>> logs = Arrays.asList(
                Arrays.asList("2023-09-03 10:00:00 INFO Log 1", "Additional info"),
                Arrays.asList("2023-09-03 10:01:00 ERROR Log 2", "Stack trace")
        );

        when(logService.retrieveLogsAsync(filename, keyword, limit))
                .thenReturn(CompletableFuture.completedFuture(logs));

        CompletableFuture<ResponseEntity<List<List<String>>>> response = logController.getLogs(filename, keyword, limit);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        response.getBody().writeTo(baos);
        String content = baos.toString();

        assertTrue(content.contains("2023-09-03 10:00:00 INFO Log 1"));
        assertTrue(content.contains("2023-09-03 10:01:00 ERROR Log 2"));
    }

    @Test
    void getLogs_ErrorResponse() throws Exception {
        String filename = "test.log";
        List<List<String>> errorResponse = Arrays.asList(Arrays.asList("An error occurred"));

        when(logService.retrieveLogsAsync(filename, null, null))
                .thenReturn(CompletableFuture.completedFuture(errorResponse));

        CompletableFuture<ResponseEntity<List<List<String>>>> response = logController.getLogs(filename, null, null);

        assertEquals(HttpStatus.OK, response.isCancelled());
        ..assertNotNull(response.);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        response.getBody().writeTo(baos);
        String content = baos.toString();

        assertTrue(content.contains("An error occurred"));
    }*/
}
