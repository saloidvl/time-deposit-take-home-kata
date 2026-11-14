package org.ikigaidigital.domain.policy;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.domain.model.PlanType;

public final class BasicInterestPolicy implements InterestPolicy {

    @Override
    public PlanType planType() {
        return PlanType.BASIC;
    }

    @Override
    public double monthlyInterest(TimeDeposit deposit) {
        // Basic earns interest after the 30-day grace period, with no upper limit (A8).
        if (deposit.getDays() > 30) {
            return deposit.getBalance() * 0.01 / 12;
        }
        return 0;
    }
}
