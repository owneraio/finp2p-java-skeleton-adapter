package io.ownera.ledger.adapter.service.model;

public class ServiceOperationResult<T> {

    public final boolean isCompleted;

    public final String correlationId;

    public final T result;

    public final int errorCode;
    public final String error;

    private ServiceOperationResult(boolean isCompleted, String correlationId, T result, String error, int errorCode) {
        this.isCompleted = isCompleted;
        this.correlationId = correlationId;
        this.result = result;
        this.error = error;
        this.errorCode = errorCode;
    }

    public static <T> ServiceOperationResult<T> completed(T result) {
        return new ServiceOperationResult<>(true, null, result, null, 0);
    }

    public static <T> ServiceOperationResult<T> delayed(String correlationId) {
        return new ServiceOperationResult<>(false, correlationId, null, null, 0);
    }

    public static <T> ServiceOperationResult<T> failed(String error, int errorCode) {
        return new ServiceOperationResult<>(true, null, null, error, errorCode);
    }

}
