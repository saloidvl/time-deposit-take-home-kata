package org.ikigaidigital;

import org.ikigaidigital.domain.model.PlanType;
import org.ikigaidigital.domain.policy.BasicInterestPolicy;
import org.ikigaidigital.domain.policy.InterestPolicy;
import org.ikigaidigital.domain.policy.PremiumInterestPolicy;
import org.ikigaidigital.domain.policy.StudentInterestPolicy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Adds one month of interest to time deposits. The rule of each plan lives in its {@link InterestPolicy};
 * this class only selects the policy and rounds the result.
 */
public class TimeDepositCalculator {
    private final Map<PlanType, InterestPolicy> policies;

    /** Uses the policies of the existing plans; a new plan's policy is registered here. */
    public TimeDepositCalculator() {
        this(List.of(new BasicInterestPolicy(), new StudentInterestPolicy(), new PremiumInterestPolicy()));
    }

    public TimeDepositCalculator(List<InterestPolicy> policies) {
        this.policies = policies.stream()
                .collect(Collectors.toMap(InterestPolicy::planType, Function.identity()));
    }

    public void updateBalance(List<TimeDeposit> timeDeposits) {
        for (TimeDeposit deposit : timeDeposits) {
            // A plan without a policy, such as an unknown code, earns no interest (A2).
            InterestPolicy policy = policies.get(PlanType.fromCode(deposit.getPlanType()));
            double interest = policy == null ? 0 : policy.monthlyInterest(deposit);

            deposit.setBalance(deposit.getBalance() + roundToCents(interest));
        }
    }

    // Interest is rounded to cents on its exact double value and added to the balance as a double (F3).
    private static double roundToCents(double interest) {
        return new BigDecimal(interest).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
