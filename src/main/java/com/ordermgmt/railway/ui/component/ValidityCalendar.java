package com.ordermgmt.railway.ui.component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

/**
 * Multi-date picker calendar with single-click toggle and weekday bulk select. Only dates within
 * the allowed range (minDate..maxDate) are selectable. Selected dates are visualized with accent
 * highlight.
 *
 * <p>Architecture: server-side {@code Set<LocalDate>} as source of truth; Div-based grid renders
 * one row per month with a 7-column day grid.
 */
public class ValidityCalendar extends Div {

    private static final String[] WEEKDAY_SHORT = {"Mo", "Di", "Mi", "Do", "Fr", "Sa", "So"};
    private static final String[] WEEKDAY_FULL = {
        "Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag", "Samstag", "Sonntag"
    };

    private final LocalDate minDate;
    private final LocalDate maxDate;
    private final Set<LocalDate> selectedDates = new LinkedHashSet<>();
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

    /** Enables or disables compact mode (single-row per month with tiny day squares). */
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

    /** Get all selected dates sorted. */
    public List<LocalDate> getSelectedDates() {
        List<LocalDate> sorted = new ArrayList<>(selectedDates);
        Collections.sort(sorted);
        return sorted;
    }

    /** Set selected dates from outside (e.g. reading from entity). */
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

    // --- Toolbar: weekday buttons + bulk actions ---

