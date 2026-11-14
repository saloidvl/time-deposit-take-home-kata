package org.ikigaidigital.application.port;

import org.ikigaidigital.domain.model.TimeDepositDetails;

import java.util.List;

public interface TimeDepositRepository {

    List<TimeDepositDetails> findAllWithWithdrawals();
}
