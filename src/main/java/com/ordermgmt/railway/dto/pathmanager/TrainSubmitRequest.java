package com.ordermgmt.railway.dto.pathmanager;

import java.util.List;
import java.util.UUID;

/** Request body for submitting a new train from order management. */
public record TrainSubmitRequest(
        UUID sourcePositionId,
        String operationalTrainNumber,
        String trainType,
        String trafficTypeCode,
        List<JourneyLocationDto> locations,
        String calendarStart,
        String calendarEnd,
        String calendarBitmap) {}
