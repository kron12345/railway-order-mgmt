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

/**
 * Interactive month-by-month validity calendar. The stateless presentation + layout building blocks
 * (buttons, header/empty cells, day-cell styles, month/week math) live in {@link
 * ValidityCalendarLayout}; this widget keeps the selection state, cell registry and click wiring.
 */
public class ValidityCalendar extends Div {

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
            add(ValidityCalendarLayout.weekdayHeader());
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
            Div button =
                    ValidityCalendarLayout.weekdayButton(
                            ValidityCalendarLayout.WEEKDAY_SHORT[index],
                            ValidityCalendarLayout.WEEKDAY_FULL[index]);
            button.addClickListener(event -> toggleWeekday(dayOfWeek));
            toolbar.add(button);
        }

        Div spacer = new Div();
        spacer.getStyle().set("flex", "1");
        toolbar.add(spacer);

        Div selectAll = ValidityCalendarLayout.actionButton("Alle", "var(--rom-status-active)");
        selectAll.addClickListener(e -> selectAll());
        Div weekdays = ValidityCalendarLayout.actionButton("Mo–Fr", "var(--rom-status-info)");
        weekdays.addClickListener(e -> selectWeekdays());
        Div clear = ValidityCalendarLayout.actionButton("Keine", "var(--rom-status-danger)");
        clear.addClickListener(e -> clearAll());

        toolbar.add(selectAll, weekdays, clear);
        return toolbar;
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
        List<YearMonth> months = ValidityCalendarLayout.monthsInRange(minDate, maxDate);

        int maxWeeks =
                months.stream()
                        .mapToInt(ValidityCalendarLayout::weeksInMonth)
                        .max()
                        .orElse(DEFAULT_MAX_WEEKS);
        int dayColumns = maxWeeks * DAYS_PER_WEEK;

        Div table = new Div();
        table.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "60px repeat(" + dayColumns + ", 20px)")
                .set("gap", "1px")
                .set("font-family", "'JetBrains Mono', monospace")
                .set("font-size", "9px");

        Div corner = ValidityCalendarLayout.compactHeaderCell("");
        corner.getStyle()
                .set("position", "sticky")
                .set("left", "0")
                .set("z-index", "2")
                .set("background", "var(--rom-bg-card)");
        table.add(corner);
        for (int week = 0; week < maxWeeks; week++) {
            for (int weekday = 0; weekday < DAYS_PER_WEEK; weekday++) {
                Div headerCell =
                        ValidityCalendarLayout.compactHeaderCell(
                                ValidityCalendarLayout.WEEKDAY_SHORT[weekday]);
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
                    table.add(ValidityCalendarLayout.compactEmptyCell());
                } else if (dayCounter > daysInMonth) {
                    table.add(ValidityCalendarLayout.compactEmptyCell());
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
            daysGrid.add(ValidityCalendarLayout.emptyCell());
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
            ValidityCalendarLayout.applyDisabled(cell);
            return;
        }

        ValidityCalendarLayout.applyUnselected(cell);
        cell.addClickListener(event -> toggleDate(date));
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
            ValidityCalendarLayout.applySelected(cell);
        } else if (occupiedDates.containsKey(date)) {
            ValidityCalendarLayout.applyOccupied(cell);
        } else {
            ValidityCalendarLayout.applyUnselected(cell);
        }
        String owner = occupiedDates.get(date);
        if (owner != null) {
            cell.getElement().setAttribute("title", "Belegt: " + owner);
        } else {
            cell.getElement().removeAttribute("title");
        }
    }

    private void updateCount() {
        countLabel.setText(selectedDates.size() + " Tage ausgewählt");
    }
}
