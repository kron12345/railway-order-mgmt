package com.ordermgmt.railway.mapper.pathmanager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.ordermgmt.railway.domain.pathmanager.model.DiffResult;
import com.ordermgmt.railway.domain.pathmanager.model.PmJourneyLocation;
import com.ordermgmt.railway.domain.pathmanager.model.PmProcessStep;
import com.ordermgmt.railway.domain.pathmanager.model.PmReferenceTrain;
import com.ordermgmt.railway.domain.pathmanager.model.PmTrainVersion;
import com.ordermgmt.railway.domain.timetable.model.TimetableRowData;
import com.ordermgmt.railway.dto.pathmanager.DiffResultDto;
import com.ordermgmt.railway.dto.pathmanager.JourneyLocationDto;
import com.ordermgmt.railway.dto.pathmanager.ProcessStepDto;
import com.ordermgmt.railway.dto.pathmanager.TrainDetailDto;
import com.ordermgmt.railway.dto.pathmanager.TrainSummaryDto;
import com.ordermgmt.railway.dto.pathmanager.TrainVersionDto;

/** Manual mapping between Path Manager entities and DTOs. */
public final class PathManagerDtoMapper {

    private static final String EMPTY_VALUE = "";
    private static final String UNKNOWN_ROUTE_LOCATION = "?";
    private static final String ROUTE_SEPARATOR = " \u2192 ";
    private static final String CHANGE_TYPE_ADDED = "ADDED";
    private static final String CHANGE_TYPE_REMOVED = "REMOVED";
    private static final String CHANGE_TYPE_CHANGED = "CHANGED";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_ARRIVAL_TIME = "arrivalTime";
    private static final String FIELD_DEPARTURE_TIME = "departureTime";
    private static final String FIELD_DWELL_TIME = "dwellTime";
    private static final String ACTIVITY_JSON_DELIMITERS = "[\\[\\]\"\\s]";
    private static final String ACTIVITY_SEPARATOR = ",";

    private PathManagerDtoMapper() {}

    public static TrainSummaryDto toSummary(PmReferenceTrain train) {
        int versionCount = train.getTrainVersions() != null ? train.getTrainVersions().size() : 0;
        String routeSummary = buildRouteSummary(train);
        return new TrainSummaryDto(
                train.getId(),
                train.getOperationalTrainNumber(),
                train.getTrainType(),
                train.getTrafficTypeCode(),
                train.getProcessState().name(),
                versionCount,
                routeSummary);
    }

    public static TrainDetailDto toDetail(
            PmReferenceTrain train,
            List<PmTrainVersion> trainVersions,
            List<PmProcessStep> processSteps) {
        return new TrainDetailDto(
                train.getId(),
                train.getOperationalTrainNumber(),
                train.getTrainType(),
                train.getTrafficTypeCode(),
                toNullableString(train.getCalendarStart()),
                toNullableString(train.getCalendarEnd()),
                train.getCalendarBitmap(),
                train.getTrainWeight(),
                train.getTrainLength(),
                train.getTrainMaxSpeed(),
                train.getBrakeType(),
                train.getSourcePositionId(),
                train.getProcessState().name(),
                trainVersions.stream().map(PathManagerDtoMapper::toVersion).toList(),
                processSteps.stream().map(PathManagerDtoMapper::toStepDto).toList());
    }

    public static TrainVersionDto toVersion(PmTrainVersion version) {
        List<JourneyLocationDto> locations =
                version.getJourneyLocations() != null
                        ? version.getJourneyLocations().stream()
                                .map(PathManagerDtoMapper::toLocationDto)
                                .toList()
                        : Collections.emptyList();
        return new TrainVersionDto(
                version.getId(),
                version.getVersionNumber(),
                version.getVersionType().name(),
                version.getLabel(),
                version.getOperationalTrainNumber(),
                locations);
    }

    public static JourneyLocationDto toLocationDto(PmJourneyLocation location) {
        List<String> activityCodes = parseActivityCodes(location.getActivities());
        return new JourneyLocationDto(
                location.getSequence(),
                location.getCountryCodeIso(),
                location.getLocationPrimaryCode(),
                location.getPrimaryLocationName(),
                location.getUopid(),
                location.getJourneyLocationType(),
                location.getArrivalTime(),
                location.getDepartureTime(),
                location.getDwellTime(),
                location.getArrivalQualifier(),
                location.getDepartureQualifier(),
                location.getSubsidiaryCode(),
                activityCodes,
                location.getAssociatedTrainOtn());
    }

