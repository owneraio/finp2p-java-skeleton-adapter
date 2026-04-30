package io.ownera.ledger.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.ownera.ledger.adapter.api.model.*;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end deserialization of a real issue-asset payload captured from production.
 * The router posts hashFunc="" and signature="" for unsigned issuance — the request
 * must deserialize cleanly and downstream Mappers must produce a usable internal model.
 *
 * Anchors the v0.28 surface against the original Hedera-adapter regression
 * (PR #41 fix to APIHashFunction.fromValue) plus all 0.28 schema shape changes.
 */
public class IssueRequestRealPayloadTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static APIIssueAssetsRequest loadPayload() throws Exception {
        try (InputStream in = IssueRequestRealPayloadTest.class
                .getResourceAsStream("/payloads/issue-empty-signature.json")) {
            assertNotNull(in, "payload fixture missing");
            return MAPPER.readValue(in, APIIssueAssetsRequest.class);
        }
    }

    @Test
    void deserializesRealPayloadWithEmptyHashFuncAndSignature() throws Exception {
        APIIssueAssetsRequest req = loadPayload();

        // hashFunc="" → UNSPECIFIED (regression for the original bug)
        assertEquals(APIHashFunction.UNSPECIFIED, req.getSignature().getHashFunc());
        assertEquals("", req.getSignature().getSignature());

        // Top-level fields
        assertEquals("e95c786b43bb020b37f2d6379185fa32", req.getNonce());
        assertEquals("30", req.getQuantity());
        assertEquals("", req.getSettlementRef());
        assertEquals(2, req.getExecutionContext().getInstructionSequenceNumber());

        // Destination account carries asset (0.28: asset embedded in account)
        APIAccount destination = req.getDestination();
        assertEquals("03d337542bc2d9921d3cc6eef2aa41d3017ad39b37a5f9f1910d669d7bc22d3442", destination.getFinId());
        assertEquals("org-a:102:a6aef480-c440-40bd-af22-88a76e0516b9", destination.getAsset().getResourceId());

        // CAIP-19 ledger identifier on the asset
        APILedgerAssetIdentifierTypeCAIP19 caip19 =
                (APILedgerAssetIdentifierTypeCAIP19) destination.getAsset().getLedgerIdentifier().getActualInstance();
        assertEquals(APILedgerAssetIdentifierTypeCAIP19.AssetIdentifierTypeEnum.CAIP_19,
                caip19.getAssetIdentifierType());
        assertEquals("hedera:testnet", caip19.getNetwork());
        assertEquals("HTS", caip19.getStandard());
        assertEquals("0.0.8823890", caip19.getTokenId());

        // EIP712 template
        APIEIP712Template eip712 = (APIEIP712Template) req.getSignature().getTemplate().getActualInstance();
        assertEquals(APIEIP712Template.TypeEnum.EIP712, eip712.getType());
        assertEquals("PrimarySale", eip712.getPrimaryType());
        assertEquals("FinP2P", eip712.getDomain().getName());
        assertEquals(Integer.valueOf(1), eip712.getDomain().getChainId());
        assertEquals("106042132ba1feb263056348760d0fc5e574d2ebc20410d707a968d0c3d69633", eip712.getHash());
        assertNotNull(eip712.getMessage().get("asset"));
        assertEquals(4, eip712.getTypes().getDefinitions().size());
    }

    @Test
    void mapsRealPayloadIntoInternalModel() throws Exception {
        // Run the full mapping chain (Mappers.* used by Controller) — catches any
        // downstream regression that leaves the request technically deserializable
        // but unusable by the service layer.
        APIIssueAssetsRequest req = loadPayload();

        io.ownera.ledger.adapter.service.model.FinIdAccount destination =
                Mappers.finIdAccountFromAPI(req.getDestination());
        assertEquals("03d337542bc2d9921d3cc6eef2aa41d3017ad39b37a5f9f1910d669d7bc22d3442", destination.finId);

        io.ownera.ledger.adapter.service.model.Asset asset = Mappers.assetFromAPI(req.getDestination());
        assertEquals("org-a:102:a6aef480-c440-40bd-af22-88a76e0516b9", asset.assetId);
        assertNotNull(asset.ledgerIdentifier, "ledgerIdentifier must propagate to internal Asset");
        assertEquals("hedera:testnet", asset.ledgerIdentifier.network);
        assertEquals("0.0.8823890", asset.ledgerIdentifier.tokenId);
        assertEquals("HTS", asset.ledgerIdentifier.standard);

        io.ownera.ledger.adapter.service.model.Signature sig = Mappers.fromAPI(req.getSignature());
        assertEquals(io.ownera.ledger.adapter.service.model.HashFunction.UNSPECIFIED, sig.hashFunction);

        io.ownera.ledger.adapter.service.model.ExecutionContext exCtx =
                Mappers.fromAPI(req.getExecutionContext());
        assertEquals(2, exCtx.sequence);
        assertEquals("org-a:106:2426cd2b-b55f-4992-ad8d-e9e14b49f7ea", exCtx.planId);
    }
}
