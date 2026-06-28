package com.ordermgmt.railway.dto.order;

import java.time.LocalDate;

import com.ordermgmt.railway.domain.order.model.OrderType;
import com.ordermgmt.railway.domain.order.model.PositionStatus;
import com.ordermgmt.railway.domain.order.model.ProcessStatus;

/**
 * Server-side filter for the lazy order list (P3). All fields optional ({@code null} / {@code
 * false} = no constraint); paging/sort travel separately in a {@code Pageable}. Replaces the
 * in-memory {@code Predicate} the master/detail list used to apply over the full list.
 *
 * <p>{@code orderType} (B2) is the derived Jahres-/Einzelbestellung badge: it has no column, so the
 * query re-derives it from the {@code createdAt} vs {@code validFrom} lead time (see {@link
 * OrderType#ofDates}). {@code incompleteOnly} (B2, SOB §5.7 production-handover check) keeps only
 * orders with at least one purchase position that is not yet TTT-{@code BOOKED}.
 */
public record OrderListQuery(
        String text,
        ProcessStatus processStatus,
        PositionStatus internalStatus,
        LocalDate validFromMin,
        LocalDate validToMax,
        String tags,
        String assignee,
        OrderType orderType,
        boolean incompleteOnly) {

    /** An empty query — matches everything. */
    public static OrderListQuery all() {
        return new OrderListQuery(null, null, null, null, null, null, null, null, false);
    }
}
