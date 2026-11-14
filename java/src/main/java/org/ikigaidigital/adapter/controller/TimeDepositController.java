package org.ikigaidigital.adapter.controller;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.ikigaidigital.adapter.controller.dto.TimeDepositResponse;
import org.ikigaidigital.application.TimeDepositService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// The REST adapter owns the API contract; the relative server URL keeps the generated document the same on any host (D12).
@OpenAPIDefinition(
        info = @Info(title = "XA Bank Time Deposits API", version = "1.0",
                description = "Reads time deposits and runs the monthly balance update."),
        servers = @Server(url = "/"))
@Tag(name = "Time deposits")
@RestController
public class TimeDepositController {

    private final TimeDepositService service;

    public TimeDepositController(TimeDepositService service) {
        this.service = service;
    }

    @Operation(operationId = "getTimeDeposits", summary = "Get all time deposits",
            description = "Returns every time deposit with its withdrawals. Deposits are ordered by id, withdrawals by date and then id.")
    @ApiResponse(responseCode = "200", description = "All time deposits")
    @GetMapping(value = "/time-deposits", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<TimeDepositResponse> getAll() {
        return service.getAll().stream().map(TimeDepositApiMapper::toResponse).toList();
    }

    // No body in or out, and not idempotent: every call accrues one more month (A3, D23).
    @Operation(operationId = "updateBalances", summary = "Update the balance of all time deposits",
            description = "Accrues one month of interest on every eligible deposit according to its plan. "
                    + "Not idempotent: each call accrues one more month of interest on the new balance, so call it once a month.")
    @ApiResponse(responseCode = "204", description = "Balances updated")
    @PostMapping("/time-deposits/balance-updates")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateBalances() {
        service.updateAllBalances();
    }
}
