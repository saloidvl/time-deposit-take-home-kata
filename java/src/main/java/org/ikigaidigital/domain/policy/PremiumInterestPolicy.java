package org.ikigaidigital.domain.policy;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.domain.model.PlanType;

public final class PremiumInterestPolicy implements InterestPolicy {

    @Override
    public PlanType planType() {
        return PlanType.PREMIUM;
    }

    @Override
    public double monthlyInterest(TimeDeposit deposit) {
        // Premium has a longer grace period: interest starts after day 45 (A8).
        if (deposit.getDays() > 45) {
            return deposit.getBalance() * 0.05 / 12;
        }
        return 0;
    }
}
