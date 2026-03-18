package io.ownera.ledger.adapter.service.workflow;

import io.ownera.ledger.adapter.service.CommonService;
import io.ownera.ledger.adapter.service.model.OperationStatus;
import io.ownera.ledger.adapter.service.model.PendingReceiptStatus;
import io.ownera.ledger.adapter.service.model.OperationMetadata;
import io.ownera.ledger.adapter.service.model.PollingResponseStrategy;
import io.ownera.ledger.adapter.service.model.ReceiptOperation;

public class OperationTrackingCommonService implements CommonService {

    private final CommonService delegate;
    private final OperationStore store;

    public OperationTrackingCommonService(CommonService delegate, OperationStore store) {
        this.delegate = delegate;
        this.store = store;
    }

    @Override
    public ReceiptOperation getReceipt(String id) {
        return delegate.getReceipt(id);
    }

    @Override
    public OperationStatus operationStatus(String cid) {
        OperationRecord record = store.findByCid(cid);
        if (record != null) {
            switch (record.status) {
                case SUCCEEDED:
                    // Delegate to underlying service which can deserialize the receipt
                    return delegate.operationStatus(cid);
                case IN_PROGRESS:
                    return new PendingReceiptStatus(cid, new OperationMetadata(new PollingResponseStrategy()));
                case FAILED:
                    return delegate.operationStatus(cid);
            }
        }
        return delegate.operationStatus(cid);
    }
}
