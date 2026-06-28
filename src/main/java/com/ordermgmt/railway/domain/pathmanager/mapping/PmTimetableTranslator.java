package com.ordermgmt.railway.domain.pathmanager.mapping;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.ordermgmt.railway.domain.pathmanager.model.PmJourneyLocation;
import com.ordermgmt.railway.domain.pathmanager.model.PmTrainVersion;
import com.ordermgmt.railway.domain.timetable.model.JourneyLocationType;
import com.ordermgmt.railway.domain.timetable.model.TimeConstraintMode;
import com.ordermgmt.railway.domain.timetable.model.TimetableRowData;
import com.ordermgmt.railway.domain.timetable.model.TttJourneyLocationDraft;
import com.ordermgmt.railway.domain.timetable.service.TttDraftBuilder;

/**
 * Pure translation between {@link TimetableRowData} and the Path-Manager
 * journey-location/route-point structures: rows ⇄ {@link PmJourneyLocation}, route-point JSON, and
 * the time/qualifier/activity resolution that maps each timetable constraint mode onto the single
 * value RailOpt expects. Stateless; extracted from {@code PathManagerService} so both the capture
 * and sync flows share one mapping.
 */
public final class PmTimetableTranslator {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private PmTimetableTranslator() {}

    // ── PmJourneyLocation → TimetableRowData (capture flow) ────────────────

    /**
     * Maps a train version's journey locations into timetable rows for archiving a captured train.
     */
    public static List<TimetableRowData> toTimetableRows(List<PmJourneyLocation> locations) {
        List<TimetableRowData> rows = new ArrayList<>();
        for (int i = 0; i < locations.size(); i++) {
            rows.add(toTimetableRow(locations.get(i), i));
        }
        return rows;
    }

    private static TimetableRowData toTimetableRow(PmJourneyLocation location, int index) {
        TimetableRowData row = new TimetableRowData();
        row.setSequence(location.getSequence() != null ? location.getSequence() : index + 1);
        row.setUopid(location.getUopid());
        row.setName(location.getPrimaryLocationName());
        row.setCountry(location.getCountryCodeIso());
        if (location.getJourneyLocationType() != null) {
            row.setJourneyLocationType(location.getJourneyLocationType());
        }
        row.setEstimatedArrival(location.getArrivalTime());
        row.setEstimatedDeparture(location.getDepartureTime());
        row.setCommercialArrival(location.getArrivalTime());
        row.setCommercialDeparture(location.getDepartureTime());
        return row;
    }

    /** Serializes timetable rows to JSON for {@code TimetableArchive.tableData}. */
    public static String writeRows(List<TimetableRowData> rows) {
        try {
            return OBJECT_MAPPER.writeValueAsString(rows);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize captured timetable rows", e);
        }
    }

    // ── TimetableRowData → PmJourneyLocation / route points ────────────────

    /**
     * Builds the journey locations for a train version from timetable rows, attaching each row's
     * TTT draft (indexed by sequence) so the persisted location carries its TTT payload.
     */
    public static List<PmJourneyLocation> buildJourneyLocations(
            List<TimetableRowData> rows, PmTrainVersion version, TttDraftBuilder tttDraftBuilder) {
        Map<Integer, TttJourneyLocationDraft> draftBySequence =
                tttDraftBuilder.indexBySequence(tttDraftBuilder.fromRows(rows));
        List<PmJourneyLocation> locations = new ArrayList<>();
        for (TimetableRowData row : rows) {
            locations.add(
                    mapRowToJourneyLocation(row, version, draftBySequence.get(row.getSequence())));
        }
        return locations;
    }

    /** Serializes the route points (sequence/uopid/name) for {@code PmRoute.routePoints}. */
    public static String routePointsToJson(List<TimetableRowData> rows) {
        try {
            return OBJECT_MAPPER.writeValueAsString(toRoutePoints(rows));
        } catch (Exception e) {
            return "[]";
        }
    }

    private static List<Map<String, Object>> toRoutePoints(List<TimetableRowData> rows) {
        List<Map<String, Object>> routePoints = new ArrayList<>();
        for (TimetableRowData row : rows) {
            routePoints.add(toRoutePoint(row));
        }
        return routePoints;
    }

