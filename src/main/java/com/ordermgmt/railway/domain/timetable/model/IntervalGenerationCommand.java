package com.ordermgmt.railway.domain.timetable.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.ordermgmt.railway.domain.order.model.Order;

/**
 * Command record encapsulating the parameters for interval-based timetable position generation.
 * Replaces the 11-parameter method signature of {@code
 * IntervalTimetableService#generateIntervalPositions}.
 */
public record IntervalGenerationCommand(
        Order order,
        String namePrefix,
        String otnStart,
        List<TimetableRowData> baseRows,
        LocalTime firstDeparture,
        LocalTime lastDeparture,
        boolean crossesMidnight,
        int intervalMinutes,
        List<LocalDate> validityDates,
        String tags,
        String comment) {}
