package io.ownera.ledger.adapter.service.mapping;

import java.util.List;

/**
 * Storage interface for finId ↔ ledger account mappings.
 * Mirrors the Node.js skeleton's account_mappings table contract.
 */
public interface AccountMappingStore {

    /** Get all mappings for a given finId, ordered by creation time. */
    List<AccountMapping> getByFinId(String finId);

    /** Get all mappings for a given ledger account, ordered by creation time. */
    List<AccountMapping> getByAccount(String account);

    /** Save a finId ↔ account mapping. Idempotent (no-op on conflict). */
    AccountMapping save(String finId, String account);

    /** Delete a specific mapping, or all mappings for finId if account is null. */
    void delete(String finId, String account);

    /** List all mappings. */
    List<AccountMapping> listAll();
}
