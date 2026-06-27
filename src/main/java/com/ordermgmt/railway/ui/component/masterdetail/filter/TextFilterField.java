package com.ordermgmt.railway.ui.component.masterdetail.filter;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Predicate;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.value.ValueChangeMode;

/**
 * Case-insensitive "contains" filter on a single extracted text attribute (e.g. tags). Inactive
 * while the field is blank.
 */
public class TextFilterField<T> implements FilterField<T> {

    private final String label;
    private final TextField field = new TextField();
    private final Function<T, String> extractor;

    public TextFilterField(String label, Function<T, String> extractor) {
        this.label = label;
        this.extractor = extractor;
        field.setLabel(label);
        field.setClearButtonVisible(true);
        field.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        field.setValueChangeMode(ValueChangeMode.LAZY);
        field.setValueChangeTimeout(200);
        field.addClassName("md-filter-field");
    }

    @Override
    public Component control() {
        return field;
    }

    /**
     * Current raw text (untrimmed), or {@code null} — read by lazy views to build a server query.
     */
    public String getTextValue() {
        return field.getValue();
    }

    @Override
    public Predicate<T> predicate() {
        String query = normalize(field.getValue());
        if (query.isEmpty()) {
            return item -> true;
        }
        return item -> {
            String value = extractor.apply(item);
            return value != null && value.toLowerCase(Locale.ROOT).contains(query);
        };
    }

    @Override
    public List<FilterChip> chips() {
        String value = field.getValue();
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return List.of(new FilterChip(label + ": " + value.trim(), field::clear));
    }

    @Override
    public void reset() {
        field.clear();
    }

    @Override
    public void onChange(Runnable listener) {
        field.addValueChangeListener(e -> listener.run());
    }

    private static String normalize(String rawValue) {
        return rawValue == null ? "" : rawValue.trim().toLowerCase(Locale.ROOT);
    }
}
