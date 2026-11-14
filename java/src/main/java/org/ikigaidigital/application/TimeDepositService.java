package org.ikigaidigital.application;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.TimeDepositCalculator;
import org.ikigaidigital.application.port.TimeDepositRepository;
import org.ikigaidigital.domain.model.TimeDepositDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TimeDepositService {

    private static final Logger log = LoggerFactory.getLogger(TimeDepositService.class);

    private final TimeDepositRepository repository;
    private final TimeDepositCalculator calculator;

    public TimeDepositService(TimeDepositRepository repository, TimeDepositCalculator calculator) {
        this.repository = repository;
        this.calculator = calculator;
    }

    @Transactional(readOnly = true)
    public List<TimeDepositDetails> getAll() {
        List<TimeDepositDetails> deposits = repository.findAllWithWithdrawals();
        log.info("Returning {} time deposits", deposits.size());
        return deposits;
    }

    // No locking: the update is assumed never to run concurrently with itself (A11).
    @Transactional
    public void updateAllBalances() {
        List<TimeDeposit> deposits = repository.findAllDeposits();
        log.info("Updating the balances of {} time deposits", deposits.size());

        calculator.updateBalance(deposits);
        repository.updateBalances(deposits);

        log.info("Saved the balances of {} time deposits", deposits.size());
    }
}
