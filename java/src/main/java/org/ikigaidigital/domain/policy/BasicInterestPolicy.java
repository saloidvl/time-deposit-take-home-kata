package org.ikigaidigital.domain.policy;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.domain.model.PlanType;

public final class BasicInterestPolicy implements InterestPolicy {

    private static final int GRACE_PERIOD_DAYS = 30;
    private static final double ANNUAL_RATE = 0.01;


    @Override
    public PlanType planType() {
        return PlanType.BASIC;
    }

    @Override
    public double monthlyInterest(TimeDeposit deposit) {
        if (deposit.getDays() > GRACE_PERIOD_DAYS) {
            return deposit.getBalance() * ANNUAL_RATE / MONTHS_IN_YEAR;
        }
        return 0;
    }
}
