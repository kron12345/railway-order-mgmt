package com.ordermgmt.railway.ui.component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

public class ValidityCalendar extends Div {

    private static final String[] WEEKDAY_SHORT = {"Mo", "Di", "Mi", "Do", "Fr", "Sa", "So"};
    private static final String[] WEEKDAY_FULL = {
        "Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag", "Samstag", "Sonntag"
    };
    private static final int DAYS_PER_WEEK = 7;
    private static final int WORKDAYS_PER_WEEK = 5;
    private static final int DEFAULT_MAX_WEEKS = 5;

    private final LocalDate minDate;
    private final LocalDate maxDate;
    private final Set<LocalDate> selectedDates = new LinkedHashSet<>();
    private final Map<LocalDate, String> occupiedDates = new HashMap<>();
    private Set<LocalDate> allowedDates;

    private final Map<LocalDate, Div> cellMap = new LinkedHashMap<>();
    private final Span countLabel = new Span();
    private boolean compact = false;

    public ValidityCalendar(LocalDate minDate, LocalDate maxDate) {
        this.minDate = minDate;
        this.maxDate = maxDate;

        setWidthFull();
        getStyle()
                .set("border", "1px solid var(--rom-border)")
                .set("border-radius", "6px")
                .set("padding", "12px")
                .set("box-sizing", "border-box");

        rebuildCalendar();
    }

    public void setCompact(boolean compact) {
        if (this.compact == compact) {
            return;
        }
        this.compact = compact;
        rebuildCalendar();
    }

    private void rebuildCalendar() {
        cellMap.clear();
        removeAll();
        add(createToolbar());
        if (!compact) {
            add(createWeekdayHeader());
        }
        add(compact ? createCompactCalendarGrid() : createCalendarGrid());
        add(createFooter());
    }

    public List<LocalDate> getSelectedDates() {
        List<LocalDate> sorted = new ArrayList<>(selectedDates);
        Collections.sort(sorted);
        return sorted;
    }

    public void setSelectedDates(Collection<LocalDate> dates) {
        selectedDates.clear();
        if (dates != null) {
            dates.stream()
                    .filter(d -> !d.isBefore(minDate) && !d.isAfter(maxDate))
                    .forEach(selectedDates::add);
        }
        updateAllCells();
        updateCount();
    }

    public void setOccupiedDates(Map<LocalDate, String> occupied) {
        occupiedDates.clear();
        if (occupied != null) {
            occupied.forEach(
                    (date, owner) -> {
                        if (!date.isBefore(minDate) && !date.isAfter(maxDate)) {
                            occupiedDates.put(date, owner);
                        }
                    });
        }
        updateAllCells();
    }

    public void setAllowedDates(Collection<LocalDate> allowed) {
        if (allowed == null || allowed.isEmpty()) {
            allowedDates = null;
        } else {
            allowedDates = new HashSet<>(allowed);
        }
        rebuildCalendar();
        updateAllCells();
    }

    private boolean isSelectable(LocalDate date) {
        return !date.isBefore(minDate)
                && !date.isAfter(maxDate)
                && (allowedDates == null || allowedDates.contains(date));
    }

    private Div createToolbar() {
        Div toolbar = new Div();
        toolbar.getStyle()
                .set("display", "flex")
                .set("gap", "4px")
                .set("flex-wrap", "wrap")
                .set("margin-bottom", "8px")
                .set("align-items", "center");

        for (int index = 0; index < DAYS_PER_WEEK; index++) {
            final DayOfWeek dayOfWeek = DayOfWeek.of(index + 1);
            Div button = weekdayButton(WEEKDAY_SHORT[index], WEEKDAY_FULL[index]);
            button.addClickListener(event -> toggleWeekday(dayOfWeek));
            toolbar.add(button);
        }

        Div spacer = new Div();
        spacer.getStyle().set("flex", "1");
        toolbar.add(spacer);

        Div selectAll = actionButton("Alle", "var(--rom-status-active)");
        selectAll.addClickListener(e -> selectAll());
        Div weekdays = actionButton("Mo–Fr", "var(--rom-status-info)");
        weekdays.addClickListener(e -> selectWeekdays());
        Div clear = actionButton("Keine", "var(--rom-status-danger)");
        clear.addClickListener(e -> clearAll());

        toolbar.add(selectAll, weekdays, clear);
        return toolbar;
    }

