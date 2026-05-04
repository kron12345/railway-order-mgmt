package com.ordermgmt.railway.domain.vehicleplanning.model;

import java.time.LocalTime;
import java.util.UUID;

/**
 * Pre-resolved schedule information for a reference train. Computed in the service layer within a
 * transaction so that lazy-loaded journey locations are accessible.
 */
public record TrainScheduleInfo(
        UUID trainId, String label, LocalTime departure, LocalTime arrival) {}
