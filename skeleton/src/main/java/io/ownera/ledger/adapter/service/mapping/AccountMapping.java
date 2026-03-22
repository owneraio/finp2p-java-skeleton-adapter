package io.ownera.ledger.adapter.service.mapping;

import java.time.Instant;
import java.util.Map;

/**
 * Represents all account mappings for a single finId.
 * Each mapping is a key-value pair (e.g. ledgerAccountId → 0x123, custodyAccountId → vault-456).
 */
public class AccountMapping {
    private final String finId;
    private final Map<String, String> fields;
    private final Instant createdAt;
    private final Instant updatedAt;

    public AccountMapping(String finId, Map<String, String> fields, Instant createdAt, Instant updatedAt) {
        this.finId = finId;
        this.fields = fields;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getFinId() { return finId; }
    public Map<String, String> getFields() { return fields; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    /**
     * Convenience: get a specific field value.
     */
    public String getField(String fieldName) {
        return fields != null ? fields.get(fieldName) : null;
    }
}
