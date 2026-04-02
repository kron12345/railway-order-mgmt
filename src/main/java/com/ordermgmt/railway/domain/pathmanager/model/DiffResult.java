package com.ordermgmt.railway.domain.pathmanager.model;

import java.util.List;

import com.ordermgmt.railway.domain.timetable.model.TimetableRowData;

/** Result of comparing order-side timetable data with PM journey locations. */
public record DiffResult(
        List<TimetableRowData> added,
        List<PmJourneyLocation> removed,
        List<ChangedLocation> changed) {

    /** A single location where order-side and PM-side data differ. */
    public record ChangedLocation(
            TimetableRowData orderSide, PmJourneyLocation pmSide, List<String> differences) {}

    public boolean hasChanges() {
        return !added.isEmpty() || !removed.isEmpty() || !changed.isEmpty();
    }
}
