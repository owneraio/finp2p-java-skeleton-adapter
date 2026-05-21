package io.ownera.ledger.adapter.service.workflow;

import io.ownera.ledger.adapter.service.model.OperationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

public class OperationExecutor {

    private static final Logger logger = LoggerFactory.getLogger(OperationExecutor.class);

    private final OperationStore store;
    private final @Nullable CallbackClient callbackClient;
    private final boolean async;
    private final @Nullable ExecutorService executorPool;
    private final OperationOutputSerializer outputSerializer;

    public OperationExecutor(OperationStore store,
                             @Nullable CallbackClient callbackClient,
                             boolean async) {
        this(store, callbackClient, async, OperationOutputSerializer.defaultSerializer());
    }

    public OperationExecutor(OperationStore store,
                             @Nullable CallbackClient callbackClient,
                             boolean async,
                             OperationOutputSerializer outputSerializer) {
        this.store = store;
        this.callbackClient = callbackClient;
        this.async = async;
        this.executorPool = async ? Executors.newCachedThreadPool() : null;
        this.outputSerializer = outputSerializer;
    }

    /**
     * Synchronous workflow (default).
     */
    public OperationExecutor(OperationStore store, @Nullable CallbackClient callbackClient) {
        this(store, callbackClient, false);
    }

    /**
     * Compute a SHA-256 inputs hash for idempotency dedup.
     * Callers use this to produce the inputsHash before calling execute().
     */
    public static String computeInputsHash(String... parts) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) digest.update((byte) 0);
                digest.update(parts[i].getBytes(StandardCharsets.UTF_8));
            }
            byte[] hash = digest.digest();
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends OperationStatus> T execute(
            String method,
            String inputsHash,
            Supplier<T> operation,
            PendingFactory<T> pendingFactory
    ) {
        OperationRecord existing = store.findByInputsHash(inputsHash);
        if (existing != null) {
            if (existing.status == OperationRecord.Status.COMPLETED && existing.result != null) {
                logger.debug("Returning cached result for method={}, cid={}", method, existing.cid);
                return (T) existing.result;
            }
            logger.debug("Operation in progress or completed (cached outputs only durable via polling) "
                    + "for method={}, cid={}", method, existing.cid);
            return pendingFactory.createPending(existing.cid);
        }

        String cid = CorrelationIdGenerator.generate();
        OperationRecord record = new OperationRecord(
                cid, method, OperationRecord.Status.IN_PROGRESS, inputsHash, null);
        // Persist a pending payload on insertion so polling returns an in-progress response
        // for the cid instead of 404 — matches Node's createServiceProxy contract where every
        // known cid is pollable (skeleton/src/workflows/service.ts).
        T pending = pendingFactory.createPending(cid);
        String pendingJson = trySerialize(cid, method, pending);
        store.save(record, pendingJson);

        if (async) {
            executorPool.submit(() -> executeOperation(cid, method, operation));
            return pending;
        }

        return executeOperation(cid, method, operation);
    }

    private <T extends OperationStatus> T executeOperation(String cid, String method, Supplier<T> operation) {
        try {
            T result = operation.get();
            String outputsJson = trySerialize(cid, method, result);
            store.updateStatus(cid, OperationRecord.Status.COMPLETED, outputsJson);

            if (callbackClient != null) {
                try {
                    callbackClient.sendCallback(cid, result);
                } catch (Exception e) {
                    logger.warn("Failed to send callback for cid={}: {}", cid, e.getMessage());
                }
            }

            return result;
        } catch (Exception e) {
            logger.error("Operation failed: method={}, cid={}, error={}", method, cid, e.getMessage());
            // Persist a failure payload so polling returns a proper failed-operation response
            // (matches Node's wrappedResponse error branch in service.ts). Polling reserves 404
            // for truly unknown cids.
            String failureJson = tryBuildFailure(cid, method, e);
            store.updateStatus(cid, OperationRecord.Status.FAILED, failureJson);
            if (!async) throw e;
            return null;
        }
    }

    private String tryBuildFailure(String cid, String method, Exception cause) {
        try {
            OperationStatus failure = WorkflowOutcomes.failureFor(method, 1, String.valueOf(cause.getMessage()));
            return outputSerializer.serialize(failure);
        } catch (Exception e) {
            logger.warn("Failed to build/serialize failure payload for method={}, cid={}: {}", method, cid, e.getMessage());
            return null;
        }
    }

    private String trySerialize(String cid, String method, OperationStatus result) {
        try {
            return outputSerializer.serialize(result);
        } catch (Exception e) {
            // Outputs are an optimization for polling/replay — never block the operation's
            // completion on a serialization failure. Log and persist with null outputs.
            logger.warn("Failed to serialize outputs for method={}, cid={}: {}", method, cid, e.getMessage());
            return null;
        }
    }

    @FunctionalInterface
    public interface PendingFactory<T> {
        T createPending(String cid);
    }
}
