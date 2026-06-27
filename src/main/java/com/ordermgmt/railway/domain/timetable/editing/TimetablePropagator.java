package com.ordermgmt.railway.domain.timetable.editing;

import static com.ordermgmt.railway.domain.timetable.editing.TimetableAnchors.hasUserEnteredArrival;
import static com.ordermgmt.railway.domain.timetable.editing.TimetableTimeMath.dayOffset;
import static com.ordermgmt.railway.domain.timetable.editing.TimetableTimeMath.firstNonNull;
import static com.ordermgmt.railway.domain.timetable.editing.TimetableTimeMath.format;
import static com.ordermgmt.railway.domain.timetable.editing.TimetableTimeMath.offsetOrZero;
import static com.ordermgmt.railway.domain.timetable.editing.TimetableTimeMath.parseTime;
import static com.ordermgmt.railway.domain.timetable.editing.TimetableTimeMath.toAbsoluteMinutes;
import static com.ordermgmt.railway.domain.timetable.editing.TimetableTimeMath.wallClock;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.ordermgmt.railway.domain.timetable.model.TimePropagationMode;
import com.ordermgmt.railway.domain.timetable.model.TimetableRowData;

/**
 * Time-propagation engine: given a row whose arrival/departure anchor changed by a delta, push the
 * change to the neighbours. {@link TimePropagationMode#STRETCH} scales the times up to the next
 * pinned row (preserving where the schedule ends); {@link TimePropagationMode#SHIFT} rigidly
 * translates every row until the first pin. Forward (departure → following) and backward (arrival →
 * preceding) are symmetric. Operates in place on the row list.
 */
public final class TimetablePropagator {

    private TimetablePropagator() {}

    /**
     * Propagate a delta forwards (towards higher indices). On STRETCH, scale the times between
     * {@code changedIndex} and the next pinned row; on SHIFT, rigidly translate until the first
     * pin.
     */
    public static void propagateForward(
            List<TimetableRowData> rows,
            int changedIndex,
            long deltaMinutes,
            TimePropagationMode mode,
            LocalTime oldAnchor,
            LocalTime newAnchor) {
        // Regel 7: forward stretch needs a real anchor ahead — either an explicit pin or a
        // user-entered arrival on the destination. Without one, fall back to SHIFT.
        if (mode == TimePropagationMode.STRETCH && !hasForwardAnchor(rows, changedIndex)) {
            shiftFollowingTimes(rows, changedIndex, deltaMinutes);
            return;
        }
        if (mode == TimePropagationMode.STRETCH) {
            stretchToNextPin(rows, changedIndex, oldAnchor, newAnchor);
        } else {
            shiftFollowingTimes(rows, changedIndex, deltaMinutes);
        }
    }

    /**
     * Propagate a delta backwards (towards lower indices), symmetric to {@link #propagateForward}.
     */
    public static void propagateBackward(
            List<TimetableRowData> rows,
            int changedIndex,
            long deltaMinutes,
            TimePropagationMode mode,
            LocalTime oldAnchor,
            LocalTime newAnchor) {
        if (mode == TimePropagationMode.STRETCH) {
            stretchToPreviousPin(rows, changedIndex, oldAnchor, newAnchor);
        } else {
            shiftPrecedingTimes(rows, changedIndex, deltaMinutes);
        }
    }

    private static boolean hasForwardAnchor(List<TimetableRowData> rows, int from) {
        for (int i = from + 1; i < rows.size(); i++) {
            if (isPinned(rows.get(i))) {
                return true;
            }
        }
        // Destination acts as an implicit pin only if its arrival was user-entered.
        if (!rows.isEmpty()) {
            TimetableRowData last = rows.get(rows.size() - 1);
            if (hasUserEnteredArrival(last)) {
                return true;
            }
        }
        return false;
    }

