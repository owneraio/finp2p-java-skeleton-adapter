package io.ownera.ledger.adapter.service.model;

public class Receipt {

    public final String id;
    public final OperationType operationType;
    public final Asset asset;
    public final Source source;
    public final Destination destination;
    public final String quantity;
    public final TransactionDetails transactionDetails;
    public final TradeDetails tradeDetails;
    public ProofPolicy proof;
    public final long timestamp;

    public Receipt(String id, OperationType operationType, Asset asset, Source source, Destination destination, String quantity,
                   TransactionDetails transactionDetails, TradeDetails tradeDetails, ProofPolicy proof, long timestamp) {
        this.id = id;
        this.operationType = operationType;
        this.asset = asset;
        this.source = source;
        this.destination = destination;
        this.quantity = quantity;
        this.transactionDetails = transactionDetails;
        this.tradeDetails = tradeDetails;
        this.proof = proof;
        this.timestamp = timestamp;
    }

    public static Receipt newIssueReceipt(String transactionId, String assetId, String finId, String quantity, String operationId, ExecutionContext exCtx) {
        Asset asset = new Asset(assetId, AssetType.FINP2P);
        Destination destination = new Destination(finId, new FinIdAccount(finId));
        TransactionDetails details = new TransactionDetails(transactionId, operationId);
        TradeDetails trade = new TradeDetails(exCtx);
        long timestamp = System.currentTimeMillis();
        return new Receipt(transactionId, OperationType.ISSUE, asset, null, destination, quantity, details, trade, null, timestamp);
    }

    public static Receipt newTransferReceipt(String transactionId, String assetId, String from, String to, String quantity, String operationId, ExecutionContext exCtx) {
        Asset asset = new Asset(assetId, AssetType.FINP2P);
        Source source = new Source(from, new FinIdAccount(from));
        Destination destination = new Destination(to, new FinIdAccount(to));
        TransactionDetails details = new TransactionDetails(transactionId, operationId);
        TradeDetails trade = new TradeDetails(exCtx);
        long timestamp = System.currentTimeMillis();
        return new Receipt(transactionId, OperationType.TRANSFER, asset, source, destination, quantity, details, trade, null, timestamp);
    }

    public static Receipt newRedeemReceipt(String transactionId, String assetId, String from, String quantity, String operationId, ExecutionContext exCtx) {
        Asset asset = new Asset(assetId, AssetType.FINP2P);
        Source source = new Source(from, new FinIdAccount(from));
        TransactionDetails details = new TransactionDetails(transactionId, operationId);
        TradeDetails trade = new TradeDetails(exCtx);
        long timestamp = System.currentTimeMillis();
        return new Receipt(transactionId, OperationType.REDEEM, asset, source, null, quantity, details, trade, null, timestamp);
    }
}
