package io.ownera.ledger.adapter.service;

public class ValidationException extends RuntimeException {
    public final int code;

    public ValidationException(int code, String message) {
        super(message);
        this.code = code;
    }
}
