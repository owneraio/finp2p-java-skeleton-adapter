package io.ownera.finp2p.signing.hashlist;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

public class HashList {

    public static byte[] hashSecondaries(
            byte[] nonce,
            AssetType assetType,
            String assetCode,
            String sellerFinId,
            String buyerFinId,
            String amount,
            AssetType settlementAssetType,
            String settlementAssetCode,
            String settlementAmount
    ) {
        List<HashGroup> hashGroups = new ArrayList<>();
        byte[] assetHash = computeAssetHash(
                nonce,
                Operation.TRANSFER,
                assetType,
                assetCode,
                AccountType.FINID, sellerFinId,
                AccountType.FINID, buyerFinId,
                amount
        );
        byte[] settlementHash = computeSettlementHash(
                settlementAssetType, settlementAssetCode,
                AccountType.FINID, buyerFinId,
                AccountType.FINID, sellerFinId,
                settlementAmount
        );
        hashGroups.add(new HashGroup(assetHash, new ArrayList<>()));
        hashGroups.add(new HashGroup(settlementHash, new ArrayList<>()));

        return computeHash(assetHash, settlementHash);
    }


    public static byte[] hashDepositRequest(
            byte[] nonce,
            AssetType assetType,
            String assetCode,
            AccountType destinationAccountType,
            String destinationAccount,
            String amount
    ) {
        ByteBuffer bb = ByteBuffer.allocate(1024);
        bb.put(nonce);
        bb.put(Operation.DEPOSIT.getValue().getBytes(UTF_8));

        bb.put(assetType.getValue().getBytes(UTF_8));
        bb.put(assetCode.getBytes(UTF_8));

        bb.put(destinationAccountType.getValue().getBytes(UTF_8));
        bb.put(destinationAccount.getBytes(UTF_8));

        bb.put(amount.getBytes(UTF_8));

        return hash(bb);
    }

    private static byte[] computeAssetHash(
            byte[] nonce,
            Operation operation,
            AssetType assetType,
            String assetCode,
            AccountType sourceAccountType,
            String sourceAccountId,
            AccountType destinationAccountType,
            String destinationAccountId,
            String amount
    ) {
        ByteBuffer bb = ByteBuffer.allocate(1024);
        bb.put(nonce);
        bb.put(operation.getValue().getBytes(UTF_8));

        bb.put(assetType.getValue().getBytes(UTF_8));
        bb.put(assetCode.getBytes(UTF_8));

        if (sourceAccountType != null) {
            bb.put(sourceAccountType.getValue().getBytes(UTF_8));
            bb.put(sourceAccountId.getBytes(UTF_8));
        }
        if (destinationAccountType != null) {
            bb.put(destinationAccountType.getValue().getBytes(UTF_8));
            bb.put(destinationAccountId.getBytes(UTF_8));
        }

        bb.put(amount.getBytes(UTF_8));

        return hash(bb);
    }

    private static byte[] computeSettlementHash(
            AssetType assetType,
            String assetCode,
            AccountType sourceAccountType,
            String sourceAccountId,
            AccountType destinationAccountType,
            String destinationAccountId,
            String amount
    ) {
        ByteBuffer bb = ByteBuffer.allocate(1024);

        bb.put(assetType.getValue().getBytes(UTF_8));
        bb.put(assetCode.getBytes(UTF_8));

        if (sourceAccountType != null) {
            bb.put(sourceAccountType.getValue().getBytes(UTF_8));
            bb.put(sourceAccountId.getBytes(UTF_8));
        }
        if (destinationAccountType != null) {
            bb.put(destinationAccountType.getValue().getBytes(UTF_8));
            bb.put(destinationAccountId.getBytes(UTF_8));
        }

        bb.put(amount.getBytes(UTF_8));

        return hash(bb);
    }


    private static byte[] computeHash(byte[]... groups) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA3-256");
            for (byte[] group : groups) {
                if (group.length > 0) {
                    digest.update(group);
                }
            }
            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unable to generate SHA3 hash", e);
        }
    }

    private static byte[] hash(ByteBuffer bytes) {
        return hash(toBytes(bytes));
    }

    private static byte[] hash(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA3-256");
            if (bytes.length > 0) {
                digest.update(bytes);
            }
            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unable to generate SHA3 hash", e);
        }
    }

    private static byte[] toBytes(ByteBuffer bb) {
        byte[] bytes = new byte[bb.position()];
        bb.rewind();
        bb.get(bytes);
        return bytes;
    }
}
