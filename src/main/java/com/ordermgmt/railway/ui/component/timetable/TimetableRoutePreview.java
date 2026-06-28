package com.ordermgmt.railway.ui.component.timetable;

import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.distanceLabel;
import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.timeOrDash;

import java.util.List;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

import com.ordermgmt.railway.domain.timetable.model.TimetableRouteResult;
import com.ordermgmt.railway.domain.timetable.model.TimetableRowData;

/**
 * Renders the compact route preview (dotted point list with cumulative distance) and the one-line
 * route summary for {@link TimetableRouteStep}. Pure rendering into a caller-owned {@link Div};
 * extracted so the route step keeps its form/calculation focus.
 */
final class TimetableRoutePreview {

    private TimetableRoutePreview() {}

    /** Rebuilds the preview list into {@code target}; hides it when there are no rows. */
    static void render(Div target, List<TimetableRowData> rows, Component i18n) {
        target.removeAll();
        if (rows == null || rows.isEmpty()) {
            target.setVisible(false);
            return;
        }
        target.setVisible(true);
        target.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "0")
                .set("padding", "12px 0");
        Span header = new Span(i18n.getTranslation("timetable.route.preview"));
        header.getStyle()
                .set("font-weight", "600")
                .set("font-size", "11px")
                .set("color", "var(--rom-text-primary)")
                .set("margin-bottom", "6px");
        target.add(header);
        for (TimetableRowData row : rows) {
            Div line = new Div();
            line.getStyle()
                    .set("display", "flex")
                    .set("align-items", "center")
                    .set("gap", "8px")
                    .set("padding", "2px 0")
                    .set("font-size", "11px");
            Span dot = new Span("●");
            dot.getStyle().set("color", dotColor(row)).set("font-size", "8px");
            Span name = new Span(row.getName());
            name.getStyle().set("color", "var(--rom-text-primary)");
            Span km = new Span(distanceLabel(row.getDistanceFromStartMeters()));
            km.getStyle()
                    .set("color", "var(--rom-text-muted)")
                    .set("font-family", "'JetBrains Mono', monospace")
                    .set("font-size", "10px");
            line.add(dot, name, km);
            target.add(line);
        }
    }

    /** One-line summary: stop count, total distance, first departure and last arrival. */
    static String summary(List<TimetableRowData> rows, TimetableRouteResult route, Component i18n) {
        if (rows == null || rows.isEmpty()) {
            return i18n.getTranslation("timetable.route.empty");
        }
        return i18n.getTranslation(
                "timetable.route.summary",
                rows.size(),
                distanceLabel(route.totalLengthMeters()),
                timeOrDash(rows.getFirst().getEstimatedDeparture()),
                timeOrDash(rows.getLast().getEstimatedArrival()));
    }

    private static String dotColor(TimetableRowData row) {
        if (row.getRoutePointRole() == null) {
            return "var(--rom-text-muted)";
        }
        return switch (row.getRoutePointRole()) {
            case ORIGIN, DESTINATION -> "var(--rom-accent)";
            case VIA -> "var(--rom-status-info, #3b82f6)";
            default -> "var(--rom-text-muted)";
        };
    }
}
