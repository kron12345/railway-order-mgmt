package com.ordermgmt.railway.ui.component.masterdetail.filter;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;

/**
 * Reusable, compact, collapsible filter area for {@link
 * com.ordermgmt.railway.ui.component.masterdetail.MasterDetailLayout}. A "⚑ Filter" toggle (with an
 * active-count badge) lives in the toolbar; clicking it expands a body of {@link FilterField}
 * controls. Below the body a chip row summarises the active constraints, each removable on its own,
 * plus a "clear all". Whenever any field changes, the combined predicate (logical AND of all
 * fields) is pushed back to the host through {@code onChange}.
 *
 * <p>The panel is generic and domain-free; concrete criteria are supplied per view, so orders,
 * businesses and customers share one look and one behaviour.
 */
public class FilterPanel<T> extends Div {

    /** UI strings, injected so the panel stays i18n-agnostic. */
    public record Labels(String toggle, String clearAll, String chipClearAria, String panelAria) {}

    private final List<FilterField<T>> fields;
    private final Consumer<Predicate<T>> onChange;
    private final Labels labels;

    private final Button toggle = new Button();
    private final Span badge = new Span();
    private final Div body = new Div();
    private final Div chipsRow = new Div();
    private boolean expanded = false;

    public FilterPanel(
            List<FilterField<T>> fields, Consumer<Predicate<T>> onChange, Labels labels) {
        this.fields = fields;
        this.onChange = onChange;
        this.labels = labels;

        addClassName("md-filter-panel");
        getElement().setAttribute("aria-label", labels.panelAria());

        buildToggle();
        buildBody();
        buildChipsRow();

        add(body, chipsRow);
        recompute();
    }

    /** The toolbar toggle; the host adds this next to the search field. */
    public Button getToggle() {
        return toggle;
    }

    private void buildToggle() {
        toggle.setText(labels.toggle());
        toggle.setIcon(VaadinIcon.FILTER.create());
        toggle.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        toggle.addClassName("md-filter-toggle");
        toggle.getElement().setAttribute("aria-expanded", "false");
        badge.addClassName("md-filter-badge");
        badge.setVisible(false);
        toggle.getElement().appendChild(badge.getElement());
        toggle.addClickListener(e -> setExpanded(!expanded));
    }

    private void buildBody() {
        body.addClassName("md-filter-body");
        body.setVisible(false);
        for (FilterField<T> field : fields) {
            field.onChange(this::recompute);
            Div cell = new Div(field.control());
            cell.addClassName("md-filter-cell");
            body.add(cell);
        }

        Button clearAll = new Button(labels.clearAll(), VaadinIcon.CLOSE_SMALL.create());
        clearAll.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        clearAll.addClassName("md-filter-clear-all");
        clearAll.addClickListener(e -> fields.forEach(FilterField::reset));
        body.add(clearAll);
    }

    private void buildChipsRow() {
        chipsRow.addClassName("md-filter-chips");
        chipsRow.setVisible(false);
    }

    private void setExpanded(boolean expanded) {
        this.expanded = expanded;
        body.setVisible(expanded);
        toggle.getElement().setAttribute("aria-expanded", String.valueOf(expanded));
        if (expanded) {
            toggle.addClassName("md-filter-toggle--open");
        } else {
            toggle.removeClassName("md-filter-toggle--open");
        }
    }

    /** Rebuild the combined predicate + chip row from the current field states. */
    private void recompute() {
        Predicate<T> combined = item -> true;
        chipsRow.removeAll();
        int activeChipCount = 0;
        for (FilterField<T> field : fields) {
            combined = combined.and(field.predicate());
            for (FilterChip chip : field.chips()) {
                chipsRow.add(buildChip(chip));
                activeChipCount++;
            }
        }
        boolean hasActiveFilters = activeChipCount > 0;
        chipsRow.setVisible(hasActiveFilters);
        badge.setText(String.valueOf(activeChipCount));
        badge.setVisible(hasActiveFilters);
        onChange.accept(combined);
    }

    private Component buildChip(FilterChip chip) {
        Span wrap = new Span();
        wrap.addClassName("md-filter-chip");

        Span text = new Span(chip.label());
        text.addClassName("md-filter-chip__label");
        wrap.add(text);

        Button remove = new Button(VaadinIcon.CLOSE_SMALL.create());
        remove.addThemeVariants(
                ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        remove.addClassName("md-filter-chip__remove");
        remove.getElement().setAttribute("aria-label", labels.chipClearAria() + " " + chip.label());
        remove.addClickListener(e -> chip.clear().run());
        wrap.add(remove);
        return wrap;
    }
}
