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
     * @param order the parent order
     * @param namePrefix prefix for generated position names (departure time is appended)
     * @param otnStart starting operational train number (incremented per position)
     * @param baseRows the template route rows to shift
     * @param firstDeparture first interval departure time
     * @param lastDeparture last interval departure time
     * @param crossesMidnight whether the interval window crosses midnight
     * @param intervalMinutes minutes between consecutive departures
     * @param validityDates calendar dates on which the positions are valid
     * @param tags comma-separated tags for the positions
     * @param comment free-text comment
     * @return list of created order positions
     */
    @Transactional
    public List<OrderPosition> generateIntervalPositions(
            Order order,
            String namePrefix,
            String otnStart,
            List<TimetableRowData> baseRows,
            LocalTime firstDeparture,
            LocalTime lastDeparture,
            boolean crossesMidnight,
            int intervalMinutes,
            List<LocalDate> validityDates,
            String tags,
            String comment) {

        List<LocalTime> departures =
                calculateDepartureTimes(
                        firstDeparture, lastDeparture, crossesMidnight, intervalMinutes);

        // Delta base: use the first row's departure so shifts are relative to the actual route data
        LocalTime baseDeparture = parseFirstRowDeparture(baseRows, firstDeparture);

        List<OrderPosition> positions = new ArrayList<>();
        for (int i = 0; i < departures.size(); i++) {
            LocalTime dep = departures.get(i);
            positions.add(
                    createShiftedPosition(
                            order,
                            namePrefix,
                            otnStart,
                            baseRows,
                            baseDeparture,
                            dep,
                            crossesMidnight,
                            validityDates,
                            tags,
                            comment,
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

        List<TimetableRowData> shifted = cloneAndShiftRows(baseRows, delta);
        String posName = namePrefix + " " + departure.format(HH_MM);
        String otn = incrementOtn(otnStart, index);

        return archiveService.saveTimetablePosition(
                order, null, posName, tags, comment, validityDates, shifted, otn);
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
        int firstMin = first.getHour() * 60 + first.getMinute();
        int lastMin = last.getHour() * 60 + last.getMinute();

        if (crossesMidnight && lastMin <= firstMin) {
            lastMin += 1440; // next day
        }

        for (int min = firstMin; min <= lastMin; min += intervalMinutes) {
            int normalized = min % 1440;
            times.add(LocalTime.of(normalized / 60, normalized % 60));
        }
        return times;
    }

    private List<TimetableRowData> cloneAndShiftRows(
            List<TimetableRowData> base, long deltaMinutes) {
        List<TimetableRowData> result = new ArrayList<>();
        for (TimetableRowData src : base) {
            TimetableRowData copy = new TimetableRowData();
            copy.setSequence(src.getSequence());
            copy.setUopid(src.getUopid());
            copy.setName(src.getName());
            copy.setCountry(src.getCountry());
            copy.setRoutePointRole(src.getRoutePointRole());
            copy.setJourneyLocationType(src.getJourneyLocationType());
            copy.setFromName(src.getFromName());
            copy.setToName(src.getToName());
            copy.setSegmentLengthMeters(src.getSegmentLengthMeters());
            copy.setDistanceFromStartMeters(src.getDistanceFromStartMeters());
            copy.setHalt(src.getHalt());
            copy.setTttRelevant(src.getTttRelevant());
            copy.setActivityCode(src.getActivityCode());
            copy.setDwellMinutes(src.getDwellMinutes());
            copy.setArrivalMode(src.getArrivalMode());
            copy.setDepartureMode(src.getDepartureMode());
            copy.setPinned(src.getPinned());
            copy.setManuallyAdded(src.getManuallyAdded());
            copy.setDeleted(src.getDeleted());

            // Shift all time fields
            copy.setEstimatedArrival(shiftTime(src.getEstimatedArrival(), deltaMinutes));
            copy.setEstimatedDeparture(shiftTime(src.getEstimatedDeparture(), deltaMinutes));
            copy.setArrivalExact(shiftTime(src.getArrivalExact(), deltaMinutes));
            copy.setDepartureExact(shiftTime(src.getDepartureExact(), deltaMinutes));
            copy.setArrivalEarliest(shiftTime(src.getArrivalEarliest(), deltaMinutes));
            copy.setArrivalLatest(shiftTime(src.getArrivalLatest(), deltaMinutes));
            copy.setDepartureEarliest(shiftTime(src.getDepartureEarliest(), deltaMinutes));
            copy.setDepartureLatest(shiftTime(src.getDepartureLatest(), deltaMinutes));
            copy.setCommercialArrival(shiftTime(src.getCommercialArrival(), deltaMinutes));
            copy.setCommercialDeparture(shiftTime(src.getCommercialDeparture(), deltaMinutes));

            result.add(copy);
        }
        return result;
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
        int fromMin = from.getHour() * 60 + from.getMinute();
        int toMin = to.getHour() * 60 + to.getMinute();
        if (nextDay) {
            toMin += 1440;
        }
        return toMin - fromMin;
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
