package io.ownera.ledger.adapter;

import io.ownera.ledger.adapter.api.model.*;
import io.ownera.ledger.adapter.service.model.*;
import io.reactivex.annotations.Nullable;

import static java.util.stream.Collectors.toList;

public class Mappers {

    public static Asset fromAPI(APIAsset asset) {
        if (asset.getActualInstance() instanceof APIFinp2pAsset) {
            APIFinp2pAsset finp2pAsset = (APIFinp2pAsset) asset.getActualInstance();
            return new Asset(finp2pAsset.getResourceId(), AssetType.FINP2P);

        } else if (asset.getActualInstance() instanceof APIFiatAsset) {
            APIFiatAsset finp2pAsset = (APIFiatAsset) asset.getActualInstance();
            return new Asset(finp2pAsset.getCode(), AssetType.FIAT);

        } else if (asset.getActualInstance() instanceof APICryptocurrencyAsset) {
            APICryptocurrencyAsset cryptoAsset = (APICryptocurrencyAsset) asset.getActualInstance();
            return new Asset(cryptoAsset.getCode(), AssetType.CRYPTOCURRENCY);

        } else {
            throw new MappingException("Unsupported asset type: " + asset.getActualInstance().getClass().getName());
        }
    }

    public static Asset fromAPI(APIFinp2pAsset asset) {
        return new Asset(asset.getResourceId(), AssetType.FINP2P);
    }

    public static AssetBind fromAPI(@Nullable APILedgerTokenId tokenId) {
        if (tokenId == null) {
            return null;
        }
        return new AssetBind(new TokenIdentifier(tokenId.getTokenId()));
    }

    public static AssetDenomination fromAPI(@Nullable APIAssetDenomination denomination) {
        if (denomination == null) {
            return null;
        }
        return new AssetDenomination(fromAPI(denomination.getType()), denomination.getCode());
    }

    public static AssetDenominationType fromAPI(APIAssetDenominationType type) {
        switch (type) {
            case FIAT:
                return AssetDenominationType.FIAT;
            case CRYPTOCURRENCY:
                return AssetDenominationType.CRYPTOCURRENCY;
            default:
                throw new MappingException("Unsupported asset denomination type: " + type);
        }
    }

    public static AssetIdentifier fromAPI(@Nullable APIAssetIdentifier identifier) {
        if (identifier == null) {
            return null;
        }
        return new AssetIdentifier(fromAPI(identifier.getAssetIdentifierType()), identifier.getAssetIdentifierValue());
    }

    public static AssetIdentifierType fromAPI(APIAssetIdentifierType type) {
        switch (type) {
            case ISIN:
                return AssetIdentifierType.ISIN;
            case CUSIP:
                return AssetIdentifierType.CUSIP;
            case SEDOL:
                return AssetIdentifierType.SEDOL;
            case DTI:
                return AssetIdentifierType.DTI;
            case CMU:
                return AssetIdentifierType.CMU;
            case FIGI:
                return AssetIdentifierType.FIGI;
            case CUSTOM:
                return AssetIdentifierType.CUSTOM;
            default:
                throw new MappingException("Unsupported asset identifier type: " + type);
        }
    }


    public static DepositAsset fromAPI(APIDepositAsset asset) {
        if (asset.getActualInstance() instanceof APIFinp2pAsset) {
            APIFinp2pAsset finp2pAsset = (APIFinp2pAsset) asset.getActualInstance();
            return new DepositAsset(finp2pAsset.getResourceId(), DepositAssetType.FINP2P);

        } else if (asset.getActualInstance() instanceof APIFiatAsset) {
            APIFiatAsset finp2pAsset = (APIFiatAsset) asset.getActualInstance();
            return new DepositAsset(finp2pAsset.getCode(), DepositAssetType.FIAT);

        } else if (asset.getActualInstance() instanceof APICryptocurrencyAsset) {
            APICryptocurrencyAsset cryptoAsset = (APICryptocurrencyAsset) asset.getActualInstance();
            return new DepositAsset(cryptoAsset.getCode(), DepositAssetType.CRYPTOCURRENCY);

        } else if (asset.getActualInstance() instanceof APICustomAsset) {
            return new DepositAsset("", DepositAssetType.CUSTOM);

        } else {
            throw new MappingException("Unsupported asset type: " + asset.getActualInstance().getClass().getName());
        }
    }

