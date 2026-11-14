package org.ikigaidigital;

import org.assertj.core.api.AutoCloseableSoftAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

// Pins the behaviour updateBalance had before the refactoring; expected values are calculated by hand.
class TimeDepositCalculatorLegacyBehaviourTest {

    private static final String BASIC = "basic";
    private static final String STUDENT = "student";
    private static final String PREMIUM = "premium";

    private final TimeDepositCalculator calculator = new TimeDepositCalculator();

    static Stream<Arguments> depositsWithoutInterest() {
        return Stream.of(
                arguments(BASIC, 29),
                arguments(BASIC, 30),
                arguments(STUDENT, 29),
                arguments(STUDENT, 30),
                arguments(STUDENT, 366),
                arguments(PREMIUM, 31),
                arguments(PREMIUM, 45),
                // plan codes are case-sensitive, and "gold" is not a plan
                arguments("Basic", 100),
                arguments("Student", 100),
                arguments("Premium", 100),
                arguments("gold", 100));
    }

    @ParameterizedTest(name = "{0}, day {1}")
    @MethodSource("depositsWithoutInterest")
    void updateBalance_depositNotEligible_balanceUnchanged(String planType, int days) {
        assertThat(balanceAfterUpdate(planType, 1000.0, days)).isEqualTo(1000.0);
    }

    static Stream<Arguments> depositsWithInterest() {
        return Stream.of(
                // 1000 * 1% / 12 = 0.8333 -> 0.83; day 366 shows basic has no upper day limit
                arguments(BASIC, 31, 1000.83),
                arguments(BASIC, 366, 1000.83),
                // 1000 * 3% / 12 = 2.5
                arguments(STUDENT, 31, 1002.5),
                arguments(STUDENT, 365, 1002.5),
                // 1000 * 5% / 12 = 4.1666 -> 4.17; day 366 shows premium has no upper day limit
                arguments(PREMIUM, 46, 1004.17),
                arguments(PREMIUM, 366, 1004.17));
    }

    @ParameterizedTest(name = "{0}, day {1}")
    @MethodSource("depositsWithInterest")
    void updateBalance_depositEligible_addsMonthlyInterest(String planType, int days, double expectedBalance) {
        assertThat(balanceAfterUpdate(planType, 1000.0, days)).isEqualTo(expectedBalance);
    }

    @Test
    void updateBalance_studentPlan_multipliesByRateBeforeDividingByTwelve() {
        // 10 * 0.03 / 12 = 0.025 -> 0.02 (a precomputed 0.03/12 factor would give 0.03)
        assertThat(balanceAfterUpdate(STUDENT, 10.0, 31)).isEqualTo(10.02);
    }

    @Test
    void updateBalance_premiumPlan_multipliesByRateBeforeDividingByTwelve() {
        // 42 * 0.05 / 12 = 0.175 -> 0.18 (a precomputed 0.05/12 factor would give 0.17)
        assertThat(balanceAfterUpdate(PREMIUM, 42.0, 46)).isEqualTo(42.18);
    }

    @Test
    void updateBalance_interestExactlyHalfCent_roundsUp() {
        // 150 * 1% / 12 = 0.125 exactly -> 0.13 (HALF_EVEN would give 0.12)
        assertThat(balanceAfterUpdate(BASIC, 150.0, 31)).isEqualTo(150.13);
    }

    @Test
    void updateBalance_decimalHalfCentStoredBelowHalfInBinary_roundsDown() {
        // 18 * 1% / 12 = 0.015 is stored as 0.01499999... -> 0.01 (BigDecimal.valueOf would give 0.02)
        assertThat(balanceAfterUpdate(BASIC, 18.0, 31)).isEqualTo(18.01);
    }

    @Test
    void updateBalance_interestRoundedNotNewBalance_balanceGetsRoundedInterest() {
        // 6 * 1% / 12 = 0.005 is stored just above half -> interest 0.01 -> 6.01
        // (rounding the new balance 6.005 instead would give 6.0)
        assertThat(balanceAfterUpdate(BASIC, 6.0, 31)).isEqualTo(6.01);
    }

    @Test
    void updateBalance_roundedInterestAddedAsDouble_keepsFloatingPointNoise() {
        // 6.02 + 0.01 is not exactly 6.03 in binary floating point (BigDecimal addition would give 6.03)
        assertThat(balanceAfterUpdate(BASIC, 6.02, 31)).isEqualTo(6.029999999999999);
    }

    @Test
    void updateBalance_depositsWithDifferentPlans_eachUsesItsOwnRulesAndIsUpdatedInPlace() {
        TimeDeposit basic = new TimeDeposit(1, BASIC, 1000.0, 31);
        TimeDeposit student = new TimeDeposit(2, STUDENT, 1000.0, 31);

        calculator.updateBalance(List.of(basic, student));

        try (AutoCloseableSoftAssertions softly = new AutoCloseableSoftAssertions()) {
            softly.assertThat(basic.getBalance()).isEqualTo(1000.83);
            softly.assertThat(student.getBalance()).isEqualTo(1002.5);
        }
    }

    @Test
    void updateBalance_unknownPlanBeforeKnownPlan_laterDepositStillEarnsInterest() {
        TimeDeposit unknown = new TimeDeposit(1, "gold", 1000.0, 31);
        TimeDeposit basic = new TimeDeposit(2, BASIC, 1000.0, 31);

        calculator.updateBalance(List.of(unknown, basic));

        try (AutoCloseableSoftAssertions softly = new AutoCloseableSoftAssertions()) {
            softly.assertThat(unknown.getBalance()).isEqualTo(1000.0);
            softly.assertThat(basic.getBalance()).isEqualTo(1000.83);
        }
    }

    @Test
    void updateBalance_emptyList_listStaysEmpty() {
        List<TimeDeposit> deposits = new ArrayList<>();

        calculator.updateBalance(deposits);

        assertThat(deposits).isEmpty();
    }

    @Test
    void updateBalance_calledTwice_compoundsOnNewBalanceWithoutAdvancingDays() {
        // student 1,000,000: 2500.00 -> 1,002,500; the next call adds 2506.25 -> 1,005,006.25
        TimeDeposit deposit = new TimeDeposit(1, STUDENT, 1_000_000.0, 31);
        List<TimeDeposit> deposits = List.of(deposit);

        calculator.updateBalance(deposits);
        calculator.updateBalance(deposits);

        try (AutoCloseableSoftAssertions softly = new AutoCloseableSoftAssertions()) {
            softly.assertThat(deposit.getBalance()).isEqualTo(1_005_006.25);
            softly.assertThat(deposit.getDays()).isEqualTo(31);
        }
    }

    private double balanceAfterUpdate(String planType, double balance, int days) {
        TimeDeposit deposit = new TimeDeposit(1, planType, balance, days);
        calculator.updateBalance(List.of(deposit));
        return deposit.getBalance();
    }
}
