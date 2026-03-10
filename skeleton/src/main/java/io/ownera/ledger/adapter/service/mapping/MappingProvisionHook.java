package io.ownera.ledger.adapter.service.mapping;

import java.util.Map;

/**
 * Hook called after an owner mapping is saved.
 * Adapters can implement this for ledger-specific provisioning.
 * Return value is merged into the response.
 */
public interface MappingProvisionHook {
    Map<String, String> afterSave(String finId, String ledgerAccountId, String status);
}
