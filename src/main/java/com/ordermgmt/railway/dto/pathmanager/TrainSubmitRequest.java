package com.ordermgmt.railway.dto.pathmanager;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request body for submitting a new train from order management. */
public record TrainSubmitRequest(
        UUID sourcePositionId,
        @NotBlank(message = "Operational train number is required")
                @Size(max = 20, message = "Operational train number too long (max 20)")
                String operationalTrainNumber,
        String trainType,
        String trafficTypeCode,
        List<JourneyLocationDto> locations,
        String calendarStart,
        String calendarEnd,
        String calendarBitmap) {}
