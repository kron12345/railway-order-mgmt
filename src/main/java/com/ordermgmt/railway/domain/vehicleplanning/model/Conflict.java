package com.ordermgmt.railway.domain.vehicleplanning.model;

import java.util.UUID;

/** Describes a scheduling conflict detected in a vehicle rotation. */
public record Conflict(
        UUID vehicleId, String vehicleLabel, int dayOfWeek, String description, Severity severity) {

    /** Severity level for a detected conflict. */
    public enum Severity {
        WARNING,
        ERROR
    }
}
