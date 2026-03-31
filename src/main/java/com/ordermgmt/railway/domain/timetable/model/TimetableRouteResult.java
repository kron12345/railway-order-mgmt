package com.ordermgmt.railway.domain.timetable.model;

import java.util.List;

/** Full routed path with ordered operational points and total length. */
public record TimetableRouteResult(List<TimetableRoutePoint> points, Double totalLengthMeters) {}
