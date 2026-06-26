package com.ordermgmt.railway.domain.timetable.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.ordermgmt.railway.domain.timetable.model.JourneyLocationType;
import com.ordermgmt.railway.domain.timetable.model.RoutePointRole;
import com.ordermgmt.railway.domain.timetable.model.TimetableRowData;
import com.ordermgmt.railway.domain.timetable.model.TttJourneyLocationDraft;
import com.ordermgmt.railway.domain.timetable.model.TttPathRequestDraft;
import com.ordermgmt.railway.domain.timetable.model.TttTiming;

/** Builds the TTT-facing draft data stream from order-management timetable rows. */
@Service
public class TttDraftBuilder {

    public TttPathRequestDraft fromRows(List<TimetableRowData> rows) {
        if (rows == null || rows.isEmpty()) {
            return new TttPathRequestDraft(List.of(), null);
        }
        List<TttJourneyLocationDraft> locations = new ArrayList<>();
        Integer referenceSequence = null;
        for (TimetableRowData row : rows) {
            boolean exported = isExportedToTtt(row);
            if (exported && referenceSequence == null) {
                referenceSequence = row.getSequence();
            }
            locations.add(toLocation(row, exported));
        }
        return new TttPathRequestDraft(locations, referenceSequence);
    }

    public TttJourneyLocationDraft toLocation(TimetableRowData row) {
        return toLocation(row, isExportedToTtt(row));
    }

    private TttJourneyLocationDraft toLocation(TimetableRowData row, boolean exported) {
        return new TttJourneyLocationDraft(
                row.getSequence(),
                row.getUopid(),
                row.getCountry(),
                row.getName(),
                JourneyLocationType.fromString(row.getJourneyLocationType()).code(),
                timings(row),
                userEnteredDwell(row) ? row.getDwellMinutes() : null,
                activities(row),
                blankToNull(row.getAssociatedTrainOtn()),
                blankToNull(row.getLocationSubsidiaryCode()),
                networkSpecificParameters(row.getNetworkSpecificParametersText()),
                exported);
    }

    private List<TttTiming> timings(TimetableRowData row) {
        List<TttTiming> timings = new ArrayList<>();
        int arrivalOffset = row.getArrivalOffset() == null ? 0 : row.getArrivalOffset();
        int departureOffset = row.getDepartureOffset() == null ? 0 : row.getDepartureOffset();

        addIfUserEntered(
                timings,
                row.getUserEnteredCommercialArrival(),
                "PLA",
                row.getCommercialArrival(),
                arrivalOffset);
        addIfUserEntered(
                timings,
                row.getUserEnteredArrivalEarliest(),
                "ELA",
                row.getArrivalEarliest(),
                arrivalOffset);
        addIfUserEntered(
                timings,
                row.getUserEnteredArrivalExact(),
                "ALA",
                row.getArrivalExact(),
                arrivalOffset);
        addIfUserEntered(
                timings,
                row.getUserEnteredArrivalLatest(),
                "LLA",
                row.getArrivalLatest(),
                arrivalOffset);
        addIfUserEntered(
                timings,
                row.getUserEnteredCommercialDeparture(),
                "PLD",
                row.getCommercialDeparture(),
                departureOffset);
        addIfUserEntered(
                timings,
                row.getUserEnteredDepartureEarliest(),
                "ELD",
                row.getDepartureEarliest(),
                departureOffset);
        addIfUserEntered(
                timings,
                row.getUserEnteredDepartureExact(),
                "ALD",
                row.getDepartureExact(),
                departureOffset);
        addIfUserEntered(
                timings,
                row.getUserEnteredDepartureLatest(),
                "LLD",
                row.getDepartureLatest(),
                departureOffset);
        return List.copyOf(timings);
    }

    private void addIfUserEntered(
            List<TttTiming> timings,
            Boolean userEntered,
            String qualifier,
            String time,
            int offset) {
        if (Boolean.TRUE.equals(userEntered) && time != null && !time.isBlank()) {
            timings.add(new TttTiming(qualifier, time, offset));
        }
    }

    private List<String> activities(TimetableRowData row) {
        if (row.getActivityCodes() != null && !row.getActivityCodes().isEmpty()) {
            return row.getActivityCodes().stream()
                    .map(this::blankToNull)
                    .filter(code -> code != null)
                    .distinct()
                    .collect(Collectors.toUnmodifiableList());
        }
        String activityCode = blankToNull(row.getActivityCode());
        return activityCode == null ? List.of() : List.of(activityCode);
    }

    private Map<String, Object> networkSpecificParameters(String text) {
        if (text == null || text.isBlank()) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (String line : text.split("\\R")) {
            if (line == null || line.isBlank()) {
                continue;
            }
            int separator = line.indexOf('=');
            if (separator <= 0) {
                result.put(line.trim(), true);
                continue;
            }
            String key = line.substring(0, separator).trim();
            String value = line.substring(separator + 1).trim();
            if (!key.isBlank()) {
                result.put(key, value);
            }
        }
        return Map.copyOf(result);
    }

    private boolean userEnteredDwell(TimetableRowData row) {
        return Boolean.TRUE.equals(row.getUserEnteredDwell()) && row.getDwellMinutes() != null;
    }

    /**
     * Single source of truth for "does this row export to TTT": a halt, a user-marked TTT-relevant
     * row, the route origin/destination, or a manually added point. Used both by the actual export
     * (here) and the UI's exported-indicator (TimetableEditingService delegates to this) so they
     * can never diverge.
     */
    public static boolean isExportedToTtt(TimetableRowData row) {
        if (row == null) return false;
        if (Boolean.TRUE.equals(row.getHalt())) return true;
        if (Boolean.TRUE.equals(row.getTttRelevant())) return true;
        if (row.getRoutePointRole() == RoutePointRole.ORIGIN
                || row.getRoutePointRole() == RoutePointRole.DESTINATION) return true;
        return Boolean.TRUE.equals(row.getManuallyAdded());
    }

    public Map<Integer, TttJourneyLocationDraft> indexBySequence(TttPathRequestDraft draft) {
        Map<Integer, TttJourneyLocationDraft> result = new LinkedHashMap<>();
        for (TttJourneyLocationDraft location : draft.journeyLocations()) {
            result.put(location.sequence(), location);
        }
        return result;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
