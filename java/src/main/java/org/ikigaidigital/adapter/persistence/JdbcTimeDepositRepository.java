package org.ikigaidigital.adapter.persistence;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.application.port.TimeDepositRepository;
import org.ikigaidigital.domain.model.TimeDepositDetails;
import org.ikigaidigital.domain.model.Withdrawal;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
class JdbcTimeDepositRepository implements TimeDepositRepository {

    // The ordering of both queries is part of the API output (A9).
    private static final String SELECT_DEPOSITS =
            "SELECT id, \"planType\", balance, days FROM \"timeDeposits\" ORDER BY id";
    private static final String SELECT_WITHDRAWALS =
            "SELECT id, \"timeDepositId\", amount, date FROM withdrawals ORDER BY date, id";
    // Only the balance is written: planType and days are maintained outside this service (A4).
    private static final String UPDATE_BALANCE =
            "UPDATE \"timeDeposits\" SET balance = ? WHERE id = ?";

    // Bounds one JDBC batch for a large number of deposits; all batches run in the caller's single transaction (D21).
    private static final int BATCH_SIZE = 1000;

    private final JdbcTemplate jdbcTemplate;

    JdbcTimeDepositRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<TimeDeposit> findAllDeposits() {
        return jdbcTemplate.query(SELECT_DEPOSITS, this::toTimeDeposit);
    }

    @Override
    public List<TimeDepositDetails> findAllWithWithdrawals() {
        Map<Integer, List<Withdrawal>> withdrawalsByDepositId = findWithdrawalsByDepositId();

        return findAllDeposits().stream()
                .map(deposit -> new TimeDepositDetails(
                        deposit, withdrawalsByDepositId.getOrDefault(deposit.getId(), List.of())))
                .toList();
    }

    @Override
    public void updateBalances(List<TimeDeposit> deposits) {
        jdbcTemplate.batchUpdate(UPDATE_BALANCE, deposits, BATCH_SIZE, this::setBalanceAndId);
    }

    private Map<Integer, List<Withdrawal>> findWithdrawalsByDepositId() {
        return jdbcTemplate.query(SELECT_WITHDRAWALS, this::toWithdrawal).stream()
                .collect(Collectors.groupingBy(Withdrawal::timeDepositId));
    }

    private TimeDeposit toTimeDeposit(ResultSet rs, int rowNum) throws SQLException {
        return new TimeDeposit(
                rs.getInt("id"),
                rs.getString("planType"),
                rs.getBigDecimal("balance").doubleValue(),
                rs.getInt("days"));
    }

    private Withdrawal toWithdrawal(ResultSet rs, int rowNum) throws SQLException {
        return new Withdrawal(
                rs.getInt("id"),
                rs.getInt("timeDepositId"),
                rs.getBigDecimal("amount"),
                rs.getObject("date", LocalDate.class));
    }

    // BigDecimal.valueOf keeps the double exactly: rounding on save would change later interest (D9).
    private void setBalanceAndId(PreparedStatement ps, TimeDeposit deposit) throws SQLException {
        ps.setBigDecimal(1, BigDecimal.valueOf(deposit.getBalance()));
        ps.setInt(2, deposit.getId());
    }
}
