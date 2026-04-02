package com.ordermgmt.railway.dto.pathmanager;

import java.util.UUID;

/** Lightweight summary of a reference train for list views. */
public record TrainSummaryDto(
        UUID id,
        String operationalTrainNumber,
        String trainType,
        String trafficTypeCode,
        String processState,
        int versionCount,
        String routeSummary) {}