    private Div weekdayButton(String label, String tooltip) {
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

    private Div actionButton(String label, String color) {
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

    private Div createWeekdayHeader() {
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

    private Div createCalendarGrid() {
        Div grid = new Div();
        grid.getStyle().set("display", "flex").set("flex-direction", "column").set("gap", "2px");

        YearMonth start = YearMonth.from(minDate);
        YearMonth end = YearMonth.from(maxDate);
        YearMonth current = start;

        while (!current.isAfter(end)) {
            grid.add(createMonthRow(current));
            current = current.plusMonths(1);
        }
        return grid;
    }

    private Div createCompactCalendarGrid() {
        List<YearMonth> months = monthsInRange();

        int maxWeeks =
                months.stream().mapToInt(this::weeksInMonth).max().orElse(DEFAULT_MAX_WEEKS);
        int dayColumns = maxWeeks * DAYS_PER_WEEK;

        Div table = new Div();
        table.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "60px repeat(" + dayColumns + ", 20px)")
                .set("gap", "1px")
                .set("font-family", "'JetBrains Mono', monospace")
                .set("font-size", "9px");

        Div corner = compactHeaderCell("");
        corner.getStyle()
                .set("position", "sticky")
                .set("left", "0")
                .set("z-index", "2")
                .set("background", "var(--rom-bg-card)");
        table.add(corner);
        for (int week = 0; week < maxWeeks; week++) {
            for (int weekday = 0; weekday < DAYS_PER_WEEK; weekday++) {
                Div headerCell = compactHeaderCell(WEEKDAY_SHORT[weekday]);
                if (weekday >= WORKDAYS_PER_WEEK) {
                    headerCell.getStyle().set("color", "rgba(148,163,184,0.3)");
                }
                table.add(headerCell);
            }
        }

        for (YearMonth yearMonth : months) {
            addCompactMonthRow(table, yearMonth, maxWeeks);
        }

        return table;
    }

    private List<YearMonth> monthsInRange() {
        List<YearMonth> months = new ArrayList<>();
        YearMonth current = YearMonth.from(minDate);
        YearMonth end = YearMonth.from(maxDate);
        while (!current.isAfter(end)) {
            months.add(current);
            current = current.plusMonths(1);
        }
        return months;
    }

    private void addCompactMonthRow(Div table, YearMonth yearMonth, int maxWeeks) {
        Div label = new Div();
        label.setText(
                yearMonth.getMonth().getDisplayName(TextStyle.SHORT, Locale.GERMAN)
                        + " "
                        + String.valueOf(yearMonth.getYear()).substring(2));
        label.getStyle()
                .set("font-weight", "600")
                .set("color", "var(--rom-text-secondary)")
                .set("display", "flex")
                .set("align-items", "center")
                .set("padding-left", "2px")
                .set("position", "sticky")
                .set("left", "0")
                .set("background", "var(--rom-bg-card)")
                .set("z-index", "1");
        table.add(label);

        int daysInMonth = yearMonth.lengthOfMonth();
        int firstDayOfWeek = yearMonth.atDay(1).getDayOfWeek().getValue();
        int dayCounter = 1;

        for (int week = 0; week < maxWeeks; week++) {
            for (int weekday = 0; weekday < DAYS_PER_WEEK; weekday++) {
                if (week == 0 && weekday < firstDayOfWeek - 1) {
                    table.add(compactEmptyCell());
                } else if (dayCounter > daysInMonth) {
                    table.add(compactEmptyCell());
                } else {
                    LocalDate date = yearMonth.atDay(dayCounter);
                    boolean inRange = isSelectable(date);
                    Div cell = compactDayCell(date, dayCounter, inRange);
                    if (inRange) {
                        cellMap.put(date, cell);
                    }
                    table.add(cell);
                    dayCounter++;
                }
            }
        }
    }

    private int weeksInMonth(YearMonth yearMonth) {
        int firstDayOfWeek = yearMonth.atDay(1).getDayOfWeek().getValue();
        int totalSlots = firstDayOfWeek - 1 + yearMonth.lengthOfMonth();
        return (totalSlots + DAYS_PER_WEEK - 1) / DAYS_PER_WEEK;
    }

    private Div compactHeaderCell(String text) {
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

    private Div compactEmptyCell() {
        Div cell = new Div();
        cell.getStyle().set("width", "20px").set("height", "20px");
        return cell;
    }

    private Div compactDayCell(LocalDate date, int day, boolean selectable) {
        Div cell = new Div();
        cell.setText(String.valueOf(day));
        cell.getStyle()
                .set("width", "22px")
                .set("height", "22px")
                .set("line-height", "22px")
                .set("text-align", "center")
                .set("font-family", "'JetBrains Mono', monospace")
                .set("font-size", "9px")
                .set("font-weight", "600")
                .set("border-radius", "3px")
                .set("cursor", selectable ? "pointer" : "default")
                .set("flex-shrink", "0")
                .set("transition", "all 0.1s");

        configureSelectableCell(cell, date, selectable);
        return cell;
    }

    private Div createMonthRow(YearMonth yearMonth) {
        Div row = new Div();
        row.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "80px repeat(7, 1fr)")
                .set("gap", "2px");

        Div label = new Div();
        label.setText(
                yearMonth.getMonth().getDisplayName(TextStyle.SHORT, Locale.GERMAN)
                        + " "
                        + yearMonth.getYear());
        label.getStyle()
                .set("font-family", "'JetBrains Mono', monospace")
                .set("font-size", "10px")
                .set("font-weight", "600")
                .set("color", "var(--rom-text-secondary)")
                .set("display", "flex")
                .set("align-items", "center")
                .set("padding-right", "8px");
        row.add(label);

        int startColumn = yearMonth.atDay(1).getDayOfWeek().getValue() - 1;
        int daysInMonth = yearMonth.lengthOfMonth();

        Div daysGrid = new Div();
        daysGrid.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(7, 1fr)")
                .set("gap", "2px")
                .set("grid-column", "2 / -1");

        for (int column = 0; column < startColumn; column++) {
            daysGrid.add(emptyCell());
        }

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = yearMonth.atDay(day);
            boolean inRange = isSelectable(date);
            Div cell = dayCell(date, day, inRange);
            if (inRange) {
                cellMap.put(date, cell);
            }
            daysGrid.add(cell);
        }

        row.removeAll();
        row.getStyle()
                .set("grid-template-columns", "none")
                .set("display", "flex")
                .set("align-items", "flex-start")
                .set("gap", "4px");
        label.getStyle().set("min-width", "70px").set("padding-top", "4px");
        daysGrid.getStyle().set("flex", "1");
        row.add(label, daysGrid);

        return row;
    }

    private Div dayCell(LocalDate date, int day, boolean selectable) {
        Div cell = new Div();
        cell.setText(String.valueOf(day));
        cell.getStyle()
                .set("text-align", "center")
                .set("font-family", "'JetBrains Mono', monospace")
                .set("font-size", "11px")
                .set("font-weight", "600")
                .set("padding", "4px 0")
                .set("border-radius", "4px")
                .set("cursor", selectable ? "pointer" : "default")
                .set("min-height", "26px")
                .set("transition", "all 0.1s");

        configureSelectableCell(cell, date, selectable);

        return cell;
    }

    private void configureSelectableCell(Div cell, LocalDate date, boolean selectable) {
        if (!selectable) {
            applyDisabledStyle(cell);
            return;
        }

        applyUnselectedStyle(cell);
        cell.addClickListener(event -> toggleDate(date));
    }

    private void applyDisabledStyle(Div cell) {
        cell.getStyle().set("color", "rgba(148,163,184,0.15)").set("background", "transparent");
    }

    private Div emptyCell() {
        Div cell = new Div();
        cell.getStyle().set("min-height", "26px");
        return cell;
    }

    private Div createFooter() {
        Div footer = new Div();
        footer.getStyle()
                .set("display", "flex")
                .set("justify-content", "space-between")
                .set("align-items", "center")
                .set("margin-top", "8px")
                .set("padding-top", "8px")
                .set("border-top", "1px solid var(--rom-border)");

        countLabel
                .getStyle()
                .set("font-family", "'JetBrains Mono', monospace")
                .set("font-size", "12px")
                .set("font-weight", "600")
                .set("color", "var(--rom-accent)");
        updateCount();

        Span hint = new Span("Klick = einzeln, Wochentag-Button = alle gleichen Tage");
        hint.getStyle().set("font-size", "10px").set("color", "var(--rom-text-muted)");

        footer.add(countLabel, hint);
        return footer;
    }

    private void toggleDate(LocalDate date) {
        if (selectedDates.contains(date)) {
            selectedDates.remove(date);
        } else {
            selectedDates.add(date);
        }
        updateCell(date);
        updateCount();
    }

    private void toggleWeekday(DayOfWeek dayOfWeek) {
        List<LocalDate> datesForDayOfWeek =
                selectableDates(date -> date.getDayOfWeek() == dayOfWeek);

        boolean allSelected = selectedDates.containsAll(datesForDayOfWeek);
        if (allSelected) {
            selectedDates.removeAll(datesForDayOfWeek);
        } else {
            selectedDates.addAll(datesForDayOfWeek);
        }

        updateAllCells();
        updateCount();
    }

    private void selectAll() {
        selectedDates.addAll(selectableDates(date -> true));
        updateAllCells();
        updateCount();
    }

    private void selectWeekdays() {
        selectedDates.addAll(selectableDates(this::isWeekday));
        updateAllCells();
        updateCount();
    }

    private List<LocalDate> selectableDates(Predicate<LocalDate> predicate) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate date = minDate;
        while (!date.isAfter(maxDate)) {
            if (isSelectable(date) && predicate.test(date)) {
                dates.add(date);
            }
            date = date.plusDays(1);
        }
        return dates;
    }

