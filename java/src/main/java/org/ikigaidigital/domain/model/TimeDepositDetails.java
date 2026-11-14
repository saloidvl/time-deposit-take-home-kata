package org.ikigaidigital.domain.model;

import org.ikigaidigital.TimeDeposit;

import java.util.List;

// TimeDeposit has no withdrawals field and must not change, so the read model combines both (D22).
public record TimeDepositDetails(TimeDeposit deposit, List<Withdrawal> withdrawals) {
}
