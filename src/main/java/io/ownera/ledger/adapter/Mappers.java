package io.ownera.ledger.adapter;

import io.ownera.ledger.adapter.api.model.*;
import io.ownera.ledger.adapter.service.model.*;

import static java.util.stream.Collectors.toList;

public class Mappers {

    public static APIApproveExecutionPlanResponse toAPI(PlanApprovalStatus status) {

    }

    public static APICreateAssetResponse toAPI(AssetCreationStatus status) {
        APICreateAssetResponse response = new APICreateAssetResponse();
        if (status instanceof PendingAssetCreation) {
            PendingAssetCreation pending = (PendingAssetCreation) status;
            response.isCompleted(false);
            response.cid(pending.correlationId);

        } else if (status instanceof FailedAssetCreation) {
            FailedAssetCreation failed = (FailedAssetCreation) status;
            response.isCompleted(true);
            response.error(new APICreateAssetOperationErrorInformation()
                    .message(failed.details.message)
                    .code(failed.details.code));

        } else if (status instanceof SuccessfulAssetCreation) {
            SuccessfulAssetCreation success = (SuccessfulAssetCreation) status;
            response.response(new APIAssetCreateResponse()
                    .ledgerAssetInfo(new APILedgerAssetInfo()
                            .ledgerTokenId(new APILedgerTokenId()
                                    .tokenId(success.result.tokenId)
                            )
                    ));

        }
        return response;
    }


    public static APIOperationStatus toAPI(OperationStatus opStatus) {
        if (opStatus.isCompleted) {
            if (opStatus.error != null) {
                APIOperationStatusReceipt receipt = new APIOperationStatusReceipt();
                receipt.setType(APIOperationStatusReceipt.TypeEnum.RECEIPT);
                receipt.setOperation(receiptOperationFailed(opStatus.errorCode, opStatus.error));
                return new OperationStatus(receipt);
            } else {
                if (opStatus.result instanceof AssetCreationStatus) {
                    APIOperationStatusCreateAsset receipt = new OperationStatusCreateAsset();
                    receipt.setType(OperationStatusCreateAsset.TypeEnum.CREATE_ASSET);
                    AssetCreationStatus assetResult = (AssetCreationStatus) opStatus.result;
                    receipt.setOperation(createAssetOperationCompleted(assetResult));
                    return new OperationStatus(receipt);
                } else if (opStatus.result instanceof ServiceTokenResult) {
                    OperationStatusReceipt receipt = new OperationStatusReceipt();
                    receipt.setType(OperationStatusReceipt.TypeEnum.RECEIPT);
                    ServiceTokenResult tokenResult = (ServiceTokenResult) opStatus.result;
                    receipt.setOperation(receiptOperationCompleted(tokenResult));
                    return new OperationStatus(receipt);
                }
            }
        }

        OperationStatusReceipt receipt = new OperationStatusReceipt();
        receipt.setType(OperationStatusReceipt.TypeEnum.RECEIPT);
        receipt.setOperation(receiptOperationDelayed(opStatus.correlationId));
        return new OperationStatus(receipt);
    }

    public static APIGetReceiptResponse toAPI(ReceiptOperation op) {
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
                .timestamp(receipt.timestamp)
                ;
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

    private static APISource toAPI(Source source) {
        if (source == null) {
            return null;
        }
        APISource apiSource = new APISource()
                .finId(source.finId);
        if (source.account instanceof FinIdAccount) {
            FinIdAccount finIdAccount = (FinIdAccount) source.account;
            apiSource.account(new APIFinIdAccount()
                    .type(APIFinIdAccount.TypeEnum.FIN_ID)
                    .finId(finIdAccount.finId));
        }
        return apiSource;
    }

    private static APIDestination toAPI(Destination destination) {
        if (destination == null) {
            return null;
        }
        APIDestination apiDestination = new APIDestination()
                .finId(destination.finId);
        if (destination.account instanceof FinIdAccount) {
            FinIdAccount finIdAccount = (FinIdAccount) destination.account;
            apiDestination.account(new APIDestinationAccount()
                    .type(APIFinIdAccount.TypeEnum.FIN_ID)
                    .finId(finIdAccount.finId));
        }
        return apiDestination;
    }

    private static APIAsset toAPI(Asset asset) {
        return new APIAsset(new APIFinp2pAsset()
                .type(APIFinp2pAsset.TypeEnum.FINP2P)
                .resourceId(asset.assetId));
    }

    private static APITransactionDetails toAPI(@javax.annotation.Nullable TransactionDetails details) {
        if (details == null) {
            return null;
        }
        return new APITransacti.onDetails()
                .transactionId(details.transactionId)
                .operationId(details.operationId);

    }

    private static APIReceiptTradeDetails toAPI(TradeDetails details) {
        if (details == null) {
            return null;
        }
        return new APIReceiptTradeDetails()
                .executionContext();

    }

    private static APIProofPolicy toAPI(@javax.annotation.Nullable ProofPolicy policy) {
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

    public static Signature fromAPI(APISignature signature) {
        return new Signature(
                signature.getSignature(),
                fromAPI(signature.getTemplate()),
                fromAPI(signature.getHashFunc())
        );
    }


    public static SignatureTemplate fromAPI(APISignatureTemplate signatureTemplate) {
        signatureTemplate.getSchemaType()

        APIHashListTemplate hashListTemplate = signatureTemplate.getAPIHashListTemplate();
        return new SignatureTemplate(
                hashListTemplate.getHashGroups().stream()
                        .map(Mappers::fromAPI).collect(toList()),
                hashListTemplate.getHash()
        );
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

    public static HashFunction fromAPI(@javax.annotation.Nullable APIHashFunction hashFunction) {
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

    public static APIHashFunction toAPI(@javax.annotation.Nullable HashFunction hashFunction) {
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


//
//    public static Asset fromAPI(APIAsset asset) {
//
//    }

}
