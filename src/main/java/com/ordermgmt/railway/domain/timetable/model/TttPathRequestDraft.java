package com.ordermgmt.railway.domain.timetable.model;

import java.util.List;

/** TTT PathRequest draft assembled by order management before any external dispatch. */
public record TttPathRequestDraft(
        List<TttJourneyLocationDraft> journeyLocations, Integer pathPlanningReferenceSequence) {}
