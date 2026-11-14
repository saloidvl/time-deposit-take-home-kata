package org.ikigaidigital.adapter.controller.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record WithdrawalResponse(int id, BigDecimal amount, LocalDate date) {
}
