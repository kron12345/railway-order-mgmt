package com.ordermgmt.railway.domain.timetable.service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;

import com.ordermgmt.railway.domain.infrastructure.model.OperationalPoint;
import com.ordermgmt.railway.domain.infrastructure.repository.OperationalPointRepository;
import com.ordermgmt.railway.domain.timetable.model.RoutePointRole;
import com.ordermgmt.railway.domain.timetable.model.TimePropagationMode;
import com.ordermgmt.railway.domain.timetable.model.TimetableRowData;

/**
 * Service for timetable row editing operations: add/remove stops, time propagation (shift/stretch),
 * and relative time input.
 */
@Service
public class TimetableEditingService {

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    private final OperationalPointRepository operationalPointRepository;

    public TimetableEditingService(OperationalPointRepository operationalPointRepository) {
        this.operationalPointRepository = operationalPointRepository;
    }

    /**
     * Inserts a new stop into the timetable at the given index. Recalculates sequence numbers and
     * estimates arrival/departure by interpolating between the surrounding rows.
     */
    public void insertStop(
            List<TimetableRowData> rows,
            int insertIndex,
            OperationalPoint operationalPoint,
            String activityCode) {

        TimetableRowData newRow = new TimetableRowData();
        newRow.setUopid(operationalPoint.getUopid());
        newRow.setName(operationalPoint.getName());
        newRow.setCountry(operationalPoint.getCountry());
        newRow.setRoutePointRole(RoutePointRole.VIA);
        newRow.setJourneyLocationType("INTERMEDIATE");
        newRow.setHalt(true);
        newRow.setActivityCode(activityCode);
        newRow.setManuallyAdded(true);
        newRow.setDwellMinutes(2);

        // Interpolate times from neighbors
        if (insertIndex > 0 && insertIndex <= rows.size()) {
            TimetableRowData before = rows.get(insertIndex - 1);
            TimetableRowData after = insertIndex < rows.size() ? rows.get(insertIndex) : null;
            interpolateTimes(newRow, before, after);
        }

        rows.add(insertIndex, newRow);
        resequence(rows);
    }

    /** Soft-deletes a row (marks as deleted but keeps it for undo). */
    public void softDeleteStop(List<TimetableRowData> rows, int index) {
        if (index < 0 || index >= rows.size()) {
            return;
        }
        TimetableRowData row = rows.get(index);
        // Cannot delete origin or destination
        if (row.getRoutePointRole() == RoutePointRole.ORIGIN
                || row.getRoutePointRole() == RoutePointRole.DESTINATION) {
            return;
        }
        row.setDeleted(!Boolean.TRUE.equals(row.getDeleted()));
    }

    /** Permanently removes soft-deleted rows and resequences. */
    public void purgeDeleted(List<TimetableRowData> rows) {
        rows.removeIf(row -> Boolean.TRUE.equals(row.getDeleted()));
        resequence(rows);
    }

    /**
     * Propagates a time change at the given row index.
     *
     * @param rows all timetable rows
     * @param changedIndex the row whose time was changed
     * @param isArrival true if arrival was changed, false if departure
     * @param newTime the new time value
     * @param mode SHIFT or STRETCH
     */
    public void propagateTimeChange(
            List<TimetableRowData> rows,
            int changedIndex,
            boolean isArrival,
            LocalTime newTime,
            TimePropagationMode mode) {

        if (changedIndex < 0 || changedIndex >= rows.size() || newTime == null) {
            return;
        }

        TimetableRowData changedRow = rows.get(changedIndex);
        String oldTimeStr =
                isArrival ? changedRow.getEstimatedArrival() : changedRow.getEstimatedDeparture();
        LocalTime oldTime = parseTime(oldTimeStr);

        // Set the new time
        String newTimeStr = newTime.format(HH_MM);
        if (isArrival) {
            changedRow.setEstimatedArrival(newTimeStr);
            changedRow.setArrivalExact(newTimeStr);
        } else {
            changedRow.setEstimatedDeparture(newTimeStr);
            changedRow.setDepartureExact(newTimeStr);
        }

        if (oldTime == null) {
            return;
        }

        long deltaMinutes = java.time.Duration.between(oldTime, newTime).toMinutes();
        if (deltaMinutes == 0) {
            return;
        }

        if (mode == TimePropagationMode.SHIFT) {
            shiftFollowingTimes(rows, changedIndex, isArrival, deltaMinutes);
        } else {
            stretchToNextPin(rows, changedIndex, isArrival, oldTime, newTime);
        }
    }

