package io.ownera.ledger.adapter.service.mapping;

public class MappingValidationException extends RuntimeException {

    public MappingValidationException(String message) {
        super(message);
    }

    public MappingValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
