package org.ikigaidigital.domain.policy;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.domain.model.PlanType;

public final class StudentInterestPolicy implements InterestPolicy {

    @Override
    public PlanType planType() {
        return PlanType.STUDENT;
    }

    @Override
    public double monthlyInterest(TimeDeposit deposit) {
        // Student earns interest after the 30-day grace period and only until day 365 (A8).
        if (deposit.getDays() > 30 && deposit.getDays() < 366) {
            return deposit.getBalance() * 0.03 / 12;
        }
        return 0;
    }
}
