package com.ordermgmt.railway.domain.order.model;

import java.time.DayOfWeek;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Helper for the Verkehrstage weekday-set stored on an expression as comma-separated ISO weekday
 * numbers (1 = Monday … 7 = Sunday). Two expressions of the same train must not share a weekday on
 * overlapping dates; this is the weekday half of that disjointness check (reused by deadline
 * rules).
 */
public final class Weekdays {

    private Weekdays() {}

    public static Set<DayOfWeek> parse(String csv) {
        Set<DayOfWeek> days = new LinkedHashSet<>();
        if (csv == null || csv.isBlank()) {
            return days;
        }
        for (String part : csv.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                days.add(DayOfWeek.of(Integer.parseInt(trimmed)));
            }
        }
        return days;
    }

    public static String format(Set<DayOfWeek> days) {
        StringBuilder formatted = new StringBuilder();
        for (DayOfWeek day : days) {
            if (formatted.length() > 0) {
                formatted.append(',');
            }
            formatted.append(day.getValue());
        }
        return formatted.toString();
    }

    /** True when the two weekday-sets share at least one day (empty sets never overlap). */
    public static boolean overlaps(String firstCsv, String secondCsv) {
        Set<DayOfWeek> firstDays = parse(firstCsv);
        firstDays.retainAll(parse(secondCsv));
        return !firstDays.isEmpty();
    }
}
