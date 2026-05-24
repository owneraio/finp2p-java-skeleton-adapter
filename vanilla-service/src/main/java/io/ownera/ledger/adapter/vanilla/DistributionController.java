package io.ownera.ledger.adapter.vanilla;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.ownera.ledger.adapter.service.BusinessException;
import io.ownera.ledger.adapter.service.model.AssetType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(DistributionController.class);

    private final DistributionService distributionService;

    public DistributionController(DistributionService distributionService) {
        this.distributionService = distributionService;
    }

    @PostMapping("/sync")
    public ResponseEntity<DistributionStatus> sync(@RequestBody AssetRequest body) {
        requireNonBlank(body.assetId, "assetId");
        AssetType assetType = body.assetTypeOrDefault();
        logger.info("Distribution sync: assetId={}, assetType={}", body.assetId, assetType);
        return ResponseEntity.ok(distributionService.syncOmnibus(body.assetId, assetType));
    }

    @GetMapping("/status")
    public ResponseEntity<DistributionStatus> status(
            @RequestParam("assetId") String assetId,
            @RequestParam(value = "assetType", required = false) String assetType) {
        requireNonBlank(assetId, "assetId");
        // Bind assetType as String + parse locally so the lowercase wire form is accepted on
        // the distribution surface only. Spring's default String→enum converter is
        // case-sensitive and would 400 on "finp2p"; AssetType itself stays case-strict so the
        // workflow proxy's inputs_hash isn't disturbed.
        AssetType resolved = assetType != null ? AssetTypeWire.parse(assetType) : AssetType.FINP2P;
        return ResponseEntity.ok(distributionService.getDistributionStatus(assetId, resolved));
    }

    @PostMapping("/distribute")
    public ResponseEntity<Map<String, String>> distribute(@RequestBody InvestorAmountRequest body) {
        requireNonBlank(body.finId, "finId");
        requireNonBlank(body.assetId, "assetId");
        requireNonBlank(body.amount, "amount");
        AssetType assetType = body.assetTypeOrDefault();
        logger.info("Distribution distribute: assetId={}, assetType={}, finId={}, amount={}",
                body.assetId, assetType, body.finId, body.amount);
        distributionService.distribute(body.finId, body.assetId, assetType, body.amount);
        return ResponseEntity.ok(Collections.singletonMap("status", "ok"));
    }

    @PostMapping("/reclaim")
    public ResponseEntity<Map<String, String>> reclaim(@RequestBody InvestorAmountRequest body) {
        requireNonBlank(body.finId, "finId");
        requireNonBlank(body.assetId, "assetId");
        requireNonBlank(body.amount, "amount");
        AssetType assetType = body.assetTypeOrDefault();
        logger.info("Distribution reclaim: assetId={}, assetType={}, finId={}, amount={}",
                body.assetId, assetType, body.finId, body.amount);
        distributionService.reclaim(body.finId, body.assetId, assetType, body.amount);
        return ResponseEntity.ok(Collections.singletonMap("status", "ok"));
    }

    @PostMapping("/flush")
    public ResponseEntity<DistributionStatus> flush(@RequestBody AssetRequest body) {
        requireNonBlank(body.assetId, "assetId");
        AssetType assetType = body.assetTypeOrDefault();
        logger.info("Distribution flush: assetId={}, assetType={}", body.assetId, assetType);
        return ResponseEntity.ok(distributionService.flushDistributions(body.assetId, assetType));
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
                            @JsonProperty("assetType")
                            @JsonDeserialize(using = AssetTypeWire.Deserializer.class)
                            @Nullable AssetType assetType) {
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
                                     @JsonProperty("assetType")
                                     @JsonDeserialize(using = AssetTypeWire.Deserializer.class)
                                     @Nullable AssetType assetType,
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
