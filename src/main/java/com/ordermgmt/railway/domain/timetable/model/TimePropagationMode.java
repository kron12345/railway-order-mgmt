package com.ordermgmt.railway.domain.timetable.model;

/** Defines how a time change at one stop propagates to other stops. */
public enum TimePropagationMode {
    /** Shift all subsequent (or preceding) times by the same delta. */
    SHIFT,
    /** Stretch/compress travel times between this stop and the next pinned stop. */
    STRETCH
}
