package io.ownera.finp2p.signing.eip712;

import io.ownera.finp2p.signing.eip712.models.TypeField;

import java.util.List;
import java.util.Map;

public interface Message {

    String getTypeName();
    Map<String, List<TypeField>> getTypes();
}
