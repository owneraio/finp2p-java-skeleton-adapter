package io.ownera.ledger.adapter.service.model.plan;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * Entry in an execution plan.
 * Aligns with Node.js skeleton's {@code ExecutionInstruction}.
 */
public final class ExecutionInstruction {
    public final int sequence;
    public final List<String> organizations;
    public final ExecutionPlanOperation operation;
    @Nullable
    public final Long timeout;

    public ExecutionInstruction(int sequence, List<String> organizations, ExecutionPlanOperation operation, @Nullable Long timeout) {
        this.sequence = sequence;
        this.organizations = organizations != null ? organizations : Collections.emptyList();
        this.operation = operation;
        this.timeout = timeout;
    }
}
