package org.ikigaidigital.domain.model;

/**
 * Plan codes only; rates and day windows live in the interest policies.
 * The enum selects a policy and is never stored: a deposit keeps its original code text.
 */
public enum PlanType {
    BASIC("basic"),
    STUDENT("student"),
    PREMIUM("premium"),
    UNKNOWN(null);

    private final String code;

    PlanType(String code) {
        this.code = code;
    }

    /** Never throws; any code that is not matched exactly, including null, gives {@link #UNKNOWN}. */
    public static PlanType fromCode(String code) {
        for (PlanType type : values()) {
            // Plan codes are matched exactly and case-sensitively; null (A10) and unrecognised codes are UNKNOWN (A2).
            if (type.code != null && type.code.equals(code)) {
                return type;
            }
        }
        return UNKNOWN;
    }
}
