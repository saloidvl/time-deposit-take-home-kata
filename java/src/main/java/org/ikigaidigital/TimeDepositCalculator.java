package org.ikigaidigital;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.ikigaidigital.domain.model.PlanType;
import org.ikigaidigital.domain.policy.BasicInterestPolicy;
import org.ikigaidigital.domain.policy.InterestPolicy;
import org.ikigaidigital.domain.policy.PremiumInterestPolicy;
import org.ikigaidigital.domain.policy.StudentInterestPolicy;

public class TimeDepositCalculator {
    private final Map<PlanType, InterestPolicy> policies;

    // The policies are listed here instead of being injected, so that new TimeDepositCalculator()
    // keeps working for existing callers (D19).
    public TimeDepositCalculator() {
        this.policies = Stream.of(
                new BasicInterestPolicy(),
                new StudentInterestPolicy(),
                new PremiumInterestPolicy())
            .collect(Collectors.toMap(InterestPolicy::planType, Function.identity()));
    }

    public void updateBalance(List<TimeDeposit> timeDeposits) {
        for (TimeDeposit deposit : timeDeposits) {
            // An unknown plan code has no policy and earns nothing, as in the legacy code (A2).
            InterestPolicy policy = policies.get(PlanType.fromCode(deposit.getPlanType()));
            if (policy == null) continue;

            double interest = roundToCents(policy.monthlyInterest(deposit));
            deposit.setBalance(deposit.getBalance() + interest);
        }
    }

    // The legacy rounding must be kept
    private static double roundToCents(double interest) {
        return new BigDecimal(interest).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
