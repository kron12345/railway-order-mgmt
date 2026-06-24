package com.ordermgmt.railway.domain.order.model;

/** Origin of a resource need — how it was created. */
public enum ResourceOrigin {
    AUTO,
    MANUAL,
    PLANNING,
    /**
     * Created from an inbound R2P order (a third party sent a timetable + personnel/vehicle order).
     */
    R2P
}
