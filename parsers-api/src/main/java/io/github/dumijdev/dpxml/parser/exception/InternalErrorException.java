package io.github.dumijdev.dpxml.parser.exception;

public class InternalErrorException extends RuntimeException {
    public InternalErrorException(Throwable cause) {
        super(cause);
    }

    public InternalErrorException(String message, Throwable cause) {
        super(message, cause);
    }
}
