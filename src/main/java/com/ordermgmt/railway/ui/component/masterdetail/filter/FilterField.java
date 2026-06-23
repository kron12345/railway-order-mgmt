package com.ordermgmt.railway.ui.component.masterdetail.filter;

import java.util.List;
import java.util.function.Predicate;

import com.vaadin.flow.component.Component;

/**
 * One criterion in the reusable {@link FilterPanel}: an editable control plus the predicate it
 * contributes to the combined filter. Implementations are generic over the list item type {@code T}
 * and free of domain dependencies, so the same field kinds serve orders, businesses and customers.
 */
public interface FilterField<T> {

    /** Editable control shown inside the (collapsible) filter body. */
    Component control();

    /**
     * Current contribution to the combined filter; {@code t -> true} when this field is inactive.
     */
    Predicate<T> predicate();

    /** Active chips for the chip row (label + per-constraint clear); empty when inactive. */
    List<FilterChip> chips();

    /** Reset to inactive — clears the control, which in turn fires {@link #onChange}. */
    void reset();

    /** Register a listener fired on every value change (client-driven or programmatic). */
    void onChange(Runnable listener);
}
