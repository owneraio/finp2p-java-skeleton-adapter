package io.ownera.ledger.adapter;

import io.ownera.ledger.adapter.api.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;

import static io.ownera.ledger.adapter.TestHelpers.*;

public abstract class AbstractInsufficientBalanceTest {

    @Autowired
    private TestRestTemplate restTemplate;
    protected TestHelpers api;

    @BeforeEach
    void setup() {
        api = new TestHelpers(restTemplate);
    }

    @Test
    void shouldFailTransferExceedingBalance() {
        String seller = randomFinId();
        String buyer = randomFinId();
        APIFinp2pAsset asset = finp2pAsset();

        api.createAsset(createAssetRequest(asset));
        api.issue(issueRequest(asset, seller, "100"));

        APIReceiptOperation op = api.transfer(transferRequest(asset, seller, buyer, "150"));
        assertErrorReceipt(op);

        api.assertBalance(seller, asset, "100");
        api.assertBalance(buyer, asset, "0");
    }

    @Test
    void shouldFailTransferExactBalancePlusOne() {
        String seller = randomFinId();
        String buyer = randomFinId();
        APIFinp2pAsset asset = finp2pAsset();

        api.createAsset(createAssetRequest(asset));
        api.issue(issueRequest(asset, seller, "500"));

        APIReceiptOperation op = api.transfer(transferRequest(asset, seller, buyer, "501"));
        assertErrorReceipt(op);

        api.assertBalance(seller, asset, "500");
        api.assertBalance(buyer, asset, "0");
    }

    @Test
    void shouldFailRedeemExceedingBalance() {
        String investor = randomFinId();
        APIFinp2pAsset asset = finp2pAsset();

        api.createAsset(createAssetRequest(asset));
        api.issue(issueRequest(asset, investor, "100"));

        APIReceiptOperation op = api.redeem(redeemRequest(asset, investor, "150", null));
        assertErrorReceipt(op);

        api.assertBalance(investor, asset, "100");
    }

    @Test
    void shouldFailRedeemFromZeroBalance() {
        String investor = randomFinId();
        APIFinp2pAsset asset = finp2pAsset();

        api.createAsset(createAssetRequest(asset));

        APIReceiptOperation op = api.redeem(redeemRequest(asset, investor, "10", null));
        assertErrorReceipt(op);

        api.assertBalance(investor, asset, "0");
    }

    @Test
    void shouldFailHoldExceedingBalance() {
        String buyer = randomFinId();
        String seller = randomFinId();
        APIFinp2pAsset asset = finp2pAsset();
        String opId = randomId();

        api.createAsset(createAssetRequest(asset));
        api.issue(issueRequest(asset, buyer, "1000"));

        APIReceiptOperation op = api.hold(holdRequest(asset, buyer, seller, "1500", opId));
        assertErrorReceipt(op);

        api.assertBalance(buyer, asset, "1000");
    }

    @Test
    void shouldFailHoldFromZeroBalance() {
        String buyer = randomFinId();
        String seller = randomFinId();
        APIFinp2pAsset asset = finp2pAsset();
        String opId = randomId();

        api.createAsset(createAssetRequest(asset));

        APIReceiptOperation op = api.hold(holdRequest(asset, buyer, seller, "100", opId));
        assertErrorReceipt(op);

        api.assertBalance(buyer, asset, "0");
    }

    @Test
    void shouldFailSecondHoldAfterPartialConsumption() {
        String buyer = randomFinId();
        String seller = randomFinId();
        APIFinp2pAsset asset = finp2pAsset();

        api.createAsset(createAssetRequest(asset));
        api.issue(issueRequest(asset, buyer, "1000"));

        String opId1 = randomId();
        APIReceiptOperation first = api.hold(holdRequest(asset, buyer, seller, "800", opId1));
        assertSuccessReceipt(first);

        String opId2 = randomId();
        APIReceiptOperation second = api.hold(holdRequest(asset, buyer, seller, "300", opId2));
        assertErrorReceipt(second);
    }

    @Test
    void shouldFailWhenCombinedOperationsExceedBalance() {
        String owner = randomFinId();
        String recipient1 = randomFinId();
        String recipient2 = randomFinId();
        APIFinp2pAsset asset = finp2pAsset();

        api.createAsset(createAssetRequest(asset));
        api.issue(issueRequest(asset, owner, "100"));

        APIReceiptOperation first = api.transfer(transferRequest(asset, owner, recipient1, "60"));
        assertSuccessReceipt(first);

        api.assertBalance(owner, asset, "40");
        api.assertBalance(recipient1, asset, "60");

        APIReceiptOperation second = api.transfer(transferRequest(asset, owner, recipient2, "50"));
        assertErrorReceipt(second);

        api.assertBalance(owner, asset, "40");
        api.assertBalance(recipient2, asset, "0");
    }
}
