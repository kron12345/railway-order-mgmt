package com.ordermgmt.railway.ui.component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.shared.Tooltip;

import com.ordermgmt.railway.domain.order.model.PurchasePosition;
import com.ordermgmt.railway.domain.order.model.PurchaseStatus;
import com.ordermgmt.railway.domain.order.model.ValidityJsonCodec;

public class PurchaseCalendarGrid extends Div {

    private static final String[] WEEKDAY_HEADERS = {"Mo", "Di", "Mi", "Do", "Fr", "Sa", "So"};
    private static final int DAYS_PER_WEEK = 7;
    private static final int WEEKEND_START_COLUMN = 5;
    private static final int DEFAULT_MAX_WEEKS = 5;
    private static final LocalDate TIMETABLE_YEAR_2027_START = LocalDate.of(2026, 12, 12);

    public PurchaseCalendarGrid(List<PurchasePosition> purchases, LocalDate from, LocalDate to) {
        setWidthFull();
        getStyle()
                .set("overflow-x", "auto")
                .set("border", "1px solid var(--rom-border)")
                .set("border-radius", "6px");

        Map<LocalDate, PurchaseStatus> statusByDate = mapByDate(purchases);
        List<YearMonth> months = buildMonthList(from, to);
        int maxWeeksPerRow =
                months.stream().mapToInt(this::weeksNeeded).max().orElse(DEFAULT_MAX_WEEKS);

        Div table = new Div();
        int columns = maxWeeksPerRow * DAYS_PER_WEEK + maxWeeksPerRow - 1;
        table.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "100px repeat(" + columns + ", minmax(24px, 1fr))")
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
        Div cornerCell = headerCell("");
        cornerCell
                .getStyle()
                .set("background", "var(--rom-bg-secondary)")
                .set("border-bottom", "1px solid var(--rom-border)")
                .set("position", "sticky")
                .set("left", "0")
                .set("z-index", "2");
        table.add(cornerCell);

