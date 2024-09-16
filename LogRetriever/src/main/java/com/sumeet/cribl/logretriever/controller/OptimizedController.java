package com.sumeet.cribl.logretriever.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.MappedByteBuffer;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


import java.nio.channels.FileChannel;

import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@RestController
@RequestMapping("/api/logs")
public class OptimizedController {

    @Value("${log.directory}")
    private String logDirectory;

    @GetMapping(value = "/kmp-mmap", produces = "text/plain;charset=UTF-8")
    public void readLogFile(
            @RequestParam String filename,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer limit,
            HttpServletResponse response) throws IOException {

        response.setContentType("text/plain;charset=UTF-8");
        PrintWriter writer = response.getWriter();

        boolean readEntireFile = (limit == null || limit == -1) && keyword == null;
        int effectiveLimit = (limit == null || limit <= 0) ? Integer.MAX_VALUE : limit;

        String reversedKeyword = (keyword != null) ? new StringBuilder(keyword).reverse().toString() : null;
        int[] kmpTable = (reversedKeyword != null) ? computeKMPTable(reversedKeyword) : null;

        try (FileChannel fileChannel = FileChannel.open(Paths.get(logDirectory, filename), StandardOpenOption.READ)) {
            long fileSize = fileChannel.size();
            long remainingBytes = fileSize;
            long mappedSize;
            long startPosition;
            int lineCount = 0;

            while (remainingBytes > 0 && (readEntireFile || lineCount < effectiveLimit)) {
                mappedSize = Math.min(remainingBytes, Integer.MAX_VALUE);
                startPosition = fileSize - remainingBytes;

                MappedByteBuffer buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startPosition, mappedSize);
                StringBuilder lineBuilder = new StringBuilder();

                for (long i = mappedSize - 1; i >= 0; i--) {
                    char c = (char) buffer.get((int) i);

                    if (c == '\n' || i == 0) {
                        if (i == 0 && c != '\n') lineBuilder.append(c);
                        if (readEntireFile || keyword == null || kmpSearch(lineBuilder, reversedKeyword, kmpTable)) {
                            String line = lineBuilder.reverse().toString();
                            writer.println(line);
                            writer.flush();
                            lineCount++;
                            if (lineCount >= effectiveLimit && !readEntireFile) break;
                        }
                        lineBuilder.setLength(0);
                    } else {
                        lineBuilder.append(c);
                    }
                }

                remainingBytes -= mappedSize;
                if (lineCount >= effectiveLimit && !readEntireFile) break;
            }
        } catch (IOException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writer.println("Error reading file: " + e.getMessage());
        } finally {
            writer.close();
        }
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
}
