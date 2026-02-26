package io.ownera.ledger.adapter;

import io.ownera.ledger.adapter.api.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;

import static io.ownera.ledger.adapter.TestHelpers.*;

public abstract class AbstractEscrowOperationsTest {

    @Autowired
    private TestRestTemplate restTemplate;
    protected TestHelpers api;

    @BeforeEach
    void setup() {
        api = new TestHelpers(restTemplate);
    }

    @Test
    void shouldHoldAndReleaseFunds() {
        String buyer = randomFinId();
        String seller = randomFinId();
        APIFinp2pAsset asset = finp2pAsset();
        String operationId = randomId();

        api.createAsset(createAssetRequest(asset));
        api.issue(issueRequest(asset, buyer, "1000"));
        api.assertBalance(buyer, asset, "1000");
        api.assertBalance(seller, asset, "0");

        APIReceiptOperation holdOp = api.hold(holdRequest(asset, buyer, seller, "600", operationId));
        APIReceipt holdReceipt = receipt(holdOp);
        assert holdReceipt.getOperationType() == APIOperationType.HOLD;
        api.assertBalance(buyer, asset, "400");

        APIReceiptOperation releaseOp = api.release(releaseRequest(asset, buyer, seller, "600", operationId));
        APIReceipt releaseReceipt = receipt(releaseOp);
        assert releaseReceipt.getOperationType() == APIOperationType.RELEASE;

        api.assertBalance(buyer, asset, "400");
        api.assertBalance(seller, asset, "600");
    }

    @Test
    void shouldHoldAndRedeemTokens() {
        String investor = randomFinId();
        APIFinp2pAsset asset = finp2pAsset();
        String operationId = randomId();

        api.createAsset(createAssetRequest(asset));
        api.issue(issueRequest(asset, investor, "1000"));
        api.assertBalance(investor, asset, "1000");

        api.hold(holdRequest(asset, investor, investor, "500", operationId));
        api.assertBalance(investor, asset, "500");

        APIReceiptOperation redeemOp = api.redeem(redeemRequest(asset, investor, "500", operationId));
        APIReceipt redeemReceipt = receipt(redeemOp);
        assert redeemReceipt.getOperationType() == APIOperationType.REDEEM;

        api.assertBalance(investor, asset, "0");
    }

    @Test
    void shouldRollbackHeldFunds() {
        String buyer = randomFinId();
        String seller = randomFinId();
        APIFinp2pAsset asset = finp2pAsset();
        String operationId = randomId();

        api.createAsset(createAssetRequest(asset));
        api.issue(issueRequest(asset, buyer, "1000"));

        api.hold(holdRequest(asset, buyer, seller, "600", operationId));
        api.assertBalance(buyer, asset, "400");

        api.rollback(rollbackRequest(asset, buyer, "600", operationId));
        api.assertBalance(buyer, asset, "1000");
    }

    @Test
    void shouldPartiallyReleaseFunds() {
        String buyer = randomFinId();
        String seller = randomFinId();
        APIFinp2pAsset asset = finp2pAsset();
        String operationId = randomId();

        api.createAsset(createAssetRequest(asset));
        api.issue(issueRequest(asset, buyer, "1000"));

        api.hold(holdRequest(asset, buyer, seller, "600", operationId));
        api.assertBalance(buyer, asset, "400");

        api.release(releaseRequest(asset, buyer, seller, "400", operationId));
        api.assertBalance(seller, asset, "400");
    }
}
