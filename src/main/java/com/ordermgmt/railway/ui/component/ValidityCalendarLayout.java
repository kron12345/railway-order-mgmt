package com.ordermgmt.railway.ui.component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.component.html.Div;

/**
 * Stateless presentation + layout building blocks for {@link ValidityCalendar}: the weekday/action
 * buttons, header and empty/spacer cells, the four day-cell state styles, and the month/week range
 * math. Extracted so the calendar widget keeps only its state, selection logic and cell wiring.
 */
final class ValidityCalendarLayout {

    static final String[] WEEKDAY_SHORT = {"Mo", "Di", "Mi", "Do", "Fr", "Sa", "So"};
    static final String[] WEEKDAY_FULL = {
        "Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag", "Samstag", "Sonntag"
    };

    private static final int DAYS_PER_WEEK = 7;

    private ValidityCalendarLayout() {}

    // ── Toolbar buttons ────────────────────────────────────────────────

    static Div weekdayButton(String label, String tooltip) {
        Div button = new Div();
        button.setText(label);
        button.getElement().setAttribute("title", "Alle " + tooltip + " auswählen/abwählen");
        button.getStyle()
                .set("padding", "3px 8px")
                .set("border-radius", "4px")
                .set("font-family", "'JetBrains Mono', monospace")
                .set("font-size", "11px")
                .set("font-weight", "600")
                .set("cursor", "pointer")
                .set("color", "var(--rom-text-secondary)")
                .set("background", "var(--rom-bg-primary)")
                .set("border", "1px solid var(--rom-border)")
                .set("transition", "all 0.15s");
        return button;
    }

    static Div actionButton(String label, String color) {
        Div button = new Div();
        button.setText(label);
        button.getStyle()
                .set("padding", "3px 8px")
                .set("border-radius", "4px")
                .set("font-family", "'JetBrains Mono', monospace")
                .set("font-size", "10px")
                .set("font-weight", "600")
                .set("cursor", "pointer")
                .set("color", color)
                .set("background", "color-mix(in srgb, " + color + " 10%, transparent)")
                .set("border", "1px solid " + color);
        return button;
    }

    // ── Header / spacer cells ──────────────────────────────────────────

    static Div weekdayHeader() {
        Div header = new Div();
        header.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "80px repeat(7, 1fr)")
                .set("gap", "2px")
                .set("margin-bottom", "2px");

        Div corner = new Div();
        corner.getStyle().set("background", "transparent");
        header.add(corner);

        for (String day : WEEKDAY_SHORT) {
            Div headerCell = new Div();
            headerCell.setText(day);
            headerCell
                    .getStyle()
                    .set("text-align", "center")
                    .set("font-family", "'JetBrains Mono', monospace")
                    .set("font-size", "10px")
                    .set("font-weight", "600")
                    .set("color", "var(--rom-text-muted)")
                    .set("padding", "4px 0");
            header.add(headerCell);
        }
        return header;
    }

    static Div compactHeaderCell(String text) {
        Div headerCell = new Div();
        headerCell.setText(text);
        headerCell
                .getStyle()
                .set("text-align", "center")
                .set("font-weight", "600")
                .set("color", "var(--rom-text-muted)")
                .set("line-height", "20px")
                .set("border-bottom", "1px solid var(--rom-border)");
        return headerCell;
    }

    static Div compactEmptyCell() {
        Div cell = new Div();
        cell.getStyle().set("width", "20px").set("height", "20px");
        return cell;
    }

    static Div emptyCell() {
        Div cell = new Div();
        cell.getStyle().set("min-height", "26px");
        return cell;
    }

    // ── Day-cell state styles ──────────────────────────────────────────

    static void applyDisabled(Div cell) {
        cell.getStyle().set("color", "rgba(148,163,184,0.15)").set("background", "transparent");
    }

    static void applyUnselected(Div cell) {
        cell.getStyle()
                .set("color", "var(--rom-text-secondary)")
                .set("background", "var(--rom-bg-primary)")
                .set("border", "1px solid transparent")
                .set("font-weight", "600");
    }

    static void applySelected(Div cell) {
        cell.getStyle()
                .set("color", "var(--rom-bg-primary)")
                .set("background", "var(--rom-accent)")
                .set("border", "1px solid transparent")
                .set("font-weight", "700");
    }

    static void applyOccupied(Div cell) {
        cell.getStyle()
                .set("color", "#b45309")
                .set("background", "rgba(245, 158, 11, 0.18)")
                .set("border", "1px dashed #f59e0b")
                .set("font-weight", "700");
    }

    // ── Month / week range math ────────────────────────────────────────

    static List<YearMonth> monthsInRange(LocalDate minDate, LocalDate maxDate) {
        List<YearMonth> months = new ArrayList<>();
        YearMonth current = YearMonth.from(minDate);
        YearMonth end = YearMonth.from(maxDate);
        while (!current.isAfter(end)) {
            months.add(current);
            current = current.plusMonths(1);
        }
        return months;
    }

    static int weeksInMonth(YearMonth yearMonth) {
        int firstDayOfWeek = yearMonth.atDay(1).getDayOfWeek().getValue();
        int totalSlots = firstDayOfWeek - 1 + yearMonth.lengthOfMonth();
        return (totalSlots + DAYS_PER_WEEK - 1) / DAYS_PER_WEEK;
    }
}
