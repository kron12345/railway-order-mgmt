package com.ordermgmt.railway.ui.component.masterdetail.filter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datepicker.DatePickerVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

/**
 * From/to date filter. Each item carries its own validity interval (start/end); an item passes when
 * that interval overlaps the chosen [from, to] window. Either bound may be left open. A {@code
 * null} start or end on the item is treated as unbounded on that side.
 */
public class DateRangeFilterField<T> implements FilterField<T> {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd.MM.yy");

    private final String labelFrom;
    private final String labelTo;
    private final DatePicker from = new DatePicker();
    private final DatePicker to = new DatePicker();
    private final Function<T, LocalDate> startExtractor;
    private final Function<T, LocalDate> endExtractor;

    public DateRangeFilterField(
            String labelFrom,
            String labelTo,
            Function<T, LocalDate> startExtractor,
            Function<T, LocalDate> endExtractor) {
        this.labelFrom = labelFrom;
        this.labelTo = labelTo;
        this.startExtractor = startExtractor;
        this.endExtractor = endExtractor;
        configure(from, labelFrom);
        configure(to, labelTo);
    }

    private void configure(DatePicker picker, String label) {
        picker.setLabel(label);
        picker.setClearButtonVisible(true);
        picker.addThemeVariants(DatePickerVariant.LUMO_SMALL);
        picker.addClassName("md-filter-field");
    }

    @Override
    public Component control() {
        var row = new HorizontalLayout(from, to);
        row.setPadding(false);
        row.setSpacing(true);
        row.addClassName("md-filter-daterange");
        return row;
    }

    /** Lower bound of the window, or {@code null} — read by lazy views to build a server query. */
    public LocalDate getFrom() {
        return from.getValue();
    }

    /** Upper bound of the window, or {@code null} — read by lazy views to build a server query. */
    public LocalDate getTo() {
        return to.getValue();
    }

    @Override
    public Predicate<T> predicate() {
        LocalDate f = from.getValue();
        LocalDate t = to.getValue();
        if (f == null && t == null) {
            return x -> true;
        }
        return x -> {
            LocalDate start = startExtractor.apply(x);
            LocalDate end = endExtractor.apply(x);
            if (f != null && end != null && end.isBefore(f)) {
                return false;
            }
            if (t != null && start != null && start.isAfter(t)) {
                return false;
            }
            return true;
        };
    }

    @Override
    public List<FilterChip> chips() {
        List<FilterChip> chips = new ArrayList<>();
        if (from.getValue() != null) {
            chips.add(new FilterChip(labelFrom + " " + from.getValue().format(FMT), from::clear));
        }
        if (to.getValue() != null) {
            chips.add(new FilterChip(labelTo + " " + to.getValue().format(FMT), to::clear));
        }
        return chips;
    }

    @Override
    public void reset() {
        from.clear();
        to.clear();
    }

    @Override
    public void onChange(Runnable listener) {
        from.addValueChangeListener(e -> listener.run());
        to.addValueChangeListener(e -> listener.run());
    }
}
