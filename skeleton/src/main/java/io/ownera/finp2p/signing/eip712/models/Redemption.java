package io.ownera.finp2p.signing.eip712.models;

import io.ownera.finp2p.signing.eip712.Message;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Redemption implements Message {

    public static final String TYPE_NAME = "Redemption";

    private static final Map<String, List<TypeField>> TYPES;

    static {
        Map<String, List<TypeField>> types = new HashMap<>();
        types.putAll(MessageTypes.DOMAIN_TYPE);
        types.putAll(MessageTypes.FINID_TYPE);
        types.putAll(MessageTypes.TERM_TYPE);
        types.put(TYPE_NAME, List.of(
                new TypeField("nonce", "string"),
                new TypeField("seller", "FinId"),
                new TypeField("issuer", "FinId"),
                new TypeField("asset", "Term"),
                new TypeField("settlement", "Term")
        ));
        TYPES = Map.copyOf(types);
    }

    private String nonce;
    private FinId seller;
    private FinId issuer;
    private Term asset;
    private Term settlement;

    public Redemption() {
    }

    public Redemption(String nonce, FinId seller, FinId issuer, Term asset, Term settlement) {
        this.nonce = nonce;
        this.seller = seller;
        this.issuer = issuer;
        this.asset = asset;
        this.settlement = settlement;
    }

    public String getNonce() {
        return nonce;
    }

    public Redemption setNonce(String nonce) {
        this.nonce = nonce;
        return this;
    }

    public FinId getSeller() {
        return seller;
    }

    public Redemption setSeller(FinId seller) {
        this.seller = seller;
        return this;
    }

    public FinId getIssuer() {
        return issuer;
    }

    public Redemption setIssuer(FinId issuer) {
        this.issuer = issuer;
        return this;
    }

    public Term getAsset() {
        return asset;
    }

    public Redemption setAsset(Term asset) {
        this.asset = asset;
        return this;
    }

    public Term getSettlement() {
        return settlement;
    }

    public Redemption setSettlement(Term settlement) {
        this.settlement = settlement;
        return this;
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
