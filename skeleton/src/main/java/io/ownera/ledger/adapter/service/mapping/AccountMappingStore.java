package io.ownera.ledger.adapter.service.mapping;

import java.util.List;

public interface AccountMappingStore {

    List<AccountMapping> getByFinId(String finId);

    List<AccountMapping> getByAccount(String account);

    AccountMapping save(String finId, String account);

    void delete(String finId, String account);

    List<AccountMapping> listAll();
}
