package io.ownera.ledger.adapter.sample.collateral;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

public class CollateralsHolder {
    @JsonProperty("collaterals")
    private List<Collateral> collaterals;

    public List<Collateral> getCollaterals() {
        return collaterals;
    }

    public CollateralsHolder setCollaterals(List<Collateral> collaterals) {
        this.collaterals = collaterals;
        return this;
    }


    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static List<Collateral> parseFrom(Object details) {
        CollateralsHolder holder = objectMapper.convertValue(details, CollateralsHolder.class);
        return holder.collaterals;
    }
}
