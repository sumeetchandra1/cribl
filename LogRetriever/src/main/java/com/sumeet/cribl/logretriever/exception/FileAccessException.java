package com.sumeet.cribl.logretriever.exception;

public class FileAccessException extends RuntimeException {
    public FileAccessException(String message, Throwable cause) {
        super(message, cause);
    }

    public FileAccessException(String message) {
        super(message);
    }
}
