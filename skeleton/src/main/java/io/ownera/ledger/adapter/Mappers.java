package io.ownera.ledger.adapter;

import io.ownera.ledger.adapter.api.model.*;
import io.ownera.ledger.adapter.service.model.*;
import javax.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

public class Mappers {

    // --- Asset mapping ---
    // 0.28: APIAsset is flat (resourceId + ledgerIdentifier); no more polymorphic fiat/cryptocurrency variants.

    public static Asset fromAPI(APIAsset asset) {
        if (asset == null) {
            throw new MappingException("Asset is required");
        }
        return new Asset(asset.getResourceId(), AssetType.FINP2P, fromAPI(asset.getLedgerIdentifier()));
    }

    public static Asset fromAPI(APIFinp2pAssetBase asset) {
        return new Asset(asset.getResourceId(), AssetType.FINP2P);
    }

    public static LedgerAssetIdentifier fromAPI(@Nullable APILedgerAssetIdentifier identifier) {
        if (identifier == null) {
            return null;
        }
        Object actual = identifier.getActualInstance();
        if (actual instanceof APILedgerAssetIdentifierTypeCAIP19) {
            APILedgerAssetIdentifierTypeCAIP19 caip19 = (APILedgerAssetIdentifierTypeCAIP19) actual;
            return new LedgerAssetIdentifier(caip19.getNetwork(), caip19.getTokenId(), caip19.getStandard());
        }
        return null;
    }

    public static AssetBind fromAPI(@Nullable APILedgerAssetBinding binding) {
        if (binding == null) {
            return null;
        }
        Object actual = binding.getActualInstance();
        if (actual instanceof APILedgerAssetIdentifierTypeCAIP19) {
            APILedgerAssetIdentifierTypeCAIP19 caip19 = (APILedgerAssetIdentifierTypeCAIP19) actual;
            return new AssetBind(new TokenIdentifier(caip19.getTokenId()));
        }
        return null;
    }

    public static AssetDenomination fromAPI(@Nullable APIAssetDenomination denomination) {
        if (denomination == null) {
            return null;
        }
        return new AssetDenomination(fromAPI(denomination.getType()), denomination.getCode());
    }

    public static AssetDenominationType fromAPI(APIAssetDenominationType type) {
        switch (type) {
            case FINP2P:
                return AssetDenominationType.FINP2P;
            case FIAT:
                return AssetDenominationType.FIAT;
            case CRYPTOCURRENCY:
                return AssetDenominationType.CRYPTOCURRENCY;
            default:
                throw new MappingException("Unsupported asset denomination type: " + type);
        }
    }

    // --- DepositAsset mapping ---
    // 0.28: only finp2p + custom variants

    public static DepositAsset fromAPI(APIDepositAsset asset) {
        Object actual = asset.getActualInstance();
        if (actual instanceof APIFinp2pAssetWithType) {
            APIFinp2pAssetWithType finp2pAsset = (APIFinp2pAssetWithType) actual;
            return new DepositAsset(finp2pAsset.getResourceId(), DepositAssetType.FINP2P);

        } else if (actual instanceof APICustomAsset) {
            return new DepositAsset("", DepositAssetType.CUSTOM);

        } else {
            throw new MappingException("Unsupported deposit asset type: " + actual.getClass().getName());
        }
    }

    // --- Account mapping ---
    // 0.28: Source/Destination/DestinationAccount are merged into APIAccount.
    // APIAccount carries finId + asset + ledgerAccount (APIAccountLedgerAccount polymorphic).

    public static FinIdAccount fromAPI(APIFinIdAccountBase account) {
        return new FinIdAccount(account.getFinId());
    }

    public static FinIdAccount finIdAccountFromAPI(APIAccount account) {
        return new FinIdAccount(account.getFinId());
    }

    // --- DepositPayoutAccount (used by /payments endpoints; keeps a separate top-level asset) ---

    public static Source sourceFromAPI(APIDepositPayoutAccount account) {
        return new Source(account.getFinId(), new FinIdAccount(account.getFinId()));
    }

    public static Destination destinationFromAPI(@Nullable APIDepositPayoutAccount account) {
        if (account == null) {
            return null;
        }
        return new Destination(account.getFinId(), new FinIdAccount(account.getFinId()));
    }

