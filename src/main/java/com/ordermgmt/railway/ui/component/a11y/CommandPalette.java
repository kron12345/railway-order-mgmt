package com.ordermgmt.railway.ui.component.a11y;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.value.ValueChangeMode;

import com.ordermgmt.railway.domain.business.service.BusinessService;
import com.ordermgmt.railway.domain.order.service.OrderService;

/**
 * Bloomberg-Function-Code-style global command palette opened via {@code Ctrl+K}. Lists all orders,
 * businesses, and order positions with fuzzy substring matching; arrow keys navigate, Enter
 * activates the highlighted item.
 *
 * <p>Mounted as a hidden dialog on the {@link com.vaadin.flow.component.UI}; the global keydown
 * listener in {@link com.ordermgmt.railway.ui.layout.MainLayout} fires a custom {@code
 * rom-command-palette} event the Java side listens for.
 */
public class CommandPalette extends Dialog {

    public record Item(String type, String label, String detail, String route, String searchKey) {}

    private static final int VALUE_CHANGE_TIMEOUT_MS = 120;
    private static final int DEFAULT_RESULT_LIMIT = 30;
    private static final int FILTERED_RESULT_LIMIT = 60;

    private final TextField input = new TextField();
    private final VerticalLayout results = new VerticalLayout();
    private final List<Item> all = new ArrayList<>();
    private final List<Item> filtered = new ArrayList<>();
    // Shown (outside the results listbox) when matches exceed the display cap, so extra hits are
    // never silently dropped — the user is told to narrow the search.
    private final Span moreHint = new Span();
    private int activeIndex = -1;

    public CommandPalette(OrderService orderService, BusinessService businessService) {
        addClassName("cmd-palette");
        setWidth("640px");
        setMaxHeight("520px");
        setDraggable(false);
        setHeaderTitle(null);
        setCloseOnEsc(true);
        setCloseOnOutsideClick(true);

        loadItems(orderService, businessService);
        applyFilter("");

        input.setPlaceholder("Suche Aufträge, Geschäfte, Positionen … (↑↓ wählen, Enter öffnet)");
        input.setPrefixComponent(VaadinIcon.SEARCH.create());
        input.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        input.setWidthFull();
        input.setValueChangeMode(ValueChangeMode.LAZY);
        input.setValueChangeTimeout(VALUE_CHANGE_TIMEOUT_MS);
        input.getElement().setAttribute("aria-label", "Command Palette Suche");
        input.getElement().setAttribute("autocomplete", "off");
        input.addValueChangeListener(e -> applyFilter(normalizeSearch(e.getValue())));

        results.addClassName("cmd-palette__results");
        results.setSpacing(false);
        results.setPadding(false);
        results.setWidthFull();
        results.getElement().setAttribute("role", "listbox");
        results.getElement().setAttribute("aria-label", "Treffer");

        moreHint.addClassName("cmd-palette__more");
        moreHint.getElement().setAttribute("aria-live", "polite");
        moreHint.getStyle()
                .set("display", "block")
                .set("padding", "6px 10px")
                .set("font-size", "11px")
                .set("color", "var(--rom-text-muted)");
        moreHint.setVisible(false);

        add(input, results, moreHint);

        // Keyboard handler at the dialog level.
        getElement()
                .addEventListener(
                        "keydown",
                        e -> {
                            String key = e.getEventData().getString("event.key");
                            switch (key) {
                                case "ArrowDown" -> setActive(activeIndex + 1);
                                case "ArrowUp" -> setActive(activeIndex - 1);
                                case "Enter" -> activate();
                                default -> {}
                            }
                        })
                .addEventData("event.key")
                .setFilter("['ArrowDown','ArrowUp','Enter'].includes(event.key)");

        addOpenedChangeListener(
                e -> {
                    if (e.isOpened()) {
                        input.clear();
                        input.focus();
                    }
                });
    }

