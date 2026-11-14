package org.ikigaidigital;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.ikigaidigital.domain.model.PlanType;
import org.ikigaidigital.domain.policy.BasicInterestPolicy;
import org.ikigaidigital.domain.policy.InterestPolicy;
import org.ikigaidigital.domain.policy.PremiumInterestPolicy;
import org.ikigaidigital.domain.policy.StudentInterestPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimeDepositCalculator {
    private static final Logger log = LoggerFactory.getLogger(TimeDepositCalculator.class);

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
            // The legacy code failed on these nulls with an implicit NullPointerException; now it is explicit (A10).
            Objects.requireNonNull(deposit.getPlanType(), "planType");
            Objects.requireNonNull(deposit.getBalance(), "balance");

            if (deposit.getBalance() < 0) {
                log.warn("Time deposit {} has a negative balance {}: interest is calculated on it as is",
                        deposit.getId(), deposit.getBalance());
            }

            double interest = roundToCents(monthlyInterest(deposit));
            deposit.setBalance(deposit.getBalance() + interest);
        }
    }

    private double monthlyInterest(TimeDeposit deposit) {
        InterestPolicy policy = policies.get(PlanType.fromCode(deposit.getPlanType()));
        if (policy == null) {
            // An unknown plan code has no policy and earns nothing, as in the legacy code (A2).
            log.warn("Time deposit {} has an unknown plan type '{}': no interest is accrued",
                    deposit.getId(), deposit.getPlanType());
            return 0;
        }
        return policy.monthlyInterest(deposit);
    }

    // The legacy rounding must be kept
    private static double roundToCents(double interest) {
        return new BigDecimal(interest).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
