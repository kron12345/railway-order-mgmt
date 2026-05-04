package com.ordermgmt.railway.ui.component.business;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Lightweight tree node for {@link BusinessLinksTree}. Three discriminated kinds:
 * {@code ORDER} (root grouping), {@code ORDER_POSITION} (mid level), {@code PURCHASE_POSITION}
 * (leaf). Only entities directly linked to the business are flagged {@code linked = true};
 * synthetic parent nodes (e.g. an OP shown only because it groups a linked PP) carry the
 * underlying entity but cannot be unlinked.
 */
public class BusinessLinkNode {

    public enum Kind {
        ORDER("AU"),
        ORDER_POSITION("AP"),
        PURCHASE_POSITION("BP");

        private final String tag;
        Kind(String tag) { this.tag = tag; }
        public String tag() { return tag; }
    }

    private final Kind kind;
    private final UUID entityId;
    private final String name;
    private final String number;
    private final boolean linked;
    private final List<BusinessLinkNode> children = new ArrayList<>();

    private BusinessLinkNode(Kind kind, UUID entityId, String name, String number, boolean linked) {
        this.kind = kind;
        this.entityId = entityId;
        this.name = name == null ? "" : name;
        this.number = number == null ? "" : number;
        this.linked = linked;
    }

    public static BusinessLinkNode order(UUID id, String name, String number) {
        return new BusinessLinkNode(Kind.ORDER, id, name, number, false);
    }

    public static BusinessLinkNode orderPosition(UUID id, String name, boolean linked) {
        return new BusinessLinkNode(Kind.ORDER_POSITION, id, name, "", linked);
    }

    public static BusinessLinkNode purchasePosition(UUID id, String name, String number,
                                                    boolean linked) {
        return new BusinessLinkNode(Kind.PURCHASE_POSITION, id, name, number, linked);
    }

    public Kind kind() { return kind; }
    public UUID entityId() { return entityId; }
    public String name() { return name; }
    public String number() { return number; }
    public boolean linked() { return linked; }
    public List<BusinessLinkNode> children() { return children; }

    /** Returns true if any field of this node contains the (lower-cased) text. */
    public boolean matches(String lowerText) {
        if (lowerText == null || lowerText.isBlank()) return true;
        return name.toLowerCase().contains(lowerText)
                || number.toLowerCase().contains(lowerText)
                || kind.tag().toLowerCase().contains(lowerText);
    }
}
