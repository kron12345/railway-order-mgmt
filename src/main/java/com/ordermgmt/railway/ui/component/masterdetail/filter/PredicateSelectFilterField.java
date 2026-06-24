package com.ordermgmt.railway.ui.component.masterdetail.filter;

import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.select.SelectVariant;
import com.vaadin.flow.function.SerializableFunction;

/**
 * Single-choice filter like {@link SelectFilterField}, but matching via a caller-supplied predicate
 * instead of value equality — for items whose criterion is multi-valued (e.g. an order position
 * with several Bestellpositionen, each carrying its own status). Inactive while no option is
 * chosen.
 */
public class PredicateSelectFilterField<T, V> implements FilterField<T> {

    private final String label;
    private final Select<V> select = new Select<>();
    private final SerializableFunction<V, String> itemLabel;
    private final BiPredicate<T, V> matcher;

    public PredicateSelectFilterField(
            String label,
            List<V> options,
            SerializableFunction<V, String> itemLabel,
            BiPredicate<T, V> matcher) {
        this.label = label;
        this.itemLabel = itemLabel;
        this.matcher = matcher;
        select.setLabel(label);
        select.setItems(options);
        select.setItemLabelGenerator(v -> v == null ? "" : itemLabel.apply(v));
        select.setEmptySelectionAllowed(true);
        select.setEmptySelectionCaption("—");
        select.addThemeVariants(SelectVariant.LUMO_SMALL);
        select.addClassName("md-filter-field");
    }

    @Override
    public Component control() {
        return select;
    }

    @Override
    public Predicate<T> predicate() {
        V selected = select.getValue();
        if (selected == null) {
            return t -> true;
        }
        return t -> matcher.test(t, selected);
    }

    @Override
    public List<FilterChip> chips() {
        V selected = select.getValue();
        if (selected == null) {
            return List.of();
        }
        return List.of(new FilterChip(label + ": " + itemLabel.apply(selected), select::clear));
    }

    @Override
    public void reset() {
        select.clear();
    }

    @Override
    public void onChange(Runnable listener) {
        select.addValueChangeListener(e -> listener.run());
    }
}
