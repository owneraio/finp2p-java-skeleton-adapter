package io.ownera.finp2p.signing.eip712.models;

public class ExecutionContext {
    private String executionPlanId;
    private String instructionSequenceNumber;

    public ExecutionContext() {
    }

    public ExecutionContext(String executionPlanId, String instructionSequenceNumber) {
        this.executionPlanId = executionPlanId;
        this.instructionSequenceNumber = instructionSequenceNumber;
    }

    public String getExecutionPlanId() {
        return executionPlanId;
    }

    public ExecutionContext setExecutionPlanId(String executionPlanId) {
        this.executionPlanId = executionPlanId;
        return this;
    }

    public String getInstructionSequenceNumber() {
        return instructionSequenceNumber;
    }

    public ExecutionContext setInstructionSequenceNumber(String instructionSequenceNumber) {
        this.instructionSequenceNumber = instructionSequenceNumber;
        return this;
    }
}
