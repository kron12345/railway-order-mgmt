package com.ordermgmt.railway.domain.timetable.model;

import java.util.ArrayList;
import java.util.List;

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
    private List<String> activityCodes = new ArrayList<>();
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

    /** OTN of the associated train (Von/Fuer linking via 0044/0045). */
    private String associatedTrainOtn;

    /** Optional LocationSubsidiaryIdentification value for the TTT draft. */
    private String locationSubsidiaryCode;

    /** Simple key=value lines that are parsed into NetworkSpecificParameter entries. */
    private String networkSpecificParametersText;

    // ── User-entered flags (TTT export gate) ─────────────────────────────
    // A field is exported as a TTT Timing entry only when its corresponding
    // userEntered* flag is true. Estimated/derived/propagated values stay
    // local and are recomputed from explicit anchors + interpolation.

    private Boolean userEnteredArrivalExact = false;
    private Boolean userEnteredArrivalEarliest = false;
    private Boolean userEnteredArrivalLatest = false;
    private Boolean userEnteredCommercialArrival = false;
    private Boolean userEnteredDepartureExact = false;
    private Boolean userEnteredDepartureEarliest = false;
    private Boolean userEnteredDepartureLatest = false;
    private Boolean userEnteredCommercialDeparture = false;
    private Boolean userEnteredDwell = false;

    // ── Day offsets (TTT TimingAtLocation/Timing/Offset) ─────────────────
    // Day delta from PlannedCalendar, e.g. +1 for "next day" on midnight
    // crossings, -1 for modifications that start the day before X.

    /** Day offset for arrival side (TTT Offset on the ELA/LLA/ALA/PLA Timing). */
    private Integer arrivalOffset = 0;

    /** Day offset for departure side (TTT Offset on the ELD/LLD/ALD/PLD Timing). */
    private Integer departureOffset = 0;
}
