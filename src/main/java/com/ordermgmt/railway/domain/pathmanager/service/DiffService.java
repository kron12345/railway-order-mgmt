package com.ordermgmt.railway.domain.pathmanager.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.ordermgmt.railway.domain.pathmanager.model.DiffResult;
import com.ordermgmt.railway.domain.pathmanager.model.PmJourneyLocation;
import com.ordermgmt.railway.domain.timetable.model.TimetableRowData;

/** Compares order-side timetable data with PM-side journey locations. */
@Service
public class DiffService {

    /**
     * Computes the diff between order-side rows and PM-side journey locations.
     *
     * <p>Matching is done by UOPID (operational point identifier). Rows/locations present on only
     * one side are reported as added/removed. Rows present on both sides are compared field by
     * field.
     *
     * @param orderSide the timetable rows from the order/archive
     * @param pmSide the journey locations from the PM train version
     * @return a DiffResult with added, removed, and changed entries
     */
    public DiffResult diff(List<TimetableRowData> orderSide, List<PmJourneyLocation> pmSide) {
        Map<String, TimetableRowData> orderByUopid = indexOrderRows(orderSide);
        Map<String, PmJourneyLocation> pmByUopid = indexPmLocations(pmSide);

        List<TimetableRowData> added = new ArrayList<>();
        List<PmJourneyLocation> removed = new ArrayList<>();
        List<DiffResult.ChangedLocation> changed = new ArrayList<>();

        for (Map.Entry<String, TimetableRowData> entry : orderByUopid.entrySet()) {
            String uopid = entry.getKey();
            TimetableRowData orderRow = entry.getValue();
            PmJourneyLocation pmLocation = pmByUopid.remove(uopid);

            if (pmLocation == null) {
                added.add(orderRow);
            } else {
                List<String> differences = compareFields(orderRow, pmLocation);
                if (!differences.isEmpty()) {
                    changed.add(new DiffResult.ChangedLocation(orderRow, pmLocation, differences));
                }
            }
        }

        removed.addAll(pmByUopid.values());

        return new DiffResult(added, removed, changed);
    }

    private Map<String, TimetableRowData> indexOrderRows(List<TimetableRowData> rows) {
        Map<String, TimetableRowData> map = new LinkedHashMap<>();
        for (TimetableRowData row : rows) {
            String key = compositeKey(row.getUopid(), row.getSequence());
            map.put(key, row);
        }
        return map;
    }

    private Map<String, PmJourneyLocation> indexPmLocations(List<PmJourneyLocation> locations) {
        Map<String, PmJourneyLocation> map = new LinkedHashMap<>();
        for (PmJourneyLocation loc : locations) {
            String key = compositeKey(loc.getUopid(), loc.getSequence());
            map.put(key, loc);
        }
        return map;
    }

    private String compositeKey(String uopid, Integer sequence) {
        String base = uopid != null ? uopid : "noid";
        return base + ":" + (sequence != null ? sequence : 0);
    }

    private List<String> compareFields(TimetableRowData order, PmJourneyLocation pm) {
        List<String> differences = new ArrayList<>();

        if (!Objects.equals(order.getName(), pm.getPrimaryLocationName())) {
            differences.add("name");
        }
        if (!Objects.equals(order.getEstimatedArrival(), pm.getArrivalTime())) {
            differences.add("arrivalTime");
        }
        if (!Objects.equals(order.getEstimatedDeparture(), pm.getDepartureTime())) {
            differences.add("departureTime");
        }
        if (!Objects.equals(order.getDwellMinutes(), pm.getDwellTime())) {
            differences.add("dwellTime");
        }

        return differences;
    }
}
