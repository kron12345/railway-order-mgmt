package com.ordermgmt.railway.ui.component.timetable;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.vaadin.flow.component.Component;

import com.ordermgmt.railway.domain.timetable.model.RoutePointRole;
import com.ordermgmt.railway.domain.timetable.model.TimeConstraintMode;
import com.ordermgmt.railway.domain.timetable.model.TimetableActivityOption;
import com.ordermgmt.railway.domain.timetable.model.TimetableRowData;

/**
 * Shared formatting and label helpers for timetable UI components.
 *
 * <p>All methods are package-private and stateless so they can be reused across {@link
 * TimetableTableStep} and the parent builder view.
 */
public final class TimetableFormatUtils {

    public static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private TimetableFormatUtils() {}

    // ── i18n label helpers (need a component for getTranslation) ────────

    public static String roleLabel(RoutePointRole role, Component ctx) {
        if (role == null) {
            return "—";
        }
        return switch (role) {
            case ORIGIN -> ctx.getTranslation("timetable.role.origin");
            case VIA -> ctx.getTranslation("timetable.role.via");
            case DESTINATION -> ctx.getTranslation("timetable.role.destination");
            case AUTO -> ctx.getTranslation("timetable.role.auto");
        };
    }

    public static String timeModeLabel(TimeConstraintMode mode, Component ctx) {
        if (mode == null) {
            return "—";
        }
        return switch (mode) {
            case NONE -> ctx.getTranslation("timetable.mode.none");
            case EXACT -> ctx.getTranslation("timetable.mode.exact");
            case WINDOW -> ctx.getTranslation("timetable.mode.window");
            case COMMERCIAL -> ctx.getTranslation("timetable.mode.commercial");
        };
    }

    public static String activityOptionLabel(TimetableActivityOption option) {
        return option.code() + " · " + option.label();
    }

    public static String activityLabel(
            TimetableRowData row, List<TimetableActivityOption> options) {
        return findActivityOption(row.getActivityCode(), options)
                .map(TimetableFormatUtils::activityOptionLabel)
                .orElse("—");
    }

    public static Optional<TimetableActivityOption> findActivityOption(
            String code, List<TimetableActivityOption> options) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return options.stream().filter(option -> code.equals(option.code())).findFirst();
    }

    // ── constraint labels ───────────────────────────────────────────────

    public static String arrivalConstraintLabel(TimetableRowData row) {
        return timeConstraintLabel(
                row.getArrivalMode(),
                row.getArrivalExact(),
                row.getArrivalEarliest(),
                row.getArrivalLatest(),
                row.getCommercialArrival());
    }

    public static String departureConstraintLabel(TimetableRowData row) {
        return timeConstraintLabel(
                row.getDepartureMode(),
                row.getDepartureExact(),
                row.getDepartureEarliest(),
                row.getDepartureLatest(),
                row.getCommercialDeparture());
    }

    public static String timeConstraintLabel(
            TimeConstraintMode mode,
            String exact,
            String earliest,
            String latest,
            String commercial) {
        TimeConstraintMode resolved = defaultMode(mode);
        return switch (resolved) {
            case NONE -> "—";
            case EXACT -> timeOrDash(exact);
            case WINDOW -> timeOrDash(earliest) + "–" + timeOrDash(latest);
            case COMMERCIAL -> timeOrDash(commercial);
        };
    }

    /** Backward-compatible overload without commercial time. */
    public static String timeConstraintLabel(
            TimeConstraintMode mode, String exact, String earliest, String latest) {
        return timeConstraintLabel(mode, exact, earliest, latest, null);
    }

    // ── value formatting ────────────────────────────────────────────────

    public static String distanceLabel(Double meters) {
        if (meters == null) {
            return "0.0 km";
        }
        return String.format(Locale.GERMANY, "%.1f km", meters / 1000D);
    }

    public static String timeOrDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    public static TimeConstraintMode defaultMode(TimeConstraintMode mode) {
        return mode != null ? mode : TimeConstraintMode.NONE;
    }

    public static LocalTime parseTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalTime.parse(value, TIME_FORMAT);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    public static String formatTime(LocalTime value) {
        return value == null ? null : value.format(TIME_FORMAT);
    }

    public static String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    public static String nvl(String value) {
        return value != null ? value : "—";
    }

    /**
     * Returns the TTT TimingQualifier code for a given constraint mode.
     *
     * <p>ALA = Actual/effective arrival, ALD = Actual/effective departure, ELA = Earliest arrival,
     * LLA = Latest arrival, PLA = Published/commercial arrival, PLD = Published/commercial
     * departure.
     *
     * @param mode the constraint mode
     * @param isArrival true for arrival qualifiers, false for departure
     * @return qualifier code string, or null if NONE
     */
    public static String timingQualifierCode(TimeConstraintMode mode, boolean isArrival) {
        TimeConstraintMode resolved = defaultMode(mode);
        return switch (resolved) {
            case NONE -> null;
            case EXACT -> isArrival ? "ALA" : "ALD";
            case WINDOW -> isArrival ? "ELA/LLA" : "ELD/LLD";
            case COMMERCIAL -> isArrival ? "PLA" : "PLD";
        };
    }
}