    private Div createToolbar() {
        Div toolbar = new Div();
        toolbar.getStyle()
                .set("display", "flex")
                .set("gap", "4px")
                .set("flex-wrap", "wrap")
                .set("margin-bottom", "8px")
                .set("align-items", "center");

        for (int i = 0; i < 7; i++) {
            final DayOfWeek dow = DayOfWeek.of(i + 1);
            Div btn = weekdayButton(WEEKDAY_SHORT[i], WEEKDAY_FULL[i]);
            btn.addClickListener(e -> toggleWeekday(dow));
            toolbar.add(btn);
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
        Div btn = new Div();
        btn.setText(label);
        btn.getElement().setAttribute("title", "Alle " + tooltip + " auswählen/abwählen");
        btn.getStyle()
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
        return btn;
    }

    private Div actionButton(String label, String color) {
        Div btn = new Div();
        btn.setText(label);
        btn.getStyle()
                .set("padding", "3px 8px")
                .set("border-radius", "4px")
                .set("font-family", "'JetBrains Mono', monospace")
                .set("font-size", "10px")
                .set("font-weight", "600")
                .set("cursor", "pointer")
                .set("color", color)
                .set("background", "color-mix(in srgb, " + color + " 10%, transparent)")
                .set("border", "1px solid " + color);
        return btn;
    }

    // --- Weekday column header ---

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
            Div h = new Div();
            h.setText(day);
            h.getStyle()
                    .set("text-align", "center")
                    .set("font-family", "'JetBrains Mono', monospace")
                    .set("font-size", "10px")
                    .set("font-weight", "600")
                    .set("color", "var(--rom-text-muted)")
                    .set("padding", "4px 0");
            header.add(h);
        }
        return header;
    }

    // --- Calendar grid: one row per month ---

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

    // --- Compact calendar grid: 7-column weekday table, one section per month ---

    private Div createCompactCalendarGrid() {
        Div grid = new Div();
        grid.getStyle().set("display", "flex").set("flex-direction", "column").set("gap", "0");

        // Sticky weekday header row: [Month label] [Mo] [Di] ... [So]
        Div header = new Div();
        header.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "56px repeat(7, 22px)")
                .set("gap", "2px")
                .set("margin-bottom", "2px")
                .set("position", "sticky")
                .set("top", "0")
                .set("background", "var(--rom-bg-card)")
                .set("z-index", "1");
        Div corner = new Div();
        header.add(corner);
        for (String day : WEEKDAY_SHORT) {
            Div h = new Div();
            h.setText(day);
            h.getStyle()
                    .set("text-align", "center")
                    .set("font-family", "'JetBrains Mono', monospace")
                    .set("font-size", "9px")
                    .set("font-weight", "600")
                    .set("color", "var(--rom-text-muted)")
                    .set("line-height", "18px");
            header.add(h);
        }
        grid.add(header);

        YearMonth start = YearMonth.from(minDate);
        YearMonth end = YearMonth.from(maxDate);
        YearMonth current = start;

        while (!current.isAfter(end)) {
            grid.add(createCompactMonthSection(current));
            current = current.plusMonths(1);
        }
        return grid;
    }

    /**
     * Creates a compact month section with the month label spanning the first column and day cells
     * aligned under their weekday columns (Mo-So).
     */
    private Div createCompactMonthSection(YearMonth ym) {
        int daysInMonth = ym.lengthOfMonth();
        int firstDow = ym.atDay(1).getDayOfWeek().getValue(); // Mo=1
        // Calculate number of rows (weeks) needed for this month
        int totalSlots = (firstDow - 1) + daysInMonth;
        int weekCount = (totalSlots + 6) / 7;

        Div section = new Div();
        section.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "56px repeat(7, 22px)")
                .set("grid-template-rows", "repeat(" + weekCount + ", 22px)")
                .set("gap", "2px")
                .set("margin-bottom", "2px");

        // Month label in the first column, spanning all rows
        Div label = new Div();
        label.setText(
                ym.getMonth().getDisplayName(TextStyle.SHORT, Locale.GERMAN)
                        + " "
                        + String.valueOf(ym.getYear()).substring(2));
        label.getStyle()
                .set("font-family", "'JetBrains Mono', monospace")
                .set("font-size", "9px")
                .set("font-weight", "600")
                .set("color", "var(--rom-text-secondary)")
                .set("grid-column", "1")
                .set("grid-row", "1 / span " + weekCount)
                .set("display", "flex")
                .set("align-items", "flex-start")
                .set("padding-top", "3px");
        section.add(label);

        // Place each day in the correct weekday column and week row
        for (int d = 1; d <= daysInMonth; d++) {
            LocalDate date = ym.atDay(d);
            int dow = date.getDayOfWeek().getValue(); // Mo=1 .. So=7
            int weekOfMonth = ((firstDow - 1) + (d - 1)) / 7 + 1;
            int gridCol = dow + 1; // +1 because col 1 is the month label

            boolean inRange = !date.isBefore(minDate) && !date.isAfter(maxDate);
            Div cell = compactDayCell(date, d, inRange);
            cell.getStyle()
                    .set("grid-column", String.valueOf(gridCol))
                    .set("grid-row", String.valueOf(weekOfMonth));
            if (inRange) {
                cellMap.put(date, cell);
            }
            section.add(cell);
        }
        return section;
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

        if (!selectable) {
            cell.getStyle().set("color", "rgba(148,163,184,0.15)").set("background", "transparent");
        } else {
            applyUnselectedStyle(cell);
            cell.addClickListener(e -> toggleDate(date));
        }
        return cell;
    }

    private Div createMonthRow(YearMonth ym) {
        Div row = new Div();
        row.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "80px repeat(7, 1fr)")
                .set("gap", "2px");

        // Month label
        Div label = new Div();
        label.setText(
                ym.getMonth().getDisplayName(TextStyle.SHORT, Locale.GERMAN) + " " + ym.getYear());
        label.getStyle()
                .set("font-family", "'JetBrains Mono', monospace")
                .set("font-size", "10px")
                .set("font-weight", "600")
                .set("color", "var(--rom-text-secondary)")
                .set("display", "flex")
                .set("align-items", "center")
                .set("padding-right", "8px");
        row.add(label);

        int firstDow = ym.atDay(1).getDayOfWeek().getValue(); // Mo=1
        int startCol = firstDow - 1;
        int daysInMonth = ym.lengthOfMonth();

        Div daysGrid = new Div();
        daysGrid.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(7, 1fr)")
                .set("gap", "2px")
                .set("grid-column", "2 / -1");

        for (int i = 0; i < startCol; i++) {
            daysGrid.add(emptyCell());
        }

        for (int d = 1; d <= daysInMonth; d++) {
            LocalDate date = ym.atDay(d);
            boolean inRange = !date.isBefore(minDate) && !date.isAfter(maxDate);
            Div cell = dayCell(date, d, inRange);
            if (inRange) {
                cellMap.put(date, cell);
            }
            daysGrid.add(cell);
        }

        // Use flex layout: fixed-width month label + flexible days grid
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

        if (!selectable) {
            cell.getStyle().set("color", "rgba(148,163,184,0.15)").set("background", "transparent");
        } else {
            applyUnselectedStyle(cell);
            cell.addClickListener(e -> toggleDate(date));
        }

        return cell;
    }

    private Div emptyCell() {
        Div cell = new Div();
        cell.getStyle().set("min-height", "26px");
        return cell;
    }

    // --- Footer with count ---

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

    // --- Selection logic ---

    private void toggleDate(LocalDate date) {
        if (selectedDates.contains(date)) {
            selectedDates.remove(date);
        } else {
            selectedDates.add(date);
        }
        updateCell(date);
        updateCount();
    }

    private void toggleWeekday(DayOfWeek dow) {
        List<LocalDate> datesForDow = new ArrayList<>();
        LocalDate d = minDate;
        while (!d.isAfter(maxDate)) {
            if (d.getDayOfWeek() == dow) datesForDow.add(d);
            d = d.plusDays(1);
        }

        // If all selected → deselect, otherwise select all
        boolean allSelected = selectedDates.containsAll(datesForDow);
        if (allSelected) {
            selectedDates.removeAll(datesForDow);
        } else {
            selectedDates.addAll(datesForDow);
        }

        updateAllCells();
        updateCount();
    }

    private void selectAll() {
        LocalDate d = minDate;
        while (!d.isAfter(maxDate)) {
            selectedDates.add(d);
            d = d.plusDays(1);
        }
        updateAllCells();
        updateCount();
    }

    private void selectWeekdays() {
        LocalDate d = minDate;
        while (!d.isAfter(maxDate)) {
            if (d.getDayOfWeek().getValue() <= 5) selectedDates.add(d);
            d = d.plusDays(1);
        }
        updateAllCells();
        updateCount();
    }

    private void clearAll() {
        selectedDates.clear();
        updateAllCells();
        updateCount();
    }

    // --- Visual updates ---

    private void updateCell(LocalDate date) {
        Div cell = cellMap.get(date);
        if (cell == null) return;
        if (selectedDates.contains(date)) {
            applySelectedStyle(cell);
        } else {
            applyUnselectedStyle(cell);
        }
    }

    private void updateAllCells() {
        cellMap.forEach(
                (date, cell) -> {
                    if (selectedDates.contains(date)) {
                        applySelectedStyle(cell);
                    } else {
                        applyUnselectedStyle(cell);
                    }
                });
    }

    private void applySelectedStyle(Div cell) {
        cell.getStyle()
                .set("color", "var(--rom-bg-primary)")
                .set("background", "var(--rom-accent)")
                .set("font-weight", "700");
    }

    private void applyUnselectedStyle(Div cell) {
        cell.getStyle()
                .set("color", "var(--rom-text-secondary)")
                .set("background", "var(--rom-bg-primary)")
                .set("font-weight", "600");
    }

    private void updateCount() {
        countLabel.setText(selectedDates.size() + " Tage ausgewählt");
    }
}
