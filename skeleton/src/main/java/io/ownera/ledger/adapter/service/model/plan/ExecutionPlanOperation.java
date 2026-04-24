package io.ownera.ledger.adapter.service.model.plan;

/**
 * Sealed union of execution plan operations.
 * Aligns with Node.js skeleton's {@code ExecutionPlanOperation}.
 * Implementers: {@link HoldInstruction}, {@link ReleaseInstruction},
 * {@link IssueInstruction}, {@link TransferInstruction}, {@link RedemptionInstruction},
 * {@link RevertHoldInstruction}, {@link AwaitInstruction}.
 */
public interface ExecutionPlanOperation {
}
