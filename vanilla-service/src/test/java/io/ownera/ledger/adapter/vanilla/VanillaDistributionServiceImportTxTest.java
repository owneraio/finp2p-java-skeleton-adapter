package io.ownera.ledger.adapter.vanilla;

import io.ownera.finp2p.FinP2PSDK;
import io.ownera.finp2p.OperationalSDK;
import io.ownera.finp2p.opapi.ApiException;
import io.ownera.finp2p.opapi.model.LedgerAssetIdentifierTypeCAIP19;
import io.ownera.finp2p.opapi.model.Transaction;
import io.ownera.finp2p.oss.GraphqlException;
import io.ownera.finp2p.oss.models.OSSLedgerAssetInfo;
import io.ownera.finp2p.oss.models.OSSLedgerIdentifier;
import io.ownera.finp2p.oss.models.OssAsset;
import io.ownera.ledger.adapter.service.model.AssetType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Parity tests for the FinP2P router round-trip on {@link VanillaDistributionService#distribute}
 * / {@link VanillaDistributionService#reclaim}. Mirrors Node's
 * {@code vanilla-service/src/service.ts} behaviour: every off-router omnibus ↔ investor move
 * also posts an {@code importTransactions(...)} to the operational API so the router's
 * who-holds-what view stays consistent.
 *
 * <p>The class focuses on the wiring contract (which SDK gets called, with what payload, and
 * under what conditions it stays silent). Storage-level assertions belong in
 * {@link VanillaDistributionServiceTest} — those still run unmocked against the real Postgres.
 */
class VanillaDistributionServiceImportTxTest {

    private LedgerStorage storage;
    private OmnibusDelegate omnibusDelegate;
    private FinP2PSDK finP2PSDK;
    private OperationalSDK operationalSDK;
    private VanillaDistributionService service;

    @BeforeEach
    void setup() {
        storage = new LedgerStorage(VanillaPostgresHolder.CTX, VanillaPostgresHolder.SCHEMA);
        omnibusDelegate = Mockito.mock(OmnibusDelegate.class);
        finP2PSDK = Mockito.mock(FinP2PSDK.class);
        operationalSDK = Mockito.mock(OperationalSDK.class);
        service = new VanillaDistributionService(storage, omnibusDelegate, finP2PSDK, operationalSDK);
    }

    @Test
    void distributeCallsImportTransactionsWithIssueAndDestinationFinId() throws Exception {
        String assetId = "asset-dist-import-" + System.nanoTime();
        String inv = "inv-dist-import-" + System.nanoTime();
        seedOmnibus(assetId, "1000");
        Mockito.when(finP2PSDK.getAsset(assetId)).thenReturn(Optional.of(ossAssetWithCAIP19Identifier(
                "eip155:1", "0xtoken", "ERC-20")));

        service.distribute(inv, assetId, AssetType.FINP2P, "300");

        ArgumentCaptor<List<Transaction>> captor = txListCaptor();
        Mockito.verify(operationalSDK).importTransactions(captor.capture());

        List<Transaction> sent = captor.getValue();
        assertEquals(1, sent.size(), "exactly one transaction per distribute");
        Transaction tx = sent.get(0);
        assertEquals(Transaction.OperationTypeEnum.ISSUE, tx.getOperationType(),
                "distribute is an issue from the router's view");
        assertEquals("300", tx.getQuantity());
        assertEquals(assetId, tx.getDestination().getFinp2pAccount().getAsset().getId());
        assertEquals(inv, tx.getDestination().getFinp2pAccount().getAccount().getFinId(),
                "destination side carries the investor finId on ISSUE");
        // Source must be absent — that's what differentiates ISSUE from TRANSFER on the wire.
        assertEquals(null, tx.getSource());

        LedgerAssetIdentifierTypeCAIP19 ledgerId = tx.getDestination().getFinp2pAccount().getAsset().getLedgerIdentifier();
        assertEquals("eip155:1", ledgerId.getNetwork());
        assertEquals("0xtoken", ledgerId.getTokenId());
        assertEquals("ERC-20", ledgerId.getStandard());
        assertEquals(LedgerAssetIdentifierTypeCAIP19.AssetIdentifierTypeEnum.CAIP_19,
                ledgerId.getAssetIdentifierType());
    }

    @Test
    void reclaimCallsImportTransactionsWithRedeemAndSourceFinId() throws Exception {
        String assetId = "asset-rec-import-" + System.nanoTime();
        String inv = "inv-rec-import-" + System.nanoTime();
        seedOmnibus(assetId, "1000");
        Mockito.when(finP2PSDK.getAsset(assetId)).thenReturn(Optional.of(ossAssetWithCAIP19Identifier(
                "eip155:1", "0xtoken", "ERC-20")));
        // Stage a distribution first so the reclaim has something to pull back. The distribute
        // call also triggers an import; reset the mock so the reclaim assertion sees only the
        // reclaim's own import.
        service.distribute(inv, assetId, AssetType.FINP2P, "300");
        Mockito.clearInvocations(operationalSDK);

        service.reclaim(inv, assetId, AssetType.FINP2P, "100");

        ArgumentCaptor<List<Transaction>> captor = txListCaptor();
        Mockito.verify(operationalSDK).importTransactions(captor.capture());
        Transaction tx = captor.getValue().get(0);
        assertEquals(Transaction.OperationTypeEnum.REDEEM, tx.getOperationType(),
                "reclaim is a redeem from the router's view");
        assertEquals("100", tx.getQuantity());
        // Source side carries the investor finId on REDEEM; destination is unset.
        assertEquals(inv, tx.getSource().getFinp2pAccount().getAccount().getFinId());
        assertEquals(null, tx.getDestination());
    }

    @Test
    void importIsSkippedWhenAssetIsUnknownToOss() throws Exception {
        // getAsset returns Optional.empty — router doesn't know the asset, so we can't form a
        // valid Transaction. Warn-log and skip; the local DB move is unaffected.
        String assetId = "asset-unknown-" + System.nanoTime();
        seedOmnibus(assetId, "1000");
        Mockito.when(finP2PSDK.getAsset(assetId)).thenReturn(Optional.empty());

        service.distribute("inv-x", assetId, AssetType.FINP2P, "10");

        Mockito.verify(operationalSDK, Mockito.never()).importTransactions(Mockito.anyList());
        // And the local move still happened — the skip only affects the router round-trip.
        assertEquals("10", storage.getBalance("inv-x", assetId).balance);
    }

    @Test
    void importIsSkippedWhenAssetHasNoLedgerIdentifier() throws Exception {
        // Asset profile only, no ledger binding yet — router has no place to record the import,
        // so the helper warn-logs and skips.
        String assetId = "asset-no-ledgerid-" + System.nanoTime();
        seedOmnibus(assetId, "1000");
        OssAsset profileOnly = new OssAsset();
        profileOnly.setId(assetId);
        // No ledgerAssetInfo set ⇒ toLedgerIdentifier returns null.
        Mockito.when(finP2PSDK.getAsset(assetId)).thenReturn(Optional.of(profileOnly));

        service.distribute("inv-x", assetId, AssetType.FINP2P, "10");

        Mockito.verify(operationalSDK, Mockito.never()).importTransactions(Mockito.anyList());
    }

    @Test
    void routerFailureDoesNotAbortTheLocalMove() throws Exception {
        // The DB move has already committed by the time importTransactions runs; throwing here
        // would force the caller to roll back, which the framework can't do. Verify the
        // exception is swallowed and the local balances stay updated.
        String assetId = "asset-router-fail-" + System.nanoTime();
        String inv = "inv-router-fail-" + System.nanoTime();
        seedOmnibus(assetId, "1000");
        Mockito.when(finP2PSDK.getAsset(assetId)).thenReturn(Optional.of(ossAssetWithCAIP19Identifier(
                "eip155:1", "0xtoken", "ERC-20")));
        Mockito.doThrow(new ApiException("router down"))
                .when(operationalSDK).importTransactions(Mockito.anyList());

        service.distribute(inv, assetId, AssetType.FINP2P, "300");

        assertEquals("300", storage.getBalance(inv, assetId).balance,
                "router failure must not roll back the local DB move");
        assertEquals("700", storage.getBalance(VanillaDistributionService.OMNIBUS_FIN_ID, assetId).balance);
    }

    @Test
    void importIsSkippedWhenSdksAreNotWired() {
        // Back-compat path: an adapter that wires the 2-arg constructor opts out of router
        // round-trips entirely. The 4-arg constructor with both nulls must behave identically.
        VanillaDistributionService backCompat = new VanillaDistributionService(
                storage, omnibusDelegate, null, null);
        String assetId = "asset-no-sdk-" + System.nanoTime();
        String inv = "inv-no-sdk-" + System.nanoTime();
        seedOmnibus(assetId, "1000");

        backCompat.distribute(inv, assetId, AssetType.FINP2P, "300");

        // Even with the SDKs available on this test instance's `operationalSDK` mock, the
        // back-compat service shouldn't touch them — it was built with nulls.
        Mockito.verifyNoInteractions(operationalSDK);
        Mockito.verifyNoInteractions(finP2PSDK);
        assertEquals("300", storage.getBalance(inv, assetId).balance);
    }

    @Test
    void importIsSkippedWhenOnlyOneSdkIsWired() throws Exception {
        // Partial wiring (FinP2PSDK supplied, OperationalSDK null) is a misconfiguration — the
        // service can't post imports without the operational client. Skip silently rather than
        // half-failing; logs flag the situation.
        VanillaDistributionService halfWired = new VanillaDistributionService(
                storage, omnibusDelegate, finP2PSDK, null);
        String assetId = "asset-half-" + System.nanoTime();
        seedOmnibus(assetId, "1000");

        halfWired.distribute("inv-half", assetId, AssetType.FINP2P, "10");

        // FinP2PSDK.getAsset must never be called when there's nowhere to post the result.
        Mockito.verify(finP2PSDK, Mockito.never()).getAsset(Mockito.anyString());
    }

    // ── helpers ────────────────────────────────────────────────────────

    private void seedOmnibus(String assetId, String amount) {
        storage.ensureAccount(VanillaDistributionService.OMNIBUS_FIN_ID, assetId);
        storage.credit(VanillaDistributionService.OMNIBUS_FIN_ID, amount, assetId,
                new LedgerDetails("seed-" + System.nanoTime(), null, "issue", null, null));
    }

    /**
     * Build a real {@link OssAsset} populated with a CAIP-19 ledger identifier. The service's
     * {@code toLedgerIdentifier} helper reads the network/tokenId/standard fields via
     * reflection on the OSS-model classes, so the test must construct actual instances rather
     * than Mockito stubs — deep-stubbing would skip the reflection target entirely.
     */
    private static OssAsset ossAssetWithCAIP19Identifier(String network, String tokenId, String standard) {
        OSSLedgerIdentifier id = new OSSLedgerIdentifier();
        id.setNetwork(network);
        id.setTokenId(tokenId);
        id.setStandard(standard);
        OSSLedgerAssetInfo info = new OSSLedgerAssetInfo();
        info.setLedgerIdentifier(id);
        OssAsset asset = new OssAsset();
        asset.setLedgerAssetInfo(info);
        return asset;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ArgumentCaptor<List<Transaction>> txListCaptor() {
        return ArgumentCaptor.forClass((Class) List.class);
    }
}
