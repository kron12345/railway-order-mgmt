package com.ordermgmt.railway.domain.timetable.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** JSON-backed timetable row stored in the archive. */
@Getter
@Setter
@NoArgsConstructor
public class TimetableRowData {

    private Integer sequence;
    private String uopid;
    private String name;
    private String country;
    private RoutePointRole routePointRole = RoutePointRole.AUTO;
    private String journeyLocationType = "INTERMEDIATE";
    private String fromName;
    private String toName;
    private Double segmentLengthMeters;
    private Double distanceFromStartMeters;
    private Boolean halt = false;
    private Boolean tttRelevant = false;
    private String activityCode;
    private Integer dwellMinutes;
    private String estimatedArrival;
    private String estimatedDeparture;
    private TimeConstraintMode arrivalMode = TimeConstraintMode.NONE;
    private String arrivalExact;
    private String arrivalEarliest;
    private String arrivalLatest;
    private TimeConstraintMode departureMode = TimeConstraintMode.NONE;
    private String departureExact;
    private String departureEarliest;
    private String departureLatest;

    /** Commercial arrival time (PLA — published timetable). */
    private String commercialArrival;

    /** Commercial departure time (PLD — published timetable). */
    private String commercialDeparture;

    /** Whether this row's times are pinned (not affected by shift/stretch propagation). */
    private Boolean pinned = false;

    /** Whether this stop was manually added (not from route calculation). */
    private Boolean manuallyAdded = false;

    /** Whether this stop is soft-deleted (strike-through, recoverable). */
    private Boolean deleted = false;
}
