package org.ikigaidigital.application.port;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.domain.model.TimeDepositDetails;

import java.util.List;

public interface TimeDepositRepository {

    List<TimeDeposit> findAllDeposits();

    List<TimeDepositDetails> findAllWithWithdrawals();

    /** Writes only the balance of each deposit, matched by id. */
    void updateBalances(List<TimeDeposit> deposits);
}
