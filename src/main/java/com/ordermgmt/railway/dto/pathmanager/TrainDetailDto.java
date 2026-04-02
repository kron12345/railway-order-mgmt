package com.ordermgmt.railway.dto.pathmanager;

import java.util.List;
import java.util.UUID;

/** Full detail of a reference train including versions and recent process steps. */
public record TrainDetailDto(
        UUID id,
        String operationalTrainNumber,
        String trainType,
        String trafficTypeCode,
        String calendarStart,
        String calendarEnd,
        String calendarBitmap,
        Integer trainWeight,
        Integer trainLength,
        Integer trainMaxSpeed,
        String brakeType,
        UUID sourcePositionId,
        String processState,
        List<TrainVersionDto> versions,
        List<ProcessStepDto> recentSteps) {}
