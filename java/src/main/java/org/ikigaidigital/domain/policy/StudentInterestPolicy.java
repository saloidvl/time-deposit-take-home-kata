package org.ikigaidigital.domain.policy;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.domain.model.PlanType;

public final class StudentInterestPolicy implements InterestPolicy {

    private static final int GRACE_PERIOD_DAYS = 30;
    private static final int FIRST_DAY_WITHOUT_INTEREST = 366;
    private static final double ANNUAL_RATE = 0.03;

    @Override
    public PlanType planType() {
        return PlanType.STUDENT;
    }

    @Override
    public double monthlyInterest(TimeDeposit deposit) {
        if (deposit.getDays() > GRACE_PERIOD_DAYS && deposit.getDays() < FIRST_DAY_WITHOUT_INTEREST) {
            return deposit.getBalance() * ANNUAL_RATE / MONTHS_IN_YEAR;
        }
        return 0;
    }
}
