package io.ownera.finp2p.signing.eip712.models;

public class TypeField {
    private final String name;
    private final String type;

    public TypeField(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }
}
