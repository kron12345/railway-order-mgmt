package com.ordermgmt.railway.ui.component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

import com.ordermgmt.railway.domain.order.model.PurchasePosition;
import com.ordermgmt.railway.domain.order.model.PurchaseStatus;

/**
 * Compact calendar grid: one row per month, columns = weekday positions (Mo-So × weeks). Each cell
 * shows the day number, colored by purchase status.
 */
public class PurchaseCalendarGrid extends Div {

    private static final String[] COL_HEADS = {"Mo", "Di", "Mi", "Do", "Fr", "Sa", "So"};

    /** Date when FPJ 2027 begins (timetable year boundary). */
    private static final LocalDate TIMETABLE_YEAR_2027_START = LocalDate.of(2026, 12, 12);

    public PurchaseCalendarGrid(List<PurchasePosition> purchases, LocalDate from, LocalDate to) {
        setWidthFull();
        getStyle()
                .set("overflow-x", "auto")
                .set("border", "1px solid var(--rom-border)")
                .set("border-radius", "6px");

        Map<LocalDate, PurchaseStatus> statusByDate = mapByDate(purchases);
        List<YearMonth> months = buildMonthList(from, to);
        int maxWeeksPerRow = months.stream().mapToInt(this::weeksNeeded).max().orElse(5);

        Div table = new Div();
        int cols = maxWeeksPerRow * 7 + maxWeeksPerRow - 1;
        table.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "100px repeat(" + cols + ", minmax(24px, 1fr))")
                .set("width", "100%")
                .set("font-family", "'JetBrains Mono', monospace")
                .set("font-size", "11px");

        addHeaderRow(table, maxWeeksPerRow);

        for (YearMonth yearMonth : months) {
            addMonthRow(table, yearMonth, maxWeeksPerRow, statusByDate);
        }

