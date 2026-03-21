package io.ownera.finp2p.signing.eip712.models;

public class TradeDetails {

    private ExecutionContext executionContext;

    public TradeDetails() {
    }

    public TradeDetails(ExecutionContext executionContext) {
        this.executionContext = executionContext;
    }

    public ExecutionContext getExecutionContext() {
        return executionContext;
    }

    public TradeDetails setExecutionContext(ExecutionContext executionContext) {
        this.executionContext = executionContext;
        return this;
    }
}
