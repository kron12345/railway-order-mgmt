package com.ordermgmt.railway.ui.component;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.BiFunction;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

import com.ordermgmt.railway.domain.order.model.PurchasePosition;
import com.ordermgmt.railway.domain.order.model.PurchaseStatus;

public class PurchaseDetailTable extends Div {

    private static final int MAX_ALL_ROWS_VISIBLE = 8;
    private static final String NO_VALUE = "—";
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter SHORT_FMT =
            DateTimeFormatter.ofPattern("dd.MM HH:mm").withZone(ZoneId.systemDefault());

    private final BiFunction<String, Object[], String> translator;

    public PurchaseDetailTable(
            List<PurchasePosition> purchases, BiFunction<String, Object[], String> translator) {
        this.translator = translator;
        setWidthFull();
        getStyle().set("max-height", "220px").set("overflow", "auto");

        add(createGrid(purchases));
    }

    private Grid<PurchasePosition> createGrid(List<PurchasePosition> purchases) {
        Grid<PurchasePosition> grid = new Grid<>(PurchasePosition.class, false);

        grid.addColumn(PurchasePosition::getPositionNumber)
                .setHeader(tr("purchase.number"))
                .setWidth("100px")
                .setFlexGrow(0);

        grid.addColumn(
                        purchase ->
                                purchase.getDebicode() != null
                                        ? purchase.getDebicode()
                                        : NO_VALUE)
                .setHeader(tr("purchase.debicode"))
                .setWidth("110px")
                .setFlexGrow(0);

        grid.addComponentColumn(purchase -> statusBadge(purchase.getPurchaseStatus()))
                .setHeader(tr("purchase.status"))
                .setWidth("120px")
                .setFlexGrow(0);

        grid.addColumn(
                        purchase ->
                                purchase.getOrderedAt() != null
                                        ? DATE_FMT.format(purchase.getOrderedAt())
                                        : NO_VALUE)
                .setHeader(tr("purchase.orderedAt"))
                .setWidth("120px")
                .setFlexGrow(0);

        grid.addColumn(
                        purchase ->
                                purchase.getStatusTimestamp() != null
                                        ? SHORT_FMT.format(purchase.getStatusTimestamp())
                                        : NO_VALUE)
                .setHeader(tr("purchase.status"))
                .setFlexGrow(1);

        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_COMPACT);
        grid.setItems(purchases);
        grid.setAllRowsVisible(purchases.size() <= MAX_ALL_ROWS_VISIBLE);
        return grid;
    }

    private Span statusBadge(PurchaseStatus status) {
        if (status == null) {
            return badge(NO_VALUE, "var(--rom-text-muted)");
        }
        String key = "purchase.status." + status.name();
        String label = tr(key);
        return switch (status) {
            case BESTAETIGT -> badge(label, "var(--rom-status-active)");
            case BESTELLT -> badge(label, "var(--rom-status-info)");
            case OFFEN -> badge(label, "var(--rom-text-muted)");
            case ABGELEHNT -> badge(label, "var(--rom-status-danger)");
            case STORNIERT -> badge(label, "var(--rom-text-muted)");
        };
    }

    private Span badge(String text, String color) {
        Span badge = new Span(text);
        badge.getStyle()
                .set("font-size", "9px")
                .set("font-weight", "600")
                .set("font-family", "'JetBrains Mono', monospace")
                .set("text-transform", "uppercase")
                .set("padding", "2px 6px")
                .set("border-radius", "3px")
                .set("color", color)
                .set("background", "color-mix(in srgb, " + color + " 12%, transparent)");
        return badge;
    }

    private String tr(String key) {
        return translator.apply(key, new Object[0]);
    }
}