    public static Source sourceFromAPI(APIAccount account) {
        DestinationAccount ledgerAccount = ledgerAccountFromAPI(account);
        SourceAccount src = (ledgerAccount instanceof SourceAccount)
                ? (SourceAccount) ledgerAccount
                : new FinIdAccount(account.getFinId());
        return new Source(account.getFinId(), src);
    }

    public static Destination destinationFromAPI(@Nullable APIAccount account) {
        if (account == null) {
            return null;
        }
        return new Destination(account.getFinId(), ledgerAccountFromAPI(account));
    }

    /**
     * Extract ledger account (wallet) from an APIAccount, falling back to FinIdAccount if none.
     */
    private static DestinationAccount ledgerAccountFromAPI(APIAccount account) {
        if (account.getLedgerAccount() != null) {
            Object actual = account.getLedgerAccount().getActualInstance();
            if (actual instanceof APIWalletLedgerAccount) {
                APIWalletLedgerAccount wallet = (APIWalletLedgerAccount) actual;
                return new CryptocurrencyWallet(wallet.getAddress());
            }
        }
        return new FinIdAccount(account.getFinId());
    }

    /**
     * Extract asset from an APIAccount (embedded since 0.28).
     */
    public static Asset assetFromAPI(APIAccount account) {
        if (account.getAsset() == null) {
            throw new MappingException("Asset missing from account");
        }
        return fromAPI(account.getAsset());
    }

    public static Destination destinationFromAPI(APIFinIdAccountBase account) {
        return new Destination(account.getFinId(), new FinIdAccount(account.getFinId()));
    }

    public static ExecutionContext fromAPI(@Nullable APIExecutionContext ctx) {
        if (ctx == null) {
            return null;
        }
        return new ExecutionContext(ctx.getExecutionPlanId(), ctx.getInstructionSequenceNumber());
    }

    // --- Plan approval response ---

