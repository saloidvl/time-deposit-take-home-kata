package org.ikigaidigital.domain.policy;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.domain.model.PlanType;

/** Interest rule of one plan. */
public interface InterestPolicy {

    /** The plan this policy applies to; never {@link PlanType#UNKNOWN}. */
    PlanType planType();

    /** Raw, unrounded interest for one month; 0 when the deposit is not eligible. */
    double monthlyInterest(TimeDeposit deposit);
}
