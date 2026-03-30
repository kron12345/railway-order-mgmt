package com.ordermgmt.railway.domain.order.model;

/** High-level fulfillment states for an order. */
public enum OrderStatus {
    DRAFT,
    SUBMITTED,
    CONFIRMED,
    IN_TRANSIT,
    DELIVERED,
    CANCELLED
}
