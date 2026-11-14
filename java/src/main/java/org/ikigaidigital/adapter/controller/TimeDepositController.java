package org.ikigaidigital.adapter.controller;

import org.ikigaidigital.adapter.controller.dto.TimeDepositResponse;
import org.ikigaidigital.application.TimeDepositService;
import org.springframework.web.bind.annotation.GetMapping;
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
}
