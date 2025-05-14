package io.github.dumijdev.dpxml.parser.exception;

public class InvalidReferenceNameException extends RuntimeException {
    public InvalidReferenceNameException() {
    }

    public InvalidReferenceNameException(String message) {
        super(message);
    }

    public InvalidReferenceNameException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidReferenceNameException(Throwable cause) {
        super(cause);
    }
}
