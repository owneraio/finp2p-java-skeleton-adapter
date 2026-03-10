package io.ownera.ledger.adapter.sample.inmemory;

import io.ownera.ledger.adapter.service.model.OperationStatus;
import io.ownera.ledger.adapter.service.workflow.OperationRecord;
import io.ownera.ledger.adapter.service.workflow.OperationStore;

import javax.annotation.Nullable;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryOperationStore implements OperationStore {

    private final ConcurrentHashMap<String, OperationRecord> byCid = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, OperationRecord> byInputsHash = new ConcurrentHashMap<>();

    @Override
    @Nullable
    public OperationRecord findByInputsHash(String inputsHash) {
        return byInputsHash.get(inputsHash);
    }

    @Override
    public void save(OperationRecord record) {
        byCid.put(record.cid, record);
        byInputsHash.put(record.inputsHash, record);
    }

    @Override
    public void updateStatus(String cid, OperationRecord.Status status, @Nullable OperationStatus result) {
        OperationRecord existing = byCid.get(cid);
        if (existing != null) {
            OperationRecord updated = existing.withStatus(status, result);
            byCid.put(cid, updated);
            byInputsHash.put(existing.inputsHash, updated);
        }
    }

    @Override
    @Nullable
    public OperationRecord findByCid(String cid) {
        return byCid.get(cid);
    }
}
