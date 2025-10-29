package io.ownera.ledger.adapter.service.model;


import io.ownera.finp2p.signing.eip712.models.Domain;
import io.ownera.finp2p.signing.eip712.Message;
import io.ownera.finp2p.signing.eip712.models.TypeField;

import java.util.List;
import java.util.Map;

public class EIP712Template implements SignatureTemplate {

    public final String primaryType;
    public final Domain domain;
    public final Message message;
    public final Map<String, List<TypeField>> types;
    public final String hash;

    public EIP712Template(String primaryType, Domain domain, Message message, Map<String, List<TypeField>> types, String hash) {
        this.primaryType = primaryType;
        this.domain = domain;
        this.message = message;
        this.types = types;
        this.hash = hash;
    }
}
