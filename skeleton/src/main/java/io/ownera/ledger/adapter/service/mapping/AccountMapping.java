package io.ownera.ledger.adapter.service.mapping;

import java.time.Instant;

public class AccountMapping {
    private final String finId;
    private final String account;
    private final Instant createdAt;
    private final Instant updatedAt;

    public AccountMapping(String finId, String account, Instant createdAt, Instant updatedAt) {
        this.finId = finId;
        this.account = account;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getFinId() { return finId; }
    public String getAccount() { return account; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
