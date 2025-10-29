package io.ownera.ledger.adapter.sample.collateral;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CertificateItem {
    @JsonProperty("name")
    private String name;
    @JsonProperty("value")
    private String value;

    public CertificateItem() {
    }

    public CertificateItem(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public CertificateItem setName(String name) {
        this.name = name;
        return this;
    }

    public String getValue() {
        return value;
    }

    public CertificateItem setValue(String value) {
        this.value = value;
        return this;
    }
}
