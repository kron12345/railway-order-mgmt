package com.ordermgmt.railway.domain.timetable.editing;

import static com.ordermgmt.railway.domain.timetable.editing.TimetableTimeMath.firstNonNull;
import static com.ordermgmt.railway.domain.timetable.editing.TimetableTimeMath.modeOrNone;
import static com.ordermgmt.railway.domain.timetable.editing.TimetableTimeMath.parseTime;

import java.time.LocalTime;

import com.ordermgmt.railway.domain.timetable.model.TimeConstraintMode;
import com.ordermgmt.railway.domain.timetable.model.TimetableRowData;

/**
 * Reads a row's "effective" time anchors for a given {@link TimeConstraintMode}. The arrival anchor
 * is the earliest meaningful arrival (it pulls the previous leg); the departure anchor is the
 * latest meaningful departure (it pushes the next leg). Also exposes the old-mode representative
 * value used by mode switches, the earliest-side values used for dwell reconciliation, and the
 * user-entered flags. Pure reads, no mutation.
 */
public final class TimetableAnchors {

    private TimetableAnchors() {}

    /**
     * Earliest meaningful arrival the train may have at this stop, given the current {@link
     * TimeConstraintMode}. Used as the anchor for backwards propagation.
     */
    public static LocalTime effectiveArrivalAnchor(TimetableRowData row) {
        TimeConstraintMode mode = modeOrNone(row.getArrivalMode());
        return switch (mode) {
            case EXACT ->
                    firstNonNull(
                            parseTime(row.getArrivalExact()), parseTime(row.getEstimatedArrival()));
            case WINDOW, AFTER ->
                    firstNonNull(
                            parseTime(row.getArrivalEarliest()),
                            parseTime(row.getEstimatedArrival()));
            case BEFORE ->
                    firstNonNull(
                            parseTime(row.getArrivalLatest()),
                            parseTime(row.getEstimatedArrival()));
            case COMMERCIAL ->
                    firstNonNull(
                            parseTime(row.getCommercialArrival()),
                            parseTime(row.getEstimatedArrival()));
            case NONE -> parseTime(row.getEstimatedArrival());
        };
    }

    /**
     * Latest meaningful departure the train may have at this stop. Used as the anchor for forwards
     * propagation: the schedule's "max time" semantic — the train is allowed to stay until LLD, so
     * the next station must be reachable from there. Intentionally LLD (not ELD) on WINDOW mode per
     * Regel 7 + planning constraint: forward computations always assume the largest permitted
     * range.
     */
    public static LocalTime effectiveDepartureAnchor(TimetableRowData row) {
        TimeConstraintMode mode = modeOrNone(row.getDepartureMode());
        return switch (mode) {
            case EXACT ->
                    firstNonNull(
                            parseTime(row.getDepartureExact()),
                            parseTime(row.getEstimatedDeparture()));
            case WINDOW, BEFORE ->
                    firstNonNull(
                            parseTime(row.getDepartureLatest()),
                            parseTime(row.getEstimatedDeparture()));
            case AFTER ->
                    firstNonNull(
                            parseTime(row.getDepartureEarliest()),
                            parseTime(row.getEstimatedDeparture()));
            case COMMERCIAL ->
                    firstNonNull(
                            parseTime(row.getCommercialDeparture()),
                            parseTime(row.getEstimatedDeparture()));
            case NONE -> parseTime(row.getEstimatedDeparture());
        };
    }

    public static LocalTime oldArrivalRepresentative(
            TimetableRowData row, TimeConstraintMode oldMode) {
        return switch (oldMode) {
            case EXACT -> parseTime(row.getArrivalExact());
            case WINDOW ->
                    firstNonNull(
                            parseTime(row.getArrivalEarliest()), parseTime(row.getArrivalLatest()));
            case AFTER -> parseTime(row.getArrivalEarliest());
            case BEFORE -> parseTime(row.getArrivalLatest());
            case COMMERCIAL -> parseTime(row.getCommercialArrival());
            case NONE -> null;
        };
    }

    public static LocalTime oldDepartureRepresentative(
            TimetableRowData row, TimeConstraintMode oldMode) {
        return switch (oldMode) {
            case EXACT -> parseTime(row.getDepartureExact());
            case WINDOW ->
                    firstNonNull(
                            parseTime(row.getDepartureLatest()),
                            parseTime(row.getDepartureEarliest()));
            case AFTER -> parseTime(row.getDepartureEarliest());
            case BEFORE -> parseTime(row.getDepartureLatest());
            case COMMERCIAL -> parseTime(row.getCommercialDeparture());
            case NONE -> null;
        };
    }

    public static LocalTime earliestArrivalForDwell(TimetableRowData row) {
        TimeConstraintMode mode = modeOrNone(row.getArrivalMode());
        return switch (mode) {
            case EXACT -> parseTime(row.getArrivalExact());
            case WINDOW, AFTER -> parseTime(row.getArrivalEarliest());
            case BEFORE -> parseTime(row.getArrivalLatest());
            case COMMERCIAL -> parseTime(row.getCommercialArrival());
            case NONE -> parseTime(row.getEstimatedArrival());
        };
    }

    public static LocalTime earliestDepartureForDwell(TimetableRowData row) {
        TimeConstraintMode mode = modeOrNone(row.getDepartureMode());
        return switch (mode) {
            case EXACT -> parseTime(row.getDepartureExact());
            case WINDOW, AFTER -> parseTime(row.getDepartureEarliest());
            case BEFORE -> parseTime(row.getDepartureLatest());
            case COMMERCIAL -> parseTime(row.getCommercialDeparture());
            case NONE -> parseTime(row.getEstimatedDeparture());
        };
    }

    /** True when at least one arrival-side constraint field has a user-entered value. */
    public static boolean hasUserEnteredArrival(TimetableRowData row) {
        return Boolean.TRUE.equals(row.getUserEnteredArrivalExact())
                || Boolean.TRUE.equals(row.getUserEnteredArrivalEarliest())
                || Boolean.TRUE.equals(row.getUserEnteredArrivalLatest())
                || Boolean.TRUE.equals(row.getUserEnteredCommercialArrival());
    }

    public static boolean hasUserEnteredDeparture(TimetableRowData row) {
        return Boolean.TRUE.equals(row.getUserEnteredDepartureExact())
                || Boolean.TRUE.equals(row.getUserEnteredDepartureEarliest())
                || Boolean.TRUE.equals(row.getUserEnteredDepartureLatest())
                || Boolean.TRUE.equals(row.getUserEnteredCommercialDeparture());
    }
}
