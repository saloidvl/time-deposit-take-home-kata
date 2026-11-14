package org.ikigaidigital.adapter.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.ikigaidigital.adapter.controller.dto.TimeDepositResponse;
import org.ikigaidigital.application.TimeDepositService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class TimeDepositController {

    private final TimeDepositService service;

    public TimeDepositController(TimeDepositService service) {
        this.service = service;
    }

    @Operation(summary = "Get all time deposits with their withdrawals")
    @GetMapping(value = "/time-deposits", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<TimeDepositResponse> getTimeDeposits() {
        return service.getAll().stream().map(TimeDepositApiMapper::toResponse).toList();
    }

    // POST and not PUT, because the update is not idempotent: every call accrues one more month (A3, D23).
    @Operation(summary = "Accrue one month of interest on all time deposits; not idempotent")
    @PostMapping("/time-deposits/balance-updates")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateBalances() {
        service.updateAllBalances();
    }
}
