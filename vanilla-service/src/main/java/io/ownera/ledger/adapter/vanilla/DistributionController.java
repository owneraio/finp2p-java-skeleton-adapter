package io.ownera.ledger.adapter.vanilla;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.ownera.ledger.adapter.service.BusinessException;
import io.ownera.ledger.adapter.service.model.AssetType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;

/**
 * HTTP surface for {@link DistributionService}, mirroring Node's
 * {@code registerDistributionRoutes} ({@code vanilla-service/src/routes.ts}).
 *
 * <ul>
 *   <li>{@code POST /distribution/sync}      — reconcile omnibus DB with on-chain balance.</li>
 *   <li>{@code GET  /distribution/status}    — read-only omnibus vs distributed.</li>
 *   <li>{@code POST /distribution/distribute} — allocate omnibus value to investor.</li>
 *   <li>{@code POST /distribution/reclaim}    — return investor value to undistributed pool.</li>
 *   <li>{@code POST /distribution/flush}      — reclaim every per-investor account to omnibus.</li>
 * </ul>
 *
 * <p>Validation failures return {@code 400}; business errors (e.g. on-chain balance below
 * distributed total, overdraft on reclaim) return {@code 409}; unexpected failures bubble up
 * as {@code 500} through Spring's default handler.
 *
 * <p>This bean is opt-in: it only activates when a {@link DistributionService} bean is
 * present in the application context.
 */
@RestController
@RequestMapping("/distribution")
public class DistributionController {

    private final DistributionService distributionService;

    public DistributionController(DistributionService distributionService) {
        this.distributionService = distributionService;
    }

    @PostMapping("/sync")
    public ResponseEntity<DistributionStatus> sync(@RequestBody AssetRequest body) {
        requireNonBlank(body.assetId, "assetId");
        return ResponseEntity.ok(distributionService.syncOmnibus(body.assetId, body.assetTypeOrDefault()));
    }

    @GetMapping("/status")
    public ResponseEntity<DistributionStatus> status(
            @RequestParam("assetId") String assetId,
            @RequestParam(value = "assetType", required = false) String assetType) {
        requireNonBlank(assetId, "assetId");
        // Bind assetType as String + parse via AssetType.fromWire so we accept the Node-style
        // lowercase form ("finp2p"). Spring's default String→enum converter is case-sensitive
        // and would 400 on lowercase.
        AssetType resolved = assetType != null ? AssetType.fromWire(assetType) : AssetType.FINP2P;
        return ResponseEntity.ok(distributionService.getDistributionStatus(assetId, resolved));
    }

    @PostMapping("/distribute")
    public ResponseEntity<Map<String, String>> distribute(@RequestBody InvestorAmountRequest body) {
        requireNonBlank(body.finId, "finId");
        requireNonBlank(body.assetId, "assetId");
        requireNonBlank(body.amount, "amount");
        distributionService.distribute(body.finId, body.assetId, body.assetTypeOrDefault(), body.amount);
        return ResponseEntity.ok(Collections.singletonMap("status", "ok"));
    }

    @PostMapping("/reclaim")
    public ResponseEntity<Map<String, String>> reclaim(@RequestBody InvestorAmountRequest body) {
        requireNonBlank(body.finId, "finId");
        requireNonBlank(body.assetId, "assetId");
        requireNonBlank(body.amount, "amount");
        distributionService.reclaim(body.finId, body.assetId, body.assetTypeOrDefault(), body.amount);
        return ResponseEntity.ok(Collections.singletonMap("status", "ok"));
    }

    @PostMapping("/flush")
    public ResponseEntity<DistributionStatus> flush(@RequestBody AssetRequest body) {
        requireNonBlank(body.assetId, "assetId");
        return ResponseEntity.ok(distributionService.flushDistributions(body.assetId, body.assetTypeOrDefault()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, String>> onBusiness(BusinessException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Collections.singletonMap("error", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> onBadRequest(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Collections.singletonMap("error", e.getMessage()));
    }

    private static void requireNonBlank(@Nullable String value, String name) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException(name + " is required");
        }
    }

    // ─── Request bodies ─────────────────────────────────────────────────────

    public static final class AssetRequest {
        public final String assetId;
        public final @Nullable AssetType assetType;

        @JsonCreator
        public AssetRequest(@JsonProperty("assetId") String assetId,
                            @JsonProperty("assetType") @Nullable AssetType assetType) {
            this.assetId = assetId;
            this.assetType = assetType;
        }

        AssetType assetTypeOrDefault() {
            return assetType != null ? assetType : AssetType.FINP2P;
        }
    }

    public static final class InvestorAmountRequest {
        public final String finId;
        public final String assetId;
        public final @Nullable AssetType assetType;
        public final String amount;

        @JsonCreator
        public InvestorAmountRequest(@JsonProperty("finId") String finId,
                                     @JsonProperty("assetId") String assetId,
                                     @JsonProperty("assetType") @Nullable AssetType assetType,
                                     @JsonProperty("amount") String amount) {
            this.finId = finId;
            this.assetId = assetId;
            this.assetType = assetType;
            this.amount = amount;
        }

        AssetType assetTypeOrDefault() {
            return assetType != null ? assetType : AssetType.FINP2P;
        }
    }
}
