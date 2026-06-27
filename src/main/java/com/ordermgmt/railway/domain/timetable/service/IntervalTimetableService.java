package com.ordermgmt.railway.domain.timetable.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ordermgmt.railway.domain.order.model.Order;
import com.ordermgmt.railway.domain.order.model.OrderPosition;
import com.ordermgmt.railway.domain.timetable.model.IntervalGenerationCommand;
import com.ordermgmt.railway.domain.timetable.model.TimetableRowData;

/**
 * Generates multiple timetable positions at fixed intervals (Taktfahrplan).
 *
 * <p>Creates one {@link OrderPosition} with full {@code TimetableArchive} per departure, shifting
 * all times from the base route by the delta from the first departure.
 */
@Service
public class IntervalTimetableService {

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");
    private static final int MINUTES_PER_DAY = 24 * 60;

    private final TimetableArchiveService archiveService;

    public IntervalTimetableService(TimetableArchiveService archiveService) {
        this.archiveService = archiveService;
    }

    /**
     * Generates interval-based timetable positions (Taktfahrplan) from a base route.
     *
     * <p>Creates one {@link OrderPosition} per departure slot. All times in the base route are
     * shifted by the delta between the base route's first departure and the target departure time.
     *
     * @param cmd the generation command containing all parameters
     * @return list of created order positions
     */
    @Transactional
    public List<OrderPosition> generateIntervalPositions(IntervalGenerationCommand cmd) {

        List<LocalTime> departures =
                calculateDepartureTimes(
                        cmd.firstDeparture(),
                        cmd.lastDeparture(),
                        cmd.crossesMidnight(),
                        cmd.intervalMinutes());

        // Delta base: use the first row's departure so shifts are relative to the actual route data
        LocalTime baseDeparture = parseFirstRowDeparture(cmd.baseRows(), cmd.firstDeparture());

        List<OrderPosition> positions = new ArrayList<>();
        for (int i = 0; i < departures.size(); i++) {
            LocalTime departure = departures.get(i);
            positions.add(
                    createShiftedPosition(
                            cmd.order(),
                            cmd.namePrefix(),
                            cmd.otnStart(),
                            cmd.baseRows(),
                            baseDeparture,
                            departure,
                            cmd.crossesMidnight(),
                            cmd.validityDates(),
                            cmd.tags(),
                            cmd.comment(),
                            i));
        }
        return positions;
    }

    /**
     * Creates a single shifted position for a specific departure time.
     *
     * @return the persisted order position
     */
    private OrderPosition createShiftedPosition(
            Order order,
            String namePrefix,
            String otnStart,
            List<TimetableRowData> baseRows,
            LocalTime baseDeparture,
            LocalTime departure,
            boolean crossesMidnight,
            List<LocalDate> validityDates,
            String tags,
            String comment,
            int index) {

        long delta =
                minutesBetween(
                        baseDeparture,
                        departure,
                        crossesMidnight && departure.isBefore(baseDeparture));

        List<TimetableRowData> shiftedRows = cloneAndShiftRows(baseRows, delta);
        String positionName = namePrefix + " " + departure.format(HH_MM);
        String operationalTrainNumber = incrementOtn(otnStart, index);

        return archiveService.saveTimetablePosition(
                order,
                null,
                positionName,
                tags,
                comment,
                validityDates,
                shiftedRows,
                operationalTrainNumber);
    }

    /**
     * Extracts the estimated departure time from the first row of the base route. Falls back to the
     * provided default if the first row has no departure set.
     */
    private LocalTime parseFirstRowDeparture(List<TimetableRowData> baseRows, LocalTime fallback) {
        if (baseRows == null || baseRows.isEmpty()) {
            return fallback;
        }
        String firstDep = baseRows.getFirst().getEstimatedDeparture();
        if (firstDep == null || firstDep.isBlank()) {
            return fallback;
        }
        try {
            return LocalTime.parse(firstDep, HH_MM);
        } catch (Exception e) {
            return fallback;
        }
    }

