import com.fasterxml.jackson.databind.ObjectMapper;
import io.ownera.finp2p.signing.eip712.models.Domain;
import io.ownera.ledger.adapter.api.model.APIReceipt;
import io.ownera.ledger.adapter.service.model.*;
import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.UUID;

import static io.ownera.finp2p.signing.SignatureUtils.generateKeyPair;
import static io.ownera.finp2p.signing.SignatureUtils.sign;
import static io.ownera.finp2p.signing.eip712.EIP712.hashMessage;
import static io.ownera.ledger.adapter.Mappers.toAPI;
import static io.ownera.ledger.adapter.service.proof.Mapper.toEIP712;

public class LedgerProofTests {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    public void calculateProof() throws IOException {
        String id = "4ec0af25-0ff2-44fc-a31f-848f9a4efb5f";
        String assetId = "bank-us:102:3608c629-9d20-4226-ad8c-c65345604bc3";
        String buyerFinId = "02740564250c90966dfca9c7e121b885bad6159e9e506f6124cc9139da95ee73de";
        String quantity = "10.0";
        String transactionId = "2bd9d63c-3ef9-49eb-a6fa-60efd70e924b";
        String planId = "bank-us:106:98ddb9d4-0e41-4aee-9f45-f52f6c5e5b2f";
        long timestamp = System.currentTimeMillis();

        Receipt receipt = new Receipt(
                id,
                OperationType.ISSUE,
                new Asset(assetId, AssetType.FINP2P),
                new Source(buyerFinId, new FinIdAccount(buyerFinId)),
                new Destination(buyerFinId, new FinIdAccount(buyerFinId)),
                quantity,
                new TransactionDetails(transactionId, UUID.randomUUID().toString()),
                new TradeDetails(new ExecutionContext(planId, 1)),
                null,
                timestamp
        );

        io.ownera.finp2p.signing.eip712.models.Receipt rcp = toEIP712(receipt);
        byte[] hash = hashMessage(rcp);
        AsymmetricCipherKeyPair keyPair = generateKeyPair();
        ECPrivateKeyParameters signerPrivateKey = (ECPrivateKeyParameters) keyPair.getPrivate();
        String signature = Hex.encodeHexString(sign(signerPrivateKey, hash));
        EIP712Template template = new EIP712Template(
                rcp.getTypeName(),
                Domain.defaultDomain(),
                rcp,
                rcp.getTypes(),
                Hex.encodeHexString(hash)
        );

        HashFunction hashFunction = HashFunction.KECCAK_256;
        receipt.proof = new SignatureProofPolicy(
                hashFunction,
                template,
                signature
        );
        APIReceipt apiReceipt = toAPI(receipt);
        System.out.println(MAPPER.
//                writerWithDefaultPrettyPrinter().
                writeValueAsString(apiReceipt)
        );
    }
}
