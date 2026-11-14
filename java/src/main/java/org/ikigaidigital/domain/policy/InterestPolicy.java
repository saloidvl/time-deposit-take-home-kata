package org.ikigaidigital.domain.policy;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.domain.model.PlanType;

// Each plan owns its whole eligibility window, including its grace period: a future plan may have another one (A8).
public interface InterestPolicy {
    int MONTHS_IN_YEAR = 12;

    PlanType planType();

    // Returns the unrounded interest: the calculator rounds, so every plan is rounded the same way (F3).
    double monthlyInterest(TimeDeposit deposit);
}
