package com.ordermgmt.railway.domain.order.model;

/** Process phases used to classify an order. */
public enum ProcessStatus {
    AUFTRAG,
    PLANUNG,
    PRODUKT_LEISTUNG,
    PRODUKTION,
    ABRECHNUNG_NACHBEREITUNG
}
