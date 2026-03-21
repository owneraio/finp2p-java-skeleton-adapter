package io.ownera.finp2p.signing.eip712.models;

import io.ownera.finp2p.signing.eip712.Message;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Receipt implements Message {

    public static final String TYPE_NAME = "Receipt";

    private static final Map<String, List<TypeField>> TYPES;

    static {
        Map<String, List<TypeField>> types = new HashMap<>();
        types.putAll(MessageTypes.DOMAIN_TYPE);
        types.put("Source", List.of(
                new TypeField("accountType", "string"),
                new TypeField("finId", "string")
        ));
        types.put("Destination", List.of(
                new TypeField("accountType", "string"),
                new TypeField("finId", "string")
        ));
        types.put("Asset", List.of(
                new TypeField("assetId", "string"),
                new TypeField("assetType", "string")
        ));
        types.put("ExecutionContext", List.of(
                new TypeField("executionPlanId", "string"),
                new TypeField("instructionSequenceNumber", "string")
        ));
        types.put("TradeDetails", List.of(
                new TypeField("executionContext", "ExecutionContext")
        ));
        types.put("TransactionDetails", List.of(
                new TypeField("operationId", "string"),
                new TypeField("transactionId", "string")
        ));

        // Receipt itself
        types.put(TYPE_NAME, List.of(
                new TypeField("id", "string"),
                new TypeField("operationType", "string"),
                new TypeField("source", "Source"),
                new TypeField("destination", "Destination"),
                new TypeField("asset", "Asset"),
                new TypeField("tradeDetails", "TradeDetails"),
                new TypeField("transactionDetails", "TransactionDetails"),
                new TypeField("quantity", "string")
        ));

        TYPES = Map.copyOf(types);
    }

    private String id;
    private String operationType;
    private Source source;
    private Destination destination;
    private Asset asset;
    private TradeDetails tradeDetails;
    private TransactionDetails transactionDetails;
    private String quantity;

    public Receipt() {
    }

    public Receipt(String id, String operationType, Source source, Destination destination,
                   Asset asset, TradeDetails tradeDetails, TransactionDetails transactionDetails, String quantity) {
        this.id = id;
        this.operationType = operationType;
        this.source = source;
        this.destination = destination;
        this.asset = asset;
        this.tradeDetails = tradeDetails;
        this.transactionDetails = transactionDetails;
        this.quantity = quantity;
    }

    public String getId() {
        return id;
    }

    public Receipt setId(String id) {
        this.id = id;
        return this;
    }

    public String getOperationType() {
        return operationType;
    }

    public Receipt setOperationType(String operationType) {
        this.operationType = operationType;
        return this;
    }

    public Source getSource() {
        return source;
    }

    public Receipt setSource(Source source) {
        this.source = source;
        return this;
    }

    public Destination getDestination() {
        return destination;
    }

    public Receipt setDestination(Destination destination) {
        this.destination = destination;
        return this;
    }

    public Asset getAsset() {
        return asset;
    }

    public Receipt setAsset(Asset asset) {
        this.asset = asset;
        return this;
    }

    public TradeDetails getTradeDetails() {
        return tradeDetails;
    }

    public Receipt setTradeDetails(TradeDetails tradeDetails) {
        this.tradeDetails = tradeDetails;
        return this;
    }

    public TransactionDetails getTransactionDetails() {
        return transactionDetails;
    }

    public Receipt setTransactionDetails(TransactionDetails transactionDetails) {
        this.transactionDetails = transactionDetails;
        return this;
    }

    public String getQuantity() {
        return quantity;
    }

    public Receipt setQuantity(String quantity) {
        this.quantity = quantity;
        return this;
    }

    @Override
    public String getTypeName() {
        return TYPE_NAME;
    }

    @Override
    public Map<String, List<TypeField>> getTypes() {
        return TYPES;
    }
}