    private static void shiftFollowingTimes(
            List<TimetableRowData> rows, int fromIndex, long deltaMinutes) {
        // The changed row itself is left untouched — its anchors are already at the new value; the
        // caller syncs the matching estimated* field separately so we don't double-shift.
        for (int i = fromIndex + 1; i < rows.size(); i++) {
            TimetableRowData row = rows.get(i);
            if (isPinned(row)) {
                break;
            }
            shiftFieldWithOffsetUpdate(row, true, deltaMinutes);
            shiftFieldWithOffsetUpdate(row, false, deltaMinutes);
        }
    }

    private static void shiftPrecedingTimes(
            List<TimetableRowData> rows, int fromIndex, long deltaMinutes) {
        for (int i = fromIndex - 1; i >= 0; i--) {
            TimetableRowData row = rows.get(i);
            if (isPinned(row)) {
                break;
            }
            shiftFieldWithOffsetUpdate(row, true, deltaMinutes);
            shiftFieldWithOffsetUpdate(row, false, deltaMinutes);
        }
    }

    private static void stretchToNextPin(
            List<TimetableRowData> rows,
            int changedIndex,
            LocalTime oldAnchor,
            LocalTime newAnchor) {
        int pinIndex = findNextPin(rows, changedIndex);
        if (pinIndex <= changedIndex) {
            return;
        }
        TimetableRowData pinRow = rows.get(pinIndex);
        LocalTime pinTime =
                firstNonNull(
                        parseTime(pinRow.getEstimatedArrival()),
                        parseTime(pinRow.getEstimatedDeparture()));
        if (pinTime == null) {
            return;
        }
        long oldSpan = Duration.between(oldAnchor, pinTime).toMinutes();
        long newSpan = Duration.between(newAnchor, pinTime).toMinutes();
        if (oldSpan <= 0 || newSpan <= 0) {
            return;
        }
        double ratio = (double) newSpan / oldSpan;
        for (int i = changedIndex + 1; i < pinIndex; i++) {
            TimetableRowData row = rows.get(i);
            stretchField(row, true, oldAnchor, newAnchor, ratio);
            stretchField(row, false, oldAnchor, newAnchor, ratio);
        }
    }

    private static void stretchToPreviousPin(
            List<TimetableRowData> rows,
            int changedIndex,
            LocalTime oldAnchor,
            LocalTime newAnchor) {
        int pinIndex = findPreviousPin(rows, changedIndex);
        if (pinIndex >= changedIndex) {
            return;
        }
        TimetableRowData pinRow = rows.get(pinIndex);
        LocalTime pinTime =
                firstNonNull(
                        parseTime(pinRow.getEstimatedDeparture()),
                        parseTime(pinRow.getEstimatedArrival()));
        if (pinTime == null) {
            return;
        }
        // Span is negative (pin before changedIndex); compute with absolute magnitudes.
        long oldSpan = Duration.between(pinTime, oldAnchor).toMinutes();
        long newSpan = Duration.between(pinTime, newAnchor).toMinutes();
        if (oldSpan <= 0 || newSpan <= 0) {
            return;
        }
        double ratio = (double) newSpan / oldSpan;
        for (int i = pinIndex + 1; i < changedIndex; i++) {
            TimetableRowData row = rows.get(i);
            scaleOffsetFromAnchor(row, true, pinTime, ratio);
            scaleOffsetFromAnchor(row, false, pinTime, ratio);
        }
    }

    /** Scale every time field's offset from {@code anchor} by {@code ratio}, in place. */
    private static void scaleOffsetFromAnchor(
            TimetableRowData row, boolean isArrival, LocalTime anchor, double ratio) {
        for (TimeFieldAccessor f : timeFields(row, isArrival)) {
            LocalTime t = parseTime(f.getter().get());
            if (t == null) {
                continue;
            }
            long offset = Duration.between(anchor, t).toMinutes();
            long newOffset = Math.round(offset * ratio);
            f.setter().accept(format(anchor.plusMinutes(newOffset)));
        }
    }

