package io.ownera.ledger.adapter.service.workflow;

import io.ownera.ledger.adapter.service.model.OperationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.function.Function;
import java.util.function.Supplier;

public class OperationExecutor {

    private static final Logger logger = LoggerFactory.getLogger(OperationExecutor.class);

    private final OperationStore store;
    private final @Nullable CallbackClient callbackClient;
    private final @Nullable Function<OperationStatus, String> resultSerializer;

    public OperationExecutor(OperationStore store,
                             @Nullable CallbackClient callbackClient,
                             @Nullable Function<OperationStatus, String> resultSerializer) {
        this.store = store;
        this.callbackClient = callbackClient;
        this.resultSerializer = resultSerializer;
    }

    public OperationExecutor(OperationStore store, @Nullable CallbackClient callbackClient) {
        this(store, callbackClient, null);
    }

    @SuppressWarnings("unchecked")
    public <T extends OperationStatus> T execute(
            String method,
            String idempotencyKey,
            String inputsRaw,
            Supplier<T> operation,
            PendingFactory<T> pendingFactory
    ) {
        // Wrap inputs as JSON object for JSONB storage
        String inputsJson = toInputsJson(method, idempotencyKey, inputsRaw);

        // Check for existing operation with same inputs (idempotency)
        OperationRecord existing = store.findByInputs(inputsJson);
        if (existing != null) {
            if (existing.status == OperationRecord.Status.SUCCEEDED) {
                logger.debug("Returning cached result for method={}, cid={}", method, existing.cid);
                return pendingFactory.createPending(existing.cid);
            }
            if (existing.status == OperationRecord.Status.IN_PROGRESS) {
                logger.debug("Operation in progress for method={}, cid={}", method, existing.cid);
                return pendingFactory.createPending(existing.cid);
            }
            // FAILED — fall through to retry
        }

        String cid = CorrelationIdGenerator.generate();
        OperationRecord record = new OperationRecord(
                cid, method, OperationRecord.Status.IN_PROGRESS, inputsJson, null);
        OperationRecord saved = store.save(record);

        // save() may return an existing record if inputs conflict (concurrent insert)
        if (!saved.cid.equals(cid)) {
            logger.debug("Concurrent insert detected, returning existing cid={}", saved.cid);
            return pendingFactory.createPending(saved.cid);
        }

        try {
            T result = operation.get();
            String outputsJson = serializeResult(result);
            store.updateStatus(cid, OperationRecord.Status.SUCCEEDED, outputsJson);

            if (callbackClient != null) {
                try {
                    callbackClient.sendCallback(cid, result);
                } catch (Exception e) {
                    logger.warn("Failed to send callback for cid={}: {}", cid, e.getMessage());
                }
            }

            return result;
        } catch (Exception e) {
            store.updateStatus(cid, OperationRecord.Status.FAILED, null);
            throw e;
        }
    }

    @FunctionalInterface
    public interface PendingFactory<T> {
        T createPending(String cid);
    }

    private static String toInputsJson(String method, String idempotencyKey, String raw) {
        // Escape for JSON string value
        String escaped = raw.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
        return "{\"method\":\"" + method + "\",\"idempotencyKey\":\"" + idempotencyKey
                + "\",\"data\":\"" + escaped + "\"}";
    }

    private @Nullable String serializeResult(@Nullable OperationStatus result) {
        if (result == null || resultSerializer == null) return null;
        try {
            return resultSerializer.apply(result);
        } catch (Exception e) {
            logger.warn("Failed to serialize operation result: {}", e.getMessage());
            return null;
        }
    }
}
