package com.ordermgmt.railway.domain.order.model;

/**
 * Origin of a change captured as an order-position version or OTN-history entry: the original plan,
 * a self-initiated {@code MODIFICATION}, an infrastructure {@code ALTERATION}, or a {@code
 * CANCELLATION}.
 */
public enum PositionChangeSource {
    INITIAL,
    MODIFICATION,
    ALTERATION,
    CANCELLATION
}
