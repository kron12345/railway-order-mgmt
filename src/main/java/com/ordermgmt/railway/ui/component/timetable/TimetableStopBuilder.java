package com.ordermgmt.railway.ui.component.timetable;

import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.formatTime;

import java.time.Duration;
import java.util.List;

import com.ordermgmt.railway.domain.infrastructure.model.OperationalPoint;
import com.ordermgmt.railway.domain.timetable.model.RoutePointRole;
import com.ordermgmt.railway.domain.timetable.model.TimetableRowData;

/**
 * Builds a {@link TimetableRowData} from the quick-add form's collected times, writing the right
 * fields per {@code TimeConstraintMode} and deriving dwell from arrival→departure. Pure factory;
 * the table step inserts the result and lets the editing service propagate neighbours.
 */
final class TimetableStopBuilder {

    private TimetableStopBuilder() {}

    /** Build a manually-added VIA halt row from the form's collected times. */
    static TimetableRowData buildStopRow(
            OperationalPoint point, String activityCode, AddStopForm.StopTimes times) {
        TimetableRowData row = new TimetableRowData();
        row.setUopid(point.getUopid());
        row.setName(point.getName());
        row.setCountry(point.getCountry());
        row.setRoutePointRole(RoutePointRole.VIA);
        row.setJourneyLocationType("INTERMEDIATE");
        row.setHalt(true);
        row.setActivityCode(activityCode);
        row.setActivityCodes(List.of(activityCode));
        row.setManuallyAdded(true);
        applyArrival(row, times);
        applyDeparture(row, times);
        // dwell derived from arrival → departure of the new row, no hardcoded default
        var arrival = times.arrivalPrimary();
        var departure = times.departurePrimary();
        if (arrival != null && departure != null) {
            long dwell = Duration.between(arrival, departure).toMinutes();
            // handleAddStop already rejects departure-before-arrival; guard defensively so a stop
            // can never receive a negative dwell.
            if (dwell >= 0) {
                row.setDwellMinutes((int) dwell);
                row.setUserEnteredDwell(true);
            }
        }
        return row;
    }

    private static void applyArrival(TimetableRowData row, AddStopForm.StopTimes times) {
        row.setArrivalMode(times.arrivalMode());
        row.setEstimatedArrival(formatTime(times.arrivalPrimary()));
        switch (times.arrivalMode()) {
            case EXACT -> {
                row.setArrivalExact(formatTime(times.arrivalPrimary()));
                row.setUserEnteredArrivalExact(true);
            }
            case WINDOW -> {
                row.setArrivalEarliest(formatTime(times.arrivalPrimary()));
                row.setArrivalLatest(formatTime(times.arrivalSecondary()));
                row.setUserEnteredArrivalEarliest(true);
                row.setUserEnteredArrivalLatest(true);
            }
            case AFTER -> {
                row.setArrivalEarliest(formatTime(times.arrivalPrimary()));
                row.setUserEnteredArrivalEarliest(true);
            }
            case BEFORE -> {
                row.setArrivalLatest(formatTime(times.arrivalPrimary()));
                row.setUserEnteredArrivalLatest(true);
            }
            case COMMERCIAL -> {
                row.setCommercialArrival(formatTime(times.arrivalPrimary()));
                row.setUserEnteredCommercialArrival(true);
            }
            case NONE -> {
                /* estimated already set */
            }
        }
    }

    private static void applyDeparture(TimetableRowData row, AddStopForm.StopTimes times) {
        row.setDepartureMode(times.departureMode());
        row.setEstimatedDeparture(formatTime(times.departurePrimary()));
        switch (times.departureMode()) {
            case EXACT -> {
                row.setDepartureExact(formatTime(times.departurePrimary()));
                row.setUserEnteredDepartureExact(true);
            }
            case WINDOW -> {
                row.setDepartureEarliest(formatTime(times.departureSecondary()));
                row.setDepartureLatest(formatTime(times.departurePrimary()));
                row.setUserEnteredDepartureEarliest(true);
                row.setUserEnteredDepartureLatest(true);
            }
            case AFTER -> {
                row.setDepartureEarliest(formatTime(times.departurePrimary()));
                row.setUserEnteredDepartureEarliest(true);
            }
            case BEFORE -> {
                row.setDepartureLatest(formatTime(times.departurePrimary()));
                row.setUserEnteredDepartureLatest(true);
            }
            case COMMERCIAL -> {
                row.setCommercialDeparture(formatTime(times.departurePrimary()));
                row.setUserEnteredCommercialDeparture(true);
            }
            case NONE -> {
                /* estimated already set */
            }
        }
    }
}
