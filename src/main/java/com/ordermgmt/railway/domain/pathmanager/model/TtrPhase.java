package com.ordermgmt.railway.domain.pathmanager.model;

/** TTR (Timetable Redesign) phases as defined in the European rail capacity framework. */
public enum TtrPhase {
    CAPACITY_STRATEGY("X-60 to X-36", "Capacity Strategy"),
    CAPACITY_MODEL("X-36 to X-18", "Capacity Model"),
    CAPACITY_SUPPLY("X-18 to X-11", "Capacity Supply"),
    ANNUAL_ORDERING("X-11 to X-8.5", "Annual Ordering (Bestellphase 2)"),
    LATE_ORDERING("X-8.5 to X-2", "Late Ordering (Bestellphase 3)"),
    AD_HOC_ORDERING("X-2 onwards", "Ad Hoc Ordering"),
    PAST("Past", "Timetable year ended");

    private final String timeline;
    private final String label;

    TtrPhase(String timeline, String label) {
        this.timeline = timeline;
        this.label = label;
    }

    public String getTimeline() {
        return timeline;
    }

    public String getLabel() {
        return label;
    }
}
