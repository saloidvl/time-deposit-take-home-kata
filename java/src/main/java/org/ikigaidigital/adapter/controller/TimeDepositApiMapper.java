package org.ikigaidigital.adapter.controller;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.adapter.controller.dto.TimeDepositResponse;
import org.ikigaidigital.adapter.controller.dto.WithdrawalResponse;
import org.ikigaidigital.domain.model.TimeDepositDetails;
import org.ikigaidigital.domain.model.Withdrawal;

import java.math.BigDecimal;

final class TimeDepositApiMapper {

    private TimeDepositApiMapper() {
    }

    static TimeDepositResponse toResponse(TimeDepositDetails details) {
        TimeDeposit deposit = details.deposit();
        return new TimeDepositResponse(
                deposit.getId(),
                deposit.getPlanType(),
                BigDecimal.valueOf(deposit.getBalance()),
                deposit.getDays(),
                details.withdrawals().stream().map(TimeDepositApiMapper::toResponse).toList());
    }

    private static WithdrawalResponse toResponse(Withdrawal withdrawal) {
        return new WithdrawalResponse(withdrawal.id(), withdrawal.amount(), withdrawal.date());
    }
}
