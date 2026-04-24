package io.ownera.ledger.adapter.service.model;

public final class PlanProposalReset implements PlanProposal {
    public final int proposedSequence;

    public PlanProposalReset(int proposedSequence) {
        this.proposedSequence = proposedSequence;
    }
}
