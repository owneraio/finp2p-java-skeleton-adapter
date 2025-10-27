package io.ownera.ledger.adapter;

import io.ownera.ledger.adapter.api.model.*;
import io.ownera.ledger.adapter.service.model.*;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

public class Mappers {

    public static CreateAssetResponse createAssetResponse(ServiceOperationResult<ServiceAssetResult> result) {
        CreateAssetResponse response = new CreateAssetResponse();
        if (result.isCompleted) {
            response.isCompleted(true);
            if (result.error != null) {
                response.error(new CreateAssetOperationErrorInformation()
                        .message(result.error)
                        .code(result.errorCode));
            } else {
                response.response(new AssetCreateResponse()
                        .ledgerAssetInfo(new LedgerAssetInfo()
                                .ledgerTokenId(new LedgerTokenId()
                                        .tokenId(result.result.assetId)
                                )
                        ));
            }
        } else {
            response.isCompleted(false);
            response.cid(result.correlationId);
        }

        return response;
    }

    public static CreateAssetOperation createAssetOperationCompleted(ServiceAssetResult result) {
        CreateAssetOperation operation = new CreateAssetOperation();
        operation.isCompleted(true);
        operation.response(new AssetCreateResponse()
                .ledgerAssetInfo(new LedgerAssetInfo()
                        .ledgerTokenId(new LedgerTokenId()
                                .tokenId(result.assetId)
                        )
                ));
        return operation;
    }

    public static CreateAssetResponse createAssetResponseFailed(int errCode, String message) {
        return new CreateAssetResponse()
                .isCompleted(true)
                .error(new CreateAssetOperationErrorInformation()
                        .message(message)
                        .code(errCode));
    }


    public static ReceiptOperation receiptOperation(ServiceOperationResult<ServiceTokenResult> result) {
        ReceiptOperation receiptOperation = new ReceiptOperation();
        if (result.isCompleted) {
            receiptOperation.isCompleted(true);
            if (result.error != null) {
                receiptOperation.error(new ReceiptOperationErrorInformation()
                        .message(result.error)
                        .code(result.errorCode));
            } else {
                receiptOperation.response(receipt(result.result));
            }
        } else {
            receiptOperation.isCompleted(false);
            receiptOperation.cid(result.correlationId);
        }
        return receiptOperation;
    }

    public static ReceiptOperation receiptOperationCompleted(ServiceTokenResult result) {
        ReceiptOperation receiptOperation = new ReceiptOperation();
        receiptOperation.isCompleted(true);
        receiptOperation.response(receipt(result));
        return receiptOperation;
    }


    public static ReceiptOperation receiptOperationDelayed(String correlationId) {
        ReceiptOperation receiptOperation = new ReceiptOperation();
        receiptOperation.isCompleted(false);
        receiptOperation.cid(correlationId);
        return receiptOperation;
    }

    public static ReceiptOperation receiptOperationFailed(int errCode, String message) {
        return receiptOperationFailed(errCode, message, emptyList());
    }

    public static ReceiptOperation receiptOperationFailed(int errCode, String message, List<RegulationErrorDetails> regulationErrorDetails) {
        ReceiptOperation receiptOperation = new ReceiptOperation()
                .isCompleted(true);
        ReceiptOperationErrorInformation error = new ReceiptOperationErrorInformation()
                .message(message)
                .code(errCode);
        for (RegulationErrorDetails item : regulationErrorDetails) {
            RegulationError regErr = new RegulationError();
            regErr.setRegulationType(item.regulationType);
            regErr.setDetails(item.description);
            error.addRegulationErrorDetailsItem(regErr);
        }
        return receiptOperation.error(error);
    }

    public static OperationStatus operationStatus(ServiceOperationStatus opStatus) {
        if (opStatus.isCompleted) {
            if (opStatus.error != null) {
                OperationStatusReceipt receipt = new OperationStatusReceipt();
                receipt.setType(OperationStatusReceipt.TypeEnum.RECEIPT);
                receipt.setOperation(receiptOperationFailed(opStatus.errorCode, opStatus.error));
                return new OperationStatus(receipt);
            } else {
                if (opStatus.result instanceof ServiceAssetResult) {
                    OperationStatusCreateAsset receipt = new OperationStatusCreateAsset();
                    receipt.setType(OperationStatusCreateAsset.TypeEnum.CREATE_ASSET);
                    ServiceAssetResult assetResult = (ServiceAssetResult) opStatus.result;
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


    public static Receipt receipt(ServiceTokenResult receipt) {
        Source source = null;
        if (receipt.sourceFinId != null && !receipt.sourceFinId.isEmpty()) {
            source = new Source()
                    .finId(receipt.sourceFinId)
                    .account(new FinIdAccount()
                            .type(FinIdAccount.TypeEnum.FIN_ID)
                            .finId(receipt.sourceFinId));
        }
        Destination destination = null;
        if (receipt.destinationFinId != null && !receipt.destinationFinId.isEmpty()) {
            destination = new Destination()
                    .finId(receipt.destinationFinId)
                    .account(new DestinationAccount(new FinIdAccount()
                            .type(FinIdAccount.TypeEnum.FIN_ID)
                            .finId(receipt.destinationFinId)
                    ));
        }
        return new Receipt()
                .id(receipt.transactionId)
                .source(source)
                .destination(destination)
                .asset(new Asset(new Finp2pAsset()
                        .type(Finp2pAsset.TypeEnum.FINP2P)
                        .resourceId(receipt.assetId)))
                .quantity(receipt.quantity)
                .transactionDetails(new TransactionDetails()
                        .transactionId(receipt.transactionId)
                );
    }


    public static ServiceSignature fromAPI(Signature signature) {
        return new ServiceSignature(
                signature.getSignature(),
                fromAPI(signature.getTemplate()),
                fromAPI(signature.getHashFunc())
        );
    }

    public static ServiceSignatureTemplate fromAPI(SignatureTemplate signatureTemplate) {
        HashListTemplate hashListTemplate = signatureTemplate.getHashListTemplate();
        return new ServiceSignatureTemplate(
                hashListTemplate.getHashGroups().stream()
                        .map(Mappers::fromAPI).collect(toList()),
                hashListTemplate.getHash()
        );
    }

    public static ServiceSignatureHashGroup fromAPI(HashGroup hashGroup) {
        return new ServiceSignatureHashGroup(
                hashGroup.getHash(),
                hashGroup.getFields().stream()
                        .map(Mappers::fromAPI)
                        .collect(toList())
        );
    }

    public static ServiceSignatureField fromAPI(Field field) {
        return new ServiceSignatureField(
                field.getName(),
                field.getType().getValue(),
                field.getValue()
        );
    }

    public static ServiceHashFunction fromAPI(HashFunction hashFunction) {
        if (hashFunction == null) {
            return null;
        }
        switch (hashFunction) {
            case SHA3_256:
                return ServiceHashFunction.SHA3_256;
            case KECCAK_256:
                return ServiceHashFunction.KECCAK_256;
            default:
                throw new IllegalArgumentException("Unsupported hash function: " + hashFunction);
        }
    }
}