    /**
     * Parses relative time input like "+5" or "-3" and returns the resolved time. Returns null if
     * the input is not relative.
     */
    public LocalTime resolveRelativeTime(String input, LocalTime baseTime) {
        if (input == null || baseTime == null) {
            return null;
        }
        String trimmed = input.trim();
        if (!trimmed.startsWith("+") && !trimmed.startsWith("-")) {
            return null;
        }
        try {
            int minutes = Integer.parseInt(trimmed);
            return baseTime.plusMinutes(minutes);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Recalculates sequence numbers starting from 1. */
    public void resequence(List<TimetableRowData> rows) {
        for (int i = 0; i < rows.size(); i++) {
            rows.get(i).setSequence(i + 1);
        }
    }

    // ── Private helpers ────────────────────────────────────────────────

    private void shiftFollowingTimes(
            List<TimetableRowData> rows, int fromIndex, boolean isArrival, long deltaMinutes) {
        // Start from next row (or same row departure if arrival changed)
        int startIdx = isArrival ? fromIndex : fromIndex + 1;
        boolean skipFirstArrival = isArrival; // don't shift the arrival we just set

        for (int i = startIdx; i < rows.size(); i++) {
            TimetableRowData row = rows.get(i);
            if (Boolean.TRUE.equals(row.getPinned())) {
                break; // Stop at pinned row
            }
            if (i == startIdx && skipFirstArrival) {
                // Only shift departure of the changed row
                shiftTimeField(row, false, deltaMinutes);
            } else {
                shiftTimeField(row, true, deltaMinutes);
                shiftTimeField(row, false, deltaMinutes);
            }
        }
    }

    private void shiftTimeField(TimetableRowData row, boolean isArrival, long deltaMinutes) {
        String fieldValue = isArrival ? row.getEstimatedArrival() : row.getEstimatedDeparture();
        LocalTime time = parseTime(fieldValue);
        if (time == null) {
            return;
        }
        String shifted = time.plusMinutes(deltaMinutes).format(HH_MM);
        if (isArrival) {
            row.setEstimatedArrival(shifted);
            if (row.getArrivalExact() != null) {
                row.setArrivalExact(shifted);
            }
        } else {
            row.setEstimatedDeparture(shifted);
            if (row.getDepartureExact() != null) {
                row.setDepartureExact(shifted);
            }
        }
    }

    private void stretchToNextPin(
            List<TimetableRowData> rows,
            int changedIndex,
            boolean isArrival,
            LocalTime oldTime,
            LocalTime newTime) {
        // Find next pinned row (or end)
        int pinIndex = rows.size() - 1;
        for (int i = changedIndex + 1; i < rows.size(); i++) {
            if (Boolean.TRUE.equals(rows.get(i).getPinned())) {
                pinIndex = i;
                break;
            }
        }

        if (pinIndex <= changedIndex) {
            return;
        }

        // Get the time at the pin point
        TimetableRowData pinRow = rows.get(pinIndex);
        LocalTime pinTime = parseTime(pinRow.getEstimatedArrival());
        if (pinTime == null) {
            pinTime = parseTime(pinRow.getEstimatedDeparture());
        }
        if (pinTime == null) {
            return;
        }

        // Calculate stretch ratio
        long oldSpanMinutes = java.time.Duration.between(oldTime, pinTime).toMinutes();
        long newSpanMinutes = java.time.Duration.between(newTime, pinTime).toMinutes();
        if (oldSpanMinutes <= 0 || newSpanMinutes <= 0) {
            return;
        }

        double ratio = (double) newSpanMinutes / oldSpanMinutes;

        // Stretch intermediate times proportionally
        for (int i = changedIndex + 1; i < pinIndex; i++) {
            TimetableRowData row = rows.get(i);
            stretchField(row, true, newTime, ratio);
            stretchField(row, false, newTime, ratio);
        }
    }

    private void stretchField(
            TimetableRowData row, boolean isArrival, LocalTime anchor, double ratio) {
        String fieldValue = isArrival ? row.getEstimatedArrival() : row.getEstimatedDeparture();
        LocalTime time = parseTime(fieldValue);
        if (time == null) {
            return;
        }
        long offsetMinutes = java.time.Duration.between(anchor, time).toMinutes();
        long newOffset = Math.round(offsetMinutes * ratio);
        String stretched = anchor.plusMinutes(newOffset).format(HH_MM);
        if (isArrival) {
            row.setEstimatedArrival(stretched);
            if (row.getArrivalExact() != null) {
                row.setArrivalExact(stretched);
            }
        } else {
            row.setEstimatedDeparture(stretched);
            if (row.getDepartureExact() != null) {
                row.setDepartureExact(stretched);
            }
        }
    }

    private void interpolateTimes(
            TimetableRowData newRow, TimetableRowData before, TimetableRowData after) {
        LocalTime depBefore = parseTime(before.getEstimatedDeparture());
        if (depBefore == null) {
            depBefore = parseTime(before.getEstimatedArrival());
        }
        LocalTime arrAfter = after != null ? parseTime(after.getEstimatedArrival()) : null;
        if (arrAfter == null && after != null) {
            arrAfter = parseTime(after.getEstimatedDeparture());
        }

        if (depBefore != null && arrAfter != null) {
            long midMinutes = java.time.Duration.between(depBefore, arrAfter).toMinutes() / 2;
            LocalTime mid = depBefore.plusMinutes(midMinutes);
            newRow.setEstimatedArrival(mid.format(HH_MM));
            int dwell = newRow.getDwellMinutes() != null ? newRow.getDwellMinutes() : 2;
            newRow.setEstimatedDeparture(mid.plusMinutes(dwell).format(HH_MM));
        } else if (depBefore != null) {
            LocalTime est = depBefore.plusMinutes(10);
            newRow.setEstimatedArrival(est.format(HH_MM));
            newRow.setEstimatedDeparture(est.plusMinutes(2).format(HH_MM));
        }
    }

    private LocalTime parseTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalTime.parse(value, HH_MM);
        } catch (Exception e) {
            return null;
        }
    }
}
