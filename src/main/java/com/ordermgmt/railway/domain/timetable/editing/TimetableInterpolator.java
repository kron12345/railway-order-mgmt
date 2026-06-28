package com.ordermgmt.railway.domain.timetable.editing;

import static com.ordermgmt.railway.domain.timetable.editing.TimetableAnchors.effectiveArrivalAnchor;
import static com.ordermgmt.railway.domain.timetable.editing.TimetableAnchors.effectiveDepartureAnchor;
import static com.ordermgmt.railway.domain.timetable.editing.TimetableAnchors.hasUserEnteredArrival;
import static com.ordermgmt.railway.domain.timetable.editing.TimetableAnchors.hasUserEnteredDeparture;
import static com.ordermgmt.railway.domain.timetable.editing.TimetableTimeMath.dayOffset;
import static com.ordermgmt.railway.domain.timetable.editing.TimetableTimeMath.format;
import static com.ordermgmt.railway.domain.timetable.editing.TimetableTimeMath.offsetOrZero;
import static com.ordermgmt.railway.domain.timetable.editing.TimetableTimeMath.toAbsoluteMinutes;
import static com.ordermgmt.railway.domain.timetable.editing.TimetableTimeMath.wallClock;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import com.ordermgmt.railway.domain.timetable.model.TimetableRowData;

/**
 * Distance-weighted time interpolation between consecutive user-entered anchor times. Each
 * user-entered (or dwell-mirrored) arrival/departure becomes an anchor point; between consecutive
 * anchors the implied average speed fills every intermediate row's estimated times. Rows before the
 * first / after the last anchor get a default cruise speed so a single anchor still derives a
 * sensible origin departure.
 */
public final class TimetableInterpolator {

    /** Default cruise speed for fringe rows before the first or after the last anchor: 70 km/h. */
    private static final double DEFAULT_SPEED_MIN_PER_METER = 60.0 / 70_000d;

    private TimetableInterpolator() {}

    public static void interpolateBetweenAnchors(List<TimetableRowData> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        List<AnchorPoint> anchors = collectAnchors(rows);
        if (anchors.isEmpty()) {
            return;
        }

        // Segments between consecutive anchors → segmental average speed.
        for (int a = 0; a < anchors.size() - 1; a++) {
            AnchorPoint p1 = anchors.get(a);
            AnchorPoint p2 = anchors.get(a + 1);
            double distSpan = p2.distance() - p1.distance();
            long timeSpan = p2.absMinutes() - p1.absMinutes();
            if (distSpan <= 0 || timeSpan <= 0) {
                continue;
            }
            double minPerMeter = (double) timeSpan / distSpan;

            for (int r = p1.rowIndex() + 1; r < p2.rowIndex(); r++) {
                fillRow(rows.get(r), p1.distance(), p1.absMinutes(), minPerMeter);
            }
        }

        // Fringe fill: rows before the first anchor and after the last anchor get default speed.
        AnchorPoint first = anchors.get(0);
        for (int r = first.rowIndex() - 1; r >= 0; r--) {
            fillRow(rows.get(r), first.distance(), first.absMinutes(), DEFAULT_SPEED_MIN_PER_METER);
        }
        AnchorPoint last = anchors.get(anchors.size() - 1);
        for (int r = last.rowIndex() + 1; r < rows.size(); r++) {
            fillRow(rows.get(r), last.distance(), last.absMinutes(), DEFAULT_SPEED_MIN_PER_METER);
        }
    }

    /**
     * Compute a row's estimated arrival (and dwell-derived departure) by linear extrapolation from
     * the given anchor, using {@code minPerMeter} as the speed. Works in both directions.
     */
    private static void fillRow(
            TimetableRowData row, double anchorDist, long anchorAbsMin, double minPerMeter) {
        Double dist = row.getDistanceFromStartMeters();
        if (dist == null) {
            return;
        }
        long arrAbs = anchorAbsMin + Math.round((dist - anchorDist) * minPerMeter);
        int dwellMinutes = row.getDwellMinutes() != null ? row.getDwellMinutes() : 0;
        long depAbs = arrAbs + dwellMinutes;
        row.setEstimatedArrival(format(wallClock(arrAbs)));
        row.setEstimatedDeparture(format(wallClock(depAbs)));
        row.setArrivalOffset(dayOffset(arrAbs));
        row.setDepartureOffset(dayOffset(depAbs));
    }

    /** A user-entered (rowIndex, distance, absoluteMinutes) interpolation anchor. */
    private record AnchorPoint(int rowIndex, double distance, long absMinutes) {}

    private static List<AnchorPoint> collectAnchors(List<TimetableRowData> rows) {
        List<AnchorPoint> anchors = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            TimetableRowData row = rows.get(i);
            Double dist = row.getDistanceFromStartMeters();
            if (dist == null) {
                continue;
            }
            if (hasIntentArrival(row)) {
                addAnchorIfPresent(
                        anchors, i, dist, effectiveArrivalAnchor(row), row.getArrivalOffset());
            }
            if (hasIntentDeparture(row)) {
                addAnchorIfPresent(
                        anchors, i, dist, effectiveDepartureAnchor(row), row.getDepartureOffset());
            }
        }
        return anchors;
    }

    private static void addAnchorIfPresent(
            List<AnchorPoint> anchors,
            int rowIndex,
            double distance,
            LocalTime time,
            Integer offset) {
        if (time != null) {
            anchors.add(
                    new AnchorPoint(
                            rowIndex, distance, toAbsoluteMinutes(time, offsetOrZero(offset))));
        }
    }

    /**
     * "Intent" check: an arrival is intentionally fixed when the user typed it directly OR typed
     * the departure side + dwell (then the arrival is a derived-but-intended value, Regel 5). Same
     * for departure. So the forward walk after a halt with WINDOW arrival + dwell starts from the
     * mirrored LLD — the planning-relevant figure.
     */
    private static boolean hasIntentArrival(TimetableRowData row) {
        if (hasUserEnteredArrival(row)) {
            return true;
        }
        return Boolean.TRUE.equals(row.getHalt())
                && Boolean.TRUE.equals(row.getUserEnteredDwell())
                && hasUserEnteredDeparture(row);
    }

    private static boolean hasIntentDeparture(TimetableRowData row) {
        if (hasUserEnteredDeparture(row)) {
            return true;
        }
        return Boolean.TRUE.equals(row.getHalt())
                && Boolean.TRUE.equals(row.getUserEnteredDwell())
                && hasUserEnteredArrival(row);
    }
}
