package com.ordermgmt.railway.dto.order;

import java.util.UUID;

/**
 * Flat projection for the ⌃K command palette: one row per (order, position) plus one row per order
 * with no positions ({@code positionId}/{@code positionName} null). Avoids initializing the orders'
 * positions / purchase-positions / resource-needs collections just to list searchable labels.
 */
public record CommandPaletteRow(
        UUID orderId,
        String orderNumber,
        String orderName,
        String customerName,
        UUID positionId,
        String positionName) {}
