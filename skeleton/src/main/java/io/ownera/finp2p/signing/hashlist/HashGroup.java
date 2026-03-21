package io.ownera.finp2p.signing.hashlist;

import org.apache.commons.codec.binary.Hex;

import java.util.List;

public class HashGroup {
    private byte[] hash;
    private List<HashField> fields;

    public HashGroup() {
    }

    public HashGroup(byte[] hash, List<HashField> fields) {
        this.hash = hash;
        this.fields = fields;
    }

    public byte[] getHash() {
        return hash;
    }

    public void setHash(byte[] hash) {
        this.hash = hash;
    }

    public List<HashField> getFields() {
        return fields;
    }

    public void setFields(List<HashField> fields) {
        this.fields = fields;
    }

    @Override
    public String toString() {
        return String.format("Hash: %s", Hex.encodeHexString(hash));
    }
}
