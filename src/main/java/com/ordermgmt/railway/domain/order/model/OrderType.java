package com.ordermgmt.railway.domain.order.model;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Derived (not persisted) order classification, shown as a badge. Distinguishes the SOB use cases
 * AF1 Jahresfahrplan (annual / ATT New&amp;Late PR) from AF3 Extrazug adhoc (ad-hoc PR).
 *
 * <p>Following the SOB Fachkonzept (§5.3: the TTR process can be derived from "Bestelldatum und
 * Betriebstag"), the type is inferred from the lead time between when the order was created and its
 * first day of validity: roughly two months or more ahead is the annual cycle, anything closer is
 * an ad-hoc single order.
 */
public enum OrderType {
    JAHRESBESTELLUNG,
    EINZELBESTELLUNG;

    /**
     * Ad-hoc boundary in calendar months before the operating date (SOB X-2; see TtrPhaseResolver).
     */
    private static final int ADHOC_MONTHS_BEFORE = 2;

    /** Returns the derived type, or {@code null} when order date or validity is unknown. */
    public static OrderType of(Order order) {
        if (order == null || order.getValidFrom() == null || order.getCreatedAt() == null) {
            return null;
        }
        LocalDate ordered = order.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate adhocBoundary = order.getValidFrom().minusMonths(ADHOC_MONTHS_BEFORE);
        // Ordered on/before X-2 months → annual cycle; within the last two months → ad-hoc single.
        return ordered.isAfter(adhocBoundary) ? EINZELBESTELLUNG : JAHRESBESTELLUNG;
    }
}