    private void loadItems(OrderService orderService, BusinessService businessService) {
        all.clear();
        // Orders + their positions, from a flat projection (one row per order/position; no entity
        // collections are initialized). An order may appear on several rows -> add it once.
        Set<UUID> seenOrderIds = new HashSet<>();
        for (var row : orderService.commandPaletteRows()) {
            String orderNumber = textOrBlank(row.orderNumber());
            if (seenOrderIds.add(row.orderId())) {
                String label =
                        (orderNumber.isEmpty() ? "—" : orderNumber)
                                + " "
                                + textOrBlank(row.orderName());
                all.add(
                        new Item(
                                "Auftrag",
                                label,
                                textOrBlank(row.customerName()),
                                "orders/" + row.orderId(),
                                normalizeSearch(label)));
            }
            if (row.positionId() != null) {
                String positionLabel = row.positionName() == null ? "—" : row.positionName();
                all.add(
                        new Item(
                                "Position",
                                positionLabel,
                                orderNumber,
                                "orders/" + row.orderId() + "/positions/" + row.positionId(),
                                normalizeSearch(positionLabel + " " + orderNumber)));
            }
        }
        businessService
                .listAll()
                .forEach(
                        business -> {
                            String label = business.getTitle() == null ? "—" : business.getTitle();
                            String description = textOrBlank(business.getDescription());
                            all.add(
                                    new Item(
                                            "Geschäft",
                                            label,
                                            description,
                                            "businesses/" + business.getId(),
                                            normalizeSearch(label + " " + description)));
                        });
    }

    private void applyFilter(String query) {
        filtered.clear();
        boolean truncated;
        if (query.isBlank()) {
            int shown = Math.min(DEFAULT_RESULT_LIMIT, all.size());
            filtered.addAll(all.subList(0, shown));
            truncated = all.size() > shown;
        } else {
            int matches = 0;
            for (Item item : all) {
                if (item.searchKey().contains(query)) {
                    matches++;
                    if (filtered.size() < FILTERED_RESULT_LIMIT) {
                        filtered.add(item);
                    }
                }
            }
            truncated = matches > filtered.size();
        }
        activeIndex = filtered.isEmpty() ? -1 : 0;
        moreHint.setText(truncated ? "Weitere Treffer — Suche eingrenzen" : "");
        moreHint.setVisible(truncated);
        renderResults();
    }

    private void renderResults() {
        results.removeAll();
        for (int i = 0; i < filtered.size(); i++) {
            results.add(buildRow(filtered.get(i), i));
        }
    }

    private Component buildRow(Item item, int index) {
        var row = new HorizontalLayout();
        row.addClassName("cmd-palette__row");
        if (index == activeIndex) {
            row.addClassName("cmd-palette__row--active");
        }
        row.setWidthFull();
        row.setSpacing(true);
        row.setPadding(false);
        row.getElement().setAttribute("role", "option");
        row.getElement().setAttribute("aria-selected", index == activeIndex ? "true" : "false");

        Span tag = new Span(item.type().toUpperCase());
        tag.addClassName("cmd-palette__tag");
        tag.addClassName("cmd-palette__tag--" + item.type().toLowerCase());

        Span label = new Span(item.label());
        label.addClassName("cmd-palette__label");

        Span detail = new Span(textOrBlank(item.detail()));
        detail.addClassName("cmd-palette__detail");

        Div spacer = new Div();
        spacer.getStyle().set("flex", "1");

        row.add(tag, label, spacer, detail);
        row.addClickListener(
                e -> {
                    activeIndex = index;
                    activate();
                });
        return row;
    }

    private void setActive(int newIndex) {
        if (filtered.isEmpty()) {
            activeIndex = -1;
            return;
        }
        activeIndex = clampResultIndex(newIndex);
        renderResults();
    }

    private void activate() {
        if (activeIndex < 0 || activeIndex >= filtered.size()) {
            return;
        }
        Item item = filtered.get(activeIndex);
        close();
        UI.getCurrent().navigate(item.route());
    }

    private String normalizeSearch(String value) {
        return textOrBlank(value).trim().toLowerCase(Locale.ROOT);
    }

    private int clampResultIndex(int index) {
        if (index < 0) {
            return 0;
        }
        return Math.min(index, filtered.size() - 1);
    }

    private String textOrBlank(String value) {
        return value != null ? value : "";
    }
}
