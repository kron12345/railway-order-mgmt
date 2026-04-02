package com.ordermgmt.railway.dto.pathmanager;

import java.util.List;
import java.util.UUID;

/** Snapshot of train data at a specific version in the path lifecycle. */
public record TrainVersionDto(
        UUID id,
        int versionNumber,
        String versionType,
        String label,
        String operationalTrainNumber,
        List<JourneyLocationDto> locations) {}
