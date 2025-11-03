package io.ownera.ledger.adapter.sample.collateral;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Custody {

    @JsonProperty("type")
    private String type;

    @JsonProperty("accountID")
    private String accountId;

    public String getType() {
        return type;
    }

    public Custody setType(String type) {
        this.type = type;
        return this;
    }

    public String getAccountId() {
        return accountId;
    }

    public Custody setAccountId(String accountId) {
        this.accountId = accountId;
        return this;
    }


}
