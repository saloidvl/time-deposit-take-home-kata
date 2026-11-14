package org.ikigaidigital.adapter.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

public record WithdrawalResponse(
        @Schema(requiredMode = REQUIRED) int id,
        @Schema(requiredMode = REQUIRED) BigDecimal amount,
        @Schema(requiredMode = REQUIRED) LocalDate date) {
}
