package com.sumeet.cribl.logretriever.exception;

public class InvalidFileNameException extends RuntimeException {
    public InvalidFileNameException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidFileNameException(String message) {
        super(message);
    }
}
