package io.ownera.ledger.adapter.service.model;

public class Receipt {

    public  final String id;
    public  final Asset asset;
    public  final Source source;
    public  final Destination destination;
    public  final String quantity;
    public  final TransactionDetails transactionDetails;
    public  final TradeDetails tradeDetails;
    public  final OperationType operationType;
    public  final ProofPolicy proof;
    public  final long timestamp;

    public Receipt(String id, Asset asset, Source source, Destination destination, String quantity, TransactionDetails transactionDetails, TradeDetails tradeDetails, OperationType operationType, ProofPolicy proof, long timestamp) {
        this.id = id;
        this.asset = asset;
        this.source = source;
        this.destination = destination;
        this.quantity = quantity;
        this.transactionDetails = transactionDetails;
        this.tradeDetails = tradeDetails;
        this.operationType = operationType;
        this.proof = proof;
        this.timestamp = timestamp;
    }
}
