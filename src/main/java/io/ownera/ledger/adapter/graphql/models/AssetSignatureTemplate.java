package io.ownera.ledger.adapter.graphql.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AssetSignatureTemplate {

    @JsonProperty("hashFunction")
    AssetHashFunction hashFunction;

    @JsonInclude(Include.NON_NULL)
    @JsonProperty("templateType")
    String templateType;

    public AssetHashFunction getHashFunction() {
        return hashFunction;
    }

    public void setHashFunction(AssetHashFunction hashFunction) {
        this.hashFunction = hashFunction;
    }

    public String getTemplateType() {
        return templateType;
    }

    public void setTemplateType(String templateType) {
        this.templateType = templateType;
    }
}
