package io.ownera.ledger.adapter.sample.collateral;

import io.ownera.finp2p.FinP2PSDK;
import io.ownera.finp2p.common.HashAlgorithm;
import io.ownera.finp2p.finapi.ApiException;
import io.ownera.finp2p.finapi.model.IntentType;
import io.ownera.finp2p.oss.GraphqlException;
import io.ownera.finp2p.oss.models.User;
import io.ownera.ledger.adapter.service.PaymentService;
import io.ownera.ledger.adapter.service.TokenService;
import io.ownera.ledger.adapter.service.TokenServiceException;
import io.ownera.ledger.adapter.service.model.*;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Executors;

import static io.ownera.ledger.adapter.sample.collateral.Mapper.successfulDeposit;
import static io.ownera.ledger.adapter.sample.collateral.Mapper.toAPI;


public class CollateralService implements PaymentService {

    private final static Logger logger = LoggerFactory.getLogger(CollateralService.class);

    private final FinP2PSDK finP2PSDK;
    private final TokenService tokenService;

    public CollateralService(FinP2PSDK finP2PSDK, TokenService tokenService) {
        this.finP2PSDK = finP2PSDK;
        this.tokenService = tokenService;
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
        logger.info("Processing custom asset deposit");
        List<Collateral> collaterals = CollateralsHolder.parseFrom(details);

        Optional<User> investor;
        try {
            investor = finP2PSDK.getUserByFinId(owner.finId);
        } catch (GraphqlException e) {
            logger.warn("Failed to retrieve investor info: {}", e.getMessage());
            throw new TokenServiceException("Failed to retrieve investor info: " + e.getMessage());
        }
        if (investor.isEmpty()) {
            logger.warn("Investor not found: {}", owner.finId);
            throw new TokenServiceException("Investor not found: " + owner.finId);
        }
        String correlationId = UUID.randomUUID().toString();
        String investorId = investor.get().getId();
        String investorOrgId = investor.get().getOrganizationId();
        String currency = "USD";

        Executors.newCachedThreadPool().submit(() -> {
            try {
                createBasket(correlationId, owner.finId, investorId, investorOrgId, currency);
            } catch (Exception e) {
                logger.error("Failed to create collateral basket: {}", e.getMessage());
            }
        });

        return new PendingDepositOperation(correlationId,
                new OperationMetadata(new CallbackResponseStrategy())
        );
    }

    private void createBasket(String cid, String investorFinId, String investorId, String investorOrgId, String currency) throws Exception {

        String tokenId = null;
        String signingKey = null;
        io.ownera.finp2p.finapi.model.AssetIdentifier identifier = new io.ownera.finp2p.finapi.model.AssetIdentifier()
                .assetIdentifierType(io.ownera.finp2p.finapi.model.AssetIdentifierType.CUSTOM)
                .assetIdentifierValue("collateral-basket");
        String name = "Collateral Basket";
        String type = "COLLATERAL_BASKET";
        String assetId = finP2PSDK.createAsset(
                name, type, investorId, currency, tokenId, HashAlgorithm.KECCAK256,
                Set.of(IntentType.LOAN_INTENT), signingKey, identifier
        );

        long now = System.currentTimeMillis() / 1000;
        finP2PSDK.createCertificate(assetId, "Repo", new Certificate()
                .addInfoItem("Basket Name", "Basket 1")
                .addInfoItem("Basket Notional Value", "100")
                .addInfoItem("Denomination Currency", currency)
                .marshal(), now, now + 31536000);

        finP2PSDK.shareResource(assetId, investorOrgId);

        String operationId = UUID.randomUUID().toString();
        logger.info("Minting asset {} for FinID {}", assetId, investorFinId);
        issueAssets(UUID.randomUUID().toString(), operationId, new FinIdAccount(investorFinId), "100", new Asset(assetId, AssetType.FINP2P));


        finP2PSDK.sendCallbackResponse(cid, successfulDeposit(cid, operationId));
    }

    private void issueAssets(String idempotencyKey, String operationId, FinIdAccount to, String quantity, Asset asset) throws ApiException {
        ReceiptOperation op = tokenService.issue(idempotencyKey, asset, to, quantity, null);
        if (op instanceof FailedReceiptStatus) {
            FailedReceiptStatus failedOp = (FailedReceiptStatus) op;
            throw new TokenServiceException("Failed to issue asset: " + failedOp.details.message);
        }
        SuccessReceiptStatus receiptStatus = (SuccessReceiptStatus) op;

        finP2PSDK.importTransactions(Collections.singletonList(
                toAPI(receiptStatus.receipt)
        ));
    }

}
