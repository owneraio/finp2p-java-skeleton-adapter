package io.ownera.ledger.adapter.service.workflow;

import io.ownera.ledger.adapter.service.model.OperationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.function.Supplier;

public class OperationExecutor {

    private static final Logger logger = LoggerFactory.getLogger(OperationExecutor.class);

    private final OperationStore store;
    private final @Nullable CallbackClient callbackClient;

    public OperationExecutor(OperationStore store, @Nullable CallbackClient callbackClient) {
        this.store = store;
        this.callbackClient = callbackClient;
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
            logger.debug("Operation in progress for method={}, cid={}", method, existing.cid);
            return pendingFactory.createPending(existing.cid);
        }

        String cid = CorrelationIdGenerator.generate();
        OperationRecord record = new OperationRecord(
                cid, method, OperationRecord.Status.IN_PROGRESS, inputsHash, null);
        store.save(record);

        try {
            T result = operation.get();
            store.updateStatus(cid, OperationRecord.Status.COMPLETED, result);

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
}
