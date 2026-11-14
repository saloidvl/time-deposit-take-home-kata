package org.ikigaidigital.adapter.controller;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.adapter.controller.dto.TimeDepositResponse;
import org.ikigaidigital.adapter.controller.dto.WithdrawalResponse;
import org.ikigaidigital.domain.model.TimeDepositDetails;

import java.math.BigDecimal;

final class TimeDepositResponseMapper {

    private TimeDepositResponseMapper() {
    }

    static TimeDepositResponse toResponse(TimeDepositDetails details) {
        TimeDeposit deposit = details.deposit();
        return new TimeDepositResponse(
                deposit.getId(),
                deposit.getPlanType(),
                // double to BigDecimal only at the boundary, shortest decimal form (D7).
                BigDecimal.valueOf(deposit.getBalance()),
                deposit.getDays(),
                details.withdrawals().stream()
                        .map(w -> new WithdrawalResponse(w.id(), w.amount(), w.date()))
                        .toList());
    }
}
