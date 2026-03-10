package io.ownera.ledger.adapter.service.model;

public class ErrorDetails {
    public final int code;
    public final String message;

    public ErrorDetails(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