    public static FinIdAccount fromAPI(APIFinIdAccount account) {
        return new FinIdAccount(account.getFinId());
    }

    public static Source fromAPI(APISource account) {
        return new Source(account.getFinId(), new FinIdAccount(account.getFinId()));
    }

    public static Destination fromAPI(@Nullable APIDestination account) {
        if (account == null) {
            return null;
        }
        return new Destination(account.getFinId(), fromAPI(account.getAccount()));
    }

    public static DestinationAccount fromAPI(APIDestinationAccount account) {
        if (account.getActualInstance() instanceof APIFinIdAccount) {
            APIFinIdAccount finIdAccount = (APIFinIdAccount) account.getActualInstance();
            return fromAPI(finIdAccount);

        } else if (account.getActualInstance() instanceof APICryptoWalletAccount) {
            APICryptoWalletAccount cryptoWalletAccount = (APICryptoWalletAccount) account.getActualInstance();
            return new CryptocurrencyWallet(cryptoWalletAccount.getAddress());
        } else if (account.getActualInstance() instanceof APIFiatAccount) {
            APIFiatAccount fiatAccount = (APIFiatAccount) account.getActualInstance();
            return new IbanIdentifier(fiatAccount.getCode());
        } else {
            throw new MappingException("Unsupported destination account type: " + account.getActualInstance().getClass().getName());
        }
    }

    public static Destination destinationFromAPI(APIFinIdAccount account) {
        return new Destination(account.getFinId(), new FinIdAccount(account.getFinId()));
    }

    public static ExecutionContext fromAPI(@Nullable APIExecutionContext ctx) {
        if (ctx == null) {
            return null;
        }
        return new ExecutionContext(ctx.getExecutionPlanId(), ctx.getInstructionSequenceNumber());
    }

    public static APIExecutionPlanApprovalOperation toAPI(PlanApprovalStatus status) {
        if (status instanceof PendingPlan) {
            PendingPlan pending = (PendingPlan) status;
            return new APIExecutionPlanApprovalOperation()
                    .cid(pending.correlationId)
                    .isCompleted(false);

        } else if (status instanceof RejectedPlan) {
            RejectedPlan rejected = (RejectedPlan) status;
            return new APIExecutionPlanApprovalOperation()
                    .cid("")
                    .isCompleted(true)
                    .approval(new APIPlanApprovalResponseApproval(
                                    new APIPlanRejected()
                                            .status(APIPlanRejected.StatusEnum.REJECTED)
                                            .failure(new APIPlanRejectedFailure(
                                                            new APIValidationFailure()
                                                                    .failureType(APIValidationFailure.FailureTypeEnum.VALIDATION_FAILURE)
                                                                    .code(rejected.details.code)
                                                                    .message(rejected.details.message)
                                                    )
                                            )
                            )
                    );
        } else if (status instanceof ApprovedPlan) {
            return new APIExecutionPlanApprovalOperation()
                    .cid("")
                    .isCompleted(true)
                    .approval(new APIPlanApprovalResponseApproval(
                            new APIPlanApproved()
                                    .status(APIPlanApproved.StatusEnum.APPROVED)
                    ));
        } else {
            throw new MappingException("Unsupported plan approval status: " + status.getClass().getName());
        }
    }

