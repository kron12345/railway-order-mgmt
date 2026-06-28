package com.ordermgmt.railway.ui.component.order;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.BiFunction;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

import com.ordermgmt.railway.domain.business.model.Business;
import com.ordermgmt.railway.domain.order.model.CoverageType;
import com.ordermgmt.railway.domain.order.model.PurchasePosition;
import com.ordermgmt.railway.domain.order.model.PurchaseStatus;
import com.ordermgmt.railway.domain.order.model.ResourceNeed;
import com.ordermgmt.railway.domain.order.model.ResourceType;
import com.ordermgmt.railway.domain.order.service.PurchaseOrderService;
import com.ordermgmt.railway.ui.component.business.BusinessChips;

/**
 * Renders one purchase-position line inside {@link ResourcePanel}: position number + description,
 * status badge, TTT/R²P state badges and their action buttons (shown only when editable), the
 * ordered-at timestamp, and the linked-business chips. Stateless — the panel passes the
 * per-purchase context and a reload callback.
 */
final class ResourcePurchaseRow {

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.systemDefault());

    private ResourcePurchaseRow() {}

    static Div build(
            PurchasePosition purchase,
            ResourceNeed need,
            boolean editable,
            String operationalTrainNumber,
            String routeLabel,
            List<Business> linkedBusinesses,
            PurchaseOrderService purchaseOrderService,
            BiFunction<String, Object[], String> translator,
            Runnable reloadAll) {
        Div row = new Div();
        row.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("gap", "6px")
                .set("padding", "2px 0 2px 12px")
                .set("flex-wrap", "wrap");

        // Position number
        Span number = new Span(purchase.getPositionNumber());
        number.getStyle()
                .set("font-family", "'JetBrains Mono', monospace")
                .set("font-size", "10px")
                .set("font-weight", "600")
                .set("color", "var(--rom-accent)");
        row.add(number);

        // Free-text description (entered in the purchase dialog) — shown muted next to the number.
        if (purchase.getDescription() != null && !purchase.getDescription().isBlank()) {
            Span desc = new Span(purchase.getDescription());
            desc.getStyle().set("font-size", "11px").set("color", "var(--rom-text-secondary)");
            row.add(desc);
        }

        // Status badge
        row.add(purchaseStatusBadge(purchase, translator));

        // TTT status for CAPACITY purchases
        if (purchase.getPmPathRequestId() != null) {
            if (purchase.getPmProcessState() != null) {
                row.add(
                        ResourceBadges.small(
                                "TTT: " + purchase.getPmProcessState(), "var(--rom-status-info)"));
            }
            if (purchase.getPmTtrPhase() != null) {
                row.add(
                        ResourceBadges.small(
                                purchase.getPmTtrPhase(), "var(--rom-text-secondary)"));
            }

            // Sync button (mutators on an unlocked order only)
            if (editable) {
                row.add(
                        PurchaseOrderButtons.sync(
                                purchase, purchaseOrderService, translator, reloadAll));
            }
        }

        // TTT order button for unordered CAPACITY purchases (mutators on an unlocked order only)
        if (editable
                && purchase.getPmPathRequestId() == null
                && need.getResourceType() == ResourceType.CAPACITY) {
            row.add(
                    PurchaseOrderButtons.ttt(
                            purchase,
                            operationalTrainNumber,
                            routeLabel,
                            purchaseOrderService,
                            translator,
                            reloadAll));
        }

        // R²P channel for non-capacity external needs (e.g. Lokführer): mock "Bestellen" →
        // BESTELLT, shown like the TTT flow so it reads the same way.
        if (isR2pPurchase(need)) {
            row.add(ResourceBadges.small("R²P", "var(--rom-status-info)"));
            if (editable && purchase.getPurchaseStatus() == PurchaseStatus.OFFEN) {
                row.add(
                        PurchaseOrderButtons.r2p(
                                purchase, purchaseOrderService, translator, reloadAll));
            }
        }

        // Ordered-at timestamp
        if (purchase.getOrderedAt() != null) {
            Span orderedAt = new Span(DT_FMT.format(purchase.getOrderedAt()));
            orderedAt
                    .getStyle()
                    .set("font-size", "9px")
                    .set("color", "var(--rom-text-muted)")
                    .set("font-family", "'JetBrains Mono', monospace");
            row.add(orderedAt);
        }

        // Linked businesses for this purchase position (clickable chips → business detail).
        if (!linkedBusinesses.isEmpty()) {
            row.add(
                    new BusinessChips(
                            linkedBusinesses, key -> translator.apply(key, new Object[0])));
        }

        return row;
    }

    /** A non-capacity external purchase is ordered via the (mock) R²P channel, not TTT. */
    private static boolean isR2pPurchase(ResourceNeed need) {
        return need.getCoverageType() == CoverageType.EXTERNAL
                && need.getResourceType() != ResourceType.CAPACITY;
    }

    private static Span purchaseStatusBadge(
            PurchasePosition purchase, BiFunction<String, Object[], String> translator) {
        String label =
                translator.apply(
                        "purchase.status." + purchase.getPurchaseStatus().name(), new Object[0]);
        String color = ResourceBadges.purchaseStatusColor(purchase.getPurchaseStatus());
        return ResourceBadges.small(label, color);
    }
}
