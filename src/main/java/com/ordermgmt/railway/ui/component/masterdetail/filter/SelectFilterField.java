package com.ordermgmt.railway.ui.component.masterdetail.filter;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.select.SelectVariant;
import com.vaadin.flow.function.SerializableFunction;

/**
 * Single-choice filter backed by a Lumo {@link Select}. Inactive while no option is chosen; once a
 * value is picked it keeps only items whose extracted value equals the selection.
 */
public class SelectFilterField<T, V> implements FilterField<T> {

    private final String label;
    private final Select<V> select = new Select<>();
    private final Function<T, V> valueExtractor;
    private final SerializableFunction<V, String> itemLabel;

    public SelectFilterField(
            String label,
            List<V> options,
            SerializableFunction<V, String> itemLabel,
            Function<T, V> valueExtractor) {
        this.label = label;
        this.valueExtractor = valueExtractor;
        this.itemLabel = itemLabel;
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

    /**
     * Current selection, or {@code null} when inactive — read by lazy views to build a server
     * query.
     */
    public V getSelectedValue() {
        return select.getValue();
    }

    @Override
    public Predicate<T> predicate() {
        V selected = select.getValue();
        if (selected == null) {
            return t -> true;
        }
        return t -> Objects.equals(valueExtractor.apply(t), selected);
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
