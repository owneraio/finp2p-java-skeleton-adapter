package io.ownera.ledger.adapter;

import io.ownera.ledger.adapter.api.model.*;
import io.ownera.ledger.adapter.service.model.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Receipt responses (issue/transfer/redeem/hold/release/rollback) embed the asset in
 * destination/source. The asset's ledgerIdentifier is a polymorphic union; the FinP2P
 * node rejects responses where it serializes as null with code 999
 * "unknown discriminator value: ".
 *
 * Pin the wire shape across all paths that return APIReceipt.
 */
public class MappersReceiptResponseTest {

    private static Asset assetWithLedger() {
        return new Asset(
                "org-a:102:2c882096-3a49-453a-a0a5-9a09cc1a182c",
                AssetType.FINP2P,
                new LedgerAssetIdentifier("hedera:testnet", "0.0.8823890", "HTS"));
    }

    private static Asset assetWithoutLedger() {
        // Adapters that don't track CAIP-19 still need the discriminator emitted.
        return new Asset("org-a:102:bare", AssetType.FINP2P);
    }

    private static Receipt issueReceipt(Asset asset) {
        FinIdAccount destination = new FinIdAccount("024e5be4e07c92f492b3c9680fd249a2b8004209ef8bb167421182a1bc0e94f264");
        return new Receipt(
                "0.0.7427478@1777525274.073000790",
                OperationType.ISSUE,
                asset,
                null,
                destination.destination(),
                "30",
                new TransactionDetails("0.0.7427478@1777525274.073000790", null),
                new TradeDetails(new ExecutionContext("org-a:106:plan", 2)),
                null,
                1777525286354L);
    }

    private static APILedgerAssetIdentifierTypeCAIP19 caip19FromReceipt(APIReceipt apiReceipt) {
        return (APILedgerAssetIdentifierTypeCAIP19)
                apiReceipt.getDestination().getAsset().getLedgerIdentifier().getActualInstance();
    }

    @Test
    void issueReceiptResponseIncludesCaip19Discriminator() {
        ReceiptOperation rcptOp = new SuccessReceiptStatus(issueReceipt(assetWithLedger()));
        APIReceiptOperation api = Mappers.toAPI(rcptOp);
        APILedgerAssetIdentifierTypeCAIP19 caip19 = caip19FromReceipt(api.getResponse());

        assertEquals(APILedgerAssetIdentifierTypeCAIP19.AssetIdentifierTypeEnum.CAIP_19,
                caip19.getAssetIdentifierType(), "discriminator must be CAIP-19");
        assertEquals("hedera:testnet", caip19.getNetwork());
        assertEquals("0.0.8823890", caip19.getTokenId());
        assertEquals("HTS", caip19.getStandard());
    }

    @Test
    void receiptWithAssetMissingLedgerIdentifierStillEmitsDiscriminator() {
        // Older adapter code paths may produce Asset without ledgerIdentifier; the wire
        // shape must still carry the discriminator (empty strings) — anything but null.
        ReceiptOperation rcptOp = new SuccessReceiptStatus(issueReceipt(assetWithoutLedger()));
        APIReceiptOperation api = Mappers.toAPI(rcptOp);
        APILedgerAssetIdentifierTypeCAIP19 caip19 = caip19FromReceipt(api.getResponse());

        assertNotNull(caip19, "ledgerIdentifier must not be null");
        assertEquals(APILedgerAssetIdentifierTypeCAIP19.AssetIdentifierTypeEnum.CAIP_19,
                caip19.getAssetIdentifierType());
        assertEquals("", caip19.getNetwork());
        assertEquals("", caip19.getTokenId());
        assertEquals("", caip19.getStandard());
    }

    @Test
    void getReceiptResponseAlsoIncludesCaip19() {
        // GET /api/assets/receipts/{id} uses toAPIGetReceiptResponse — same code path.
        ReceiptOperation rcptOp = new SuccessReceiptStatus(issueReceipt(assetWithLedger()));
        APIGetReceiptResponse resp = Mappers.toAPIGetReceiptResponse(rcptOp);
        APILedgerAssetIdentifierTypeCAIP19 caip19 = caip19FromReceipt(resp.getResponse());
        assertEquals(APILedgerAssetIdentifierTypeCAIP19.AssetIdentifierTypeEnum.CAIP_19,
                caip19.getAssetIdentifierType());
    }
}