    private boolean isWeekday(LocalDate date) {
        return date.getDayOfWeek().getValue() <= WORKDAYS_PER_WEEK;
    }

    private void clearAll() {
        selectedDates.clear();
        updateAllCells();
        updateCount();
    }

    private void updateCell(LocalDate date) {
        Div cell = cellMap.get(date);
        if (cell != null) {
            styleCell(date, cell);
        }
    }

    private void updateAllCells() {
        cellMap.forEach(this::styleCell);
    }

    private void styleCell(LocalDate date, Div cell) {
        if (selectedDates.contains(date)) {
            applySelectedStyle(cell);
        } else if (occupiedDates.containsKey(date)) {
            applyOccupiedStyle(cell);
        } else {
            applyUnselectedStyle(cell);
        }
        String owner = occupiedDates.get(date);
        if (owner != null) {
            cell.getElement().setAttribute("title", "Belegt: " + owner);
        } else {
            cell.getElement().removeAttribute("title");
        }
    }

    private void applySelectedStyle(Div cell) {
        cell.getStyle()
                .set("color", "var(--rom-bg-primary)")
                .set("background", "var(--rom-accent)")
                .set("border", "1px solid transparent")
                .set("font-weight", "700");
    }

    private void applyUnselectedStyle(Div cell) {
        cell.getStyle()
                .set("color", "var(--rom-text-secondary)")
                .set("background", "var(--rom-bg-primary)")
                .set("border", "1px solid transparent")
                .set("font-weight", "600");
    }

    private void applyOccupiedStyle(Div cell) {
        cell.getStyle()
                .set("color", "#b45309")
                .set("background", "rgba(245, 158, 11, 0.18)")
                .set("border", "1px dashed #f59e0b")
                .set("font-weight", "700");
    }

    private void updateCount() {
        countLabel.setText(selectedDates.size() + " Tage ausgewählt");
    }
}
