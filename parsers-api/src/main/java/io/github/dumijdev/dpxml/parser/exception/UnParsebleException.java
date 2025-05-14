package io.github.dumijdev.dpxml.parser.exception;

public class UnParsebleException extends RuntimeException{
    public UnParsebleException() {
    }

    public UnParsebleException(String message) {
        super(message);
    }

    public UnParsebleException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnParsebleException(Throwable cause) {
        super(cause);
    }
}
