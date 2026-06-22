package com.ordermgmt.railway.domain.timetable.model;

/**
 * Supported TTT timing modes for arrival/departure values.
 *
 * <p>Maps to TTT TimingQualifierCodes (TimingAtLocation/Timing 0..*):
 *
 * <ul>
 *   <li>NONE → no Timing entry exported
 *   <li>EXACT → ALA (arrival) / ALD (departure) — single exact time
 *   <li>WINDOW → ELA+LLA (arrival) / ELD+LLD (departure) — both bounds
 *   <li>AFTER → ELA only (arrival) / ELD only (departure) — "frühestens" / "nach"
 *   <li>BEFORE → LLA only (arrival) / LLD only (departure) — "spätestens" / "vor"
 *   <li>COMMERCIAL → PLA / PLD — published timetable time
 * </ul>
 *
 * <p>AFTER and BEFORE are half-window single-bound constraints: TTT supports them via a single
 * Timing entry with the appropriate qualifier code, no requirement that ELA/LLA must occur in
 * pairs. Useful e.g. for "Abfahrt nach 10:00" (ELD only) or "Ankunft vor 09:30" (LLA only).
 */
public enum TimeConstraintMode {
    NONE,
    EXACT,
    WINDOW,
    AFTER,
    BEFORE,
    COMMERCIAL
}
