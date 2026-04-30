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
        APIAsset asset = finp2pAsset();

        APICreateAssetResponse createResp = api.createAsset(createAssetRequest(asset));
        // Lock in CAIP-19 discriminator on the response: FinP2P node rejects identifiers
        // missing assetIdentifierType ("unknown discriminator value").
        APILedgerAssetIdentifierTypeCAIP19 caip19 =
                (APILedgerAssetIdentifierTypeCAIP19) createResp.getResponse().getLedgerAssetInfo()
                        .getLedgerIdentifier().getActualInstance();
        assert caip19.getAssetIdentifierType() == APILedgerAssetIdentifierTypeCAIP19.AssetIdentifierTypeEnum.CAIP_19
                : "ledgerIdentifier missing CAIP-19 discriminator";
        assert caip19.getTokenId() != null && !caip19.getTokenId().isEmpty()
                : "ledgerIdentifier.tokenId is required";
        assert caip19.getNetwork() != null : "ledgerIdentifier.network must be non-null";
        assert caip19.getStandard() != null : "ledgerIdentifier.standard must be non-null";

        APIReceiptOperation issueOp = api.issue(issueRequest(asset, issuer, "1000"));
        APIReceipt issueReceipt = receipt(issueOp);
        assert issueReceipt.getQuantity().equals("1000");
        assert issueReceipt.getOperationType() == APIOperationType.ISSUE;
        // Receipt's destination.asset.ledgerIdentifier must carry the CAIP-19 discriminator —
        // FinP2P node rejects null with code 999 "unknown discriminator value".
        APILedgerAssetIdentifierTypeCAIP19 issueRcptCaip19 =
                (APILedgerAssetIdentifierTypeCAIP19) issueReceipt.getDestination().getAsset()
                        .getLedgerIdentifier().getActualInstance();
        assert issueRcptCaip19.getAssetIdentifierType() == APILedgerAssetIdentifierTypeCAIP19.AssetIdentifierTypeEnum.CAIP_19
                : "issue receipt destination.asset.ledgerIdentifier must have CAIP-19 discriminator";

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
        APIAsset asset = finp2pAsset();

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
        APIAsset asset = finp2pAsset();

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
