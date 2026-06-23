package com.ordermgmt.railway.ui.component.order;

import com.vaadin.flow.component.html.Span;

import com.ordermgmt.railway.domain.order.model.PurchaseStatus;
import com.ordermgmt.railway.domain.order.model.ResourceType;

/** Small badge + colour helpers shared by the resource / purchase rows in {@link ResourcePanel}. */
final class ResourceBadges {

    private ResourceBadges() {}

    /** A compact mono pill in the given colour (tinted background + faint border). */
    static Span small(String text, String color) {
        Span badge = new Span(text);
        badge.getStyle()
                .set("font-size", "9px")
                .set("font-family", "'JetBrains Mono', monospace")
                .set("font-weight", "600")
                .set("padding", "1px 5px")
                .set("border-radius", "3px")
                .set("color", color)
                .set("background", "color-mix(in srgb, " + color + " 10%, transparent)")
                .set("border", "1px solid color-mix(in srgb, " + color + " 20%, transparent)");
        return badge;
    }

    static String resourceColor(ResourceType type) {
        return switch (type) {
            case CAPACITY -> "var(--rom-status-info)";
            case VEHICLE -> "var(--rom-status-active)";
            case PERSONNEL -> "var(--rom-status-warning)";
        };
    }

    static String purchaseStatusColor(PurchaseStatus status) {
        return switch (status) {
            case OFFEN -> "var(--rom-text-muted)";
            case BESTELLT -> "var(--rom-status-info)";
            case BESTAETIGT -> "var(--rom-status-active)";
            case ABGELEHNT -> "var(--rom-status-danger)";
            case STORNIERT -> "var(--rom-status-neutral)";
        };
    }
}
