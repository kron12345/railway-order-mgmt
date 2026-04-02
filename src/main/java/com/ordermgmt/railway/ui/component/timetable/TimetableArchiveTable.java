package com.ordermgmt.railway.ui.component.timetable;

import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.arrivalConstraintLabel;
import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.departureConstraintLabel;
import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.timeOrDash;
import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.timingQualifierCode;

import java.util.List;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import com.ordermgmt.railway.domain.timetable.model.RoutePointRole;
import com.ordermgmt.railway.domain.timetable.model.TimetableRowData;

/** Read-only, Div-based timetable table with color-coded rows. */
public class TimetableArchiveTable extends VerticalLayout {

    private final List<TimetableRowData> rows;

    public TimetableArchiveTable(List<TimetableRowData> rows, String routeSummary) {
        this.rows = rows;
        setPadding(false);
        setSpacing(false);
        setSizeFull();
        getStyle()
                .set("background", "var(--rom-bg-card)")
                .set("border", "1px solid var(--rom-border)")
                .set("border-radius", "6px")
                .set("overflow-y", "auto");

        add(createCardHeader(routeSummary), createTableHeader());
        for (int i = 0; i < rows.size(); i++) {
            add(createRow(rows.get(i), i));
        }
    }

    private Component createCardHeader(String routeSummary) {
        Div wrapper = new Div();
        H2 heading = new H2(getTranslation("timetable.archive.title"));
        heading.getStyle()
                .set("margin", "0")
                .set("font-size", "var(--lumo-font-size-l)")
                .set("color", "var(--rom-text-primary)")
                .set("padding", "12px 16px 8px 16px");

        Span sub = new Span(routeSummary);
        sub.getStyle()
                .set("font-size", "11px")
                .set("font-family", "'JetBrains Mono', monospace")
                .set("color", "var(--rom-text-muted)")
                .set("padding", "0 16px 12px 16px")
                .set("display", "block");

        wrapper.add(heading, sub);
        return wrapper;
    }

    private Component createTableHeader() {
        Div header = new Div();
        applyGridColumns(header);
        header.getStyle()
                .set("padding", "6px 12px")
                .set("font-size", "10px")
                .set("font-family", "'JetBrains Mono', monospace")
                .set("font-weight", "600")
                .set("text-transform", "uppercase")
                .set("letter-spacing", "0.06em")
                .set("color", "var(--rom-accent)")
                .set("background", "var(--rom-bg-secondary)")
                .set("border-bottom", "1px solid var(--rom-border)");

        header.add(
                headerCell("#"),
                headerCell(getTranslation("timetable.table.point")),
                headerCell(getTranslation("timetable.table.arrival")),
                headerCell(getTranslation("timetable.editor.dwell")),
                headerCell(getTranslation("timetable.table.departure")),
                headerCell(getTranslation("timetable.table.activity")),
                headerCell("TTT"));
        return header;
    }

    private Component createRow(TimetableRowData row, int index) {
        Div rowDiv = new Div();
        applyGridColumns(rowDiv);
        rowDiv.getStyle()
                .set("padding", "6px 12px")
                .set("font-size", "12px")
                .set("border-bottom", "1px solid var(--rom-border-subtle)");

        if (index % 2 == 1) {
            rowDiv.getStyle().set("background", "var(--rom-bg-secondary)");
        }
        if (Boolean.TRUE.equals(row.getDeleted())) {
            rowDiv.getStyle().set("text-decoration", "line-through").set("opacity", "0.45");
        }
        applyRowColors(rowDiv, row);

        rowDiv.add(
                seqCell(row),
                nameCell(row),
                timeCell(arrivalDisplay(row)),
                dwellCell(row),
                timeCell(departureDisplay(row)),
                activityCell(row),
                qualifierCell(row));
        return rowDiv;
    }

    // ── Cell builders ─────────────────────────────────────────────────

    private Span headerCell(String text) {
        Span cell = new Span(text);
        cell.getStyle().set("overflow", "hidden").set("text-overflow", "ellipsis");
        return cell;
    }

