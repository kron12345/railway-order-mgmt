package com.ordermgmt.railway.domain.timetable.model;

/** Routed operational point enriched with map and timetable context. */
public record TimetableRoutePoint(
        String uopid,
        String name,
        String country,
        Double latitude,
        Double longitude,
        Double segmentLengthMeters,
        Double distanceFromStartMeters,
        RoutePointRole routePointRole,
        String journeyLocationType,
        String fromName,
        String toName) {}
