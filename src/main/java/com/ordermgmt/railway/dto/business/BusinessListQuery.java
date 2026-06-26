package com.ordermgmt.railway.dto.business;

import java.time.LocalDate;

import com.ordermgmt.railway.domain.business.model.BusinessStatus;

/**
 * Server-side filter for the lazy business list (P3). All fields optional ({@code null} = no
 * constraint); paging/sort travel separately in a {@code Pageable}.
 */
public record BusinessListQuery(
        String text,
        BusinessStatus status,
        LocalDate validFromMin,
        LocalDate validToMax,
        String tags,
        String assignee) {

    /** An empty query — matches everything. */
    public static BusinessListQuery all() {
        return new BusinessListQuery(null, null, null, null, null, null);
    }
}
