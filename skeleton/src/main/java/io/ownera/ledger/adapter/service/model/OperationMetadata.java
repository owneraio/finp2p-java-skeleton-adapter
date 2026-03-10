package io.ownera.ledger.adapter.service.model;

public class OperationMetadata {
    public final OperationResponseStrategy responseStrategy;

    public OperationMetadata(OperationResponseStrategy responseStrategy) {
        this.responseStrategy = responseStrategy;
    }
}
