package com.ordermgmt.railway.ui.component.timetable;

import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.DASH;
import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.arrivalConstraintLabel;
import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.departureConstraintLabel;
import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.findActivityOption;
import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.timeOrDash;
import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.timingQualifierCode;

import java.util.List;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Span;

import com.ordermgmt.railway.domain.timetable.model.TimeConstraintMode;
import com.ordermgmt.railway.domain.timetable.model.TimetableActivityOption;
import com.ordermgmt.railway.domain.timetable.model.TimetableRowData;
import com.ordermgmt.railway.domain.timetable.service.TimetableEditingService;

/**
 * Renders the {@link TimetableTableStep} grid cells and their tooltips: TTT-aware arrival/departure
 * /point/dwell cells (user-entered contractual values get the bold accent + "TTT" badge) and the
 * per-column tooltips. Split out of the table step so each stays readable. Holds the rows list so
 * it can tell origin/destination apart (those endpoints suppress the opposite-side time).
 */
class TimetableRowCells {

    private final TimetableEditingService editingService;
    private final List<TimetableActivityOption> activityOptions;
    private final Component i18n;
    private final List<TimetableRowData> rows;

    TimetableRowCells(
            TimetableEditingService editingService,
            List<TimetableActivityOption> activityOptions,
            Component i18n,
            List<TimetableRowData> rows) {
        this.editingService = editingService;
        this.activityOptions = activityOptions;
        this.i18n = i18n;
        this.rows = rows;
    }

    // ── Cell renderers ────────────────────────────────────────────────

    /**
     * Arrival/departure cell. User-entered (TTT-exportable) values render bold-accent with a "TTT"
     * badge; endpoints suppress the side they cannot have (origin no arrival, destination none).
     */
    Span renderTimeCell(TimetableRowData row, boolean isArrival) {
        if (isArrival && isOrigin(row)) {
            return new Span();
        }
        if (!isArrival && isDestination(row)) {
            return new Span();
        }
        boolean userEntered =
                isArrival
                        ? editingService.hasUserEnteredArrival(row)
                        : editingService.hasUserEnteredDeparture(row);
        String text = isArrival ? combinedArrivalLabel(row) : combinedDepartureLabel(row);
        if (text.startsWith("● ")) {
            text = text.substring(2);
        }
        return tttCell(text, userEntered);
    }

    /** Operational-point name cell; marked TTT when the row is sent in the Path Request. */
    Span renderPointCell(TimetableRowData row) {
        return tttCell(
                row.getName() == null ? "" : row.getName(), editingService.isExportedToTtt(row));
    }

    /** Dwell cell; marked TTT when the user explicitly entered a dwell. */
    Span renderDwellCell(TimetableRowData row) {
        return tttCell(dwellLabel(row), Boolean.TRUE.equals(row.getUserEnteredDwell()));
    }

    private Span tttCell(String text, boolean ttt) {
        Span span = new Span();
        if (ttt && text != null && !text.isBlank()) {
            Span badge = new Span("TTT");
            badge.getStyle()
                    .set("font-size", "9px")
                    .set("font-weight", "700")
                    .set("background", "var(--rom-accent)")
                    .set("color", "var(--rom-bg-base, #1a1a1a)")
                    .set("padding", "1px 5px")
                    .set("border-radius", "3px")
                    .set("margin-right", "6px")
                    .set("letter-spacing", "0.04em")
                    .set("vertical-align", "middle");
            Span value = new Span(text);
            value.getStyle().set("font-weight", "600").set("color", "var(--rom-accent)");
            span.add(badge, value);
        } else {
            span.setText(text == null ? "" : text);
            if (text != null && !text.isBlank()) {
                span.getStyle().set("color", "var(--rom-text-muted, var(--rom-text-primary))");
            }
        }
        return span;
    }

    private String combinedArrivalLabel(TimetableRowData row) {
        String marker = editingService.hasUserEnteredArrival(row) ? "● " : "";
        String offsetSuffix = formatOffsetSuffix(row.getArrivalOffset());
        String constraint = arrivalConstraintLabel(row);
        if (!DASH.equals(constraint)) {
            String qualifier = timingQualifierCode(row.getArrivalMode(), true);
            return marker
                    + constraint
                    + (qualifier != null ? " [" + qualifier + "]" : "")
                    + offsetSuffix;
        }
        return timeOrDash(row.getEstimatedArrival()) + offsetSuffix;
    }

    private String combinedDepartureLabel(TimetableRowData row) {
        String marker = editingService.hasUserEnteredDeparture(row) ? "● " : "";
        String offsetSuffix = formatOffsetSuffix(row.getDepartureOffset());
        String constraint = departureConstraintLabel(row);
        if (!DASH.equals(constraint)) {
            String qualifier = timingQualifierCode(row.getDepartureMode(), false);
            return marker
                    + constraint
                    + (qualifier != null ? " [" + qualifier + "]" : "")
                    + offsetSuffix;
        }
        return timeOrDash(row.getEstimatedDeparture()) + offsetSuffix;
    }

