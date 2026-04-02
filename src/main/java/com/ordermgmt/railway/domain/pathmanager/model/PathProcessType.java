package com.ordermgmt.railway.domain.pathmanager.model;

/** TTT process types as defined in TSI TAF/TAP specification. */
public enum PathProcessType {
    ANNUAL_NEW(0),
    ANNUAL_LATE(1),
    AD_HOC(2),
    FEASIBILITY_STUDY(4),
    MODIFICATION(5),
    ALTERATION(6),
    CANCELLATION(10);

    private final int code;

    PathProcessType(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    /** Resolves from the TTT numeric code. */
    public static PathProcessType fromCode(int code) {
        for (PathProcessType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown PathProcessType code: " + code);
    }
}