    private Span seqCell(TimetableRowData row) {
        Span cell = new Span(row.getSequence() != null ? String.valueOf(row.getSequence()) : "");
        cell.getStyle()
                .set("font-family", "'JetBrains Mono', monospace")
                .set("color", "var(--rom-text-muted)")
                .set("font-size", "11px");
        return cell;
    }

    private Span nameCell(TimetableRowData row) {
        Span cell = new Span(row.getName() != null ? row.getName() : "\u2014");
        cell.getStyle()
                .set("font-weight", isOriginOrDestination(row) ? "600" : "400")
                .set("overflow", "hidden")
                .set("text-overflow", "ellipsis")
                .set("white-space", "nowrap");
        return cell;
    }

    private Span timeCell(String value) {
        Span cell = new Span(value);
        cell.getStyle().set("font-family", "'JetBrains Mono', monospace").set("font-size", "12px");
        return cell;
    }

    private Span dwellCell(TimetableRowData row) {
        String text =
                row.getDwellMinutes() != null && row.getDwellMinutes() > 0
                        ? "+" + row.getDwellMinutes()
                        : "\u2014";
        Span cell = new Span(text);
        cell.getStyle()
                .set("font-family", "'JetBrains Mono', monospace")
                .set("font-size", "11px")
                .set("color", "var(--rom-text-muted)");
        return cell;
    }

    private Span activityCell(TimetableRowData row) {
        String code = row.getActivityCode();
        Span cell = new Span(code != null && !code.isBlank() ? code : "\u2014");
        cell.getStyle().set("font-size", "11px");
        return cell;
    }

    private Span qualifierCell(TimetableRowData row) {
        String arrQual = timingQualifierCode(row.getArrivalMode(), true);
        String depQual = timingQualifierCode(row.getDepartureMode(), false);
        String combined = joinQualifiers(arrQual, depQual);
        Span cell = new Span(combined != null ? combined : "\u2014");
        cell.getStyle()
                .set("font-family", "'JetBrains Mono', monospace")
                .set("font-size", "10px")
                .set("color", "var(--rom-text-muted)");
        return cell;
    }

    // ── Row styling ───────────────────────────────────────────────────

    private void applyRowColors(Div rowDiv, TimetableRowData row) {
        if (isOriginOrDestination(row)) {
            rowDiv.getStyle()
                    .set("color", "var(--rom-accent)")
                    .set("font-weight", "600")
                    .set("border-left", "3px solid var(--rom-accent)");
        } else if (Boolean.TRUE.equals(row.getHalt())) {
            rowDiv.getStyle()
                    .set("color", "var(--rom-status-active)")
                    .set(
                            "background",
                            "color-mix(in srgb, var(--rom-status-active) 4%, transparent)");
        } else {
            rowDiv.getStyle().set("color", "var(--rom-text-muted)");
        }
    }

    private void applyGridColumns(Div div) {
        div.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "30px 1fr 55px 40px 55px 80px 50px")
                .set("gap", "8px")
                .set("align-items", "center");
    }

    // ── Display helpers ───────────────────────────────────────────────

    private String arrivalDisplay(TimetableRowData row) {
        String constraint = arrivalConstraintLabel(row);
        if (!"\u2014".equals(constraint)) {
            return constraint;
        }
        return timeOrDash(row.getEstimatedArrival());
    }

    private String departureDisplay(TimetableRowData row) {
        String constraint = departureConstraintLabel(row);
        if (!"\u2014".equals(constraint)) {
            return constraint;
        }
        return timeOrDash(row.getEstimatedDeparture());
    }

    private boolean isOriginOrDestination(TimetableRowData row) {
        return row.getRoutePointRole() == RoutePointRole.ORIGIN
                || row.getRoutePointRole() == RoutePointRole.DESTINATION;
    }

    private String joinQualifiers(String arr, String dep) {
        if (arr == null && dep == null) {
            return null;
        }
        if (arr != null && dep != null) {
            return arr + "/" + dep;
        }
        return arr != null ? arr : dep;
    }
}