    private String formatOffsetSuffix(Integer offset) {
        if (offset == null || offset == 0) {
            return "";
        }
        return offset > 0 ? " +" + offset + "d" : " " + offset + "d";
    }

    private String dwellLabel(TimetableRowData row) {
        Integer dwell = row.getDwellMinutes();
        return dwell != null && dwell > 0 ? dwell + "'" : "";
    }

    // ── Tooltips ──────────────────────────────────────────────────────

    String pointTooltip(TimetableRowData row) {
        StringBuilder sb = new StringBuilder();
        if (row.getName() != null) {
            sb.append(row.getName());
        }
        if (row.getUopid() != null && !row.getUopid().isBlank()) {
            sb.append(" (").append(row.getCountry() == null ? "" : row.getCountry() + " ");
            sb.append(row.getUopid()).append(")");
        }
        if (row.getRoutePointRole() != null) {
            sb.append("\n").append(row.getRoutePointRole().name());
        }
        return sb.toString();
    }

    String arrivalTooltip(TimetableRowData row) {
        return timeSideTooltip(
                true,
                row.getArrivalMode(),
                row.getArrivalExact(),
                row.getArrivalEarliest(),
                row.getArrivalLatest(),
                row.getCommercialArrival(),
                row.getEstimatedArrival());
    }

    String departureTooltip(TimetableRowData row) {
        return timeSideTooltip(
                false,
                row.getDepartureMode(),
                row.getDepartureExact(),
                row.getDepartureEarliest(),
                row.getDepartureLatest(),
                row.getCommercialDeparture(),
                row.getEstimatedDeparture());
    }

    private String timeSideTooltip(
            boolean arrival,
            TimeConstraintMode mode,
            String exact,
            String earliest,
            String latest,
            String commercial,
            String estimated) {
        if (mode == null) {
            mode = TimeConstraintMode.NONE;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(t(arrival ? "timetable.editor.arrival" : "timetable.editor.departure"));
        sb.append(" — ").append(t("timetable.timeMode." + mode.name())).append("\n");
        switch (mode) {
            case EXACT -> sb.append(arrival ? "ALA: " : "ALD: ").append(timeOrDash(exact));
            case WINDOW ->
                    sb.append(arrival ? "ELA: " : "ELD: ")
                            .append(timeOrDash(earliest))
                            .append("\n")
                            .append(arrival ? "LLA: " : "LLD: ")
                            .append(timeOrDash(latest));
            case AFTER ->
                    sb.append("≥ ")
                            .append(arrival ? "ELA: " : "ELD: ")
                            .append(timeOrDash(earliest));
            case BEFORE ->
                    sb.append("≤ ").append(arrival ? "LLA: " : "LLD: ").append(timeOrDash(latest));
            case COMMERCIAL ->
                    sb.append(arrival ? "PLA: " : "PLD: ").append(timeOrDash(commercial));
            case NONE -> sb.append(DASH);
        }
        if (estimated != null && !estimated.isBlank()) {
            sb.append("\n")
                    .append(
                            t(
                                    arrival
                                            ? "timetable.editor.estimatedArrival"
                                            : "timetable.editor.estimatedDeparture"))
                    .append(": ")
                    .append(estimated);
        }
        return sb.toString();
    }

    String dwellTooltip(TimetableRowData row) {
        Integer dwell = row.getDwellMinutes();
        if (dwell == null || dwell <= 0) {
            return Boolean.TRUE.equals(row.getHalt()) ? t("timetable.editor.halt") : DASH;
        }
        return t("timetable.editor.dwell") + ": " + dwell + " min";
    }

    String activityTooltip(TimetableRowData row) {
        if (row.getActivityCodes() != null && !row.getActivityCodes().isEmpty()) {
            return row.getActivityCodes().stream()
                    .map(
                            code ->
                                    findActivityOption(code, activityOptions)
                                            .map(opt -> opt.code() + " — " + opt.label())
                                            .orElse(code))
                    .reduce((left, right) -> left + "\n" + right)
                    .orElse(DASH);
        }
        return findActivityOption(row.getActivityCode(), activityOptions)
                .map(opt -> opt.code() + " — " + opt.label())
                .orElseGet(
                        () ->
                                Boolean.TRUE.equals(row.getHalt())
                                        ? t("timetable.stop.activityRequired")
                                        : DASH);
    }

    private boolean isOrigin(TimetableRowData row) {
        return row != null && !rows.isEmpty() && row == rows.getFirst();
    }

    private boolean isDestination(TimetableRowData row) {
        return row != null && !rows.isEmpty() && row == rows.getLast();
    }

    private String t(String key, Object... params) {
        return i18n.getTranslation(key, params);
    }
}
