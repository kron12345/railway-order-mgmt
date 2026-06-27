package com.ordermgmt.railway.ui.component.masterdetail.filter;

import java.util.List;
import java.util.function.Predicate;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.checkbox.Checkbox;

/**
 * Boolean filter rendered as a checkbox (e.g. "assigned to me"). While unchecked it passes
 * everything; while checked it applies the supplied predicate.
 */
public class ToggleFilterField<T> implements FilterField<T> {

    private final String label;
    private final Checkbox checkbox;
    private final Predicate<T> activePredicate;

    public ToggleFilterField(String label, Predicate<T> activePredicate) {
        this.label = label;
        this.activePredicate = activePredicate;
        this.checkbox = new Checkbox(label);
        checkbox.addClassName("md-filter-field");
    }

    @Override
    public Component control() {
        return checkbox;
    }

    /** Whether the toggle is on — read by lazy views to build a server query. */
    public boolean isToggled() {
        return Boolean.TRUE.equals(checkbox.getValue());
    }

    @Override
    public Predicate<T> predicate() {
        return Boolean.TRUE.equals(checkbox.getValue()) ? activePredicate : item -> true;
    }

    @Override
    public List<FilterChip> chips() {
        return Boolean.TRUE.equals(checkbox.getValue())
                ? List.of(new FilterChip(label, () -> checkbox.setValue(false)))
                : List.of();
    }

    @Override
    public void reset() {
        checkbox.setValue(false);
    }

    @Override
    public void onChange(Runnable listener) {
        checkbox.addValueChangeListener(e -> listener.run());
    }
}
