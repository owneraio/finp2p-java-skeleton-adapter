package io.ownera.ledger.adapter.sample.collateral;

import io.ownera.finp2p.FinP2PSDK;
import io.ownera.finp2p.common.HashAlgorithm;
import io.ownera.finp2p.finapi.ApiException;
import io.ownera.finp2p.oss.GraphqlException;
import io.ownera.finp2p.oss.models.User;
import io.ownera.ledger.adapter.service.PaymentService;
import io.ownera.ledger.adapter.service.TokenServiceException;
import io.ownera.ledger.adapter.service.model.*;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;

public class CollateralService implements PaymentService {

    private FinP2PSDK finP2PSDK;

    public CollateralService(FinP2PSDK finP2PSDK) {
        this.finP2PSDK = finP2PSDK;
    }

    @Override
    public DepositOperation getDepositInstruction(String idempotencyKey, Source owner, Destination destination,
                                                  DepositAsset asset, @Nullable String amount, @Nullable Object details, @Nullable String nonce, @Nullable Signature signature) {
        if (asset.assetType == DepositAssetType.CUSTOM) {
            return startCollateralBasketCreation(owner, details);
        }

        String operationId = UUID.randomUUID().toString();
        return new SuccessfulDepositOperation(
                new DepositInstruction(
                        destination,
                        "Deposit for collateral",
                        Collections.emptyList(),
                        operationId, null)
        );
    }

    @Override
    public ReceiptOperation payout(String idempotencyKey, Source source, @Nullable Destination destination, Asset asset,
                                   String quantity, @Nullable String description, @Nullable String nonce, @Nullable Signature signature) {
        return new FailedReceiptStatus(new ErrorDetails(1, "Not supported"));
    }


    private DepositOperation startCollateralBasketCreation(Source owner, Object details) {
        List<Collateral> collaterals = CollateralsHolder.parseFrom(details);

        Optional<User> investor;
        try {
            investor = finP2PSDK.getUserByFinId(owner.finId);
        } catch (GraphqlException e) {
            throw new TokenServiceException("Failed to retrieve investor info: " + e.getMessage());
        }
        if (investor.isEmpty()) {
            throw new TokenServiceException("Investor not found: " + owner.finId);
        }
        String investorId = investor.get().getId();
        String investorOrgId = investor.get().getOrganizationId();
        String correlationId = UUID.randomUUID().toString();
        Executors.newCachedThreadPool().submit(() -> {
            // create asset
            // share asset
            // issue asset to investor

        });

        return new PendingDepositOperation(correlationId,
                new OperationMetadata(new CallbackResponseStrategy())
        );
    }

    private void createAsset(String name, String type, String issuerId, String denomination,
                             @javax.annotation.Nullable String tokenId) throws InterruptedException, ApiException {
        // String name, String type, String issuerId, String paymentAssetCode, String tokenId, HashAlgorithm hashAlgorithm
        String assetId = finP2PSDK.createAsset(name, type, issuerId, denomination, tokenId, HashAlgorithm.KECCAK256);

    }
}
