package io.ownera.finp2p.signing.eip712.models;

import io.ownera.finp2p.signing.eip712.Message;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Buying implements Message {

    public static final String TYPE_NAME = "Buying";

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

    private String nonce;
    private FinId buyer;
    private FinId seller;
    private Term asset;
    private Term settlement;

    public Buying() {}

    public Buying(String nonce, FinId buyer, FinId seller, Term asset, Term settlement) {
        this.nonce = nonce;
        this.buyer = buyer;
        this.seller = seller;
        this.asset = asset;
        this.settlement = settlement;
    }

    public String getNonce() {
        return nonce;
    }

    public Buying setNonce(String nonce) {
        this.nonce = nonce;
        return this;
    }

    public FinId getBuyer() {
        return buyer;
    }

    public Buying setBuyer(FinId buyer) {
        this.buyer = buyer;
        return this;
    }

    public FinId getSeller() {
        return seller;
    }

    public Buying setSeller(FinId seller) {
        this.seller = seller;
        return this;
    }

    public Term getAsset() {
        return asset;
    }

    public Buying setAsset(Term asset) {
        this.asset = asset;
        return this;
    }

    public Term getSettlement() {
        return settlement;
    }

    public Buying setSettlement(Term settlement) {
        this.settlement = settlement;
        return this;
    }

    @Override
    public String getTypeName() { return TYPE_NAME; }

    @Override
    public Map<String, List<TypeField>> getTypes() { return TYPES; }
}
