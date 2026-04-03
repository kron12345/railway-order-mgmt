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

    private PathManagerDtoMapper() {}

    // ── Reference Train → Summary ──────────────────────────────────────

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

    // ── Reference Train → Detail ───────────────────────────────────────

    public static TrainDetailDto toDetail(
            PmReferenceTrain train, List<PmTrainVersion> versions, List<PmProcessStep> steps) {
        return new TrainDetailDto(
                train.getId(),
                train.getOperationalTrainNumber(),
                train.getTrainType(),
                train.getTrafficTypeCode(),
                train.getCalendarStart() != null ? train.getCalendarStart().toString() : null,
                train.getCalendarEnd() != null ? train.getCalendarEnd().toString() : null,
                train.getCalendarBitmap(),
                train.getTrainWeight(),
                train.getTrainLength(),
                train.getTrainMaxSpeed(),
                train.getBrakeType(),
                train.getSourcePositionId(),
                train.getProcessState().name(),
                versions.stream().map(PathManagerDtoMapper::toVersion).toList(),
                steps.stream().map(PathManagerDtoMapper::toStepDto).toList());
    }

    // ── Train Version → DTO ────────────────────────────────────────────

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

    // ── Journey Location → DTO ─────────────────────────────────────────

    public static JourneyLocationDto toLocationDto(PmJourneyLocation loc) {
        List<String> activityCodes = parseActivityCodes(loc.getActivities());
        return new JourneyLocationDto(
                loc.getSequence(),
                loc.getCountryCodeIso(),
                loc.getLocationPrimaryCode(),
                loc.getPrimaryLocationName(),
                loc.getUopid(),
                loc.getJourneyLocationType(),
                loc.getArrivalTime(),
                loc.getDepartureTime(),
                loc.getDwellTime(),
                loc.getArrivalQualifier(),
                loc.getDepartureQualifier(),
                loc.getSubsidiaryCode(),
                activityCodes,
                loc.getAssociatedTrainOtn());
    }

    // ── Process Step → DTO ─────────────────────────────────────────────

    public static ProcessStepDto toStepDto(PmProcessStep step) {
        return new ProcessStepDto(
                step.getId(),
                step.getStepType(),
                step.getFromState(),
                step.getToState(),
                step.getTypeOfInformation(),
                step.getComment(),
                step.getSimulatedBy(),
                step.getCreatedAt() != null ? step.getCreatedAt().toString() : null);
    }

    // ── DiffResult → DTO ───────────────────────────────────────────────

    public static DiffResultDto toDiffResultDto(DiffResult result) {
        List<DiffResultDto.DiffEntryDto> entries = new ArrayList<>();

        for (TimetableRowData added : result.added()) {
            entries.add(
                    new DiffResultDto.DiffEntryDto(
                            added.getUopid(), added.getName(), "ADDED", Map.of()));
        }

        for (PmJourneyLocation removed : result.removed()) {
            entries.add(
                    new DiffResultDto.DiffEntryDto(
                            removed.getUopid(),
                            removed.getPrimaryLocationName(),
                            "REMOVED",
                            Map.of()));
        }

        for (DiffResult.ChangedLocation changed : result.changed()) {
            Map<String, String[]> fieldDiffs =
                    buildFieldDiffs(changed.orderSide(), changed.pmSide(), changed.differences());
            entries.add(
                    new DiffResultDto.DiffEntryDto(
                            changed.pmSide().getUopid(),
                            changed.pmSide().getPrimaryLocationName(),
                            "CHANGED",
                            fieldDiffs));
        }

        return new DiffResultDto(entries);
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private static String buildRouteSummary(PmReferenceTrain train) {
        List<PmTrainVersion> versions = train.getTrainVersions();
        if (versions == null || versions.isEmpty()) {
            return "";
        }
        PmTrainVersion latest = versions.get(versions.size() - 1);
        List<PmJourneyLocation> locations = latest.getJourneyLocations();
        if (locations == null || locations.isEmpty()) {
            return "";
        }
        String first = locations.get(0).getPrimaryLocationName();
        String last = locations.get(locations.size() - 1).getPrimaryLocationName();
        if (Objects.equals(first, last)) {
            return first != null ? first : "";
        }
        return (first != null ? first : "?") + " \u2192 " + (last != null ? last : "?");
    }

    private static List<String> parseActivityCodes(String activitiesJson) {
        if (activitiesJson == null || activitiesJson.isBlank()) {
            return Collections.emptyList();
        }
        // Simple JSON array parse: ["0001","0002"] -> List
        String stripped = activitiesJson.replaceAll("[\\[\\]\"\\s]", "");
        if (stripped.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(stripped.split(","));
    }

    private static Map<String, String[]> buildFieldDiffs(
            TimetableRowData order, PmJourneyLocation pm, List<String> differences) {
        Map<String, String[]> diffs = new LinkedHashMap<>();
        for (String field : differences) {
            String[] values = diffValuesForField(field, order, pm);
            diffs.put(field, values);
        }
        return diffs;
    }

    private static String[] diffValuesForField(
            String field, TimetableRowData order, PmJourneyLocation pm) {
        return switch (field) {
            case "name" ->
                    new String[] {
                        order.getName() != null ? order.getName() : "",
                        pm.getPrimaryLocationName() != null ? pm.getPrimaryLocationName() : ""
                    };
            case "arrivalTime" ->
                    new String[] {
                        order.getEstimatedArrival() != null ? order.getEstimatedArrival() : "",
                        pm.getArrivalTime() != null ? pm.getArrivalTime() : ""
                    };
            case "departureTime" ->
                    new String[] {
                        order.getEstimatedDeparture() != null ? order.getEstimatedDeparture() : "",
                        pm.getDepartureTime() != null ? pm.getDepartureTime() : ""
                    };
            case "dwellTime" ->
                    new String[] {
                        order.getDwellMinutes() != null ? order.getDwellMinutes().toString() : "",
                        pm.getDwellTime() != null ? pm.getDwellTime().toString() : ""
                    };
            default -> new String[] {"", ""};
        };
    }
}
