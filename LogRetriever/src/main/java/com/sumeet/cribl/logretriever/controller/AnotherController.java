package com.sumeet.cribl.logretriever.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@RestController
@RequestMapping("/api/logs")
public class AnotherController {

    private static final Logger logger = LoggerFactory.getLogger(AnotherController.class);

    @Value("${log.directory}")
    private String logDirectory;

    private static final long CHUNK_SIZE = 1L * 1024 * 1024 * 1024; // 1 GB chunk size

    @GetMapping(value = "/chunk-mmap", produces = "text/plain;charset=UTF-8")
    public void readLogFile(
            @RequestParam String filename,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer limit,
            HttpServletResponse response) throws IOException {

        logger.info("Received request for file: {}, keywords: {}, limit: {}", filename, keyword, limit);

        response.setContentType("text/plain;charset=UTF-8");
        PrintWriter writer = response.getWriter();

        boolean readEntireFile = (limit == null || limit == -1) && keyword == null;
        int effectiveLimit = (limit == null || limit <= 0) ? Integer.MAX_VALUE : limit;

        String reversedKeyword = (keyword != null) ? new StringBuilder(keyword).reverse().toString() : null;
        int[] kmpTable = (reversedKeyword != null) ? computeKMPTable(reversedKeyword) : null;

        try (FileChannel fileChannel = FileChannel.open(Paths.get(logDirectory, filename), StandardOpenOption.READ)) {
            long fileSize = fileChannel.size();
            long remainingBytes = fileSize;
            int lineCount = 0;
            StringBuilder carryOver = new StringBuilder();

            while (remainingBytes > 0 && (readEntireFile || lineCount < effectiveLimit)) {
                long chunkSize = Math.min(remainingBytes, CHUNK_SIZE);
                long startPosition = fileSize - remainingBytes;

                lineCount = processChunk(fileChannel, startPosition, chunkSize, reversedKeyword, kmpTable,
                        readEntireFile, effectiveLimit, lineCount, writer, carryOver);

                remainingBytes -= chunkSize;
                if (lineCount >= effectiveLimit && !readEntireFile) break;
            }

            // Process any remaining content in carryOver
            if (carryOver.length() > 0) {
                String line = carryOver.reverse().toString();
                if (readEntireFile || reversedKeyword == null || kmpSearch(carryOver, reversedKeyword, kmpTable)) {
                    writer.println(line);
                    writer.flush();
                }
            }
        } catch (IOException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writer.println("Error reading file: " + e.getMessage());
        } finally {
            writer.close();
        }
    }

    private int processChunk(FileChannel fileChannel, long startPosition, long chunkSize,
                             String reversedKeyword, int[] kmpTable, boolean readEntireFile,
                             int effectiveLimit, int lineCount, PrintWriter writer,
                             StringBuilder carryOver) throws IOException {
        MappedByteBuffer buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startPosition, chunkSize);
        StringBuilder lineBuilder = new StringBuilder();

        for (long i = chunkSize - 1; i >= 0; i--) {
            char c = (char) buffer.get((int) i);

            if (c == '\n') {
                lineBuilder.append(carryOver);
                if (readEntireFile || reversedKeyword == null || kmpSearch(lineBuilder, reversedKeyword, kmpTable)) {
                    String line = lineBuilder.reverse().toString();
                    writer.println(line);
                    writer.flush();
                    lineCount++;
                    if (lineCount >= effectiveLimit && !readEntireFile) break;
                }
                lineBuilder.setLength(0);
                carryOver.setLength(0);
            } else {
                lineBuilder.append(c);
            }
        }

        // Handle the case where a line spans across chunks
        carryOver.insert(0, lineBuilder);

        unmap(buffer);
        return lineCount;
    }

    private boolean kmpSearch(StringBuilder text, String pattern, int[] table) {
        int j = 0;
        for (int i = 0; i < text.length(); i++) {
            while (j > 0 && text.charAt(i) != pattern.charAt(j)) {
                j = table[j - 1];
            }
            if (text.charAt(i) == pattern.charAt(j)) {
                j++;
            }
            if (j == pattern.length()) {
                return true;
            }
        }
        return false;
    }

    private int[] computeKMPTable(String pattern) {
        int[] table = new int[pattern.length()];
        int j = 0;
        for (int i = 1; i < pattern.length(); i++) {
            while (j > 0 && pattern.charAt(i) != pattern.charAt(j)) {
                j = table[j - 1];
            }
            if (pattern.charAt(i) == pattern.charAt(j)) {
                j++;
            }
            table[i] = j;
        }
        return table;
    }

    private void unmap(MappedByteBuffer buffer) {
        if (buffer == null) return;

        try {
            MethodHandle unmapper = MethodHandles.lookup().findVirtual(
                    MappedByteBuffer.class,
                    "invokeCleaner",
                    MethodType.methodType(void.class)
            );
            unmapper.invoke(buffer);
        } catch (Throwable e) {
            logger.warn("Failed to unmap the buffer. System resources may not be immediately released.", e);
        }
    }
}
