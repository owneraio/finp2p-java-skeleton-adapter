package io.ownera.finp2p.signing;

import io.ownera.finp2p.signing.hashlist.*;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.generators.ECKeyPairGenerator;
import org.bouncycastle.crypto.params.*;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.Hex;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.*;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;

public class SignatureUtils {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static final String SECP256K1_CURVE = "secp256k1";
    static final ECParameterSpec curve = ECNamedCurveTable.getParameterSpec(SECP256K1_CURVE);


    public static byte[] sign(ECPrivateKeyParameters privateKey, byte[] signatureBytes) {
        ECDSASigner signer = new ECDSASigner();
        signer.init(true, privateKey);
        BigInteger[] signature = signer.generateSignature(signatureBytes);
        ByteBuffer bb = ByteBuffer.allocate(64);
        bb.put(to32BytesArray(signature[0]));
        bb.put(to32BytesArray(signature[1]));
        return bb.array();
    }

    private static byte[] to32BytesArray(BigInteger bi) {
        byte[] array = bi.toByteArray();
        if (array[0] == 0) { //i.e. array.length = 33
            byte[] tmp = new byte[array.length - 1];
            System.arraycopy(array, 1, tmp, 0, tmp.length);
            array = tmp;
        }
        if (array.length == 31) {
            byte[] tmp = new byte[array.length + 1];
            tmp[0] = 0;
            System.arraycopy(array, 0, tmp, 1, array.length);
            array = tmp;
        }
        return array;
    }


    public static void verify(String signerFinId, byte[] signature, byte[] payload) throws SignatureException {
        verify(Hex.decode(signerFinId), signature, payload);
    }

    public static void verify(ECPublicKeyParameters signerPublicKey, byte[] signature, byte[] payload) throws SignatureException {
        verify(signerPublicKey.getQ().getEncoded(false), signature, payload);
    }

    public static void verify(byte[] signerPubKeyBytes, byte[] signature, byte[] payload) throws SignatureException {
        ECNamedCurveParameterSpec params = new ECNamedCurveParameterSpec(SECP256K1_CURVE, curve.getCurve(), curve.getG(), curve.getN());
        ECPoint publicKeyPoint = curve.getCurve().decodePoint(signerPubKeyBytes);

        byte[] rBytes = Arrays.copyOfRange(signature, 0, 32);
        byte[] sBytes = Arrays.copyOfRange(signature, 32, 64);

        ECDSASigner signer = new ECDSASigner();
        ECPublicKeyParameters publicKeyParameters = new ECPublicKeyParameters(publicKeyPoint, new ECDomainParameters(params.getCurve(), params.getG(), params.getN()));
        signer.init(false, publicKeyParameters);

        boolean valid = signer.verifySignature(
                payload,
                new BigInteger(1, rBytes),
                new BigInteger(1, sBytes)
        );

        if (!valid) {
            throw new SignatureException("Failed to verify signature");
        }
    }


    /**
     * Corda only supports SHA2-256 in 4.6
     */
    public static byte[] SHA3(byte[] payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA3-256");
            return digest.digest(payload);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unable to generate SHA3 hash", e);
        }
    }

    private static final Random random = new Random();

    /**
     * Rather than using default ASN.1 encoding, we extract and return the raw public key.
     */
    public static byte[] generateNonce() {
        byte[] bytes = new byte[24];
        random.nextBytes(bytes);
        return bytes;
    }

    public static AsymmetricCipherKeyPair generateKeyPair() {
        ECKeyPairGenerator generator = new ECKeyPairGenerator();
        ECDomainParameters domain = new ECDomainParameters(curve.getCurve(), curve.getG(), curve.getN(), curve.getH(), curve.getSeed());
        ECKeyGenerationParameters keygenParams = new ECKeyGenerationParameters(domain, new SecureRandom());
        generator.init(keygenParams);
        return generator.generateKeyPair();
    }

    public static AsymmetricCipherKeyPair parseKey(String base64Key) throws IOException {
        byte[] keyBytes = Base64.decode(base64Key);

        ECPrivateKeyParameters privateKey = (ECPrivateKeyParameters) PrivateKeyFactory.createKey(keyBytes);
        ECDomainParameters domain = new ECDomainParameters(curve.getCurve(), curve.getG(), curve.getN(), curve.getH(), curve.getSeed());
        ECPoint q = domain.getG().multiply(privateKey.getD());
        ECPublicKeyParameters publicKey = new ECPublicKeyParameters(q, domain);
        return new AsymmetricCipherKeyPair(publicKey, privateKey);
    }


    public static String toFinId(ECPublicKeyParameters key) {
        return Hex.toHexString(key.getQ().getEncoded(false));
    }

}
