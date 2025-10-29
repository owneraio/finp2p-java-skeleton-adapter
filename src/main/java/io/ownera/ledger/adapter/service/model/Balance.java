package io.ownera.ledger.adapter.service.model;

public class Balance {
    public final String current;
    public final String available;
    public final String held;
    public Balance(String current, String available, String held) {
        this.current = current;
        this.available = available;
        this.held = held;
    }
}
