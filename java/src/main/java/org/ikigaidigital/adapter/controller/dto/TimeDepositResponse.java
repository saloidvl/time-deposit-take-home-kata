package org.ikigaidigital.adapter.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

public record TimeDepositResponse(
        @Schema(requiredMode = REQUIRED) int id,
        @Schema(requiredMode = REQUIRED) String planType,
        @Schema(requiredMode = REQUIRED, description = "Current balance; already reflects withdrawals") BigDecimal balance,
        @Schema(requiredMode = REQUIRED, description = "Age of the deposit in days; maintained outside this service") int days,
        @Schema(requiredMode = REQUIRED, description = "Withdrawals made from this deposit") List<WithdrawalResponse> withdrawals) {
}
