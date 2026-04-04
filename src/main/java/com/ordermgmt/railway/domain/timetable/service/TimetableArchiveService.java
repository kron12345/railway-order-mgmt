package com.ordermgmt.railway.domain.timetable.service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.ordermgmt.railway.domain.order.model.CoverageType;
import com.ordermgmt.railway.domain.order.model.Order;
import com.ordermgmt.railway.domain.order.model.OrderPosition;
import com.ordermgmt.railway.domain.order.model.PositionStatus;
import com.ordermgmt.railway.domain.order.model.PositionType;
import com.ordermgmt.railway.domain.order.model.ResourceNeed;
import com.ordermgmt.railway.domain.order.model.ResourceType;
import com.ordermgmt.railway.domain.order.model.ValidityJsonCodec;
import com.ordermgmt.railway.domain.order.repository.OrderPositionRepository;
import com.ordermgmt.railway.domain.timetable.model.TimeConstraintMode;
import com.ordermgmt.railway.domain.timetable.model.TimetableActivityCatalog;
import com.ordermgmt.railway.domain.timetable.model.TimetableActivityOption;
import com.ordermgmt.railway.domain.timetable.model.TimetableArchive;
import com.ordermgmt.railway.domain.timetable.model.TimetableRowData;
import com.ordermgmt.railway.domain.timetable.repository.TimetableArchiveRepository;

import lombok.RequiredArgsConstructor;

