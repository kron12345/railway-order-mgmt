package com.ordermgmt.railway.domain.timetable.model;

/**
 * Supported TTT timing modes for arrival/departure values.
 *
 * <p>Maps to TTT TimingQualifierCodes: NONE = no constraint, EXACT = ALA/ALD (effective time),
 * WINDOW = ELA+LLA / ELD+LLD (time window), COMMERCIAL = PLA/PLD (commercial/published time).
 */
public enum TimeConstraintMode {
    NONE,
    EXACT,
    WINDOW,
    COMMERCIAL
}
