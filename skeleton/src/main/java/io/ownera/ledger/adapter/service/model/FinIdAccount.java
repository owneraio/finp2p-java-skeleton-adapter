package io.ownera.ledger.adapter.service.model;

import javax.annotation.Nullable;

public class FinIdAccount implements SourceAccount, DestinationAccount {
    public final String finId;
    /**
     * Organization ID that owns this FinID. Not carried by the 0.28 adapter API;
     * adapters populate this from their own org lookup (e.g. via FinP2PSDK).
     * Aligns with Node.js skeleton's FinIdAccount.orgId.
     */
    @Nullable
    public final String orgId;
    /**
     * Custodian organization ID. Not carried by the 0.28 adapter API;
     * adapters populate this externally.
     */
    @Nullable
    public final String custodianOrgId;

    public FinIdAccount(String finId) {
        this(finId, null, null);
    }

    public FinIdAccount(String finId, @Nullable String orgId, @Nullable String custodianOrgId) {
        this.finId = finId;
        this.orgId = orgId;
        this.custodianOrgId = custodianOrgId;
    }

    public Source source() {
        return new Source(finId, this);
    }

    public Destination destination() {
        return new Destination(finId, this);
    }
}
