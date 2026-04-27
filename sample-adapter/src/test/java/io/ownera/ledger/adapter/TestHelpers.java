package io.ownera.ledger.adapter;

import io.ownera.ledger.adapter.api.model.*;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class TestHelpers {

    private final TestRestTemplate rest;

    public TestHelpers(TestRestTemplate rest) {
        this.rest = rest;
    }

    public static String randomId() {
        return UUID.randomUUID().toString();
    }

    public static String randomFinId() {
        byte[] bytes = new byte[33];
        new java.security.SecureRandom().nextBytes(bytes);
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    // --- API calls ---

    public APICreateAssetResponse createAsset(APICreateAssetRequest request) {
        HttpEntity<APICreateAssetRequest> entity = withIdempotencyKey(request);
        ResponseEntity<APICreateAssetResponse> resp =
                rest.postForEntity("/api/assets/create", entity, APICreateAssetResponse.class);
        assertEquals(200, resp.getStatusCodeValue(), "createAsset failed: " + resp.getBody());
        return resp.getBody();
    }

    public APIReceiptOperation issue(APIIssueAssetsRequest request) {
        HttpEntity<APIIssueAssetsRequest> entity = withIdempotencyKey(request);
        ResponseEntity<APIReceiptOperation> resp =
                rest.postForEntity("/api/assets/issue", entity, APIReceiptOperation.class);
        assertEquals(200, resp.getStatusCodeValue(), "issue failed");
        return resp.getBody();
    }

    public APIReceiptOperation transfer(APITransferAssetRequest request) {
        HttpEntity<APITransferAssetRequest> entity = withIdempotencyKey(request);
        ResponseEntity<APIReceiptOperation> resp =
                rest.postForEntity("/api/assets/transfer", entity, APIReceiptOperation.class);
        assertEquals(200, resp.getStatusCodeValue(), "transfer failed");
        return resp.getBody();
    }

    public APIReceiptOperation redeem(APIRedeemAssetsRequest request) {
        HttpEntity<APIRedeemAssetsRequest> entity = withIdempotencyKey(request);
        ResponseEntity<APIReceiptOperation> resp =
                rest.postForEntity("/api/assets/redeem", entity, APIReceiptOperation.class);
        assertEquals(200, resp.getStatusCodeValue(), "redeem failed");
        return resp.getBody();
    }

    public APIReceiptOperation hold(APIHoldOperationRequest request) {
        HttpEntity<APIHoldOperationRequest> entity = withIdempotencyKey(request);
        ResponseEntity<APIReceiptOperation> resp =
                rest.postForEntity("/api/assets/hold", entity, APIReceiptOperation.class);
        assertEquals(200, resp.getStatusCodeValue(), "hold failed");
        return resp.getBody();
    }

    public APIReceiptOperation release(APIReleaseOperationRequest request) {
        HttpEntity<APIReleaseOperationRequest> entity = withIdempotencyKey(request);
        ResponseEntity<APIReceiptOperation> resp =
                rest.postForEntity("/api/assets/release", entity, APIReceiptOperation.class);
        assertEquals(200, resp.getStatusCodeValue(), "release failed");
        return resp.getBody();
    }

    public APIReceiptOperation rollback(APIRollbackOperationRequest request) {
        HttpEntity<APIRollbackOperationRequest> entity = withIdempotencyKey(request);
        ResponseEntity<APIReceiptOperation> resp =
                rest.postForEntity("/api/assets/rollback", entity, APIReceiptOperation.class);
        assertEquals(200, resp.getStatusCodeValue(), "rollback failed");
        return resp.getBody();
    }

    public APIGetAssetBalanceResponse getBalance(APIAsset asset, String finId) {
        // 0.28: owner is APIAccount with embedded asset
        APIGetAssetBalanceRequest req = new APIGetAssetBalanceRequest()
                .owner(new APIAccount().finId(finId).asset(asset));
        ResponseEntity<APIGetAssetBalanceResponse> resp =
                rest.postForEntity("/api/assets/getBalance", req, APIGetAssetBalanceResponse.class);
        assertEquals(200, resp.getStatusCodeValue());
        return resp.getBody();
    }

    public APIGetReceiptResponse getReceipt(String transactionId) {
        ResponseEntity<APIGetReceiptResponse> resp =
                rest.getForEntity("/api/assets/receipts/" + transactionId, APIGetReceiptResponse.class);
        assertEquals(200, resp.getStatusCodeValue());
        return resp.getBody();
    }

    // --- Assertion helpers ---

    public void assertBalance(String finId, APIAsset asset, String expectedBalance) {
        APIGetAssetBalanceResponse balance = getBalance(asset, finId);
        assertNotNull(balance);
        assertEquals(expectedBalance, balance.getBalance(),
                "Balance mismatch for finId=" + finId);
    }

    public static void assertSuccessReceipt(APIReceiptOperation op) {
        assertNotNull(op);
        assertTrue(op.getIsCompleted(), "Expected completed operation");
        assertNull(op.getError(), "Expected no error, got: " +
                (op.getError() != null ? op.getError().getMessage() : ""));
        assertNotNull(op.getResponse(), "Expected response receipt");
    }

    public static void assertErrorReceipt(APIReceiptOperation op) {
        assertNotNull(op);
        assertTrue(op.getIsCompleted(), "Expected completed operation");
        assertNotNull(op.getError(), "Expected error in receipt");
    }

    public static APIReceipt receipt(APIReceiptOperation op) {
        assertSuccessReceipt(op);
        return op.getResponse();
    }

    // --- Request builders ---

    /**
     * 0.28: APIAsset is flat (resourceId + optional ledgerIdentifier), no more polymorphism.
     */
    public static APIAsset finp2pAsset() {
        return new APIAsset()
                .resourceId("bank101:102:unique-" + randomId());
    }

    /**
     * 0.28: Issue destination is APIAccount with embedded asset.
     */
    public static APIIssueAssetsRequest issueRequest(APIAsset asset, String finId, String quantity) {
        return new APIIssueAssetsRequest()
                .destination(new APIAccount().finId(finId).asset(asset))
                .quantity(quantity);
    }

    /**
     * 0.28: Transfer source/destination are APIAccount (with embedded asset); no top-level asset.
     */
    public static APITransferAssetRequest transferRequest(APIAsset asset,
                                                          String fromFinId, String toFinId, String quantity) {
        return new APITransferAssetRequest()
                .nonce(randomId())
                .source(new APIAccount().finId(fromFinId).asset(asset))
                .destination(new APIAccount().finId(toFinId).asset(asset))
                .quantity(quantity)
                .signature(defaultSignature());
    }

    public static APIHoldOperationRequest holdRequest(APIAsset asset,
                                                      String sourceFinId, String destFinId,
                                                      String quantity, String operationId) {
        return new APIHoldOperationRequest()
                .nonce(randomId())
                .source(new APIAccount().finId(sourceFinId).asset(asset))
                .destination(new APIAccount().finId(destFinId).asset(asset))
                .quantity(quantity)
                .operationId(operationId)
                .signature(defaultSignature());
    }

    public static APIReleaseOperationRequest releaseRequest(APIAsset asset,
                                                            String sourceFinId, String destFinId,
                                                            String quantity, String operationId) {
        return new APIReleaseOperationRequest()
                .source(new APIAccount().finId(sourceFinId).asset(asset))
                .destination(new APIAccount().finId(destFinId).asset(asset))
                .quantity(quantity)
                .operationId(operationId);
    }

    public static APIRollbackOperationRequest rollbackRequest(APIAsset asset,
                                                              String sourceFinId,
                                                              String quantity, String operationId) {
        return new APIRollbackOperationRequest()
                .source(new APIAccount().finId(sourceFinId).asset(asset))
                .quantity(quantity)
                .operationId(operationId);
    }

    /**
     * 0.28: Redeem source is APIAccount with embedded asset.
     */
    public static APIRedeemAssetsRequest redeemRequest(APIAsset asset,
                                                       String sourceFinId, String quantity, String operationId) {
        return new APIRedeemAssetsRequest()
                .nonce(randomId())
                .source(new APIAccount().finId(sourceFinId).asset(asset))
                .quantity(quantity)
                .operationId(operationId)
                .signature(defaultSignature());
    }

    public static APICreateAssetRequest createAssetRequest(APIAsset asset) {
        // 0.28: createAsset takes APIFinp2pAssetBase (not full APIAsset)
        return new APICreateAssetRequest()
                .asset(new APIFinp2pAssetBase().resourceId(asset.getResourceId()));
    }

    private static APISignature defaultSignature() {
        return new APISignature()
                .signature("0000")
                .hashFunc(APIHashFunction.KECCAK_256)
                .template(new APISignatureTemplate(
                        new APIHashListTemplate()
                                .type(APIHashListTemplate.TypeEnum.HASHLIST)
                                .hash("0000")));
    }

    private <T> HttpEntity<T> withIdempotencyKey(T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Idempotency-Key", randomId());
        headers.set("Content-Type", "application/json");
        return new HttpEntity<>(body, headers);
    }
}
