package io.ownera.ledger.adapter.service.workflow;

import io.ownera.ledger.adapter.service.model.CallbackResponseStrategy;
import io.ownera.ledger.adapter.service.model.OperationMetadata;
import io.ownera.ledger.adapter.service.model.OperationStatus;
import io.ownera.ledger.adapter.service.model.PollingResponseStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * JDK dynamic proxy that adds durable async workflow semantics around a service interface.
 *
 * <p>Mirrors the Node skeleton's {@code createServiceProxy}
 * (skeleton/src/workflows/service.ts): when a proxied method is invoked, the proxy persists a
 * pending row (with serialized input args + pending output payload), submits the real method
 * to a background executor, and returns the pending payload immediately. When the real method
 * completes, {@link #finalize} writes the result to the store first and only then sends a
 * callback — so a crash between persistence and callback never drops a finalized operation
 * (it stays in the row; startup replay re-finalizes it).
 *
 * <p>Idempotency: each call hashes (method, encoded args) into an {@code inputs_hash}. A
 * second identical call short-circuits to the existing row's outputs without re-running the
 * underlying service method.
 *
 * <p>Crash recovery is handled by {@link WorkflowRecovery}, which replays {@code IN_PROGRESS}
 * rows through the proxy's {@link #replay(String, Object[])} entrypoint at startup.
 */
public class WorkflowServiceProxy {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowServiceProxy.class);

    private final Object target;
    private final OperationStore store;
    private final @Nullable CallbackClient callbackClient;
    private final ExecutorService executor;
    private final WorkflowArgsCodec argsCodec;
    private final OperationOutputSerializer outputSerializer;
    private final Set<String> proxiedMethods;
    private final OperationMetadata opMetadata;

    public WorkflowServiceProxy(Object target,
                                OperationStore store,
                                @Nullable CallbackClient callbackClient,
                                ExecutorService executor,
                                WorkflowArgsCodec argsCodec,
                                OperationOutputSerializer outputSerializer,
                                Set<String> proxiedMethods) {
        this.target = target;
        this.store = store;
        this.callbackClient = callbackClient;
        this.executor = executor;
        this.argsCodec = argsCodec;
        this.outputSerializer = outputSerializer;
        this.proxiedMethods = proxiedMethods;
        // Match Node: when a callback client is wired, advertise callback strategy in pending
        // payloads so the router knows not to poll.
        this.opMetadata = new OperationMetadata(
                callbackClient != null ? new CallbackResponseStrategy() : new PollingResponseStrategy());
    }

    /** Wrap {@code target} in a JDK dynamic proxy that implements {@code iface}. */
    @SuppressWarnings("unchecked")
    public <T> T asProxy(Class<T> iface) {
        return (T) Proxy.newProxyInstance(
                iface.getClassLoader(),
                new Class<?>[]{iface},
                new ProxyInvocationHandler(this));
    }

    /** Convenience: construct + asProxy in one call. */
    public static <T> T wrap(Class<T> iface,
                             T target,
                             OperationStore store,
                             @Nullable CallbackClient callbackClient,
                             ExecutorService executor,
                             WorkflowArgsCodec argsCodec,
                             OperationOutputSerializer outputSerializer,
                             Set<String> proxiedMethods) {
        return new WorkflowServiceProxy(target, store, callbackClient, executor, argsCodec,
                outputSerializer, proxiedMethods).asProxy(iface);
    }

    /**
     * Re-run a previously-persisted invocation. Called by {@link WorkflowRecovery} at startup
     * for each {@code IN_PROGRESS} row, so a crashed-mid-flight operation eventually reaches
     * {@code COMPLETED} or {@code FAILED} and unblocks any waiting caller / callback.
     */
    public void replay(String cid, Object[] args) {
        Method method = findProxiedMethod(cid);
        if (method == null) return;
        executor.submit(() -> executeAndFinalize(method, args, cid));
    }

    private @Nullable Method findProxiedMethod(String cid) {
        OperationRecord rec = store.findByCid(cid);
        if (rec == null) {
            logger.warn("Replay requested for unknown cid={}", cid);
            return null;
        }
        for (Method m : target.getClass().getMethods()) {
            if (proxiedMethods.contains(m.getName()) && m.getName().equals(rec.method)) {
                return m;
            }
        }
        logger.warn("Replay: no proxied method named '{}' on target {}", rec.method, target.getClass());
        return null;
    }

    Object invokeProxied(Method method, Object[] args) {
        String methodName = method.getName();
        String inputsJson = argsCodec.encode(args == null ? new Object[0] : args);
        String inputsHash = OperationExecutor.computeInputsHash(methodName, inputsJson);

        // Optimistic fast-path: most duplicate calls are spread far enough apart in time that the
        // row already exists when the second arrives. Skip the insert attempt in that case.
        OperationRecord existing = store.findByInputsHash(inputsHash);
        if (existing != null) {
            return resolveExisting(methodName, existing);
        }

        String cid = CorrelationIdGenerator.generate();
        OperationStatus pending = WorkflowOutcomes.pendingFor(methodName, cid, opMetadata);
        String pendingJson = trySerialize(pending, methodName, cid);

        OperationRecord record = new OperationRecord(
                cid, methodName, OperationRecord.Status.IN_PROGRESS, inputsHash, null);

        // Race-safe insert: ON CONFLICT (inputs_hash) DO NOTHING. If we lose to a concurrent
        // identical call, tryInsert returns false and we read the winner's row instead of
        // bubbling a unique-constraint violation up to the caller. Mirrors Node's saveOperation
        // returning (op, inserted) from skeleton/src/workflows/storage.ts.
        boolean inserted = store.tryInsert(record, inputsJson, pendingJson);
        if (!inserted) {
            OperationRecord winner = store.findByInputsHash(inputsHash);
            if (winner != null) {
                logger.debug("Lost insert race for method={}; returning winner's outputs cid={}",
                        methodName, winner.cid);
                return resolveExisting(methodName, winner);
            }
            // Should not happen: the conflict said the row exists, but lookup found nothing.
            // Fall through to pending so the caller has something pollable.
            logger.warn("tryInsert reported conflict but findByInputsHash returned null for method={}", methodName);
            return pending;
        }

        // Won the race — kick off background work.
        Object[] argsCopy = args == null ? new Object[0] : args.clone();
        executor.submit(() -> executeAndFinalize(method, argsCopy, cid));
        return pending;
    }

    private Object resolveExisting(String methodName, OperationRecord existing) {
        if (existing.status == OperationRecord.Status.COMPLETED && existing.result != null) {
            logger.debug("Returning cached completed result for method={}, cid={}", methodName, existing.cid);
            return existing.result;
        }
        // In-flight or failed-without-result: hand back a pending placeholder so the caller can
        // poll. Matches Node: the row is the source of truth for status, the response shape is
        // built from the persisted outputs once finalization completes.
        return WorkflowOutcomes.pendingFor(methodName, existing.cid, opMetadata);
    }

    private void executeAndFinalize(Method method, Object[] args, String cid) {
        OperationStatus outcome;
        OperationRecord.Status dbStatus;
        try {
            // setAccessible: the interface method is public, but the target may live in a
            // package-private class (common in tests, and possible in production); without
            // this, reflection enforces visibility on the declaring class and refuses.
            method.setAccessible(true);
            outcome = (OperationStatus) method.invoke(target, args);
            dbStatus = OperationRecord.Status.COMPLETED;
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            logger.error("Operation failed: method={}, cid={}, error={}", method.getName(), cid, cause.getMessage());
            outcome = WorkflowOutcomes.failureFor(method.getName(), 1, String.valueOf(cause.getMessage()));
            dbStatus = OperationRecord.Status.FAILED;
        } catch (Exception e) {
            logger.error("Reflective invoke failed: method={}, cid={}, error={}", method.getName(), cid, e.getMessage());
            outcome = WorkflowOutcomes.failureFor(method.getName(), 1, String.valueOf(e.getMessage()));
            dbStatus = OperationRecord.Status.FAILED;
        }
        finalize(cid, dbStatus, outcome);
    }

    /**
     * Persist the outcome, then send the callback. The order matters: a crash between persist
     * and callback leaves a {@code COMPLETED}/{@code FAILED} row that a future poll can read,
     * and the next callback attempt comes from a re-finalization on startup, not from a
     * silently-dropped success.
     */
    void finalize(String cid, OperationRecord.Status dbStatus, OperationStatus outcome) {
        String outcomeJson = trySerialize(outcome, "<finalize>", cid);
        try {
            store.updateStatus(cid, dbStatus, outcomeJson);
        } catch (Exception e) {
            logger.error("Failed to persist outcome for cid={} — skipping callback so restart can retry: {}",
                    cid, e.getMessage());
            return;
        }
        if (callbackClient != null) {
            try {
                callbackClient.sendCallback(cid, outcome);
            } catch (Exception e) {
                logger.warn("Callback failed for cid={}: {}", cid, e.getMessage());
            }
        }
    }

    private String trySerialize(OperationStatus status, String method, String cid) {
        try {
            return outputSerializer.serialize(status);
        } catch (Exception e) {
            logger.warn("Failed to serialize outputs for method={}, cid={}: {}", method, cid, e.getMessage());
            return null;
        }
    }

    private static final class ProxyInvocationHandler implements InvocationHandler {
        private final WorkflowServiceProxy proxy;

        ProxyInvocationHandler(WorkflowServiceProxy proxy) {
            this.proxy = proxy;
        }

        @Override
        public Object invoke(Object proxyInstance, Method method, Object[] args) throws Throwable {
            if (proxy.proxiedMethods.contains(method.getName())) {
                return proxy.invokeProxied(method, args);
            }
            // Pass-through for non-proxied methods (e.g. balance queries, proposalStatus).
            try {
                method.setAccessible(true);
                return method.invoke(proxy.target, args);
            } catch (InvocationTargetException e) {
                throw e.getCause() != null ? e.getCause() : e;
            }
        }
    }
}
