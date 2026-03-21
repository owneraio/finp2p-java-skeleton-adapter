package io.ownera.finp2p.signing.eip712.models;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PrivateOffer extends Buying {

    public static final String TYPE_NAME = "PrivateOffer";

    private static final Map<String, List<TypeField>> TYPES;

    static {
        Map<String, List<TypeField>> types = new HashMap<>();
        types.putAll(MessageTypes.DOMAIN_TYPE);
        types.putAll(MessageTypes.FINID_TYPE);
        types.putAll(MessageTypes.TERM_TYPE);
        types.put(TYPE_NAME, List.of(
                new TypeField("nonce", "string"),
                new TypeField("buyer", "FinId"),
                new TypeField("seller", "FinId"),
                new TypeField("asset", "Term"),
                new TypeField("settlement", "Term")
        ));
        TYPES = Map.copyOf(types);
    }

    public PrivateOffer() {
    }

    public PrivateOffer(String nonce, FinId buyer, FinId seller, Term asset, Term settlement) {
        super(nonce, buyer, seller, asset, settlement);
    }

    @Override
    public String getTypeName() {
        return TYPE_NAME;
    }

    @Override
    public Map<String, List<TypeField>> getTypes() {
        return TYPES;
    }
}