        add(table);
    }

    private void addHeaderRow(Div table, int maxWeeks) {
        Div corner = headerCell("");
        corner.getStyle()
                .set("background", "var(--rom-bg-secondary)")
                .set("border-bottom", "1px solid var(--rom-border)")
                .set("position", "sticky")
                .set("left", "0")
                .set("z-index", "2");
        table.add(corner);

        for (int w = 0; w < maxWeeks; w++) {
            if (w > 0) table.add(weekSep(true));
            for (int d = 0; d < 7; d++) {
                Div h = headerCell(COL_HEADS[d]);
                if (d >= 5) h.getStyle().set("color", "rgba(148,163,184,0.3)");
                table.add(h);
            }
        }
    }

    private void addMonthRow(
            Div table,
            YearMonth yearMonth,
            int maxWeeks,
            Map<LocalDate, PurchaseStatus> statusByDate) {
        LocalDate firstOfMonth = yearMonth.atDay(1);
        int startCol = firstOfMonth.getDayOfWeek().getValue() - 1; // Mo=0
        int daysInMonth = yearMonth.lengthOfMonth();

        // Month label
        String timetableYear =
                firstOfMonth.isBefore(TIMETABLE_YEAR_2027_START) ? "FPJ 2026" : "FPJ 2027";
        Div label = new Div();
        label.getStyle()
                .set("background", "var(--rom-bg-secondary)")
                .set("padding", "6px 8px")
                .set("border-bottom", "1px solid var(--rom-border)")
                .set("border-right", "1px solid var(--rom-border)")
                .set("position", "sticky")
                .set("left", "0")
                .set("z-index", "2")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("justify-content", "center");
        Span monthName =
                new Span(
                        yearMonth.getMonth().getDisplayName(TextStyle.SHORT, Locale.GERMAN)
                                + " "
                                + yearMonth.getYear());
        monthName
                .getStyle()
                .set("font-weight", "600")
                .set("font-size", "10px")
                .set("color", "var(--rom-text-secondary)");
        Span timetableYearTag = new Span(timetableYear);
        timetableYearTag.getStyle().set("font-size", "8px").set("color", "var(--rom-accent)");
        label.add(monthName, timetableYearTag);
        table.add(label);

        int dayIdx = 1;
        for (int week = 0; week < maxWeeks; week++) {
            if (week > 0) table.add(weekSep(false));
            for (int weekday = 0; weekday < 7; weekday++) {
                int position = week * 7 + weekday;
                boolean inRange = position >= startCol && dayIdx <= daysInMonth;

                if (inRange) {
                    LocalDate date = yearMonth.atDay(dayIdx);
                    boolean isWeekend = weekday >= 5;
                    PurchaseStatus status = statusByDate.get(date);
                    boolean isTimetableYearBoundary = date.equals(TIMETABLE_YEAR_2027_START);

                    table.add(dayCell(dayIdx, status, isWeekend, isTimetableYearBoundary, date));
                    dayIdx++;
                } else {
                    table.add(emptyCell());
                }
            }
        }
    }

    private Div dayCell(
            int day, PurchaseStatus status, boolean weekend, boolean fpjLine, LocalDate date) {
        Div cell = new Div();
        cell.addClassName("cal-cell");
        cell.getElement().setAttribute("tabindex", "0");
        cell.getElement().setAttribute("role", "gridcell");
        // Show day number + status symbol for color-blind users
        String symbol = statusSymbol(status, weekend);
        cell.setText(day + symbol);

        String bg;
        String color;
        if (weekend && status == null) {
            bg = "rgba(148,163,184,0.02)";
            color = "rgba(148,163,184,0.15)";
        } else if (status == null) {
            bg = "rgba(148,163,184,0.06)";
            color = "var(--rom-text-muted)";
        } else {
            bg =
                    switch (status) {
                        case BESTAETIGT -> "rgba(52,211,153,0.2)";
                        case BESTELLT -> "rgba(96,165,250,0.2)";
                        case OFFEN -> "rgba(148,163,184,0.06)";
                        case ABGELEHNT -> "rgba(248,113,113,0.2)";
                        case STORNIERT -> "rgba(107,114,128,0.1)";
                    };
            color =
                    switch (status) {
                        case BESTAETIGT -> "var(--rom-status-active)";
                        case BESTELLT -> "var(--rom-status-info)";
                        case OFFEN -> "var(--rom-text-muted)";
                        case ABGELEHNT -> "var(--rom-status-danger)";
                        case STORNIERT -> "var(--rom-text-muted)";
                    };
        }

        cell.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("min-height", "28px")
                .set("font-weight", "600")
                .set("background", bg)
                .set("color", color)
                .set("border-bottom", "1px solid var(--rom-border)")
                .set("cursor", "default");

        if (fpjLine) {
            cell.getStyle().set("border-left", "2px solid var(--rom-accent)");
        }

        String statusText = status != null ? status.name() : (weekend ? "WE" : "—");
        String tip =
                String.format(
                        "%02d.%02d.%d %s", day, date.getMonthValue(), date.getYear(), statusText);
        cell.getElement().setAttribute("aria-label", tip);
        com.vaadin.flow.component.shared.Tooltip.forComponent(cell)
                .withText(tip)
                .withPosition(com.vaadin.flow.component.shared.Tooltip.TooltipPosition.TOP);

        return cell;
    }

    private Div emptyCell() {
        Div cell = new Div();
        cell.getStyle()
                .set("min-height", "28px")
                .set("border-bottom", "1px solid var(--rom-border)");
        return cell;
    }

    private Div headerCell(String text) {
        Div h = new Div();
        h.setText(text);
        h.getStyle()
                .set("text-align", "center")
                .set("padding", "4px 2px")
                .set("font-size", "9px")
                .set("font-weight", "600")
                .set("color", "var(--rom-text-muted)")
                .set("background", "var(--rom-bg-secondary)")
                .set("border-bottom", "1px solid var(--rom-border)")
                .set("text-transform", "uppercase");
        return h;
    }

    private String statusSymbol(PurchaseStatus status, boolean weekend) {
        if (weekend && status == null) return "";
        if (status == null) return "";
        return switch (status) {
            case BESTAETIGT -> "✓";
            case BESTELLT -> "→";
            case OFFEN -> "";
            case ABGELEHNT -> "✗";
            case STORNIERT -> "–";
        };
    }

    private Div weekSep(boolean isHeader) {
        Div sep = new Div();
        sep.getStyle()
                .set("width", "3px")
                .set("min-height", "28px")
                .set("background", isHeader ? "var(--rom-bg-secondary)" : "var(--rom-bg-primary)")
                .set("border-bottom", "1px solid var(--rom-border)");
        return sep;
    }

    private int weeksNeeded(YearMonth ym) {
        int startCol = ym.atDay(1).getDayOfWeek().getValue() - 1;
        return (int) Math.ceil((startCol + ym.lengthOfMonth()) / 7.0);
    }

    private List<YearMonth> buildMonthList(LocalDate from, LocalDate to) {
        List<YearMonth> list = new java.util.ArrayList<>();
        YearMonth current = YearMonth.from(from);
        YearMonth end = YearMonth.from(to);
        while (!current.isAfter(end)) {
            list.add(current);
            current = current.plusMonths(1);
        }
        return list;
    }

    private Map<LocalDate, PurchaseStatus> mapByDate(List<PurchasePosition> purchases) {
        Map<LocalDate, PurchaseStatus> map = new java.util.HashMap<>();
        for (PurchasePosition purchase : purchases) {
            for (LocalDate date : extractDates(purchase)) {
                map.put(date, purchase.getPurchaseStatus());
            }
        }
        return map;
    }

    private List<LocalDate> extractDates(PurchasePosition position) {
        List<LocalDate> dates = new java.util.ArrayList<>();
        String validity = position.getValidity();
        if (validity != null && !validity.isBlank()) {
            try {
                var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                mapper.configure(
                        com.fasterxml.jackson.databind.DeserializationFeature
                                .FAIL_ON_UNKNOWN_PROPERTIES,
                        false);
                var array = mapper.readTree(validity);
                if (array.isArray()) {
                    for (var segment : array) {
                        var startNode = segment.get("startDate");
                        var endNode = segment.get("endDate");
                        if (startNode == null || endNode == null) continue;
                        LocalDate start = LocalDate.parse(startNode.asText());
                        LocalDate end = LocalDate.parse(endNode.asText());
                        if (end.isBefore(start)
                                || java.time.temporal.ChronoUnit.DAYS.between(start, end) > 366)
                            continue;
                        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
                            dates.add(d);
                        }
                    }
                }
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger(PurchaseCalendarGrid.class)
                        .debug("Failed to parse validity JSON", e);
            }
        }
        // Fallback: use orderedAt if no validity segments
        if (dates.isEmpty() && position.getOrderedAt() != null) {
            dates.add(
                    position.getOrderedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate());
        }
        return dates;
    }
}
