package io.ownera.finp2p.signing.eip712.models;

public class Term {
    private String assetId;
    private String assetType;
    private String amount;

    public Term() {
    }

    public Term(String assetId, String assetType, String amount) {
        this.assetId = assetId;
        this.assetType = assetType;
        this.amount = amount;
    }

    public String getAssetId() {
        return assetId;
    }

    public Term setAssetId(String assetId) {
        this.assetId = assetId;
        return this;
    }

    public String getAssetType() {
        return assetType;
    }

    public Term setAssetType(String assetType) {
        this.assetType = assetType;
        return this;
    }

    public String getAmount() {
        return amount;
    }

    public Term setAmount(String amount) {
        this.amount = amount;
        return this;
    }
}
