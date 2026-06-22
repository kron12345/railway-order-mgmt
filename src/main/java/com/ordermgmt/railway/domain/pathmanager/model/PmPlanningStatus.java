package com.ordermgmt.railway.domain.pathmanager.model;

/**
 * Planning status of a reference train as reported back from RailOpt's rotation/resource planning.
 *
 * <p>Orthogonal to {@link PathProcessState} (the TTT path-ordering lifecycle): a train can be
 * BOOKED yet still UNPLANNED, or PLANNED before it is booked. Values follow the RailOpt planning
 * beats: not yet planned, planned, parked on a shelf ("Ablage"), or assigned to a physical
 * vehicle/crew.
 */
public enum PmPlanningStatus {
    UNPLANNED,
    PLANNED,
    ON_SHELF,
    ON_PHYSICAL_RESOURCE
}
