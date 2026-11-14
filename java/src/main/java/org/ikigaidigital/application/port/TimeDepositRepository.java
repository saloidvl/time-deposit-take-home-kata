package org.ikigaidigital.application.port;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.domain.model.TimeDepositDetails;

import java.util.List;

public interface TimeDepositRepository {

    List<TimeDeposit> findAllDeposits();

    List<TimeDepositDetails> findAllWithWithdrawals();

    void updateBalances(List<TimeDeposit> deposits);
}
