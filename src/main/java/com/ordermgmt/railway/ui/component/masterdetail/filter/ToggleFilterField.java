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
    private final Checkbox box;
    private final Predicate<T> activePredicate;

    public ToggleFilterField(String label, Predicate<T> activePredicate) {
        this.label = label;
        this.activePredicate = activePredicate;
        this.box = new Checkbox(label);
        box.addClassName("md-filter-field");
    }

    @Override
    public Component control() {
        return box;
    }

    @Override
    public Predicate<T> predicate() {
        return Boolean.TRUE.equals(box.getValue()) ? activePredicate : t -> true;
    }

    @Override
    public List<FilterChip> chips() {
        return Boolean.TRUE.equals(box.getValue())
                ? List.of(new FilterChip(label, () -> box.setValue(false)))
                : List.of();
    }

    @Override
    public void reset() {
        box.setValue(false);
    }

    @Override
    public void onChange(Runnable listener) {
        box.addValueChangeListener(e -> listener.run());
    }
}
