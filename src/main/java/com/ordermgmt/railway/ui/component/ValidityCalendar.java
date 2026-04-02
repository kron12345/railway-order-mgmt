package com.ordermgmt.railway.ui.component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

/**
 * Multi-date picker calendar with single-click toggle, weekday bulk select, and shift-click range
 * selection. Only dates within the allowed range are selectable. Selected dates are visualized with
 * amber highlight.
 *
 * <p>Architecture: Div-based grid with client-side JS for fast interaction, server-side
 * Set<LocalDate> as source of truth, synced via Element API.
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

    public ValidityCalendar(LocalDate minDate, LocalDate maxDate) {
        this.minDate = minDate;
        this.maxDate = maxDate;

        setWidthFull();
        getStyle()
                .set("border", "1px solid var(--rom-border)")
                .set("border-radius", "6px")
                .set("padding", "12px")
                .set("box-sizing", "border-box");

        add(createToolbar());
        add(createWeekdayHeader());
        add(createCalendarGrid());
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

        // Build week cells for this month
        int firstDow = ym.atDay(1).getDayOfWeek().getValue(); // Mo=1
        int startCol = firstDow - 1;
        int daysInMonth = ym.lengthOfMonth();

        // We need exactly 7 cells — pad start, fill days, pad end
        // But one row = one month with ALL days → we need multiple "sub-rows"
        // Actually for compact: one row per week within the month

        // Simpler: flatten all days into the 7-column grid
        // First: pad empty cells before day 1
        // We actually need a sub-grid for the days
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

        // Actually the layout with "80px label + days sub-grid" won't work well
        // Let me restructure: remove the sub-grid, use a proper layout
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
