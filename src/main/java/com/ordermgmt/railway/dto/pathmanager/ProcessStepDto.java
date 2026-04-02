package com.ordermgmt.railway.dto.pathmanager;

import java.util.UUID;

/** Audit record of a single process step in the path lifecycle. */
public record ProcessStepDto(
        UUID id,
        String stepType,
        String fromState,
        String toState,
        Integer typeOfInformation,
        String comment,
        String simulatedBy,
        String createdAt) {}
