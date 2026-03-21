package io.ownera.finp2p.signing.eip712.models;

import io.ownera.finp2p.signing.eip712.Message;

import java.util.List;
import java.util.Map;

public class EIP712Message {
    private final Domain domain;
    private final Message message;
    private final String primaryType;
    private final Map<String, List<TypeField>> types;

    public EIP712Message(Domain domain, Message message, String primaryType, Map<String, List<TypeField>> types) {
        this.domain = domain;
        this.message = message;
        this.primaryType = primaryType;
        this.types = types;
    }

    public Domain getDomain() {
        return domain;
    }

    public Message getMessage() {
        return message;
    }

    public String getPrimaryType() {
        return primaryType;
    }

    public Map<String, List<TypeField>> getTypes() {
        return types;
    }
}