    public static APIApproveExecutionPlanResponse toAPIResponse(PlanApprovalStatus status) {
        if (status instanceof PendingPlan) {
            PendingPlan pending = (PendingPlan) status;
            return new APIApproveExecutionPlanResponse()
                    .cid(pending.correlationId)
                    .isCompleted(false);

        } else if (status instanceof RejectedPlan) {
            RejectedPlan rejected = (RejectedPlan) status;
            return new APIApproveExecutionPlanResponse()
                    .cid("")
                    .isCompleted(true)
                    .approval(new APIPlanApprovalResponseApproval(
                                    new APIPlanRejected()
                                            .status(APIPlanRejected.StatusEnum.REJECTED)
                                            .failure(new APIPlanRejectedFailure(
                                                            new APIValidationFailure()
                                                                    .failureType(APIValidationFailure.FailureTypeEnum.VALIDATION_FAILURE)
                                                                    .code(rejected.details.code)
                                                                    .message(rejected.details.message)
                                                    )
                                            )
                            )
                    );
        } else if (status instanceof ApprovedPlan) {
            return new APIApproveExecutionPlanResponse()
                    .cid("")
                    .isCompleted(true)
                    .approval(new APIPlanApprovalResponseApproval(
                            new APIPlanApproved()
                                    .status(APIPlanApproved.StatusEnum.APPROVED)
                    ));
        } else {
            throw new MappingException("Unsupported plan approval status: " + status.getClass().getName());
        }
    }

    public static APICreateAssetOperation toAPI(AssetCreationStatus status) {
        APICreateAssetOperation operation = new APICreateAssetOperation();
        if (status instanceof PendingAssetCreation) {
            PendingAssetCreation pending = (PendingAssetCreation) status;
            operation.isCompleted(false);
            operation.cid(pending.correlationId);

        } else if (status instanceof FailedAssetCreation) {
            FailedAssetCreation failed = (FailedAssetCreation) status;
            operation.isCompleted(true);
            operation.cid("");
            operation.error(new APICreateAssetOperationErrorInformation()
                    .message(failed.details.message)
                    .code(failed.details.code));

        } else if (status instanceof SuccessfulAssetCreation) {
            SuccessfulAssetCreation success = (SuccessfulAssetCreation) status;
            operation.isCompleted(true);
            operation.cid("");
            operation.response(new APIAssetCreateResponse()
                    .ledgerAssetInfo(new APILedgerAssetInfo()
                            .ledgerTokenId(new APILedgerTokenId()
                                    .tokenId(success.result.tokenId)
                            )
                    ));

        }
        return operation;
    }

    public static APICreateAssetResponse toAPIResponse(AssetCreationStatus status) {
        APICreateAssetResponse response = new APICreateAssetResponse();
        if (status instanceof PendingAssetCreation) {
            PendingAssetCreation pending = (PendingAssetCreation) status;
            response.isCompleted(false);
            response.cid(pending.correlationId);

        } else if (status instanceof FailedAssetCreation) {
            FailedAssetCreation failed = (FailedAssetCreation) status;
            response.isCompleted(true);
            response.setCid("");
            response.error(new APICreateAssetOperationErrorInformation()
                    .message(failed.details.message)
                    .code(failed.details.code));

        } else if (status instanceof SuccessfulAssetCreation) {
            SuccessfulAssetCreation success = (SuccessfulAssetCreation) status;
            response.setIsCompleted(true);
            response.setCid("");
            response.response(new APIAssetCreateResponse()
                    .ledgerAssetInfo(new APILedgerAssetInfo()
                            .ledgerTokenId(new APILedgerTokenId()
                                    .tokenId(success.result.tokenId)
                            )
                    ));

        }
        return response;
    }

    public static APICreateAssetResponse failedAssetOperation(int code, String message) {
        APICreateAssetResponse response = new APICreateAssetResponse();
        response.setIsCompleted(true);
        response.setCid("");
        response.error(new APICreateAssetOperationErrorInformation()
                .message(message)
                .code(code));
        return response;
    }

