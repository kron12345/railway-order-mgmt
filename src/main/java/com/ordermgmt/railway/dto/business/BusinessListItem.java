package com.ordermgmt.railway.dto.business;

import java.time.LocalDate;
import java.util.UUID;

import com.ordermgmt.railway.domain.business.model.BusinessStatus;

/**
 * Read projection for the lazy business list (P3): the fields a card shows plus the linked
 * order-/purchase-position counts — fetched via aggregate {@code SIZE(...)} in the query, never by
 * initializing the n:m collections.
 */
public record BusinessListItem(
        UUID id,
        String title,
        BusinessStatus status,
        LocalDate validTo,
        LocalDate dueDate,
        String assignmentType,
        String assignmentName,
        String tags,
        int linkedOrderPositionCount,
        int linkedPurchasePositionCount,
        boolean automatic) {}
