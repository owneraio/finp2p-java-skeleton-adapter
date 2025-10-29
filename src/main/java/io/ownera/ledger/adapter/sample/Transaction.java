package io.ownera.ledger.adapter.sample;

import io.ownera.ledger.adapter.service.model.*;


public class Transaction {

    public final String id;
    public final FinIdAccount source;
    public final Destination destination;
    public final String quantity;
    public final Asset asset;
    public final ExecutionContext executionContext;
    public final OperationType operationType;
    public final String operationId;
    public final long timestamp;

    public Transaction(String id, FinIdAccount source, Destination destination,
                       String quantity, Asset asset, ExecutionContext executionContext,
                       OperationType operationType, String operationId, long timestamp) {
        this.id = id;
        this.source = source;
        this.destination = destination;
        this.quantity = quantity;
        this.asset = asset;
        this.executionContext = executionContext;
        this.operationType = operationType;
        this.operationId = operationId;
        this.timestamp = timestamp;
    }

    public Receipt toReceipt() {
        Source src = null;
        if (source != null) {
            src = source.source();
        }

        return new Receipt(id, operationType, asset, src, destination, quantity,
                new TransactionDetails(id, operationId), new TradeDetails(executionContext), null, timestamp);
    }


}
