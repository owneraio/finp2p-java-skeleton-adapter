package io.ownera.ledger.adapter;

import io.ownera.ledger.adapter.api.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;

import static io.ownera.ledger.adapter.TestHelpers.*;

public abstract class AbstractTokenLifecycleTest {

    @Autowired
    private TestRestTemplate restTemplate;
    protected TestHelpers api;

    @BeforeEach
    void setup() {
        api = new TestHelpers(restTemplate);
    }

    @Test
    void shouldIssueTransferAndVerifyBalances() {
        String issuer = randomFinId();
        String buyer = randomFinId();
        APIFinp2pAsset asset = finp2pAsset();

        api.createAsset(createAssetRequest(asset));

        APIReceiptOperation issueOp = api.issue(issueRequest(asset, issuer, "1000"));
        APIReceipt issueReceipt = receipt(issueOp);
        assert issueReceipt.getQuantity().equals("1000");
        assert issueReceipt.getOperationType() == APIOperationType.ISSUE;

        api.assertBalance(issuer, asset, "1000");

        APIReceiptOperation transferOp = api.transfer(transferRequest(asset, issuer, buyer, "600"));
        APIReceipt transferReceipt = receipt(transferOp);
        assert transferReceipt.getQuantity().equals("600");
        assert transferReceipt.getOperationType() == APIOperationType.TRANSFER;

        api.assertBalance(issuer, asset, "400");
        api.assertBalance(buyer, asset, "600");
    }

    @Test
    void shouldIssueAndRedeemTokens() {
        String issuer = randomFinId();
        APIFinp2pAsset asset = finp2pAsset();

        api.createAsset(createAssetRequest(asset));

        api.issue(issueRequest(asset, issuer, "500"));
        api.assertBalance(issuer, asset, "500");

        APIReceiptOperation redeemOp = api.redeem(redeemRequest(asset, issuer, "200", null));
        APIReceipt redeemReceipt = receipt(redeemOp);
        assert redeemReceipt.getQuantity().equals("200");
        assert redeemReceipt.getOperationType() == APIOperationType.REDEEM;

        api.assertBalance(issuer, asset, "300");
    }

    @Test
    void shouldHandleMultipleTransfers() {
        String alice = randomFinId();
        String bob = randomFinId();
        String charlie = randomFinId();
        APIFinp2pAsset asset = finp2pAsset();

        api.createAsset(createAssetRequest(asset));
        api.issue(issueRequest(asset, alice, "1000"));

        api.transfer(transferRequest(asset, alice, bob, "300"));
        api.assertBalance(alice, asset, "700");
        api.assertBalance(bob, asset, "300");

        api.transfer(transferRequest(asset, alice, charlie, "200"));
        api.assertBalance(alice, asset, "500");
        api.assertBalance(charlie, asset, "200");

        api.transfer(transferRequest(asset, bob, charlie, "100"));
        api.assertBalance(bob, asset, "200");
        api.assertBalance(charlie, asset, "300");
    }
}
