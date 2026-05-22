package io.ownera.ledger.adapter.vanilla.web;

import io.ownera.ledger.adapter.service.model.AssetType;
import io.ownera.ledger.adapter.vanilla.DistributionStatus;
import io.ownera.ledger.adapter.vanilla.LedgerDetails;
import io.ownera.ledger.adapter.vanilla.LedgerStorage;
import io.ownera.ledger.adapter.vanilla.OmnibusDelegate;
import io.ownera.ledger.adapter.vanilla.VanillaDistributionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end HTTP tests for the {@code /distribution/*} routes via {@link TestRestTemplate}.
 * Verifies each route's status code, response shape, and side effects on the underlying
 * {@link LedgerStorage}.
 */
@SpringBootTest(
        classes = DistributionTestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DistributionControllerTest {

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private LedgerStorage storage;

    @Autowired
    private OmnibusDelegate omnibusDelegate;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(omnibusDelegate);
    }

    private void seedOmnibus(String assetId, String balance) {
        storage.ensureAccount(VanillaDistributionService.OMNIBUS_FIN_ID, assetId);
        storage.credit(VanillaDistributionService.OMNIBUS_FIN_ID, balance, assetId,
                new LedgerDetails("seed-" + System.nanoTime(), null, "issue", null, null));
    }

    private static <T> HttpEntity<T> json(T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    @Test
    void getStatusReturnsDistributionStatusForAsset() {
        String assetId = "asset-status-" + System.nanoTime();
        seedOmnibus(assetId, "1000");

        ResponseEntity<DistributionStatus> resp = rest.getForEntity(
                "/distribution/status?assetId=" + assetId, DistributionStatus.class);

        assertEquals(200, resp.getStatusCodeValue());
        DistributionStatus s = resp.getBody();
        assertNotNull(s);
        assertEquals(assetId, s.assetId);
        assertEquals(AssetType.FINP2P, s.assetType);
        assertEquals("1000", s.omnibusBalance);
        assertEquals("0",    s.distributedBalance);
        assertEquals("1000", s.availableBalance);
    }

    @Test
    void getStatusRejectsMissingAssetId() {
        ResponseEntity<Map> resp = rest.getForEntity(
                "/distribution/status", Map.class);
        // Spring's @RequestParam without `required=false` returns 400 itself; either way the
        // route must NOT 200.
        assertNotEquals(200, resp.getStatusCodeValue());
    }

    @Test
    void postDistributeMovesValueFromOmnibusToInvestor() {
        String assetId = "asset-dist-" + System.nanoTime();
        String inv = "inv-" + System.nanoTime();
        seedOmnibus(assetId, "500");

        Map<String, Object> body = new HashMap<>();
        body.put("finId", inv);
        body.put("assetId", assetId);
        body.put("amount", "150");

        ResponseEntity<Map> resp = rest.postForEntity("/distribution/distribute", json(body), Map.class);
        assertEquals(200, resp.getStatusCodeValue());
        assertEquals("ok", resp.getBody().get("status"));

        assertEquals("350", storage.getBalance(VanillaDistributionService.OMNIBUS_FIN_ID, assetId).balance);
        assertEquals("150", storage.getBalance(inv, assetId).balance);
    }

    @Test
    void postDistributeRejectsMissingFields() {
        Map<String, Object> body = new HashMap<>();
        body.put("assetId", "asset-X");
        // finId + amount missing
        ResponseEntity<Map> resp = rest.postForEntity("/distribution/distribute", json(body), Map.class);
        assertEquals(400, resp.getStatusCodeValue());
        assertNotNull(resp.getBody());
        assertNotNull(resp.getBody().get("error"));
    }

    @Test
    void postReclaimMovesValueFromInvestorBackToOmnibus() {
        String assetId = "asset-reclaim-" + System.nanoTime();
        String inv = "inv-" + System.nanoTime();
        seedOmnibus(assetId, "500");
        // Pre-distribute so reclaim has something to pull back.
        VanillaDistributionService svc = new VanillaDistributionService(storage, omnibusDelegate);
        svc.distribute(inv, assetId, AssetType.FINP2P, "200");

        Map<String, Object> body = new HashMap<>();
        body.put("finId", inv);
        body.put("assetId", assetId);
        body.put("amount", "120");

        ResponseEntity<Map> resp = rest.postForEntity("/distribution/reclaim", json(body), Map.class);
        assertEquals(200, resp.getStatusCodeValue());

        assertEquals("80",  storage.getBalance(inv, assetId).balance);
        assertEquals("420", storage.getBalance(VanillaDistributionService.OMNIBUS_FIN_ID, assetId).balance);
    }

    @Test
    void postSyncReconcilesAgainstDelegate() {
        String assetId = "asset-sync-" + System.nanoTime();
        String inv = "inv-" + System.nanoTime();
        seedOmnibus(assetId, "500");
        new VanillaDistributionService(storage, omnibusDelegate)
                .distribute(inv, assetId, AssetType.FINP2P, "200");

        Mockito.when(omnibusDelegate.getOmnibusBalance(assetId, AssetType.FINP2P)).thenReturn("700");

        Map<String, Object> body = new HashMap<>();
        body.put("assetId", assetId);
        ResponseEntity<DistributionStatus> resp = rest.postForEntity(
                "/distribution/sync", json(body), DistributionStatus.class);

        assertEquals(200, resp.getStatusCodeValue());
        DistributionStatus s = resp.getBody();
        assertNotNull(s);
        assertEquals("700", s.omnibusBalance);
        assertEquals("200", s.distributedBalance);
        assertEquals("500", s.availableBalance);
    }

    @Test
    void postSyncReturns409WhenOnChainLessThanDistributed() {
        String assetId = "asset-sync-bad-" + System.nanoTime();
        String inv = "inv-" + System.nanoTime();
        seedOmnibus(assetId, "500");
        new VanillaDistributionService(storage, omnibusDelegate)
                .distribute(inv, assetId, AssetType.FINP2P, "400");

        // On-chain reports 200 — less than the 400 already distributed.
        Mockito.when(omnibusDelegate.getOmnibusBalance(assetId, AssetType.FINP2P)).thenReturn("200");

        Map<String, Object> body = new HashMap<>();
        body.put("assetId", assetId);
        ResponseEntity<Map> resp = rest.postForEntity("/distribution/sync", json(body), Map.class);

        assertEquals(409, resp.getStatusCodeValue());
        assertNotNull(resp.getBody().get("error"));
        assertTrue(resp.getBody().get("error").toString().contains("200"));
    }

    @Test
    void postFlushReclaimsEveryInvestorBack() {
        String assetId = "asset-flush-" + System.nanoTime();
        String inv1 = "inv-1-" + System.nanoTime();
        String inv2 = "inv-2-" + System.nanoTime();
        seedOmnibus(assetId, "1000");
        VanillaDistributionService svc = new VanillaDistributionService(storage, omnibusDelegate);
        svc.distribute(inv1, assetId, AssetType.FINP2P, "300");
        svc.distribute(inv2, assetId, AssetType.FINP2P, "200");

        Map<String, Object> body = new HashMap<>();
        body.put("assetId", assetId);
        ResponseEntity<DistributionStatus> resp = rest.postForEntity(
                "/distribution/flush", json(body), DistributionStatus.class);

        assertEquals(200, resp.getStatusCodeValue());
        DistributionStatus s = resp.getBody();
        assertEquals("1000", s.omnibusBalance);
        assertEquals("0",    s.distributedBalance);
        assertEquals("1000", s.availableBalance);
        assertEquals("0", storage.getBalance(inv1, assetId).balance);
        assertEquals("0", storage.getBalance(inv2, assetId).balance);
    }
}