    /**
     * Stretch every time field on the side: rebase from {@code oldAnchor} to {@code newAnchor},
     * scaling its offset by {@code ratio}, so neighbour rows' effective anchors stay consistent
     * regardless of their mode.
     */
    private static void stretchField(
            TimetableRowData row,
            boolean isArrival,
            LocalTime oldAnchor,
            LocalTime newAnchor,
            double ratio) {
        for (TimeFieldAccessor f : timeFields(row, isArrival)) {
            LocalTime t = parseTime(f.getter().get());
            if (t == null) {
                continue;
            }
            long originalOffset = Duration.between(oldAnchor, t).toMinutes();
            long newOffset = Math.round(originalOffset * ratio);
            f.setter().accept(format(newAnchor.plusMinutes(newOffset)));
        }
    }

    /** Lightweight getter/setter pair so the shift/stretch loops can iterate over fields. */
    private record TimeFieldAccessor(Supplier<String> getter, Consumer<String> setter) {}

    /** All time fields for one side of a row (arrival or departure) in iteration order. */
    private static List<TimeFieldAccessor> timeFields(TimetableRowData row, boolean isArrival) {
        if (isArrival) {
            return List.of(
                    new TimeFieldAccessor(row::getEstimatedArrival, row::setEstimatedArrival),
                    new TimeFieldAccessor(row::getArrivalExact, row::setArrivalExact),
                    new TimeFieldAccessor(row::getArrivalEarliest, row::setArrivalEarliest),
                    new TimeFieldAccessor(row::getArrivalLatest, row::setArrivalLatest),
                    new TimeFieldAccessor(row::getCommercialArrival, row::setCommercialArrival));
        }
        return List.of(
                new TimeFieldAccessor(row::getEstimatedDeparture, row::setEstimatedDeparture),
                new TimeFieldAccessor(row::getDepartureExact, row::setDepartureExact),
                new TimeFieldAccessor(row::getDepartureEarliest, row::setDepartureEarliest),
                new TimeFieldAccessor(row::getDepartureLatest, row::setDepartureLatest),
                new TimeFieldAccessor(row::getCommercialDeparture, row::setCommercialDeparture));
    }

    private static int findNextPin(List<TimetableRowData> rows, int from) {
        for (int i = from + 1; i < rows.size(); i++) {
            if (isPinned(rows.get(i))) {
                return i;
            }
        }
        return rows.size() - 1;
    }

    private static int findPreviousPin(List<TimetableRowData> rows, int from) {
        for (int i = from - 1; i >= 0; i--) {
            if (isPinned(rows.get(i))) {
                return i;
            }
        }
        return 0;
    }

    /**
     * Apply a minute delta to every time field on the given side, updating the side's day offset on
     * the row when a time wraps past midnight (or before day 0).
     */
    private static void shiftFieldWithOffsetUpdate(
            TimetableRowData row, boolean isArrival, long deltaMinutes) {
        int currentOffset =
                isArrival
                        ? offsetOrZero(row.getArrivalOffset())
                        : offsetOrZero(row.getDepartureOffset());
        Integer newOffset = null;
        for (TimeFieldAccessor f : timeFields(row, isArrival)) {
            LocalTime t = parseTime(f.getter().get());
            if (t == null) {
                continue;
            }
            long abs = toAbsoluteMinutes(t, currentOffset) + deltaMinutes;
            f.setter().accept(format(wallClock(abs)));
            if (newOffset == null) {
                newOffset = dayOffset(abs);
            }
        }
        if (newOffset != null) {
            if (isArrival) {
                row.setArrivalOffset(newOffset);
            } else {
                row.setDepartureOffset(newOffset);
            }
        }
    }

    public static boolean isPinned(TimetableRowData row) {
        return Boolean.TRUE.equals(row.getPinned());
    }
}
