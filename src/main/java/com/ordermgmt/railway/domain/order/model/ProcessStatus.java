package com.ordermgmt.railway.domain.order.model;

/**
 * Process phases used to classify an order, listed in chronological workflow order. The declaration
 * order is significant: the order-detail status stepper relies on {@link Enum#ordinal()} to render
 * completed vs. current vs. upcoming phases, so constants must stay in workflow sequence.
 */
public enum ProcessStatus {
    AUFTRAG,
    PLANUNG,
    PRODUKT_LEISTUNG,
    PRODUKTION,
    ABRECHNUNG_NACHBEREITUNG
}