    private static Map<String, Object> toRoutePoint(TimetableRowData row) {
        Map<String, Object> routePoint = new LinkedHashMap<>();
        routePoint.put("sequence", row.getSequence());
        routePoint.put("uopid", row.getUopid() != null ? row.getUopid() : "");
        routePoint.put("name", row.getName() != null ? row.getName() : "");
        return routePoint;
    }

    private static PmJourneyLocation mapRowToJourneyLocation(
            TimetableRowData row, PmTrainVersion version, TttJourneyLocationDraft draft) {
        PmJourneyLocation location = new PmJourneyLocation();
        location.setTrainVersion(version);
        location.setSequence(row.getSequence());
        location.setPrimaryLocationName(row.getName());
        location.setUopid(row.getUopid());
        location.setCountryCodeIso(row.getCountry());
        location.setJourneyLocationType(
                JourneyLocationType.fromString(row.getJourneyLocationType()).code());

        location.setArrivalTime(resolveTime(row, true));
        location.setDepartureTime(resolveTime(row, false));
        location.setArrivalOffset(row.getArrivalOffset() == null ? 0 : row.getArrivalOffset());
        location.setDepartureOffset(
                row.getDepartureOffset() == null ? 0 : row.getDepartureOffset());
        location.setDwellTime(row.getDwellMinutes());
        location.setArrivalQualifier(toTttQualifier(row.getArrivalMode(), true));
        location.setDepartureQualifier(toTttQualifier(row.getDepartureMode(), false));
        location.setActivities(toActivitiesJson(row));
        location.setAssociatedTrainOtn(row.getAssociatedTrainOtn());
        location.setTttPayload(writeTttPayload(draft));
        return location;
    }

    private static String toActivitiesJson(TimetableRowData row) {
        List<String> activityCodes = activityCodes(row);
        if (activityCodes.isEmpty()) {
            return null;
        }
        return activityCodes.stream()
                .map(code -> "\"" + code + "\"")
                .collect(Collectors.joining(",", "[", "]"));
    }

    private static List<String> activityCodes(TimetableRowData row) {
        if (row.getActivityCodes() != null && !row.getActivityCodes().isEmpty()) {
            return row.getActivityCodes();
        }
        if (row.getActivityCode() != null && !row.getActivityCode().isBlank()) {
            return List.of(row.getActivityCode());
        }
        return List.of();
    }

    private static String writeTttPayload(TttJourneyLocationDraft draft) {
        if (draft == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(draft);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not serialize TTT draft payload.", exception);
        }
    }

    private static String resolveTime(TimetableRowData row, boolean arrival) {
        if (arrival) {
            return resolveArrivalTime(row);
        }
        return resolveDepartureTime(row);
    }

    private static String resolveArrivalTime(TimetableRowData row) {
        TimeConstraintMode mode =
                row.getArrivalMode() == null ? TimeConstraintMode.NONE : row.getArrivalMode();
        return resolveTime(
                mode,
                row.getArrivalExact(),
                row.getArrivalEarliest(),
                row.getArrivalLatest(),
                row.getCommercialArrival(),
                row.getEstimatedArrival());
    }

    private static String resolveDepartureTime(TimetableRowData row) {
        TimeConstraintMode mode =
                row.getDepartureMode() == null ? TimeConstraintMode.NONE : row.getDepartureMode();
        return resolveTime(
                mode,
                row.getDepartureExact(),
                row.getDepartureEarliest(),
                row.getDepartureLatest(),
                row.getCommercialDeparture(),
                row.getEstimatedDeparture());
    }

    private static String resolveTime(
            TimeConstraintMode mode,
            String exact,
            String earliest,
            String latest,
            String commercial,
            String estimated) {
        return switch (mode) {
            case EXACT -> exact;
            case WINDOW, AFTER -> earliest;
            case BEFORE -> latest;
            case COMMERCIAL -> commercial;
            case NONE -> estimated;
        };
    }

    private static String toTttQualifier(TimeConstraintMode mode, boolean arrival) {
        if (mode == null || mode == TimeConstraintMode.NONE) {
            return null;
        }
        return switch (mode) {
            case EXACT -> arrival ? "ALA" : "ALD";
            case WINDOW, AFTER -> arrival ? "ELA" : "ELD";
            case BEFORE -> arrival ? "LLA" : "LLD";
            case COMMERCIAL -> arrival ? "PLA" : "PLD";
            case NONE -> null;
        };
    }
}