    public static ProcessStepDto toStepDto(PmProcessStep step) {
        return new ProcessStepDto(
                step.getId(),
                step.getStepType(),
                step.getFromState(),
                step.getToState(),
                step.getTypeOfInformation(),
                step.getComment(),
                step.getSimulatedBy(),
                toNullableString(step.getCreatedAt()));
    }

    public static DiffResultDto toDiffResultDto(DiffResult diffResult) {
        List<DiffResultDto.DiffEntryDto> entries = new ArrayList<>();

        for (TimetableRowData added : diffResult.added()) {
            entries.add(
                    new DiffResultDto.DiffEntryDto(
                            added.getUopid(), added.getName(), CHANGE_TYPE_ADDED, Map.of()));
        }

        for (PmJourneyLocation removed : diffResult.removed()) {
            entries.add(
                    new DiffResultDto.DiffEntryDto(
                            removed.getUopid(),
                            removed.getPrimaryLocationName(),
                            CHANGE_TYPE_REMOVED,
                            Map.of()));
        }

        for (DiffResult.ChangedLocation changed : diffResult.changed()) {
            Map<String, String[]> fieldDiffs =
                    buildFieldDiffs(changed.orderSide(), changed.pmSide(), changed.differences());
            entries.add(
                    new DiffResultDto.DiffEntryDto(
                            changed.pmSide().getUopid(),
                            changed.pmSide().getPrimaryLocationName(),
                            CHANGE_TYPE_CHANGED,
                            fieldDiffs));
        }

        return new DiffResultDto(entries);
    }

    private static String buildRouteSummary(PmReferenceTrain train) {
        List<PmTrainVersion> versions = train.getTrainVersions();
        if (versions == null || versions.isEmpty()) {
            return EMPTY_VALUE;
        }
        PmTrainVersion latestVersion = versions.get(versions.size() - 1);
        List<PmJourneyLocation> locations = latestVersion.getJourneyLocations();
        if (locations == null || locations.isEmpty()) {
            return EMPTY_VALUE;
        }
        String first = locations.get(0).getPrimaryLocationName();
        String last = locations.get(locations.size() - 1).getPrimaryLocationName();
        if (Objects.equals(first, last)) {
            return first != null ? first : EMPTY_VALUE;
        }
        return routeLocationName(first) + ROUTE_SEPARATOR + routeLocationName(last);
    }

    private static List<String> parseActivityCodes(String activitiesJson) {
        if (activitiesJson == null || activitiesJson.isBlank()) {
            return Collections.emptyList();
        }
        String stripped = activitiesJson.replaceAll(ACTIVITY_JSON_DELIMITERS, EMPTY_VALUE);
        if (stripped.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(stripped.split(ACTIVITY_SEPARATOR));
    }

    private static Map<String, String[]> buildFieldDiffs(
            TimetableRowData orderSide, PmJourneyLocation pathManagerSide, List<String> differences) {
        Map<String, String[]> fieldDiffs = new LinkedHashMap<>();
        for (String field : differences) {
            fieldDiffs.put(field, diffValuesForField(field, orderSide, pathManagerSide));
        }
        return fieldDiffs;
    }

    private static String[] diffValuesForField(
            String field, TimetableRowData orderSide, PmJourneyLocation pathManagerSide) {
        return switch (field) {
            case FIELD_NAME ->
                    diffValues(orderSide.getName(), pathManagerSide.getPrimaryLocationName());
            case FIELD_ARRIVAL_TIME ->
                    diffValues(orderSide.getEstimatedArrival(), pathManagerSide.getArrivalTime());
            case FIELD_DEPARTURE_TIME ->
                    diffValues(
                            orderSide.getEstimatedDeparture(),
                            pathManagerSide.getDepartureTime());
            case FIELD_DWELL_TIME ->
                    diffValues(orderSide.getDwellMinutes(), pathManagerSide.getDwellTime());
            default -> diffValues(EMPTY_VALUE, EMPTY_VALUE);
        };
    }

    private static String[] diffValues(Object orderSideValue, Object pathManagerSideValue) {
        return new String[] {
            toDisplayString(orderSideValue), toDisplayString(pathManagerSideValue)
        };
    }

    private static String toDisplayString(Object value) {
        return value != null ? value.toString() : EMPTY_VALUE;
    }

    private static String toNullableString(Object value) {
        return value != null ? value.toString() : null;
    }

    private static String routeLocationName(String locationName) {
        return locationName != null ? locationName : UNKNOWN_ROUTE_LOCATION;
    }
}
