package com.ordermgmt.railway.domain.timetable.model;

/**
 * TTT JourneyLocationTypeCode as defined in TSI TAF/TAP Sector Schema.
 *
 * <p>See TTT Anlage 1, Section 5.5.
 */
public enum JourneyLocationType {
    ORIGIN("01", "Origin"),
    INTERMEDIATE("02", "Intermediate"),
    DESTINATION("03", "Destination"),
    HANDOVER("04", "Handover"),
    INTERCHANGE("05", "Interchange"),
    HANDOVER_AND_INTERCHANGE("06", "Handover and Interchange"),
    STATE_BORDER("07", "State Border"),
    NETWORK_BORDER("09", "Network Border");

    private final String code;
    private final String label;

    JourneyLocationType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    /** Resolves from the legacy string field or TTT code. */
    public static JourneyLocationType fromString(String value) {
        if (value == null || value.isBlank()) {
            return INTERMEDIATE;
        }
        for (JourneyLocationType type : values()) {
            if (type.name().equalsIgnoreCase(value) || type.code.equals(value)) {
                return type;
            }
        }
        return INTERMEDIATE;
    }
}
