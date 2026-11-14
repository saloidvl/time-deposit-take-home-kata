package org.ikigaidigital.domain.policy;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.domain.model.PlanType;

public final class PremiumInterestPolicy implements InterestPolicy {

    private static final int GRACE_PERIOD_DAYS = 45;
    private static final double ANNUAL_RATE = 0.05;

    @Override
    public PlanType planType() {
        return PlanType.PREMIUM;
    }

    @Override
    public double monthlyInterest(TimeDeposit deposit) {
        if (deposit.getDays() > GRACE_PERIOD_DAYS) {
            return deposit.getBalance() * ANNUAL_RATE / MONTHS_IN_YEAR;
        }
        return 0;
    }
}
