package com.ordermgmt.railway.domain.timetable.editing;

import static com.ordermgmt.railway.domain.timetable.editing.TimetableAnchors.earliestArrivalForDwell;
import static com.ordermgmt.railway.domain.timetable.editing.TimetableAnchors.earliestDepartureForDwell;
import static com.ordermgmt.railway.domain.timetable.editing.TimetableAnchors.hasUserEnteredArrival;
import static com.ordermgmt.railway.domain.timetable.editing.TimetableAnchors.hasUserEnteredDeparture;
import static com.ordermgmt.railway.domain.timetable.editing.TimetableAnchors.oldArrivalRepresentative;
import static com.ordermgmt.railway.domain.timetable.editing.TimetableAnchors.oldDepartureRepresentative;
import static com.ordermgmt.railway.domain.timetable.editing.TimetableTimeMath.format;
import static com.ordermgmt.railway.domain.timetable.editing.TimetableTimeMath.modeOrNone;
import static com.ordermgmt.railway.domain.timetable.editing.TimetableTimeMath.parseTime;

import java.time.Duration;
import java.time.LocalTime;

import com.ordermgmt.railway.domain.timetable.model.RoutePointRole;
import com.ordermgmt.railway.domain.timetable.model.TimeConstraintMode;
import com.ordermgmt.railway.domain.timetable.model.TimetableRowData;

/**
 * Per-row constraint rules: halt enforcement (Regeln 3/4/5), arrival↔departure mirroring with the
 * dwell, mode-switch value preservation, dwell-derived departure, and dwell reconciliation. All
 * operate on a single row in place; no propagation across rows.
 */
public final class TimetableConstraintEditor {

    private TimetableConstraintEditor() {}

    /**
     * Apply the timetable editing rules (Regeln 3 + 4 + 5):
     *
     * <ul>
     *   <li>Halt=false → clear all constraint fields and dwell, reset modes to NONE. Estimated
     *       values (used for interpolation) are preserved. Pin remains user-controlled.
     *   <li>Halt=true with dwell + 1 side filled → mirror the dwell into the other side using the
     *       <em>same</em> mode (rule 5).
     *   <li>Halt=true with both sides filled (no dwell, or contradicting dwell) → drop dwell.
     * </ul>
     *
     * <p>Idempotent and safe after every editor sync; only user-entered constraint fields mutate.
     */
    public static void applyHaltRules(TimetableRowData row) {
        if (row == null) {
            return;
        }

        // Origin and destination are implicit halts with no dwell. They constrain only one side:
        // origin → departure-only, destination → arrival-only. Strip the irrelevant fields so
        // they never sneak into the TTT export.
        if (row.getRoutePointRole() == RoutePointRole.ORIGIN) {
            row.setHalt(true);
            row.setDwellMinutes(null);
            row.setUserEnteredDwell(false);
            clearArrivalConstraints(row);
            return;
        }
        if (row.getRoutePointRole() == RoutePointRole.DESTINATION) {
            row.setHalt(true);
            row.setDwellMinutes(null);
            row.setUserEnteredDwell(false);
            clearDepartureConstraints(row);
            return;
        }

        if (!Boolean.TRUE.equals(row.getHalt())) {
            clearArrivalConstraints(row);
            clearDepartureConstraints(row);
            row.setDwellMinutes(null);
            row.setUserEnteredDwell(false);
            return;
        }

        boolean arrEntered = hasUserEnteredArrival(row);
        boolean depEntered = hasUserEnteredDeparture(row);
        boolean dwellEntered =
                Boolean.TRUE.equals(row.getUserEnteredDwell()) && row.getDwellMinutes() != null;

        if (arrEntered && depEntered) {
            // Both sides explicit → dwell becomes implicit. Drop any user-entered dwell.
            row.setDwellMinutes(null);
            row.setUserEnteredDwell(false);
            return;
        }

        if (dwellEntered && (arrEntered || depEntered)) {
            // Mirror the entered side onto the empty side using the same mode.
            if (arrEntered) {
                mirrorArrivalToDeparture(row);
            } else {
                mirrorDepartureToArrival(row);
            }
        }
        // Else: dwell only, or estimated-only — both legal in step 1, nothing to enforce here.
    }