    public static APIDepositOperation toAPI(DepositOperation status) {
        APIDepositOperation operation = new APIDepositOperation();
        if (status instanceof PendingDepositOperation) {
            PendingDepositOperation pending = (PendingDepositOperation) status;
            operation.isCompleted(false);
            operation.cid(pending.correlationId);

        } else if (status instanceof FailedDepositOperation) {
            FailedDepositOperation failed = (FailedDepositOperation) status;
            operation.isCompleted(true);
            operation.error(new APICreateAssetOperationErrorInformation()
                    .message(failed.details.message)
                    .code(failed.details.code));

        } else if (status instanceof SuccessfulDepositOperation) {
            SuccessfulDepositOperation success = (SuccessfulDepositOperation) status;
            DepositInstruction instr = success.depositInstruction;
            operation.response(new APIDepositInstruction()
                    .account(toAPI(instr.destination))
                    .description(instr.description)
                    .paymentOptions(instr.paymentOptions.stream().map(Mappers::toAPI).collect(toList()))
                    .details(instr.details)
                    .operationId(instr.operationId)
            );
        }
        return operation;
    }

    public static APIDepositInstructionResponse toAPIResponse(DepositOperation status) {
        APIDepositInstructionResponse response = new APIDepositInstructionResponse();
        if (status instanceof PendingDepositOperation) {
            PendingDepositOperation pending = (PendingDepositOperation) status;
            response.isCompleted(false);
            response.cid(pending.correlationId);

        } else if (status instanceof FailedDepositOperation) {
            FailedDepositOperation failed = (FailedDepositOperation) status;
            response.isCompleted(true);
            response.cid("");
            response.error(new APICreateAssetOperationErrorInformation()
                    .message(failed.details.message)
                    .code(failed.details.code));

        } else if (status instanceof SuccessfulDepositOperation) {
            SuccessfulDepositOperation success = (SuccessfulDepositOperation) status;
            DepositInstruction instr = success.depositInstruction;
            response.isCompleted(true);
            response.cid("");
            response.response(new APIDepositInstruction()
                    .account(toAPI(instr.destination))
                    .description(instr.description)
                    .paymentOptions(instr.paymentOptions.stream().map(Mappers::toAPI).collect(toList()))
                    .details(instr.details)
                    .operationId(instr.operationId)
            );
        }
        return response;
    }

    private static APIPaymentMethod toAPI(PaymentMethod method) {
        return new APIPaymentMethod()
                .description(method.description)
                .currency(method.currency)
                .methodInstruction(toAPI(method.methodInstruction));
    }

    private static APIPaymentMethodMethodInstruction toAPI(@Nullable PaymentMethodInstruction instruction) {
        if (instruction == null) {
            return null;
        }
        if (instruction instanceof WireTransfer) {
            WireTransfer wt = (WireTransfer) instruction;
            return new APIPaymentMethodMethodInstruction(
                    new APIWireTransfer()
                            .type(APIWireTransfer.TypeEnum.WIRE_TRANSFER)
            );

        } else if (instruction instanceof WireTransferUsa) {
            WireTransferUsa wtu = (WireTransferUsa) instruction;
            return new APIPaymentMethodMethodInstruction(
                    new APIWireTransferUSA()
                            .type(APIWireTransferUSA.TypeEnum.WIRE_TRANSFER_USA)
            );

        } else if (instruction instanceof CryptoTransfer) {
            CryptoTransfer ct = (CryptoTransfer) instruction;
            return new APIPaymentMethodMethodInstruction(
                    new APICryptoTransfer()
                            .type(APICryptoTransfer.TypeEnum.CRYPTO_TRANSFER)
            );

        } else if (instruction instanceof PaymentInstruction) {
            PaymentInstruction pi = (PaymentInstruction) instruction;
            return new APIPaymentMethodMethodInstruction(
                    new APIPaymentInstructions()
                            .type(APIPaymentInstructions.TypeEnum.PAYMENT_INSTRUCTIONS)
                            .instruction(pi.instruction)
            );

        } else {
            throw new MappingException("Unsupported payment method instruction type: " + instruction.getClass().getName());
        }
    }

