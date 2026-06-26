package com.ordermgmt.railway.dto.order;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import com.ordermgmt.railway.domain.order.model.OrderType;
import com.ordermgmt.railway.domain.order.model.PositionStatus;
import com.ordermgmt.railway.domain.order.model.ProcessStatus;

/**
 * Read projection for the lazy order list (P3): the order fields a card shows plus an aggregate
 * position count — fetched via a query projection, never by initializing the positions collection.
 */
public record OrderListItem(
        UUID id,
        String orderNumber,
        String name,
        String customerName,
        LocalDate validFrom,
        LocalDate validTo,
        ProcessStatus processStatus,
        PositionStatus internalStatus,
        String assignmentType,
        String assignmentName,
        String tags,
        Instant createdAt,
        long positionCount) {

    /** Derived order-type badge (same rule as {@link OrderType#of}); {@code null} when unknown. */
    public OrderType orderType() {
        if (createdAt == null || validFrom == null) {
            return null;
        }
        return OrderType.ofDates(createdAt.atZone(ZoneId.systemDefault()).toLocalDate(), validFrom);
    }
}
