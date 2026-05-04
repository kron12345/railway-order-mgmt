package com.ordermgmt.railway.domain.vehicleplanning.model;

import java.time.LocalTime;
import java.util.UUID;

/**
 * Display-ready train information, pre-resolved from the latest version's journey locations. Used
 * by the rotation planning UI to avoid lazy-loading issues in the Vaadin UI thread.
 */
public record TrainDisplayInfo(
        UUID trainId,
        String otn,
        String fromLocation,
        String toLocation,
        LocalTime departure,
        LocalTime arrival) {

    public String timeRange() {
        String dep = departure != null ? departure.toString() : "?";
        String arr = arrival != null ? arrival.toString() : "?";
        return dep + "\u2013" + arr;
    }

    public String route() {
        String from = fromLocation != null ? fromLocation : "?";
        String to = toLocation != null ? toLocation : "?";
        return from + " \u2192 " + to;
    }

    /** Label for ComboBox display: "101 — Bern → Zürich (06:00–07:30)" */
    public String comboLabel() {
        return otn + " \u2014 " + route() + " (" + timeRange() + ")";
    }
}
