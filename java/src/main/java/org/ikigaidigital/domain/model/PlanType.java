package org.ikigaidigital.domain.model;

public enum PlanType {
    BASIC("basic"),
    STUDENT("student"),
    PREMIUM("premium"),
    UNKNOWN(null);

    private final String code;

    PlanType(String code) {
        this.code = code;
    }

    // The match is exact and case-sensitive, as in the legacy code: "Basic" is not the basic plan (A2).
    public static PlanType fromCode(String code) {
        for (PlanType type : values()) {
            if (type.code != null && type.code.equals(code)) {
                return type;
            }
        }
        return UNKNOWN;
    }
}