    public static APIOperationStatus toAPI(OperationStatus opStatus) {
        if (opStatus instanceof AssetCreationStatus) {
            AssetCreationStatus assetCreationStatus = (AssetCreationStatus) opStatus;
            return new APIOperationStatus(new APIOperationStatusCreateAsset()
                    .type(APIOperationStatusCreateAsset.TypeEnum.CREATE_ASSET)
                    .operation(toAPI(assetCreationStatus))
            );
        } else if (opStatus instanceof DepositOperation) {
            DepositOperation depositOperation = (DepositOperation) opStatus;
            return new APIOperationStatus(new APIOperationStatusDeposit()
                    .type(APIOperationStatusDeposit.TypeEnum.DEPOSIT)
                    .operation(toAPI(depositOperation))
            );
        } else if (opStatus instanceof PlanApprovalStatus) {
            PlanApprovalStatus planApprovalStatus = (PlanApprovalStatus) opStatus;
            return new APIOperationStatus(new APIOperationStatusApproval()
                    .type(APIOperationStatusApproval.TypeEnum.APPROVAL)
                    .operation(toAPI(planApprovalStatus))
            );
        } else if (opStatus instanceof ReceiptOperation) {
            ReceiptOperation receiptOperation = (ReceiptOperation) opStatus;
            return new APIOperationStatus(new APIOperationStatusReceipt()
                    .type(APIOperationStatusReceipt.TypeEnum.RECEIPT)
                    .operation(toAPI(receiptOperation))
            );
        } else {
            throw new MappingException("Unsupported operation status type: " + opStatus.getClass().getName());
        }
    }

    public static APIReceiptOperation toAPI(ReceiptOperation op) {
        APIReceiptOperation operation = new APIReceiptOperation();
        if (op instanceof SuccessReceiptStatus) {
            SuccessReceiptStatus success = (SuccessReceiptStatus) op;
            operation.cid("");
            operation.isCompleted(true);
            operation.response((toAPI(success.receipt)));

        } else if (op instanceof FailedReceiptStatus) {
            FailedReceiptStatus failed = (FailedReceiptStatus) op;
            operation.cid("");
            operation.isCompleted(true);
            operation.error(new APIReceiptOperationErrorInformation()
                    .code(failed.details.code)
                    .message(failed.details.message));

        } else if (op instanceof PendingReceiptStatus) {
            PendingReceiptStatus pending = (PendingReceiptStatus) op;
            operation.cid(pending.correlationId);
            operation.isCompleted(false);
        }
        return operation;
    }

    public static APIReceiptOperation failedTokenOperation(int code, String message) {
        APIReceiptOperation operation = new APIReceiptOperation();
        operation.cid("");
        operation.isCompleted(true);
        operation.error(new APIReceiptOperationErrorInformation()
                .code(code)
                .message(message));
        return operation;
    }

    public static APIGetReceiptResponse toAPIGetReceiptResponse(ReceiptOperation op) {
        APIGetReceiptResponse response = new APIGetReceiptResponse();
        if (op instanceof SuccessReceiptStatus) {
            SuccessReceiptStatus success = (SuccessReceiptStatus) op;
            response.cid("");
            response.isCompleted(true);
            response.response((toAPI(success.receipt)));

        } else if (op instanceof FailedReceiptStatus) {
            FailedReceiptStatus failed = (FailedReceiptStatus) op;
            response.cid("");
            response.isCompleted(true);
            response.error(new APIReceiptOperationErrorInformation()
                    .code(failed.details.code)
                    .message(failed.details.message));

        } else if (op instanceof PendingReceiptStatus) {
            PendingReceiptStatus pending = (PendingReceiptStatus) op;
            response.cid(pending.correlationId);
            response.isCompleted(false);
        }
        return response;
    }


    public static APIReceipt toAPI(Receipt receipt) {
        return new APIReceipt()
                .id(receipt.id)
                .operationType(toAPI(receipt.operationType))
                .source(toAPI(receipt.source))
                .destination(toAPI(receipt.destination))
                .asset(toAPI(receipt.asset))
                .quantity(receipt.quantity)
                .transactionDetails(toAPI(receipt.transactionDetails))
                .tradeDetails(toAPI(receipt.tradeDetails))
                .proof(toAPI(receipt.proof))
                .timestamp(receipt.timestamp);
    }

