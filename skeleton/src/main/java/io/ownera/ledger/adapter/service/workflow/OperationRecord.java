package io.ownera.ledger.adapter.service.workflow;

import javax.annotation.Nullable;

public class OperationRecord {

    public enum Status {
        IN_PROGRESS("in_progress"),
        SUCCEEDED("succeeded"),
        FAILED("failed");

        private final String dbValue;

        Status(String dbValue) {
            this.dbValue = dbValue;
        }

        public String dbValue() {
            return dbValue;
        }

        public static Status fromDbValue(String value) {
            for (Status s : values()) {
                if (s.dbValue.equals(value)) return s;
            }
            throw new IllegalArgumentException("Unknown status: " + value);
        }
    }

    public final String cid;
    public final String method;
    public final Status status;
    public final String inputs;
    public final @Nullable String outputs;

    public OperationRecord(String cid, String method, Status status,
                           String inputs, @Nullable String outputs) {
        this.cid = cid;
        this.method = method;
        this.status = status;
        this.inputs = inputs;
        this.outputs = outputs;
    }
}
