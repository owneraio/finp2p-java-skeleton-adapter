package io.ownera.ledger.adapter.graphql.models;

public class AssetConfig {

    AssetSignatureTemplate signatureTemplate;

    public AssetSignatureTemplate getSignatureTemplate() {
        return signatureTemplate;
    }

    public void setSignatureTemplate(AssetSignatureTemplate signatureTemplate) {
        this.signatureTemplate = signatureTemplate;
    }

}
