package com.ordermgmt.railway.domain.order.model;

/**
 * Role of an order position in the train hierarchy: a stable train identity ({@code ZUG}) or one of
 * its (OTN × Verkehrstage) expressions ({@code AUSPRAEGUNG}). {@code null} marks a legacy flat
 * position, treated as a train with a single implicit expression.
 */
public enum PositionVariantType {
    ZUG,
    AUSPRAEGUNG
}
