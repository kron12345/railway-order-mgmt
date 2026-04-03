package com.ordermgmt.railway.dto.pathmanager;

import java.util.List;

/** Journey location (stop) in a train's itinerary. */
public record JourneyLocationDto(
        Integer sequence,
        String countryCodeIso,
        String locationPrimaryCode,
        String primaryLocationName,
        String uopid,
        String journeyLocationType,
        String arrivalTime,
        String departureTime,
        Integer dwellTime,
        String arrivalQualifier,
        String departureQualifier,
        String subsidiaryCode,
        List<String> activityCodes,
        String associatedTrainOtn) {}
