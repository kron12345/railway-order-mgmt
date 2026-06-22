package com.ordermgmt.railway.ui.component.a11y;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

    private final TextField input = new TextField();
    private final VerticalLayout results = new VerticalLayout();
    private final List<Item> all = new ArrayList<>();
    private final List<Item> filtered = new ArrayList<>();
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
        input.setValueChangeTimeout(120);
        input.getElement().setAttribute("aria-label", "Command Palette Suche");
        input.getElement().setAttribute("autocomplete", "off");
        input.addValueChangeListener(
                e ->
                        applyFilter(
                                e.getValue() == null
                                        ? ""
                                        : e.getValue().trim().toLowerCase(Locale.ROOT)));

        results.addClassName("cmd-palette__results");
        results.setSpacing(false);
        results.setPadding(false);
        results.setWidthFull();
        results.getElement().setAttribute("role", "listbox");
        results.getElement().setAttribute("aria-label", "Treffer");

        add(input, results);

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
        // Orders + their positions.
        orderService
                .findAllWithPositions()
                .forEach(
                        o -> {
                            String label =
                                    (o.getOrderNumber() == null ? "—" : o.getOrderNumber())
                                            + " "
                                            + (o.getName() == null ? "" : o.getName());
                            all.add(
                                    new Item(
                                            "Auftrag",
                                            label,
                                            o.getCustomer() != null
                                                            && o.getCustomer().getName() != null
                                                    ? o.getCustomer().getName()
                                                    : "",
                                            "orders/" + o.getId(),
                                            label.toLowerCase(Locale.ROOT)));
                            if (o.getPositions() != null) {
                                o.getPositions()
                                        .forEach(
                                                p -> {
                                                    String pLabel =
                                                            p.getName() == null ? "—" : p.getName();
                                                    all.add(
                                                            new Item(
                                                                    "Position",
                                                                    pLabel,
                                                                    o.getOrderNumber() == null
                                                                            ? ""
                                                                            : o.getOrderNumber(),
                                                                    "orders/"
                                                                            + o.getId()
                                                                            + "/positions/"
                                                                            + p.getId(),
                                                                    (pLabel
                                                                                    + " "
                                                                                    + (o
                                                                                                            .getOrderNumber()
                                                                                                    == null
                                                                                            ? ""
                                                                                            : o
                                                                                                    .getOrderNumber()))
                                                                            .toLowerCase(
                                                                                    Locale.ROOT)));
                                                });
                            }
                        });
        businessService
                .listAll()
                .forEach(
                        b -> {
                            String label = b.getTitle() == null ? "—" : b.getTitle();
                            all.add(
                                    new Item(
                                            "Geschäft",
                                            label,
                                            b.getDescription() == null ? "" : b.getDescription(),
                                            "businesses/" + b.getId(),
                                            (label
                                                            + " "
                                                            + (b.getDescription() == null
                                                                    ? ""
                                                                    : b.getDescription()))
                                                    .toLowerCase(Locale.ROOT)));
                        });
    }

    private void applyFilter(String q) {
        filtered.clear();
        if (q.isBlank()) {
            // Show top 30 by default.
            filtered.addAll(all.subList(0, Math.min(30, all.size())));
        } else {
            for (Item it : all) {
                if (it.searchKey().contains(q)) filtered.add(it);
                if (filtered.size() >= 60) break;
            }
        }
        activeIndex = filtered.isEmpty() ? -1 : 0;
        renderResults();
    }

    private void renderResults() {
        results.removeAll();
        for (int i = 0; i < filtered.size(); i++) {
            results.add(buildRow(filtered.get(i), i));
        }
    }

    private Component buildRow(Item it, int index) {
        var row = new HorizontalLayout();
        row.addClassName("cmd-palette__row");
        if (index == activeIndex) row.addClassName("cmd-palette__row--active");
        row.setWidthFull();
        row.setSpacing(true);
        row.setPadding(false);
        row.getElement().setAttribute("role", "option");
        row.getElement().setAttribute("aria-selected", index == activeIndex ? "true" : "false");

        Span tag = new Span(it.type().toUpperCase());
        tag.addClassName("cmd-palette__tag");
        tag.addClassName("cmd-palette__tag--" + it.type().toLowerCase());

        Span lbl = new Span(it.label());
        lbl.addClassName("cmd-palette__label");

        Span detail = new Span(it.detail() == null ? "" : it.detail());
        detail.addClassName("cmd-palette__detail");

        Div spacer = new Div();
        spacer.getStyle().set("flex", "1");

        row.add(tag, lbl, spacer, detail);
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
        if (newIndex < 0) newIndex = 0;
        if (newIndex >= filtered.size()) newIndex = filtered.size() - 1;
        activeIndex = newIndex;
        renderResults();
    }

    private void activate() {
        if (activeIndex < 0 || activeIndex >= filtered.size()) return;
        Item it = filtered.get(activeIndex);
        close();
        UI.getCurrent().navigate(it.route());
    }
}
