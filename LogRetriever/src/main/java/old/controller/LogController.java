package old.controller;

import old.service.LogService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/logs")
public class LogController {
    private final LogService logService;

    @Autowired
    public LogController(LogService logService) {
        this.logService = logService;
    }

    @GetMapping
    public void getLogs(
            @RequestParam String filename,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer limit,
            HttpServletResponse httpServletResponse) throws FileNotFoundException {

        httpServletResponse.setContentType("text/plain");
        Path filePath = Paths.get("/var/log/", filename);

        //BufferedReader bufferedReader = new BufferedReader(new FileReader( + filename));
        /*
                   String line;
            while ((line = bufferedReader.readLine()) != null) {

                System.out.println(line);
                System.out.println("-");

                if(keyword == null) {
                    pw.println(line);
                } else if (line.contains(keyword)) {
                    pw.println(line);
                }
            }

            PrintWriter pw = httpServletResponse.getWriter();
         */

        //Path filePath = Paths.get("/var/log/", filename);
        int bufferSize = 25 * 1024 * 1024; // 10MB buffer
        byte[] buffer = new byte[bufferSize];
        int bytesRead = 0;
        int linesRead = 0;

        try (RandomAccessFile file = new RandomAccessFile("/var/log/" + filename, "r")) {
            long pointer = file.length();

            while (pointer > 0 && linesRead < ((limit != null && limit > 0 ) ? limit : Integer.MAX_VALUE)) {
                int bytesToRead = (int) Math.min(bufferSize, pointer);
                pointer -= bytesToRead;

                file.seek(pointer);
                bytesRead = file.read(buffer, 0, bytesToRead);

                for (int i = bytesRead - 1; i >= 0 && linesRead < ((limit != null && limit > 0 ) ? limit : Integer.MAX_VALUE ); i--) {
                    if (buffer[i] == '\n') {
                        String line = new String(buffer, i + 1, bytesRead - i - 1, StandardCharsets.UTF_8);
                        httpServletResponse.getOutputStream().write(line.getBytes(StandardCharsets.UTF_8));
                        httpServletResponse.getOutputStream().flush();
                        bytesRead = i;
                        linesRead++;
                    }
                }
            }

            if (linesRead < ((limit != null && limit > 0 ) ? limit : Integer.MAX_VALUE) && bytesRead > 0) {
                String line = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
                httpServletResponse.getOutputStream().write(line.getBytes(StandardCharsets.UTF_8));
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to read the log file from the end", e);
        }

    }
        //return logService.retrieveLogsAsync(filename, keyword, limit)
            //    .thenApply(this::mapToResponseEntity);


        private ResponseEntity<List<List<String>>> mapToResponseEntity(List<List<String>> logs) {
        if (logs.size() == 1 && logs.get(0).size() == 1) {
            String message = logs.get(0).get(0);
            if (message.startsWith("Log file not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(logs);
            } else if (message.startsWith("Access denied")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(logs);
            } else if (message.startsWith("Invalid file")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(logs);
            } else if (message.startsWith("An error occurred")) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(logs);
            }
        }
        return ResponseEntity.ok(logs);
    }
}