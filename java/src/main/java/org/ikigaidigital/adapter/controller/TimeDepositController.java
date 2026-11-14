package org.ikigaidigital.adapter.controller;

import org.ikigaidigital.adapter.controller.dto.TimeDepositResponse;
import org.ikigaidigital.application.TimeDepositService;
import org.springframework.http.HttpStatus;
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

    @GetMapping("/time-deposits")
    public List<TimeDepositResponse> getAll() {
        return service.getAll().stream().map(TimeDepositResponseMapper::toResponse).toList();
    }

    // No body in or out, and not idempotent: every call accrues one more month (A3, D23).
    @PostMapping("/time-deposits/balance-updates")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateBalances() {
        service.updateAllBalances();
    }
}
