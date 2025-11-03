package io.ownera.ledger.adapter.sample.collateral;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

public class Certificate {
    @JsonProperty("info")
    private List<CertificateItem> info;

    public List<CertificateItem> getInfo() {
        return info;
    }

    public Certificate setInfo(List<CertificateItem> info) {
        this.info = info;
        return this;
    }

    public Certificate addInfoItem(String name, String value) {
        this.info.add(new CertificateItem(name, value));
        return this;
    }

    public ObjectMapper getMapper() {
        return mapper;
    }

    private final ObjectMapper mapper = new ObjectMapper();

    public String marshal() throws JsonProcessingException {
        return mapper.writeValueAsString(this);
    }
}
