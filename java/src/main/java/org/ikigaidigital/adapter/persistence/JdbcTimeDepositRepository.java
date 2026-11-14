package org.ikigaidigital.adapter.persistence;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.application.port.TimeDepositRepository;
import org.ikigaidigital.domain.model.TimeDepositDetails;
import org.ikigaidigital.domain.model.Withdrawal;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

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

    private final JdbcClient jdbc;

    JdbcTimeDepositRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    // Two queries and grouping in Java avoid one query per deposit; the ordering is part of the output (A9).
    @Override
    public List<TimeDepositDetails> findAllWithWithdrawals() {
        List<TimeDeposit> deposits = jdbc
                .sql("SELECT id, \"planType\", balance, days FROM \"timeDeposits\" ORDER BY id")
                .query((rs, rowNum) -> new TimeDeposit(
                        rs.getInt("id"),
                        rs.getString("planType"),
                        // numeric to double on read, storage never rounds (D7, D9).
                        rs.getBigDecimal("balance").doubleValue(),
                        rs.getInt("days")))
                .list();

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
