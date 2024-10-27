package io.ownera.ledger.adapter.service.model;

public class ServiceOperationStatus {

    public final boolean isCompleted;

    public final String correlationId;

    public final Object result;

    public final int errorCode;
    public final String error;

    private ServiceOperationStatus(boolean isCompleted, String correlationId, Object result, String error, int errorCode) {
        this.isCompleted = isCompleted;
        this.correlationId = correlationId;
        this.result = result;
        this.error = error;
        this.errorCode = errorCode;
    }

    public static ServiceOperationStatus completed(Object result) {
        return new ServiceOperationStatus(true, null, result, null, 0);
    }

    public static ServiceOperationStatus delayed(String correlationId) {
        return new ServiceOperationStatus(false, correlationId, null, null, 0);
    }

    public static ServiceOperationStatus failed(String error, int errorCode) {
        return new ServiceOperationStatus(true, null, null, error, errorCode);
    }
}
