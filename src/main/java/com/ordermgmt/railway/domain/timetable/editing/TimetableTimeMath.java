package com.ordermgmt.railway.domain.timetable.editing;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import com.ordermgmt.railway.domain.timetable.model.TimeConstraintMode;

/**
 * Pure time/offset arithmetic shared by the timetable-editing helpers: HH:mm parse/format, absolute
 * minute conversion with day offsets, midnight-wrapping wall-clock, and a few small null/enum
 * guards. No state, no domain rules — just the math the propagation/interpolation logic builds on.
 */
public final class TimetableTimeMath {

    public static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");
    public static final long MINUTES_PER_DAY = 1440L;

    private TimetableTimeMath() {}

    public static LocalTime parseTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalTime.parse(value, HH_MM);
        } catch (Exception e) {
            return null;
        }
    }

    public static String format(LocalTime time) {
        return time.format(HH_MM);
    }

    /** Convert a (LocalTime, dayOffset) pair into absolute minutes since day 0. */
    public static long toAbsoluteMinutes(LocalTime time, int offsetDays) {
        if (time == null) {
            return 0L;
        }
        return offsetDays * MINUTES_PER_DAY + (time.toSecondOfDay() / 60L);
    }

    /** Modular wall-clock time from absolute minutes — wraps within [00:00, 23:59]. */
    public static LocalTime wallClock(long absMinutes) {
        long mod = ((absMinutes % MINUTES_PER_DAY) + MINUTES_PER_DAY) % MINUTES_PER_DAY;
        return LocalTime.of((int) (mod / 60), (int) (mod % 60));
    }

    /** Day offset for absolute minutes (negative if before day 0). */
    public static int dayOffset(long absMinutes) {
        return (int) Math.floorDiv(absMinutes, MINUTES_PER_DAY);
    }

    public static boolean sameInstant(LocalTime a, int aOff, LocalTime b, int bOff) {
        return a.equals(b) && aOff == bOff;
    }

    @SafeVarargs
    public static <T> T firstNonNull(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    public static int offsetOrZero(Integer offset) {
        return offset == null ? 0 : offset;
    }

    public static TimeConstraintMode modeOrNone(TimeConstraintMode mode) {
        return mode == null ? TimeConstraintMode.NONE : mode;
    }
}
