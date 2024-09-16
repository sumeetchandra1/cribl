package com.sumeet.cribl.logretriever.controller;

import com.sumeet.cribl.logretriever.exception.LogRetrievalException;
import com.sumeet.cribl.logretriever.service.LogFileService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

@RestController
@RequestMapping("/api/logs")
public class LogFileController {

    private static final Logger logger = LoggerFactory.getLogger(LogFileController.class);
    private final LogFileService logFileService;

    @Autowired
    public LogFileController(LogFileService logFileService) {
        this.logFileService = logFileService;
    }

    /*@GetMapping
    public ResponseEntity<?> getLogs(
            @RequestParam String filename,
            @RequestParam(required = false) String keywords,
            @RequestParam(required = false) Integer limit,
            HttpServletResponse httpServletResponse) {

        logger.info("Received request for file: {}, keywords: {}, limit: {}", filename, keywords, limit);

        try {
            Object fileContent = logFileService.getFilteredFile(filename, keywords, limit);

            if (fileContent instanceof Resource) {
                return handleResourceResponse((Resource) fileContent, filename);
                //return handleStreamingResponse((InputStream)fileContent, filename, httpServletResponse);
            } else {
                logger.error("Unexpected return type from service: {}", fileContent.getClass().getName());
                return ResponseEntity.internalServerError().build();
            }
        } catch (LogRetrievalException e) {
            logger.error("Error transferring file: {}", e.getMessage() + e.getCause());
            return ResponseEntity.badRequest().body(e.getMessage() + e.getCause());
        } catch (Exception e) {
            logger.error("Unexpected error during file transfer", e);
            return ResponseEntity.internalServerError().body("An unexpected error occurred");
        }
    }*/

    @GetMapping
    public void getLogs(
            @RequestParam String filename,
            @RequestParam(required = false) String keywords,
            @RequestParam(required = false) Integer limit,
            @RequestHeader(value = "Range", required = false) String rangeHeader,
            HttpServletResponse response) {

        logger.info("Received request for file: {}, keywords: {}, limit: {}", filename, keywords, limit);

        try {
            Object fileContent = logFileService.getFilteredFile(filename, keywords, limit);

            if (fileContent instanceof InputStream) {
                //InputStream inputStream = ((InputStreamResource) fileContent).getInputStream();
                handleStreamingResponse((InputStream)fileContent, filename, response);
            } else {
                logger.error("Unexpected return type from service: {}", fileContent.getClass().getName());
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } catch (LogRetrievalException e) {
            logger.error("Error transferring file: {}", e.getMessage() + e.getCause());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            try {
                response.getWriter().write(e.getMessage() + e.getCause());
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        } catch (Exception e) {
            logger.error("Unexpected error during file transfer", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try {
                response.getWriter().write("An unexpected error occurred");
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    private ResponseEntity<Resource> handleResourceResponse(Resource resource, String filename) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }

    /*private ResponseEntity<StreamingResponseBody> handleStreamingResponse(InputStream inputStream, String filename) {
        StreamingResponseBody responseBody = outputStream -> {
            byte[] buffer = new byte[8192];
            int bytesRead;
            try {
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    outputStream.flush();
                }
            }
            finally {
                inputStream.close();
            }
        };

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(responseBody);
    }*/

    private void handleStreamingResponse(InputStream inputStream, String filename, HttpServletResponse httpServletResponse) throws IOException {

        httpServletResponse.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        httpServletResponse.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");

        byte[] buffer = new byte[8192];
        int bytesRead;
        try (OutputStream out = httpServletResponse.getOutputStream()) {
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                out.flush();
            }
        } finally {
            inputStream.close();
        }
    }
}

