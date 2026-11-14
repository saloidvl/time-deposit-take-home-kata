package org.ikigaidigital.adapter.persistence;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.application.port.TimeDepositRepository;
import org.ikigaidigital.domain.model.TimeDepositDetails;
import org.ikigaidigital.domain.model.Withdrawal;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Rows are mapped straight to domain models, with no persistence model classes (D18).
 */
@Repository
class JdbcTimeDepositRepository implements TimeDepositRepository {

    private static final String SELECT_DEPOSITS = "SELECT id, \"planType\", balance, days FROM \"timeDeposits\" ORDER BY id";

    private static final RowMapper<TimeDeposit> DEPOSIT_MAPPER = (rs, rowNum) -> new TimeDeposit(
            rs.getInt("id"),
            rs.getString("planType"),
            // numeric to double on read, storage never rounds (D7, D9).
            rs.getBigDecimal("balance").doubleValue(),
            rs.getInt("days"));

    private final JdbcTemplate jdbcTemplate;
    private final JdbcClient jdbc;

    // JdbcClient has no batch API, so the JdbcTemplate it wraps is kept for the batched update.
    JdbcTimeDepositRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.jdbc = JdbcClient.create(jdbcTemplate);
    }

    @Override
    public List<TimeDeposit> findAllDeposits() {
        return jdbc.sql(SELECT_DEPOSITS).query(DEPOSIT_MAPPER).list();
    }

    // Only the balance is written: planType and days are maintained outside this service (A4). One batch avoids a round trip per deposit.
    @Override
    public void updateBalances(List<TimeDeposit> deposits) {
        jdbcTemplate.batchUpdate(
                "UPDATE \"timeDeposits\" SET balance = ? WHERE id = ?",
                deposits,
                deposits.size(),
                (ps, deposit) -> {
                    // double to numeric through the shortest decimal form, so nothing is rounded (D7, D9).
                    ps.setBigDecimal(1, BigDecimal.valueOf(deposit.getBalance()));
                    ps.setInt(2, deposit.getId());
                });
    }

    // Two queries and grouping in Java avoid one query per deposit; the ordering is part of the output (A9).
    @Override
    public List<TimeDepositDetails> findAllWithWithdrawals() {
        List<TimeDeposit> deposits = findAllDeposits();

        Map<Integer, List<Withdrawal>> withdrawalsByDeposit = new HashMap<>();
        jdbc.sql("SELECT id, \"timeDepositId\", amount, date FROM withdrawals ORDER BY date, id")
                .query(rs -> {
                    withdrawalsByDeposit
                            .computeIfAbsent(rs.getInt("timeDepositId"), key -> new ArrayList<>())
                            .add(new Withdrawal(
                                    rs.getInt("id"),
                                    rs.getBigDecimal("amount"),
                                    rs.getObject("date", LocalDate.class)));
                });

        return deposits.stream()
                .map(deposit -> new TimeDepositDetails(deposit, withdrawalsByDeposit.getOrDefault(deposit.getId(), List.of())))
                .toList();
    }
}
