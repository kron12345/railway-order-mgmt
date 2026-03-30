package com.ordermgmt.railway.domain.order.model;

/** Internal workflow states for an order position. */
public enum PositionStatus {
    IN_BEARBEITUNG,
    FREIGEGEBEN,
    UEBERARBEITEN,
    UEBERMITTELT,
    BEANTRAGT,
    ABGESCHLOSSEN,
    ANNULLIERT
}