    private static APIOperationType toAPI(OperationType type) {
        switch (type) {
            case ISSUE:
                return APIOperationType.ISSUE;
            case TRANSFER:
                return APIOperationType.TRANSFER;
            case REDEEM:
                return APIOperationType.REDEEM;
            case HOLD:
                return APIOperationType.HOLD;
            case RELEASE:
                return APIOperationType.RELEASE;
            default:
                throw new MappingException("Unsupported operation type: " + type);
        }
    }

    private static APISource toAPI(@Nullable Source source) {
        if (source == null) {
            return null;
        }
        APISource apiSource = new APISource()
                .finId(source.finId);
        if (source.account instanceof FinIdAccount) {
            FinIdAccount finIdAccount = (FinIdAccount) source.account;
            apiSource.account(toAPI(finIdAccount));
        }
        return apiSource;
    }

    private static APIDestination toAPI(@Nullable Destination destination) {
        if (destination == null) {
            return null;
        }
        APIDestination apiDestination = new APIDestination()
                .finId(destination.finId);
        if (destination.account instanceof FinIdAccount) {
            FinIdAccount finIdAccount = (FinIdAccount) destination.account;
            apiDestination.account(new APIDestinationAccount(toAPI(finIdAccount)));

        } else if (destination.account instanceof CryptocurrencyWallet) {
            CryptocurrencyWallet cryptocurrencyWallet = (CryptocurrencyWallet) destination.account;
            apiDestination.account(new APIDestinationAccount(toAPI(cryptocurrencyWallet)));
        } else if (destination.account instanceof IbanIdentifier) {
            IbanIdentifier ibanIdentifier = (IbanIdentifier) destination.account;
            apiDestination.account(new APIDestinationAccount(toAPI(ibanIdentifier)));
        }

        return apiDestination;
    }

    private static APIFinIdAccount toAPI(FinIdAccount account) {
        return new APIFinIdAccount()
                .type(APIFinIdAccount.TypeEnum.FIN_ID)
                .finId(account.finId);
    }

    private static APICryptoWalletAccount toAPI(CryptocurrencyWallet account) {
        return new APICryptoWalletAccount()
                .type(APICryptoWalletAccount.TypeEnum.CRYPTO_WALLET)
                .address(account.address);
    }

    private static APIFiatAccount toAPI(IbanIdentifier account) {
        return new APIFiatAccount()
                .type(APIFiatAccount.TypeEnum.FIAT_ACCOUNT)
                .code(account.code);
    }

    private static APIAsset toAPI(Asset asset) {
        return new APIAsset(new APIFinp2pAsset()
                .type(APIFinp2pAsset.TypeEnum.FINP2P)
                .resourceId(asset.assetId));
    }

    private static APITransactionDetails toAPI(@Nullable TransactionDetails details) {
        if (details == null) {
            return null;
        }
        return new APITransactionDetails()
                .transactionId(details.transactionId)
                .operationId(details.operationId);

    }

    private static APIReceiptTradeDetails toAPI(TradeDetails details) {
        if (details == null) {
            return null;
        }
        APIReceiptTradeDetails apiDetails = new APIReceiptTradeDetails();
        if (details.executionContext != null) {
            apiDetails.setExecutionContext(new APIReceiptExecutionContext()
                    .executionPlanId(details.executionContext.planId));
        }
        return apiDetails;

    }

    private static APIProofPolicy toAPI(@Nullable ProofPolicy policy) {
        if (policy == null) {
            return null;
        }
        if (policy instanceof NoProofPolicy) {
            return new APIProofPolicy(
                    new APINoProofPolicy()
                            .type(APINoProofPolicy.TypeEnum.NO_PROOF_POLICY)
            );

        } else if (policy instanceof SignatureProofPolicy) {
            SignatureProofPolicy signature = (SignatureProofPolicy) policy;
            return new APIProofPolicy(new APISignatureProofPolicy()
                    .type(APISignatureProofPolicy.TypeEnum.SIGNATURE_PROOF_POLICY)
                    .signature(new APISignature()
                            .signature(signature.signature)
                            .hashFunc(toAPI(signature.hashFunction))
                            .template(toAPI(signature.template)))

            );

        } else {
            throw new MappingException("Unsupported proof policy type: " + policy.getClass().getName());
        }

    }