/** Parses archived timetable tables and persists timetable-backed order positions. */
@Service
@RequiredArgsConstructor
@Transactional
public class TimetableArchiveService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final TimetableArchiveRepository timetableArchiveRepository;
    private final OrderPositionRepository orderPositionRepository;

    @Transactional(readOnly = true)
    public Optional<TimetableArchive> findArchive(OrderPosition position) {
        return capacityNeed(position)
                .flatMap(resourceNeed -> Optional.ofNullable(resourceNeed.getLinkedFahrplanId()))
                .flatMap(timetableArchiveRepository::findById);
    }

    @Transactional(readOnly = true)
    public List<TimetableRowData> readRows(TimetableArchive archive) {
        if (archive == null || archive.getTableData() == null || archive.getTableData().isBlank()) {
            return List.of();
        }
        try {
            return List.of(
                    OBJECT_MAPPER.readValue(archive.getTableData(), TimetableRowData[].class));
        } catch (IOException exception) {
            throw new IllegalStateException("Could not parse archived timetable data.", exception);
        }
    }

    @Transactional(readOnly = true)
    public List<TimetableRowData> readRows(OrderPosition position) {
        return findArchive(position).map(this::readRows).orElseGet(List::of);
    }

    @Transactional(readOnly = true)
    public List<LocalDate> parseValidityDates(String validityJson) {
        return ValidityJsonCodec.fromJson(validityJson);
    }

    public String toValidityJson(List<LocalDate> dates) {
        return ValidityJsonCodec.toJson(dates);
    }

    public List<TimetableActivityOption> activityOptions() {
        return TimetableActivityCatalog.all();
    }

    public OrderPosition saveTimetablePosition(
            Order order,
            OrderPosition existingPosition,
            String name,
            String tags,
            String comment,
            List<LocalDate> validityDates,
            List<TimetableRowData> rows,
            String operationalTrainNumber) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Position name is required");
        }
        if (name.length() > 255) {
            throw new IllegalArgumentException("Position name too long (max 255)");
        }
        if (comment != null && comment.length() > 2000) {
            throw new IllegalArgumentException("Comment too long (max 2000)");
        }
        if (operationalTrainNumber != null && operationalTrainNumber.length() > 20) {
            throw new IllegalArgumentException("OTN too long (max 20)");
        }
        if (rows == null || rows.isEmpty()) {
            throw new IllegalArgumentException("Timetable rows are required.");
        }

        List<TimetableRowData> preparedRows = prepareRows(rows);
        validateRows(preparedRows);

        TimetableArchive archive =
                findArchiveForExistingPosition(existingPosition).orElseGet(TimetableArchive::new);
        archive.setTimetableType("FAHRPLAN");
        archive.setOperationalTrainNumber(blankToNull(operationalTrainNumber));
        archive.setRouteSummary(routeSummary(preparedRows));
        archive.setTableData(writeRows(preparedRows));
        archive = timetableArchiveRepository.save(archive);

        OrderPosition position = existingPosition != null ? existingPosition : new OrderPosition();
        position.setOrder(order);
        position.setType(PositionType.FAHRPLAN);
        position.setName(name.trim());
        position.setTags(blankToNull(tags));
        position.setComment(blankToNull(comment));
        position.setFromLocation(preparedRows.getFirst().getName());
        position.setToLocation(preparedRows.getLast().getName());
        position.setOperationalTrainNumber(blankToNull(operationalTrainNumber));
        position.setValidity(toValidityJson(validityDates));
        position.setStart(resolvePositionStart(validityDates, preparedRows));
        position.setEnd(resolvePositionEnd(validityDates, preparedRows));
        if (position.getInternalStatus() == null) {
            position.setInternalStatus(PositionStatus.IN_BEARBEITUNG);
        }

        ResourceNeed resourceNeed = ensureCapacityResourceNeed(position);
        resourceNeed.setLinkedFahrplanId(archive.getId());

        return orderPositionRepository.save(position);
    }

    private Optional<TimetableArchive> findArchiveForExistingPosition(
            OrderPosition existingPosition) {
        if (existingPosition == null) {
            return Optional.empty();
        }
        return findArchive(
                orderPositionRepository
                        .findById(existingPosition.getId())
                        .orElse(existingPosition));
    }

    private Optional<ResourceNeed> capacityNeed(OrderPosition position) {
        if (position == null) {
            return Optional.empty();
        }
        return position.getResourceNeeds().stream()
                .filter(resourceNeed -> resourceNeed.getResourceType() == ResourceType.CAPACITY)
                .findFirst();
    }

    private ResourceNeed ensureCapacityResourceNeed(OrderPosition position) {
        for (ResourceNeed resourceNeed : position.getResourceNeeds()) {
            if (resourceNeed.getResourceType() == ResourceType.CAPACITY) {
                return resourceNeed;
            }
        }

        ResourceNeed resourceNeed = new ResourceNeed();
        resourceNeed.setOrderPosition(position);
        resourceNeed.setResourceType(ResourceType.CAPACITY);
        // Capacity is ordered against infrastructure externally in the current process model.
        resourceNeed.setCoverageType(CoverageType.EXTERNAL);
        position.getResourceNeeds().add(resourceNeed);
        return resourceNeed;
    }

    private List<TimetableRowData> prepareRows(List<TimetableRowData> rows) {
        List<TimetableRowData> preparedRows = new ArrayList<>();
        for (int index = 0; index < rows.size(); index++) {
            TimetableRowData row = rows.get(index);
            row.setSequence(index + 1);
            row.setTttRelevant(isTttRelevant(row));
            preparedRows.add(row);
        }
        return preparedRows;
    }

    private void validateRows(List<TimetableRowData> rows) {
        for (int index = 0; index < rows.size(); index++) {
            TimetableRowData row = rows.get(index);
            boolean isOrigin = index == 0;
            boolean isDestination = index == rows.size() - 1;

            validateTimeMode(
                    row.getArrivalMode(),
                    row.getArrivalExact(),
                    row.getArrivalEarliest(),
                    row.getArrivalLatest());
            validateTimeMode(
                    row.getDepartureMode(),
                    row.getDepartureExact(),
                    row.getDepartureEarliest(),
                    row.getDepartureLatest());

            if (isOrigin
                    && row.getDepartureMode() == TimeConstraintMode.NONE
                    && blank(row.getEstimatedDeparture())) {
                throw new IllegalArgumentException("Origin requires a departure time.");
            }
            if (isDestination
                    && row.getArrivalMode() == TimeConstraintMode.NONE
                    && blank(row.getEstimatedArrival())) {
                throw new IllegalArgumentException("Destination requires an arrival time.");
            }

            if (Boolean.TRUE.equals(row.getHalt())) {
                if (!isOrigin
                        && row.getArrivalMode() == TimeConstraintMode.NONE
                        && blank(row.getEstimatedArrival())) {
                    throw new IllegalArgumentException("Halts require an arrival time.");
                }
                if (!isDestination
                        && row.getDepartureMode() == TimeConstraintMode.NONE
                        && blank(row.getEstimatedDeparture())) {
                    throw new IllegalArgumentException("Halts require a departure time.");
                }
                if (blank(row.getActivityCode())) {
                    throw new IllegalArgumentException("Halts require an activity code.");
                }
            }
        }
    }

    private void validateTimeMode(
            TimeConstraintMode mode, String exact, String earliest, String latest) {
        if (mode == null || mode == TimeConstraintMode.NONE) {
            return;
        }
        if (mode == TimeConstraintMode.EXACT) {
            if (parseTime(exact) == null) {
                throw new IllegalArgumentException("Exact timetable time is missing.");
            }
            return;
        }
        LocalTime earliestTime = parseTime(earliest);
        LocalTime latestTime = parseTime(latest);
        if (earliestTime == null || latestTime == null || latestTime.isBefore(earliestTime)) {
            throw new IllegalArgumentException("Invalid timetable time window.");
        }
    }

    private String writeRows(List<TimetableRowData> rows) {
        try {
            return OBJECT_MAPPER.writeValueAsString(rows);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not serialize timetable rows.", exception);
        }
    }

    private String routeSummary(List<TimetableRowData> rows) {
        return rows.getFirst().getName() + " \u2192 " + rows.getLast().getName();
    }

    private LocalDateTime resolvePositionStart(
            List<LocalDate> validityDates, List<TimetableRowData> rows) {
        LocalDate date =
                validityDates.isEmpty()
                        ? LocalDate.now()
                        : validityDates.stream().sorted().findFirst().orElse(LocalDate.now());
        LocalTime time = firstMeaningfulTime(rows.getFirst(), false);
        return date.atTime(time != null ? time : LocalTime.MIDNIGHT);
    }

    private LocalDateTime resolvePositionEnd(
            List<LocalDate> validityDates, List<TimetableRowData> rows) {
        LocalDate date =
                validityDates.isEmpty()
                        ? LocalDate.now()
                        : validityDates.stream()
                                .sorted()
                                .reduce((first, second) -> second)
                                .orElse(LocalDate.now());
        LocalTime time = firstMeaningfulTime(rows.getLast(), true);
        return date.atTime(time != null ? time : LocalTime.of(23, 59));
    }

    private LocalTime firstMeaningfulTime(TimetableRowData row, boolean arrival) {
        if (arrival) {
            if (row.getArrivalMode() == TimeConstraintMode.EXACT) {
                return parseTime(row.getArrivalExact());
            }
            if (row.getArrivalMode() == TimeConstraintMode.WINDOW) {
                return parseTime(row.getArrivalLatest());
            }
            return parseTime(row.getEstimatedArrival());
        }
        if (row.getDepartureMode() == TimeConstraintMode.EXACT) {
            return parseTime(row.getDepartureExact());
        }
        if (row.getDepartureMode() == TimeConstraintMode.WINDOW) {
            return parseTime(row.getDepartureEarliest());
        }
        return parseTime(row.getEstimatedDeparture());
    }

    private boolean isTttRelevant(TimetableRowData row) {
        return row.getArrivalMode() != null && row.getArrivalMode() != TimeConstraintMode.NONE
                || row.getDepartureMode() != null
                        && row.getDepartureMode() != TimeConstraintMode.NONE
                || !blank(row.getActivityCode())
                || Boolean.TRUE.equals(row.getHalt());
    }

    private LocalTime parseTime(String value) {
        if (blank(value)) {
            return null;
        }
        return LocalTime.parse(value, TIME_FORMAT);
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private String blankToNull(String value) {
        return blank(value) ? null : value.trim();
    }
}
