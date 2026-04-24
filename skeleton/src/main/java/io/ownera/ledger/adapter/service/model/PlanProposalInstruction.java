package io.ownera.ledger.adapter.service.model;

public final class PlanProposalInstruction implements PlanProposal {
    public final int instructionSequence;

    public PlanProposalInstruction(int instructionSequence) {
        this.instructionSequence = instructionSequence;
    }
}
