package org.ikigaidigital.application;

import org.ikigaidigital.application.port.TimeDepositRepository;
import org.ikigaidigital.domain.model.TimeDepositDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Inbound port: the use cases are the public methods of this class, with no interface because it would have one implementation and one caller (D13).
 */
@Service
public class TimeDepositService {

    private final TimeDepositRepository repository;

    public TimeDepositService(TimeDepositRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<TimeDepositDetails> getAll() {
        return repository.findAllWithWithdrawals();
    }
}
