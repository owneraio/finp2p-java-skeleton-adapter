package io.ownera.ledger.adapter;

import io.ownera.ledger.adapter.api.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;

import static io.ownera.ledger.adapter.TestHelpers.*;

public abstract class AbstractBusinessLogicTest {

    @Autowired
    private TestRestTemplate restTemplate;
    protected TestHelpers api;

    @BeforeEach
    void setup() {
        api = new TestHelpers(restTemplate);
    }

    @Test
    void shouldFailRollbackOnNonExistentHold() {
        String buyer = randomFinId();
        APIFinp2pAsset asset = finp2pAsset();

        api.createAsset(createAssetRequest(asset));
        api.issue(issueRequest(asset, buyer, "1000"));

        APIReceiptOperation op = api.rollback(rollbackRequest(asset, buyer, "100", "non-existent-op"));
        assertErrorReceipt(op);
    }

    @Test
    void shouldFailReleaseOnNonExistentOperationId() {
        String buyer = randomFinId();
        String seller = randomFinId();
        APIFinp2pAsset asset = finp2pAsset();

        api.createAsset(createAssetRequest(asset));
        api.issue(issueRequest(asset, buyer, "1000"));

        APIReceiptOperation op = api.release(releaseRequest(asset, buyer, seller, "100", "non-existent-op"));
        assertErrorReceipt(op);
    }

    @Test
    void shouldFailDoubleRelease() {
        String buyer = randomFinId();
        String seller = randomFinId();
        APIFinp2pAsset asset = finp2pAsset();
        String operationId = randomId();

        api.createAsset(createAssetRequest(asset));
        api.issue(issueRequest(asset, buyer, "1000"));

        api.hold(holdRequest(asset, buyer, seller, "500", operationId));

        APIReceiptOperation first = api.release(releaseRequest(asset, buyer, seller, "500", operationId));
        assertSuccessReceipt(first);

        APIReceiptOperation second = api.release(releaseRequest(asset, buyer, seller, "500", operationId));
        assertErrorReceipt(second);
    }

    @Test
    void shouldFailDoubleRollback() {
        String buyer = randomFinId();
        String seller = randomFinId();
        APIFinp2pAsset asset = finp2pAsset();
        String operationId = randomId();

        api.createAsset(createAssetRequest(asset));
        api.issue(issueRequest(asset, buyer, "1000"));

        api.hold(holdRequest(asset, buyer, seller, "500", operationId));

        APIReceiptOperation first = api.rollback(rollbackRequest(asset, buyer, "500", operationId));
        assertSuccessReceipt(first);

        APIReceiptOperation second = api.rollback(rollbackRequest(asset, buyer, "500", operationId));
        assertErrorReceipt(second);
    }

    @Test
    void shouldFailRollbackAfterRelease() {
        String buyer = randomFinId();
        String seller = randomFinId();
        APIFinp2pAsset asset = finp2pAsset();
        String operationId = randomId();

        api.createAsset(createAssetRequest(asset));
        api.issue(issueRequest(asset, buyer, "1000"));

        api.hold(holdRequest(asset, buyer, seller, "500", operationId));
        api.release(releaseRequest(asset, buyer, seller, "500", operationId));

        APIReceiptOperation op = api.rollback(rollbackRequest(asset, buyer, "500", operationId));
        assertErrorReceipt(op);
    }

    @Test
    void shouldRetrieveReceiptByTransactionId() {
        String finId = randomFinId();
        APIFinp2pAsset asset = finp2pAsset();

        api.createAsset(createAssetRequest(asset));
        APIReceiptOperation issueOp = api.issue(issueRequest(asset, finId, "100"));
        APIReceipt issueReceipt = receipt(issueOp);

        String txId = issueReceipt.getTransactionDetails().getTransactionId();
        APIGetReceiptResponse retrieved = api.getReceipt(txId);

        assert retrieved.getIsCompleted();
        assert retrieved.getResponse().getId().equals(issueReceipt.getId());
    }
}
