package com.ordermgmt.railway.domain.order.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Resolves the operating days a position actually runs: its explicit validity date-set, or — when
 * that is unset — the days derived from its weekday template within its own start/end range. Empty
 * when the position is unscheduled (no validity, no weekday template, or no range). Shared by the
 * Verkehrstage picker, deadline evaluation, and the per-demand calendar so the "Bedarf days ⊆
 * expression days" rule and disjointness all read the same source.
 */
public final class OperatingDays {

    private OperatingDays() {}

    public static List<LocalDate> of(OrderPosition position) {
        List<LocalDate> fromValidity = ValidityJsonCodec.fromJson(position.getValidity());
        if (!fromValidity.isEmpty()) {
            return fromValidity;
        }
        Set<DayOfWeek> weekdays = Weekdays.parse(position.getWeekdays());
        if (weekdays.isEmpty() || position.getStart() == null || position.getEnd() == null) {
            return List.of();
        }
        List<LocalDate> days = new ArrayList<>();
        LocalDate end = position.getEnd().toLocalDate();
        for (LocalDate d = position.getStart().toLocalDate(); !d.isAfter(end); d = d.plusDays(1)) {
            if (weekdays.contains(d.getDayOfWeek())) {
                days.add(d);
            }
        }
        return days;
    }
}