    public static void clearArrivalConstraints(TimetableRowData row) {
        row.setArrivalMode(TimeConstraintMode.NONE);
        row.setArrivalExact(null);
        row.setArrivalEarliest(null);
        row.setArrivalLatest(null);
        row.setCommercialArrival(null);
        row.setUserEnteredArrivalExact(false);
        row.setUserEnteredArrivalEarliest(false);
        row.setUserEnteredArrivalLatest(false);
        row.setUserEnteredCommercialArrival(false);
    }

    public static void clearDepartureConstraints(TimetableRowData row) {
        row.setDepartureMode(TimeConstraintMode.NONE);
        row.setDepartureExact(null);
        row.setDepartureEarliest(null);
        row.setDepartureLatest(null);
        row.setCommercialDeparture(null);
        row.setUserEnteredDepartureExact(false);
        row.setUserEnteredDepartureEarliest(false);
        row.setUserEnteredDepartureLatest(false);
        row.setUserEnteredCommercialDeparture(false);
    }

    private static void mirrorArrivalToDeparture(TimetableRowData row) {
        Integer dwell = row.getDwellMinutes();
        if (dwell == null || dwell < 0) {
            return;
        }
        TimeConstraintMode mode = modeOrNone(row.getArrivalMode());
        // Same-mode rule (Regel 5): force departure mode to match arrival.
        row.setDepartureMode(mode);
        switch (mode) {
            case EXACT -> {
                LocalTime t = parseTime(row.getArrivalExact());
                if (t != null) {
                    row.setDepartureExact(format(t.plusMinutes(dwell)));
                }
            }
            case WINDOW -> {
                LocalTime e = parseTime(row.getArrivalEarliest());
                LocalTime l = parseTime(row.getArrivalLatest());
                if (e != null) {
                    row.setDepartureEarliest(format(e.plusMinutes(dwell)));
                }
                if (l != null) {
                    row.setDepartureLatest(format(l.plusMinutes(dwell)));
                }
            }
            case AFTER -> {
                LocalTime e = parseTime(row.getArrivalEarliest());
                if (e != null) {
                    row.setDepartureEarliest(format(e.plusMinutes(dwell)));
                }
            }
            case BEFORE -> {
                LocalTime l = parseTime(row.getArrivalLatest());
                if (l != null) {
                    row.setDepartureLatest(format(l.plusMinutes(dwell)));
                }
            }
            case COMMERCIAL -> {
                LocalTime t = parseTime(row.getCommercialArrival());
                if (t != null) {
                    row.setCommercialDeparture(format(t.plusMinutes(dwell)));
                }
            }
            case NONE -> {}
        }
    }

    private static void mirrorDepartureToArrival(TimetableRowData row) {
        Integer dwell = row.getDwellMinutes();
        if (dwell == null || dwell < 0) {
            return;
        }
        TimeConstraintMode mode = modeOrNone(row.getDepartureMode());
        row.setArrivalMode(mode);
        switch (mode) {
            case EXACT -> {
                LocalTime t = parseTime(row.getDepartureExact());
                if (t != null) {
                    row.setArrivalExact(format(t.minusMinutes(dwell)));
                }
            }
            case WINDOW -> {
                LocalTime e = parseTime(row.getDepartureEarliest());
                LocalTime l = parseTime(row.getDepartureLatest());
                if (e != null) {
                    row.setArrivalEarliest(format(e.minusMinutes(dwell)));
                }
                if (l != null) {
                    row.setArrivalLatest(format(l.minusMinutes(dwell)));
                }
            }
            case AFTER -> {
                LocalTime e = parseTime(row.getDepartureEarliest());
                if (e != null) {
                    row.setArrivalEarliest(format(e.minusMinutes(dwell)));
                }
            }
            case BEFORE -> {
                LocalTime l = parseTime(row.getDepartureLatest());
                if (l != null) {
                    row.setArrivalLatest(format(l.minusMinutes(dwell)));
                }
            }
            case COMMERCIAL -> {
                LocalTime t = parseTime(row.getCommercialDeparture());
                if (t != null) {
                    row.setCommercialArrival(format(t.minusMinutes(dwell)));
                }
            }
            case NONE -> {}
        }
    }

