package io.ownera.ledger.adapter.service.mapping;

import java.util.Map;

/**
 * Hook called after an owner mapping is saved.
 * Adapters can implement this for ledger-specific provisioning
 * (e.g., on-ledger credentials, KYC grants).
 * <p>
 * Return value is merged into the response (e.g. credentialCid, credentialStatus).
 */
public interface MappingProvisionHook {
    Map<String, String> afterSave(String finId, String ledgerAccountId, String status);
}