        for (int week = 0; week < maxWeeks; week++) {
            if (week > 0) {
                table.add(weekSeparator(true));
            }
            for (int weekday = 0; weekday < DAYS_PER_WEEK; weekday++) {
                Div headerCell = headerCell(WEEKDAY_HEADERS[weekday]);
                if (weekday >= WEEKEND_START_COLUMN) {
                    headerCell.getStyle().set("color", "rgba(148,163,184,0.3)");
                }
                table.add(headerCell);
            }
        }
    }

    private void addMonthRow(
            Div table,
            YearMonth yearMonth,
            int maxWeeks,
            Map<LocalDate, PurchaseStatus> statusByDate) {
        LocalDate firstOfMonth = yearMonth.atDay(1);
        int firstWeekdayColumn = firstOfMonth.getDayOfWeek().getValue() - 1;
        int daysInMonth = yearMonth.lengthOfMonth();

        table.add(monthLabel(yearMonth));

        int dayOfMonth = 1;
        for (int week = 0; week < maxWeeks; week++) {
            if (week > 0) {
                table.add(weekSeparator(false));
            }
            for (int weekday = 0; weekday < DAYS_PER_WEEK; weekday++) {
                int position = week * 7 + weekday;
                boolean inRange = position >= firstWeekdayColumn && dayOfMonth <= daysInMonth;

                if (inRange) {
                    LocalDate date = yearMonth.atDay(dayOfMonth);
                    boolean isWeekend = weekday >= WEEKEND_START_COLUMN;
                    PurchaseStatus status = statusByDate.get(date);
                    boolean isTimetableYearBoundary = date.equals(TIMETABLE_YEAR_2027_START);

                    table.add(
                            dayCell(dayOfMonth, status, isWeekend, isTimetableYearBoundary, date));
                    dayOfMonth++;
                } else {
                    table.add(emptyCell());
                }
            }
        }
    }

    private Div monthLabel(YearMonth yearMonth) {
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

        Span timetableYearTag = new Span(timetableYearLabel(yearMonth.atDay(1)));
        timetableYearTag.getStyle().set("font-size", "8px").set("color", "var(--rom-accent)");

        label.add(monthName, timetableYearTag);
        return label;
    }

    private String timetableYearLabel(LocalDate date) {
        return date.isBefore(TIMETABLE_YEAR_2027_START) ? "FPJ 2026" : "FPJ 2027";
    }

    private Div dayCell(
            int day,
            PurchaseStatus status,
            boolean weekend,
            boolean timetableYearBoundary,
            LocalDate date) {
        Div cell = new Div();
        cell.addClassName("cal-cell");
        cell.getElement().setAttribute("tabindex", "0");
        cell.getElement().setAttribute("role", "gridcell");
        cell.setText(day + statusSymbol(status));

        DayCellStyle cellStyle = dayCellStyle(status, weekend);

        cell.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("min-height", "28px")
                .set("font-weight", "600")
                .set("background", cellStyle.background())
                .set("color", cellStyle.color())
                .set("border-bottom", "1px solid var(--rom-border)")
                .set("cursor", "default");

        if (timetableYearBoundary) {
            cell.getStyle().set("border-left", "2px solid var(--rom-accent)");
        }

        String tooltip = tooltipText(day, date, status, weekend);
        cell.getElement().setAttribute("aria-label", tooltip);
        Tooltip.forComponent(cell).withText(tooltip).withPosition(Tooltip.TooltipPosition.TOP);

        return cell;
    }

    private DayCellStyle dayCellStyle(PurchaseStatus status, boolean weekend) {
        if (weekend && status == null) {
            return new DayCellStyle("rgba(148,163,184,0.02)", "rgba(148,163,184,0.15)");
        }
        if (status == null) {
            return new DayCellStyle("rgba(148,163,184,0.06)", "var(--rom-text-muted)");
        }
        return switch (status) {
            case BESTAETIGT -> new DayCellStyle("rgba(52,211,153,0.2)", "var(--rom-status-active)");
            case BESTELLT -> new DayCellStyle("rgba(96,165,250,0.2)", "var(--rom-status-info)");
            case OFFEN -> new DayCellStyle("rgba(148,163,184,0.06)", "var(--rom-text-muted)");
            case ABGELEHNT -> new DayCellStyle("rgba(248,113,113,0.2)", "var(--rom-status-danger)");
            case STORNIERT -> new DayCellStyle("rgba(107,114,128,0.1)", "var(--rom-text-muted)");
        };
    }

    private String tooltipText(int day, LocalDate date, PurchaseStatus status, boolean weekend) {
        String statusText = status != null ? status.name() : (weekend ? "WE" : "—");
        return String.format(
                "%02d.%02d.%d %s", day, date.getMonthValue(), date.getYear(), statusText);
    }

    private Div emptyCell() {
        Div cell = new Div();
        cell.getStyle()
                .set("min-height", "28px")
                .set("border-bottom", "1px solid var(--rom-border)");
        return cell;
    }

    private Div headerCell(String text) {
        Div headerCell = new Div();
        headerCell.setText(text);
        headerCell
                .getStyle()
                .set("text-align", "center")
                .set("padding", "4px 2px")
                .set("font-size", "9px")
                .set("font-weight", "600")
                .set("color", "var(--rom-text-muted)")
                .set("background", "var(--rom-bg-secondary)")
                .set("border-bottom", "1px solid var(--rom-border)")
                .set("text-transform", "uppercase");
        return headerCell;
    }

    private String statusSymbol(PurchaseStatus status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case BESTAETIGT -> "✓";
            case BESTELLT -> "→";
            case OFFEN -> "";
            case ABGELEHNT -> "✗";
            case STORNIERT -> "–";
        };
    }

    private Div weekSeparator(boolean header) {
        Div separator = new Div();
        separator
                .getStyle()
                .set("width", "3px")
                .set("min-height", "28px")
                .set("background", header ? "var(--rom-bg-secondary)" : "var(--rom-bg-primary)")
                .set("border-bottom", "1px solid var(--rom-border)");
        return separator;
    }

    private int weeksNeeded(YearMonth yearMonth) {
        int firstWeekdayColumn = yearMonth.atDay(1).getDayOfWeek().getValue() - 1;
        return (firstWeekdayColumn + yearMonth.lengthOfMonth() + DAYS_PER_WEEK - 1) / DAYS_PER_WEEK;
    }

    private List<YearMonth> buildMonthList(LocalDate from, LocalDate to) {
        List<YearMonth> months = new ArrayList<>();
        YearMonth current = YearMonth.from(from);
        YearMonth end = YearMonth.from(to);
        while (!current.isAfter(end)) {
            months.add(current);
            current = current.plusMonths(1);
        }
        return months;
    }

    private Map<LocalDate, PurchaseStatus> mapByDate(List<PurchasePosition> purchases) {
        Map<LocalDate, PurchaseStatus> statusByDate = new HashMap<>();
        for (PurchasePosition purchase : purchases) {
            for (LocalDate date : extractDates(purchase)) {
                statusByDate.put(date, purchase.getPurchaseStatus());
            }
        }
        return statusByDate;
    }

    private List<LocalDate> extractDates(PurchasePosition position) {
        List<LocalDate> dates = ValidityJsonCodec.fromJson(position.getValidity());
        if (dates.isEmpty() && position.getOrderedAt() != null) {
            dates.add(position.getOrderedAt().atZone(ZoneId.systemDefault()).toLocalDate());
        }
        return dates;
    }

    private record DayCellStyle(String background, String color) {}
}