    public static Signature fromAPI(@Nullable APISignature signature) {
        if (signature == null) {
            return null;
        }
        return new Signature(
                signature.getSignature(),
                fromAPI(signature.getTemplate()),
                fromAPI(signature.getHashFunc())
        );
    }


    public static SignatureTemplate fromAPI(APISignatureTemplate signatureTemplate) {
        if (signatureTemplate.getActualInstance() instanceof APIHashListTemplate) {
            APIHashListTemplate hashListTemplate = (APIHashListTemplate) signatureTemplate.getActualInstance();
            return fromAPI(hashListTemplate);
        } else if (signatureTemplate.getActualInstance() instanceof APIEIP712Template) {
            return fromAPI((APIEIP712Template) signatureTemplate.getActualInstance());
        } else {
            throw new MappingException("Unsupported signature template type: " + signatureTemplate.getActualInstance().getClass().getName());
        }
    }

    public static APISignatureTemplate toAPI(SignatureTemplate signatureTemplate) {
        if (signatureTemplate instanceof HashListTemplate) {
            HashListTemplate hashListTemplate = (HashListTemplate) signatureTemplate;
            return new APISignatureTemplate(toAPI(hashListTemplate));

        } else if (signatureTemplate instanceof EIP712Template) {
            EIP712Template eip712Template = (EIP712Template) signatureTemplate;
            return new APISignatureTemplate(toAPI(eip712Template));
        } else {
            throw new MappingException("Unsupported signature template type: " + signatureTemplate.getClass().getName());
        }
    }

    public static HashListTemplate fromAPI(APIHashListTemplate template) {
        return new HashListTemplate(
                template.getHashGroups().stream().map(Mappers::fromAPI).collect(toList()),
                template.getHash()
        );
    }

    public static EIP712Template fromAPI(APIEIP712Template template) {
        return new EIP712Template();
    }


    public static APIHashListTemplate toAPI(HashListTemplate template) {
        return new APIHashListTemplate()
                .type(APIHashListTemplate.TypeEnum.HASH_LIST)
                .hash(template.hash)
                .hashGroups(template.hashGroups.stream().map(Mappers::toAPI).collect(toList()));
    }

    public static APIEIP712Template toAPI(EIP712Template template) {
        return new APIEIP712Template()
                .type(APIEIP712Template.TypeEnum.EIP712);
    }


    public static HashGroup fromAPI(APIHashGroup hashGroup) {
        return new HashGroup(
                hashGroup.getHash(),
                hashGroup.getFields().stream()
                        .map(Mappers::fromAPI)
                        .collect(toList())
        );
    }

    public static APIHashGroup toAPI(HashGroup hashGroup) {
        return new APIHashGroup()
                .hash(hashGroup.hash)
                .fields(hashGroup.fields.stream().map(Mappers::toAPI).collect(toList()));
    }

    public static HashField fromAPI(APIField field) {
        return new HashField(
                field.getName(),
                field.getType().getValue(),
                field.getValue()
        );
    }

    public static APIField toAPI(HashField field) {
        return new APIField()
                .name(field.name)
                .type(APIField.TypeEnum.fromValue(field.type))
                .value(field.value);
    }

    public static HashFunction fromAPI(@Nullable APIHashFunction hashFunction) {
        if (hashFunction == null) {
            return null;
        }
        switch (hashFunction) {
            case SHA3_256:
                return HashFunction.SHA3_256;
            case KECCAK_256:
                return HashFunction.KECCAK_256;
            default:
                throw new MappingException("Unsupported hash function: " + hashFunction);
        }
    }

    public static APIHashFunction toAPI(@Nullable HashFunction hashFunction) {
        if (hashFunction == null) {
            return null;
        }
        switch (hashFunction) {
            case SHA3_256:
                return APIHashFunction.SHA3_256;
            case KECCAK_256:
                return APIHashFunction.KECCAK_256;
            default:
                throw new MappingException("Unsupported hash function: " + hashFunction);
        }
    }


}
