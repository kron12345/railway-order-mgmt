package com.ordermgmt.railway.ui.component.timetable;

import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.formatTime;
import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.parseTime;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;

import com.ordermgmt.railway.domain.timetable.model.TimetableRowData;

/**
 * Fills missing estimated arrival/departure times by walking forward from the first row that has a
 * time, using segment distances and an assumed 70 km/h cruise speed, and flags rows whose implied
 * speed is physically implausible (likely data error). Pure functions over the row list.
 */
final class TimetableEstimateCalculator {

    /** Assumed travel speed (m/s) equivalent to 70 km/h. */
    private static final double ASSUMED_SPEED_MPS = 70_000D / 3_600D;

    /** Maximum plausible speed (km/h) for a rail segment; rows exceeding this get a warning. */
    private static final double MAX_SPEED_KMH = 200.0;

    private TimetableEstimateCalculator() {}

    /**
     * If any rows after the first row with a departure time are missing estimates, fill them based
     * on segment distances and the assumed speed.
     */
    static void recalculateIfNeeded(List<TimetableRowData> rows) {
        TimetableRowData anchorRow = null;
        int anchorIdx = -1;

        for (int i = 0; i < rows.size(); i++) {
            TimetableRowData row = rows.get(i);
            String dep = resolveEffectiveDeparture(row);
            if (dep != null && !dep.isBlank()) {
                anchorRow = row;
                anchorIdx = i;
                break;
            }
        }
        if (anchorRow == null || anchorIdx >= rows.size() - 1) {
            return;
        }

        boolean hasGaps = false;
        for (int i = anchorIdx + 1; i < rows.size(); i++) {
            if (rows.get(i).getEstimatedArrival() == null
                    || rows.get(i).getEstimatedArrival().isBlank()) {
                hasGaps = true;
                break;
            }
        }

        if (hasGaps) {
            fillEstimatesFromAnchor(rows, anchorRow, anchorIdx);
        }
    }

    private static void fillEstimatesFromAnchor(
            List<TimetableRowData> rows, TimetableRowData anchorRow, int anchorIdx) {
        String depStr = resolveEffectiveDeparture(anchorRow);
        LocalTime cursor = parseTime(depStr);
        if (cursor == null) {
            return;
        }
        if (anchorRow.getEstimatedDeparture() == null
                || anchorRow.getEstimatedDeparture().isBlank()) {
            anchorRow.setEstimatedDeparture(formatTime(cursor));
        }

        for (int i = anchorIdx + 1; i < rows.size(); i++) {
            TimetableRowData row = rows.get(i);
            double segmentMeters =
                    row.getSegmentLengthMeters() != null ? row.getSegmentLengthMeters() : 0D;
            long travelSec = Math.round(segmentMeters / ASSUMED_SPEED_MPS);
            cursor = cursor.plusSeconds(travelSec);

            if (row.getEstimatedArrival() == null || row.getEstimatedArrival().isBlank()) {
                row.setEstimatedArrival(formatTime(cursor));
            } else {
                cursor = parseTime(row.getEstimatedArrival());
                if (cursor == null) {
                    return;
                }
            }

            if (Boolean.TRUE.equals(row.getHalt()) && row.getDwellMinutes() != null) {
                cursor = cursor.plusMinutes(row.getDwellMinutes());
            }

            if (i < rows.size() - 1) {
                if (row.getEstimatedDeparture() == null || row.getEstimatedDeparture().isBlank()) {
                    row.setEstimatedDeparture(formatTime(cursor));
                } else {
                    cursor = parseTime(row.getEstimatedDeparture());
                    if (cursor == null) {
                        return;
                    }
                }
            }
        }
    }

    /**
     * Effective departure string for a row: exact/window/commercial/estimated, then arrival side.
     */
    static String resolveEffectiveDeparture(TimetableRowData row) {
        if (row.getDepartureExact() != null && !row.getDepartureExact().isBlank()) {
            return row.getDepartureExact();
        }
        if (row.getDepartureEarliest() != null && !row.getDepartureEarliest().isBlank()) {
            return row.getDepartureEarliest();
        }
        if (row.getCommercialDeparture() != null && !row.getCommercialDeparture().isBlank()) {
            return row.getCommercialDeparture();
        }
        if (row.getEstimatedDeparture() != null && !row.getEstimatedDeparture().isBlank()) {
            return row.getEstimatedDeparture();
        }
        if (row.getArrivalExact() != null && !row.getArrivalExact().isBlank()) {
            return row.getArrivalExact();
        }
        if (row.getArrivalEarliest() != null && !row.getArrivalEarliest().isBlank()) {
            return row.getArrivalEarliest();
        }
        if (row.getEstimatedArrival() != null && !row.getEstimatedArrival().isBlank()) {
            return row.getEstimatedArrival();
        }
        return null;
    }

    /**
     * Whether the implied speed from the previous row's departure to this row's arrival is absurd.
     */
    static boolean hasUnrealisticSpeed(List<TimetableRowData> rows, TimetableRowData row) {
        return calculateImpliedSpeedKmh(rows, row) > MAX_SPEED_KMH;
    }

    private static double calculateImpliedSpeedKmh(
            List<TimetableRowData> rows, TimetableRowData row) {
        if (row.getSegmentLengthMeters() == null || row.getSegmentLengthMeters() <= 0) {
            return 0;
        }
        LocalTime arrival = parseTime(row.getEstimatedArrival());
        if (arrival == null) {
            return 0;
        }
        int idx = rows.indexOf(row);
        if (idx <= 0) {
            return 0;
        }
        TimetableRowData previousRow = rows.get(idx - 1);
        LocalTime previousDeparture = parseTime(previousRow.getEstimatedDeparture());
        if (previousDeparture == null) {
            return 0;
        }
        long travelTimeSeconds = Duration.between(previousDeparture, arrival).getSeconds();
        if (travelTimeSeconds <= 0) {
            return 0;
        }
        double distanceKm = row.getSegmentLengthMeters() / 1000.0;
        double travelTimeHours = travelTimeSeconds / 3600.0;
        return distanceKm / travelTimeHours;
    }
}