    /** Calculates all departure times from first to last at the given interval. */
    public List<LocalTime> calculateDepartureTimes(
            LocalTime first, LocalTime last, boolean crossesMidnight, int intervalMinutes) {

        List<LocalTime> times = new ArrayList<>();
        int firstMinute = minutesSinceMidnight(first);
        int lastMinute = minutesSinceMidnight(last);

        if (crossesMidnight && lastMinute <= firstMinute) {
            lastMinute += MINUTES_PER_DAY;
        }

        for (int minute = firstMinute; minute <= lastMinute; minute += intervalMinutes) {
            int normalizedMinute = minute % MINUTES_PER_DAY;
            times.add(LocalTime.of(normalizedMinute / 60, normalizedMinute % 60));
        }
        return times;
    }

    private List<TimetableRowData> cloneAndShiftRows(
            List<TimetableRowData> baseRows, long deltaMinutes) {
        List<TimetableRowData> shiftedRows = new ArrayList<>();
        for (TimetableRowData src : baseRows) {
            TimetableRowData copy = new TimetableRowData();
            copyRouteData(src, copy);
            copyPlanningData(src, copy);
            copyShiftedTimes(src, copy, deltaMinutes);
            shiftedRows.add(copy);
        }
        return shiftedRows;
    }

    private void copyRouteData(TimetableRowData source, TimetableRowData target) {
        target.setSequence(source.getSequence());
        target.setUopid(source.getUopid());
        target.setName(source.getName());
        target.setCountry(source.getCountry());
        target.setRoutePointRole(source.getRoutePointRole());
        target.setJourneyLocationType(source.getJourneyLocationType());
        target.setFromName(source.getFromName());
        target.setToName(source.getToName());
        target.setSegmentLengthMeters(source.getSegmentLengthMeters());
        target.setDistanceFromStartMeters(source.getDistanceFromStartMeters());
    }

    private void copyPlanningData(TimetableRowData source, TimetableRowData target) {
        target.setHalt(source.getHalt());
        target.setTttRelevant(source.getTttRelevant());
        target.setActivityCode(source.getActivityCode());
        target.setDwellMinutes(source.getDwellMinutes());
        target.setArrivalMode(source.getArrivalMode());
        target.setDepartureMode(source.getDepartureMode());
        target.setPinned(source.getPinned());
        target.setManuallyAdded(source.getManuallyAdded());
        target.setDeleted(source.getDeleted());
    }

    private void copyShiftedTimes(
            TimetableRowData source, TimetableRowData target, long deltaMinutes) {
        target.setEstimatedArrival(shiftTime(source.getEstimatedArrival(), deltaMinutes));
        target.setEstimatedDeparture(shiftTime(source.getEstimatedDeparture(), deltaMinutes));
        target.setArrivalExact(shiftTime(source.getArrivalExact(), deltaMinutes));
        target.setDepartureExact(shiftTime(source.getDepartureExact(), deltaMinutes));
        target.setArrivalEarliest(shiftTime(source.getArrivalEarliest(), deltaMinutes));
        target.setArrivalLatest(shiftTime(source.getArrivalLatest(), deltaMinutes));
        target.setDepartureEarliest(shiftTime(source.getDepartureEarliest(), deltaMinutes));
        target.setDepartureLatest(shiftTime(source.getDepartureLatest(), deltaMinutes));
        target.setCommercialArrival(shiftTime(source.getCommercialArrival(), deltaMinutes));
        target.setCommercialDeparture(shiftTime(source.getCommercialDeparture(), deltaMinutes));
    }

    private String shiftTime(String timeStr, long deltaMinutes) {
        if (timeStr == null || timeStr.isBlank()) {
            return timeStr;
        }
        try {
            LocalTime t = LocalTime.parse(timeStr, HH_MM);
            return t.plusMinutes(deltaMinutes).format(HH_MM);
        } catch (Exception e) {
            return timeStr;
        }
    }

    private long minutesBetween(LocalTime from, LocalTime to, boolean nextDay) {
        int fromMin = minutesSinceMidnight(from);
        int toMin = minutesSinceMidnight(to);
        if (nextDay) {
            toMin += MINUTES_PER_DAY;
        }
        return toMin - fromMin;
    }

    private int minutesSinceMidnight(LocalTime time) {
        return time.getHour() * 60 + time.getMinute();
    }

    /** If base is numeric, returns base+offset as string. Otherwise returns base unchanged. */
    String incrementOtn(String base, int offset) {
        if (base == null || base.isBlank()) {
            return null;
        }
        try {
            int num = Integer.parseInt(base.trim());
            return String.valueOf(num + offset);
        } catch (NumberFormatException e) {
            return base;
        }
    }
}
