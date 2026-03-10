package io.ownera.ledger.adapter.service;

public class BusinessException extends RuntimeException {
    public final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
