package io.ownera.ledger.adapter.service.workflow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Startup hook that replays {@code IN_PROGRESS} operations through a {@link WorkflowServiceProxy}.
 *
 * <p>Mirrors the Node skeleton's recovery step in {@code createServiceProxy}: after migrations
 * land, the proxy iterates proxied methods, fetches each method's pending operations from
 * storage, and re-invokes them via {@code executeAndFinalize}. A crash mid-flight no longer
 * leaves an operation stuck in {@code IN_PROGRESS} — it will be re-driven to {@code COMPLETED}
 * or {@code FAILED} after restart.
 *
 * <p>Wiring: the adapter calls {@link #replayAll()} once during application startup after the
 * data source / schema are ready. In Spring this can be an
 * {@code ApplicationReadyEvent} listener.
 */
public class WorkflowRecovery {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowRecovery.class);

    private final OperationStore store;
    private final WorkflowServiceProxy proxy;
    private final List<String> proxiedMethods;
    private final WorkflowArgsCodec argsCodec;

    public WorkflowRecovery(OperationStore store,
                            WorkflowServiceProxy proxy,
                            List<String> proxiedMethods,
                            WorkflowArgsCodec argsCodec) {
        this.store = store;
        this.proxy = proxy;
        this.proxiedMethods = proxiedMethods;
        this.argsCodec = argsCodec;
    }

    public void replayAll() {
        for (String method : proxiedMethods) {
            List<OperationStore.PendingOperation> pending = store.findPending(method);
            if (pending.isEmpty()) continue;
            logger.info("Replaying {} pending operation(s) for method={}", pending.size(), method);
            for (OperationStore.PendingOperation op : pending) {
                if (op.inputsJson == null) {
                    logger.warn("Skipping replay for cid={} method={}: no persisted inputs (was the row written by a pre-PR1 build?)",
                            op.cid, method);
                    continue;
                }
                try {
                    Object[] args = argsCodec.decode(op.inputsJson);
                    proxy.replay(op.cid, args);
                } catch (Exception e) {
                    logger.error("Failed to decode inputs for cid={} method={}: {}", op.cid, method, e.getMessage(), e);
                }
            }
        }
    }
}