    /**
     * Mode-switch preservation: when the user changes the constraint mode of one side, copy
     * whatever single value was held in the old mode into the new fields, so no data is silently
     * lost.
     */
    public static void preserveOnModeSwitch(
            TimetableRowData row,
            boolean arrivalSide,
            TimeConstraintMode oldMode,
            TimeConstraintMode newMode) {
        if (row == null || oldMode == null || newMode == null || oldMode == newMode) {
            return;
        }

        LocalTime source =
                arrivalSide
                        ? oldArrivalRepresentative(row, oldMode)
                        : oldDepartureRepresentative(row, oldMode);

        if (arrivalSide) {
            row.setArrivalExact(null);
            row.setArrivalEarliest(null);
            row.setArrivalLatest(null);
            row.setCommercialArrival(null);
        } else {
            row.setDepartureExact(null);
            row.setDepartureEarliest(null);
            row.setDepartureLatest(null);
            row.setCommercialDeparture(null);
        }

        if (source == null || newMode == TimeConstraintMode.NONE) {
            return;
        }
        String formatted = format(source);
        switch (newMode) {
            case EXACT -> {
                if (arrivalSide) {
                    row.setArrivalExact(formatted);
                } else {
                    row.setDepartureExact(formatted);
                }
            }
            case WINDOW -> {
                if (arrivalSide) {
                    row.setArrivalEarliest(formatted);
                    row.setArrivalLatest(formatted);
                } else {
                    row.setDepartureEarliest(formatted);
                    row.setDepartureLatest(formatted);
                }
            }
            case AFTER -> {
                if (arrivalSide) {
                    row.setArrivalEarliest(formatted);
                } else {
                    row.setDepartureEarliest(formatted);
                }
            }
            case BEFORE -> {
                if (arrivalSide) {
                    row.setArrivalLatest(formatted);
                } else {
                    row.setDepartureLatest(formatted);
                }
            }
            case COMMERCIAL -> {
                if (arrivalSide) {
                    row.setCommercialArrival(formatted);
                } else {
                    row.setCommercialDeparture(formatted);
                }
            }
            case NONE -> {}
        }
    }

    /**
     * If the row is a halt with a positive dwell, mirror its arrival anchors to the departure side,
     * offset by the dwell — only when the user hasn't entered explicit departure values yet (so we
     * never overwrite intent). Refreshes nothing else. Returns whether the departure side was
     * filled.
     */
    public static boolean deriveDepartureFromDwell(TimetableRowData row) {
        if (row == null || !Boolean.TRUE.equals(row.getHalt())) {
            return false;
        }
        Integer dwell = row.getDwellMinutes();
        if (dwell == null || dwell <= 0) {
            return false;
        }

        TimeConstraintMode arrMode = modeOrNone(row.getArrivalMode());
        if (arrMode == TimeConstraintMode.NONE) {
            return false;
        }
        if (!isDepartureEmpty(row)) {
            return false;
        }

        return switch (arrMode) {
            case EXACT -> mirrorExact(row, dwell);
            case WINDOW -> mirrorWindow(row, dwell);
            case AFTER -> mirrorAfter(row, dwell);
            case BEFORE -> mirrorBefore(row, dwell);
            case COMMERCIAL -> mirrorCommercial(row, dwell);
            case NONE -> false;
        };
    }

