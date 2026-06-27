package com.ordermgmt.railway.ui.component.order;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import com.vaadin.flow.component.html.Div;

import com.ordermgmt.railway.domain.order.model.OrderPosition;
import com.ordermgmt.railway.domain.order.model.PurchasePosition;
import com.ordermgmt.railway.domain.order.model.PurchaseStatus;

/**
 * Compact, aggregated status cluster for an order position's Bestellpositionen. Combines the
 * procurement status ({@link PurchaseStatus}) and the external TTT status ({@code pmProcessState})
 * into small count badges, so a collapsed / compact position row shows its order health at a glance
 * without expanding the resource panel.
 */
final class PurchaseStatusRollup {

    private PurchaseStatusRollup() {}

    /**
     * Builds the cluster; returns an empty {@link Div} when the position has no Bestellpositionen.
     */
    static Div build(OrderPosition position, BiFunction<String, Object[], String> t) {
        Div cluster = new Div();
        cluster.addClassName("op-status-rollup");
        cluster.getStyle()
                .set("display", "flex")
                .set("flex-wrap", "wrap")
                .set("align-items", "center")
                .set("gap", "5px");

        List<PurchasePosition> purchases =
                position.getPurchasePositions() == null
                        ? List.of()
                        : new ArrayList<>(position.getPurchasePositions());
        if (purchases.isEmpty()) {
            return cluster;
        }

        // Procurement rollup — count per PurchaseStatus, in enum order.
        Map<PurchaseStatus, Long> procurementCounts = new LinkedHashMap<>();
        for (PurchasePosition purchase : purchases) {
            PurchaseStatus status =
                    purchase.getPurchaseStatus() != null
                            ? purchase.getPurchaseStatus()
                            : PurchaseStatus.OFFEN;
            procurementCounts.merge(status, 1L, Long::sum);
        }
        for (PurchaseStatus status : PurchaseStatus.values()) {
            long count = procurementCounts.getOrDefault(status, 0L);
            if (count > 0) {
                cluster.add(
                        ResourceBadges.small(
                                count
                                        + " "
                                        + t.apply(
                                                "purchase.status." + status.name(), new Object[0]),
                                ResourceBadges.purchaseStatusColor(status)));
            }
        }

        Map<String, Long> tttCounts = new LinkedHashMap<>();
        for (PurchasePosition purchase : purchases) {
            String state = purchase.getPmProcessState();
            if (state != null && !state.isBlank()) {
                tttCounts.merge(state, 1L, Long::sum);
            }
        }
        if (!tttCounts.isEmpty()) {
            cluster.add(ResourceBadges.small("TTT", "var(--rom-text-muted)"));
            tttCounts.forEach(
                    (state, count) ->
                            cluster.add(
                                    ResourceBadges.small(
                                            count + " " + tttLabel(state, t), tttColor(state))));
        }
        return cluster;
    }

    private static String tttLabel(String state, BiFunction<String, Object[], String> t) {
        String key = "pm.state." + state;
        String label = t.apply(key, new Object[0]);
        // Fall back to the raw state if no translation is registered for it.
        return label == null || label.isBlank() || label.contains(key) ? state : label;
    }

    private static String tttColor(String state) {
        return switch (state) {
            case "BOOKED" -> "var(--rom-status-active)";
            case "DRAFT_OFFERED", "FINAL_OFFERED" -> "var(--rom-status-info)";
            case "REVISION_REQUESTED",
                    "MODIFICATION_REQUESTED",
                    "ALTERATION_OFFERED",
                    "ALTERATION_ANNOUNCED" ->
                    "var(--rom-status-warning)";
            case "CANCELED", "WITHDRAWN", "NO_ALTERNATIVE", "SUPERSEDED" ->
                    "var(--rom-status-danger)";
            default -> "var(--rom-text-secondary)";
        };
    }
}
