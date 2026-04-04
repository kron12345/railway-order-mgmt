package com.ordermgmt.railway.domain.pathmanager.model;

import java.util.UUID;

/**
 * Command record encapsulating fields for updating a reference train header. Replaces the
 * 8-parameter method signature of {@code PathManagerService#updateTrainHeader}.
 */
public record TrainHeaderUpdate(
        UUID trainId,
        String operationalTrainNumber,
        String trainType,
        String trafficTypeCode,
        Integer trainWeight,
        Integer trainLength,
        Integer trainMaxSpeed,
        String brakeType) {}
