package io.ownera.ledger.adapter.sample;

import io.ownera.ledger.adapter.service.model.OperationStatus;
import io.ownera.ledger.adapter.service.workflow.OperationRecord;
import io.ownera.ledger.adapter.service.workflow.OperationStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import javax.annotation.Nullable;
import java.sql.ResultSet;
import java.util.List;

public class JdbcOperationStore implements OperationStore {

    private final JdbcTemplate jdbc;

    public JdbcOperationStore(JdbcTemplate jdbc, com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        this.jdbc = jdbc;
    }

    @Override
    @Nullable
    public OperationRecord findByInputsHash(String inputsHash) {
        List<OperationRecord> results = jdbc.query(
                "SELECT cid, method, status, inputs_hash FROM operation_records WHERE inputs_hash = ?",
                ROW_MAPPER, inputsHash);
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public void save(OperationRecord record) {
        jdbc.update(
                "INSERT INTO operation_records (cid, method, status, inputs_hash, result_json) VALUES (?, ?, ?, ?, ?)",
                record.cid, record.method, record.status.name(), record.inputsHash, null);
    }

    @Override
    public void updateStatus(String cid, OperationRecord.Status status, @Nullable OperationStatus result) {
        jdbc.update(
                "UPDATE operation_records SET status = ? WHERE cid = ?",
                status.name(), cid);
    }

    @Override
    @Nullable
    public OperationRecord findByCid(String cid) {
        List<OperationRecord> results = jdbc.query(
                "SELECT cid, method, status, inputs_hash FROM operation_records WHERE cid = ?",
                ROW_MAPPER, cid);
        return results.isEmpty() ? null : results.get(0);
    }

    private static final RowMapper<OperationRecord> ROW_MAPPER = (ResultSet rs, int rowNum) -> new OperationRecord(
            rs.getString("cid"),
            rs.getString("method"),
            OperationRecord.Status.valueOf(rs.getString("status")),
            rs.getString("inputs_hash"),
            null
    );
}
