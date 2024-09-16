package com.sumeet.cribl.logretriever.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/api/logs")
public class HighlyOptimizedMemoryMappedController {

    @Value("${log.directory}")
    private String logDirectory;

    private static final int CHUNK_SIZE = 64 * 1024 * 1024; // 64MB chunks
    private static final int BUFFER_SIZE = 8192; //8 * 1024 * 1024; //
    private final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    @GetMapping(value = "/optimized-mmap", produces = "text/plain;charset=UTF-8")
    public void readLogFile(
            @RequestParam String filename,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer limit,
            HttpServletResponse response) throws IOException {

        response.setContentType("text/plain;charset=UTF-8");
        Writer writer = new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8);

        boolean readEntireFile = true; //(limit == null || limit == -1) && keyword == null;
        int effectiveLimit = (limit == null || limit <= 0) ? Integer.MAX_VALUE : limit;

        try (FileChannel fileChannel = FileChannel.open(Paths.get(logDirectory, filename), StandardOpenOption.READ)) {
            long fileSize = fileChannel.size();
            List<Future<ChunkResult>> futures = new ArrayList<>();
            AtomicInteger lineCount = new AtomicInteger(0);

            int[] kmpTable = (keyword != null) ? computeKMPTable(keyword) : null;

            for (long position = fileSize; position > 0; position -= CHUNK_SIZE) {
                long chunkStart = Math.max(0, position - CHUNK_SIZE);
                long chunkSize = position - chunkStart;

                Future<ChunkResult> future = executorService.submit(() ->
                        processChunk(fileChannel, chunkStart, chunkSize, keyword, kmpTable, readEntireFile, effectiveLimit, lineCount)
                );
                futures.add(future);
            }

            for (int i = futures.size() - 1; i >= 0; i--) {
                ChunkResult result = futures.get(i).get();
                for (int j = result.getLines().size() - 1; j >= 0; j--) {
                    String line = result.getLines().get(j);
                    writer.write(line);
                    writer.write('\n');
                    writer.flush();
                }
                if (lineCount.get() >= effectiveLimit && !readEntireFile) {
                    break;
                }
            }

        } catch (Exception e) {
            throw new IOException("Error processing file", e);
        } finally {
            writer.flush();
            writer.close();
        }
    }

    private ChunkResult processChunk(FileChannel fileChannel, long start, long size, String keyword,
                                     int[] kmpTable, boolean readEntireFile, int effectiveLimit,
                                     AtomicInteger globalLineCount) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
        StringBuilder lineBuilder = new StringBuilder(200);
        List<String> lines = new ArrayList<>();
        long position = start + size;

        while (position > start && (readEntireFile || globalLineCount.get() < effectiveLimit)) {
            int bytesToRead = (int) Math.min(BUFFER_SIZE, position - start);
            buffer.clear().limit(bytesToRead);
            fileChannel.read(buffer, position - bytesToRead);
            buffer.flip();

            while (buffer.hasRemaining()) {
                char c = (char) buffer.get();
                if (c == '\n') {
                    String line = lineBuilder.reverse().toString(); // Reverse the line here
                    lineBuilder.setLength(0); // Clear the StringBuilder
                    if (readEntireFile || keyword == null || kmpSearch(line, keyword, kmpTable)) {
                        lines.add(line);
                        if (globalLineCount.incrementAndGet() >= effectiveLimit && !readEntireFile) {
                            return new ChunkResult(lines);
                        }
                    }
                } else {
                    lineBuilder.append(c);
                }
            }
            position -= bytesToRead;
        }

        if (lineBuilder.length() > 0) {
            String line = lineBuilder.reverse().toString(); // Reverse the last line
            if (readEntireFile || keyword == null || kmpSearch(line, keyword, kmpTable)) {
                lines.add(line);
                globalLineCount.incrementAndGet();
            }
        }

        return new ChunkResult(lines);
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

    private boolean kmpSearch(String text, String pattern, int[] table) {
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

    private static class ChunkResult {
        private final List<String> lines;

        ChunkResult(List<String> lines) {
            this.lines = lines;
        }

        List<String> getLines() {
            return lines;
        }
    }
}
