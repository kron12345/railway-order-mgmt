package com.ordermgmt.railway.dto.order;

import java.time.LocalDate;

import com.ordermgmt.railway.domain.order.model.PositionStatus;
import com.ordermgmt.railway.domain.order.model.ProcessStatus;

/**
 * Server-side filter for the lazy order list (P3). All fields optional ({@code null} = no
 * constraint); paging/sort travel separately in a {@code Pageable}. Replaces the in-memory {@code
 * Predicate} the master/detail list used to apply over the full list.
 */
public record OrderListQuery(
        String text,
        ProcessStatus processStatus,
        PositionStatus internalStatus,
        LocalDate validFromMin,
        LocalDate validToMax,
        String tags,
        String assignee) {

    /** An empty query — matches everything. */
    public static OrderListQuery all() {
        return new OrderListQuery(null, null, null, null, null, null, null);
    }
}