    public static APIExecutionPlanApprovalOperation toAPI(PlanApprovalStatus status) {
        if (status instanceof PendingPlan) {
            PendingPlan pending = (PendingPlan) status;
            return new APIExecutionPlanApprovalOperation()
                    .cid(pending.correlationId)
                    .isCompleted(false)
                    .operationMetadata(toAPIOpt(pending.metadata));

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
                                                                    .failureType(APIValidationFailure.FailureTypeEnum.VALIDATIONFAILURE)
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
                    .isCompleted(false)
                    .operationMetadata(toAPIOpt(pending.metadata));

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
                                                                    .failureType(APIValidationFailure.FailureTypeEnum.VALIDATIONFAILURE)
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

    // --- Asset creation response ---

    public static APICreateAssetOperation toAPI(AssetCreationStatus status) {
        APICreateAssetOperation operation = new APICreateAssetOperation();
        if (status instanceof PendingAssetCreation) {
            PendingAssetCreation pending = (PendingAssetCreation) status;
            operation.isCompleted(false);
            operation.cid(pending.correlationId);
            operation.operationMetadata(toAPIOpt(pending.metadata));

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
                            .ledgerIdentifier(new APILedgerAssetIdentifier(
                                    new APILedgerAssetIdentifierTypeCAIP19()
                                            .tokenId(success.result.tokenId)
                            ))
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
            response.operationMetadata(toAPIOpt(pending.metadata));

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
                            .ledgerIdentifier(new APILedgerAssetIdentifier(
                                    new APILedgerAssetIdentifierTypeCAIP19()
                                            .tokenId(success.result.tokenId)
                            ))
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

    // --- Deposit operation response ---

    public static APIDepositOperation toAPI(DepositOperation status) {
        APIDepositOperation operation = new APIDepositOperation();
        if (status instanceof PendingDepositOperation) {
            PendingDepositOperation pending = (PendingDepositOperation) status;
            operation.isCompleted(false);
            operation.cid(pending.correlationId);
            operation.operationMetadata(toAPIOpt(pending.metadata));

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
            response.operationMetadata(toAPIOpt(pending.metadata));

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
            return new APIPaymentMethodMethodInstruction(
                    new APIWireTransfer()
                            .type(APIWireTransfer.TypeEnum.WIRETRANSFER)
            );

        } else if (instruction instanceof WireTransferUsa) {
            return new APIPaymentMethodMethodInstruction(
                    new APIWireTransferUSA()
                            .type(APIWireTransferUSA.TypeEnum.WIRETRANSFERUSA)
            );

        } else if (instruction instanceof CryptoTransfer) {
            return new APIPaymentMethodMethodInstruction(
                    new APICryptoTransfer()
                            .type(APICryptoTransfer.TypeEnum.CRYPTOTRANSFER)
            );

        } else if (instruction instanceof PaymentInstruction) {
            PaymentInstruction pi = (PaymentInstruction) instruction;
            return new APIPaymentMethodMethodInstruction(
                    new APIPaymentInstructions()
                            .type(APIPaymentInstructions.TypeEnum.PAYMENTINSTRUCTIONS)
                            .instruction(pi.instruction)
            );

        } else {
            throw new MappingException("Unsupported payment method instruction type: " + instruction.getClass().getName());
        }
    }

    // --- Operation status response ---

    public static APIOperationStatus toAPI(OperationStatus opStatus) {
        if (opStatus instanceof AssetCreationStatus) {
            AssetCreationStatus assetCreationStatus = (AssetCreationStatus) opStatus;
            return new APIOperationStatus(new APIOperationStatusCreateAsset()
                    .type(APIOperationStatusCreateAsset.TypeEnum.CREATEASSET)
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
            operation.operationMetadata(toAPIOpt(pending.metadata));
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
            response.operationMetadata(toAPIOpt(pending.metadata));
        }
        return response;
    }

    public static APIReceipt toAPI(Receipt receipt) {
        return new APIReceipt()
                .id(receipt.id)
                .operationType(toAPI(receipt.operationType))
                .source(toAPI(receipt.source, receipt.asset))
                .destination(toAPI(receipt.destination, receipt.asset))
                .quantity(receipt.quantity)
                .transactionDetails(toAPI(receipt.transactionDetails))
                .tradeDetails(toAPI(receipt.tradeDetails))
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
            case ROLLBACK:
                return APIOperationType.RELEASE;
            default:
                throw new MappingException("Unsupported operation type: " + type);
        }
    }

    // --- Source/Destination → APIAccount (embeds asset since 0.28) ---

    private static APIAccount toAPI(@Nullable Source source, @Nullable Asset asset) {
        if (source == null) {
            return null;
        }
        APIAccount account = new APIAccount().finId(source.finId);
        if (asset != null) {
            account.asset(toAPIAsset(asset));
        }
        if (source.account instanceof CryptocurrencyWallet) {
            CryptocurrencyWallet wallet = (CryptocurrencyWallet) source.account;
            account.ledgerAccount(new APIAccountLedgerAccount(
                    new APIWalletLedgerAccount()
                            .type("wallet")
                            .address(wallet.address)
            ));
        }
        return account;
    }

    private static APIAccount toAPI(@Nullable Destination destination, @Nullable Asset asset) {
        if (destination == null) {
            return null;
        }
        APIAccount account = new APIAccount().finId(destination.finId);
        if (asset != null) {
            account.asset(toAPIAsset(asset));
        }
        if (destination.account instanceof CryptocurrencyWallet) {
            CryptocurrencyWallet wallet = (CryptocurrencyWallet) destination.account;
            account.ledgerAccount(new APIAccountLedgerAccount(
                    new APIWalletLedgerAccount()
                            .type("wallet")
                            .address(wallet.address)
            ));
        }
        return account;
    }

    private static APIFinIdAccountBase toAPI(FinIdAccount account) {
        return new APIFinIdAccountBase()
                .type(APIFinIdAccountBase.TypeEnum.FINID)
                .finId(account.finId);
    }

    private static APIAsset toAPIAsset(Asset asset) {
        return new APIAsset()
                .resourceId(asset.assetId);
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

    // --- Signature & template mapping ---

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
        APIEIP712Domain domain = template.getDomain();
        long chainId = domain != null && domain.getChainId() != null ? domain.getChainId() : 0;
        String verifyingContract = domain != null ? domain.getVerifyingContract() : null;

        Map<String, Object> types = new HashMap<>();
        if (template.getTypes() != null && template.getTypes().getDefinitions() != null) {
            for (APIEIP712TypeDefinition def : template.getTypes().getDefinitions()) {
                if (def.getName() != null && def.getFields() != null) {
                    types.put(def.getName(), def.getFields().stream()
                            .map(f -> {
                                Map<String, String> field = new HashMap<>();
                                field.put("name", f.getName());
                                field.put("type", f.getType());
                                return field;
                            })
                            .collect(Collectors.toList()));
                }
            }
        }

        Map<String, Object> message = new HashMap<>();
        if (template.getMessage() != null) {
            template.getMessage().forEach((k, v) -> message.put(k, v));
        }

        return new EIP712Template(
                template.getPrimaryType(),
                chainId,
                verifyingContract,
                types,
                message,
                template.getHash()
        );
    }

    public static APIHashListTemplate toAPI(HashListTemplate template) {
        return new APIHashListTemplate()
                .type(APIHashListTemplate.TypeEnum.HASHLIST)
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
            case SHA3_2562:
                return HashFunction.SHA3_256;
            case KECCAK_256:
            case KECCAK_2562:
                return HashFunction.KECCAK_256;
            case BLAKE2B:
            case UNSPECIFIED:
                return HashFunction.UNSPECIFIED;
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
            case UNSPECIFIED:
                return APIHashFunction.UNSPECIFIED;
            default:
                throw new MappingException("Unsupported hash function: " + hashFunction);
        }
    }

    // --- PayoutAsset mapping (0.28: finp2p only) ---

    public static Asset fromAPI(APIPayoutAsset asset) {
        if (asset == null) {
            throw new MappingException("Payout asset is required");
        }
        return new Asset(asset.getId(), AssetType.FINP2P);
    }

    // --- Balance mapping ---

    public static APIAssetBalanceInfoResponse balanceToAPI(APIAsset asset, APIFinIdAccountBase account, Balance balance) {
        return new APIAssetBalanceInfoResponse()
                .account(account)
                .asset(asset)
                .balanceInfo(new APIAssetBalance()
                        .asset(asset)
                        .current(balance.current)
                        .available(balance.available)
                        .held(balance.held)
                );
    }

    // --- Payout response mapping ---

    public static APIPayoutResponse toAPIPayoutResponse(ReceiptOperation op) {
        APIPayoutResponse response = new APIPayoutResponse();
        if (op instanceof SuccessReceiptStatus) {
            SuccessReceiptStatus success = (SuccessReceiptStatus) op;
            response.cid("");
            response.isCompleted(true);
            response.response(toAPI(success.receipt));

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
            response.operationMetadata(toAPIOpt(pending.metadata));
        }
        return response;
    }

    // --- Operation metadata mapping ---

    public static APIOperationMetadata toAPIOpt(@Nullable OperationMetadata metadata) {
        if (metadata == null || metadata.responseStrategy == null) {
            return null;
        }
        return toAPI(metadata);
    }

    public static APIOperationMetadata toAPI(OperationMetadata metadata) {
        APIOperationMetadataOperationResponseStrategy strategy;
        if (metadata.responseStrategy instanceof PollingResponseStrategy) {
            strategy = new APIOperationMetadataOperationResponseStrategy(
                    new APIPollingResultsStrategy()
                            .type(APIPollingResultsStrategy.TypeEnum.POLL)
                            .polling(new APIPollingResultsStrategyPolling(
                                    new APIRandomPollingInterval()
                                            .type(APIRandomPollingInterval.TypeEnum.RANDOM)
                            ))
            );
        } else if (metadata.responseStrategy instanceof CallbackResponseStrategy) {
            strategy = new APIOperationMetadataOperationResponseStrategy(
                    new APICallbackResultsStrategy()
                            .type(APICallbackResultsStrategy.TypeEnum.CALLBACK)
                            .callback(new APICallbackResultsStrategyCallback(
                                    new APICallbackEndpoint()
                                            .type(APICallbackEndpoint.TypeEnum.ENDPOINT)
                            ))
            );
        } else {
            return null;
        }
        return new APIOperationMetadata().operationResponseStrategy(strategy);
    }
}
