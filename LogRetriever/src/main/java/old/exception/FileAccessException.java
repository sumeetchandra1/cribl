package old.exception;

public class FileAccessException extends RuntimeException {
    public FileAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