    private static boolean isDepartureEmpty(TimetableRowData row) {
        TimeConstraintMode mode = modeOrNone(row.getDepartureMode());
        if (mode == TimeConstraintMode.NONE) {
            return true;
        }
        return switch (mode) {
            case EXACT -> row.getDepartureExact() == null || row.getDepartureExact().isBlank();
            case WINDOW ->
                    (row.getDepartureEarliest() == null || row.getDepartureEarliest().isBlank())
                            && (row.getDepartureLatest() == null
                                    || row.getDepartureLatest().isBlank());
            case AFTER ->
                    row.getDepartureEarliest() == null || row.getDepartureEarliest().isBlank();
            case BEFORE -> row.getDepartureLatest() == null || row.getDepartureLatest().isBlank();
            case COMMERCIAL ->
                    row.getCommercialDeparture() == null || row.getCommercialDeparture().isBlank();
            case NONE -> true;
        };
    }

    private static boolean mirrorExact(TimetableRowData row, int dwell) {
        LocalTime ala = parseTime(row.getArrivalExact());
        if (ala == null) {
            return false;
        }
        row.setDepartureMode(TimeConstraintMode.EXACT);
        row.setDepartureExact(format(ala.plusMinutes(dwell)));
        return true;
    }

    private static boolean mirrorWindow(TimetableRowData row, int dwell) {
        LocalTime ela = parseTime(row.getArrivalEarliest());
        LocalTime lla = parseTime(row.getArrivalLatest());
        if (ela == null || lla == null) {
            return false;
        }
        row.setDepartureMode(TimeConstraintMode.WINDOW);
        row.setDepartureEarliest(format(ela.plusMinutes(dwell)));
        row.setDepartureLatest(format(lla.plusMinutes(dwell)));
        return true;
    }

    private static boolean mirrorCommercial(TimetableRowData row, int dwell) {
        LocalTime pla = parseTime(row.getCommercialArrival());
        if (pla == null) {
            return false;
        }
        row.setDepartureMode(TimeConstraintMode.COMMERCIAL);
        row.setCommercialDeparture(format(pla.plusMinutes(dwell)));
        return true;
    }

    private static boolean mirrorAfter(TimetableRowData row, int dwell) {
        LocalTime ela = parseTime(row.getArrivalEarliest());
        if (ela == null) {
            return false;
        }
        row.setDepartureMode(TimeConstraintMode.AFTER);
        row.setDepartureEarliest(format(ela.plusMinutes(dwell)));
        return true;
    }

    private static boolean mirrorBefore(TimetableRowData row, int dwell) {
        LocalTime lla = parseTime(row.getArrivalLatest());
        if (lla == null) {
            return false;
        }
        row.setDepartureMode(TimeConstraintMode.BEFORE);
        row.setDepartureLatest(format(lla.plusMinutes(dwell)));
        return true;
    }

    /**
     * Reconcile {@code dwellMinutes} with the row's effective arrival/departure on the earliest
     * side. Only acts on halt rows where both anchors exist and the delta is non-negative, and only
     * when the existing dwell is missing or contradicts the actual delta. Returns whether dwell
     * changed.
     */
    public static boolean reconcileDwell(TimetableRowData row) {
        if (row == null || !Boolean.TRUE.equals(row.getHalt())) {
            return false;
        }
        LocalTime arr = earliestArrivalForDwell(row);
        LocalTime dep = earliestDepartureForDwell(row);
        if (arr == null || dep == null || dep.isBefore(arr)) {
            return false;
        }
        int actual = (int) Duration.between(arr, dep).toMinutes();
        Integer current = row.getDwellMinutes();
        if (current != null && current == actual) {
            return false;
        }
        row.setDwellMinutes(actual);
        return true;
    }
}
