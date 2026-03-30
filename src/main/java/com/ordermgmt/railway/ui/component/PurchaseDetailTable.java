package com.ordermgmt.railway.ui.component;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

import com.ordermgmt.railway.domain.order.model.PurchasePosition;
import com.ordermgmt.railway.domain.order.model.PurchaseStatus;

/**
 * Scrollable detail table for purchase positions with status badges.
 */
public class PurchaseDetailTable extends Div {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter SHORT_FMT =
            DateTimeFormatter.ofPattern("dd.MM HH:mm").withZone(ZoneId.systemDefault());

    public PurchaseDetailTable(List<PurchasePosition> purchases) {
        setWidthFull();
        getStyle().set("max-height", "220px").set("overflow", "auto");

        Grid<PurchasePosition> grid = new Grid<>(PurchasePosition.class, false);

        grid.addColumn(PurchasePosition::getPositionNumber)
                .setHeader("Nr.")
                .setWidth("100px").setFlexGrow(0);

        grid.addColumn(p -> p.getDebicode() != null ? p.getDebicode() : "—")
                .setHeader("Debicode")
                .setWidth("110px").setFlexGrow(0);

        grid.addComponentColumn(p -> statusBadge(p.getPurchaseStatus()))
                .setHeader("Status")
                .setWidth("120px").setFlexGrow(0);

        grid.addColumn(p -> p.getOrderedAt() != null ? DATE_FMT.format(p.getOrderedAt()) : "—")
                .setHeader("Bestellt am")
                .setWidth("120px").setFlexGrow(0);

        grid.addColumn(p -> p.getStatusTimestamp() != null ? SHORT_FMT.format(p.getStatusTimestamp()) : "—")
                .setHeader("Rückmeldung")
                .setFlexGrow(1);

        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_COMPACT);
        grid.setItems(purchases);
        grid.setAllRowsVisible(purchases.size() <= 8);

        add(grid);
    }

    private Span statusBadge(PurchaseStatus status) {
        if (status == null) return badge("—", "var(--rom-text-muted)");
        return switch (status) {
            case BESTAETIGT -> badge("Bestätigt", "var(--rom-status-active)");
            case BESTELLT -> badge("Bestellt", "var(--rom-status-info)");
            case OFFEN -> badge("Offen", "var(--rom-text-muted)");
            case ABGELEHNT -> badge("Abgelehnt", "var(--rom-status-danger)");
            case STORNIERT -> badge("Storniert", "var(--rom-text-muted)");
        };
    }

    private Span badge(String text, String color) {
        Span b = new Span(text);
        b.getStyle()
                .set("font-size", "9px")
                .set("font-weight", "600")
                .set("font-family", "'JetBrains Mono', monospace")
                .set("text-transform", "uppercase")
                .set("padding", "2px 6px")
                .set("border-radius", "3px")
                .set("color", color)
                .set("background", "color-mix(in srgb, " + color + " 12%, transparent)");
        return b;
    }
}
