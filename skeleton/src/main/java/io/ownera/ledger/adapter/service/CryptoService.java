package io.ownera.ledger.adapter.service;

import io.ownera.finp2p.signing.SignatureUtils;
import io.ownera.finp2p.signing.eip712.EIP712;
import io.ownera.ledger.adapter.service.model.Receipt;
import io.ownera.ledger.adapter.service.proof.Mapper;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;

import java.io.IOException;
import java.security.SignatureException;

public class CryptoService {

    public byte[] sign(String base64PrivateKey, byte[] payload) throws IOException {
        AsymmetricCipherKeyPair keyPair = SignatureUtils.parseKey(base64PrivateKey);
        ECPrivateKeyParameters privateKey = (ECPrivateKeyParameters) keyPair.getPrivate();
        return SignatureUtils.sign(privateKey, payload);
    }

    public boolean verify(String signerFinId, byte[] signature, byte[] payload) {
        try {
            SignatureUtils.verify(signerFinId, signature, payload);
            return true;
        } catch (SignatureException e) {
            return false;
        }
    }

    public byte[] hashEIP712Receipt(Receipt receipt) throws IOException {
        return EIP712.hashMessage(Mapper.toEIP712(receipt));
    }

    public String toFinId(String base64Key) throws IOException {
        AsymmetricCipherKeyPair keyPair = SignatureUtils.parseKey(base64Key);
        ECPublicKeyParameters publicKey = (ECPublicKeyParameters) keyPair.getPublic();
        return SignatureUtils.toFinId(publicKey);
    }

    public AsymmetricCipherKeyPair parseKey(String base64Key) throws IOException {
        return SignatureUtils.parseKey(base64Key);
    }
}
