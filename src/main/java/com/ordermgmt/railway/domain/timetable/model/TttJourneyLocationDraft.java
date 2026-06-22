package com.ordermgmt.railway.domain.timetable.model;

import java.util.List;
import java.util.Map;

/** TTT-ready view of one PlannedJourneyLocation produced by order management. */
public record TttJourneyLocationDraft(
        Integer sequence,
        String uopid,
        String countryCodeIso,
        String primaryLocationName,
        String journeyLocationTypeCode,
        List<TttTiming> timings,
        Integer dwellTime,
        List<String> trainActivityTypes,
        String associatedTrainOtn,
        String locationSubsidiaryCode,
        Map<String, Object> networkSpecificParameters,
        boolean exportedToTtt) {}
