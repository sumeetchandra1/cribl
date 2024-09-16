package com.sumeet.cribl.logretriever;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LogRetrieverApplicationTests {

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	public void testRetrieveLogsWorkflow() {

		String fileName = "abcd_10MB_file";
		//"http://localhost:8080/api/logs?filename=%s&&limit=10"
		String Url = String.format("http://localhost:8080/api/logs?filename=%s", fileName);

		/*ResponseEntity<List<LogEntryResponse>> response = restTemplate.exchange(Url,
				HttpMethod.GET,
				null,
				new ParameterizedTypeReference<List<LogEntryResponse>>() {});

		for (LogEntryResponse entry : response.getBody()) {
			System.out.println(entry.getLogEntry());
		}*/
	}
}
