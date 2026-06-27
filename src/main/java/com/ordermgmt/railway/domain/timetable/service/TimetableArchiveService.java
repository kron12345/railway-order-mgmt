package com.ordermgmt.railway.domain.timetable.service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
import com.ordermgmt.railway.domain.pathmanager.service.PathManagerService;
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
    private static final String TIMETABLE_POSITION_TYPE = "FAHRPLAN";
    private static final int MAX_POSITION_NAME_LENGTH = 255;
    private static final int MAX_COMMENT_LENGTH = 2000;
    private static final int MAX_OPERATIONAL_TRAIN_NUMBER_LENGTH = 20;
    private static final LocalTime DEFAULT_END_TIME = LocalTime.of(23, 59);

    private final TimetableArchiveRepository timetableArchiveRepository;
    private final OrderPositionRepository orderPositionRepository;
    private final PathManagerService pathManagerService;

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

    /**
     * Clones the source position's timetable archive onto the target (a freshly created
     * expression), linking the copy via the target's CAPACITY resource need so the builder opens
     * pre-filled. No-op when the source has no archive (e.g. a seeded position never built in the
     * editor).
     */
    public void cloneArchiveTo(UUID sourceId, UUID targetId) {
        OrderPosition source = orderPositionRepository.findById(sourceId).orElseThrow();
        Optional<TimetableArchive> sourceArchive = findArchive(source);
        if (sourceArchive.isEmpty()) {
            return;
        }
        TimetableArchive src = sourceArchive.get();
        TimetableArchive copy = new TimetableArchive();
        copy.setTimetableType(src.getTimetableType());
        copy.setOperationalTrainNumber(src.getOperationalTrainNumber());
        copy.setRouteSummary(src.getRouteSummary());
        copy.setTableData(src.getTableData());
        copy = timetableArchiveRepository.save(copy);

        OrderPosition target = orderPositionRepository.findById(targetId).orElseThrow();
        ensureCapacityResourceNeed(target).setLinkedFahrplanId(copy.getId());
        orderPositionRepository.save(target);
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
        validateTimetablePositionInput(name, comment, rows, operationalTrainNumber);

        List<TimetableRowData> preparedRows = prepareRows(rows);
        validateRows(preparedRows);

        TimetableArchive archive =
                saveArchive(existingPosition, preparedRows, operationalTrainNumber);

        OrderPosition position =
                savePosition(
                        order,
                        existingPosition,
                        name,
                        tags,
                        comment,
                        validityDates,
                        preparedRows,
                        operationalTrainNumber,
                        archive);

        pathManagerService.syncFromTimetable(position, archive, preparedRows);
        return orderPositionRepository.save(position);
    }

    private void validateTimetablePositionInput(
            String name,
            String comment,
            List<TimetableRowData> rows,
            String operationalTrainNumber) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Position name is required");
        }
        if (name.length() > MAX_POSITION_NAME_LENGTH) {
            throw new IllegalArgumentException("Position name too long (max 255)");
        }
        if (comment != null && comment.length() > MAX_COMMENT_LENGTH) {
            throw new IllegalArgumentException("Comment too long (max 2000)");
        }
        if (operationalTrainNumber != null
                && operationalTrainNumber.length() > MAX_OPERATIONAL_TRAIN_NUMBER_LENGTH) {
            throw new IllegalArgumentException("OTN too long (max 20)");
        }
        if (rows == null || rows.isEmpty()) {
            throw new IllegalArgumentException("Timetable rows are required.");
        }
    }

    private TimetableArchive saveArchive(
            OrderPosition existingPosition,
            List<TimetableRowData> preparedRows,
            String operationalTrainNumber) {
        TimetableArchive archive =
                findArchiveForExistingPosition(existingPosition).orElseGet(TimetableArchive::new);
        archive.setTimetableType(TIMETABLE_POSITION_TYPE);
        archive.setOperationalTrainNumber(blankToNull(operationalTrainNumber));
        archive.setRouteSummary(routeSummary(preparedRows));
        archive.setTableData(writeRows(preparedRows));
        return timetableArchiveRepository.save(archive);
    }

    private OrderPosition savePosition(
            Order order,
            OrderPosition existingPosition,
            String name,
            String tags,
            String comment,
            List<LocalDate> validityDates,
            List<TimetableRowData> preparedRows,
            String operationalTrainNumber,
            TimetableArchive archive) {
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

            validateTimeMode(
                    row.getArrivalMode(),
                    row.getArrivalExact(),
                    row.getArrivalEarliest(),
                    row.getArrivalLatest(),
                    row.getCommercialArrival());
            validateTimeMode(
                    row.getDepartureMode(),
                    row.getDepartureExact(),
                    row.getDepartureEarliest(),
                    row.getDepartureLatest(),
                    row.getCommercialDeparture());

            validateRequiredRouteTimes(row, index == 0, index == rows.size() - 1);
            validateHalt(row, index == 0, index == rows.size() - 1);
        }
    }

    private void validateRequiredRouteTimes(
            TimetableRowData row, boolean isOrigin, boolean isDestination) {
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
    }

    private void validateHalt(TimetableRowData row, boolean isOrigin, boolean isDestination) {
        if (!Boolean.TRUE.equals(row.getHalt())) {
            return;
        }
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

    private void validateTimeMode(
            TimeConstraintMode mode,
            String exact,
            String earliest,
            String latest,
            String commercial) {
        if (mode == null || mode == TimeConstraintMode.NONE) {
            return;
        }
        switch (mode) {
            case EXACT -> {
                if (parseTime(exact) == null) {
                    throw new IllegalArgumentException("Exact timetable time is missing.");
                }
            }
            case WINDOW -> {
                LocalTime earliestTime = parseTime(earliest);
                LocalTime latestTime = parseTime(latest);
                if (earliestTime == null
                        || latestTime == null
                        || latestTime.isBefore(earliestTime)) {
                    throw new IllegalArgumentException("Invalid timetable time window.");
                }
            }
            case AFTER -> {
                if (parseTime(earliest) == null) {
                    throw new IllegalArgumentException("Earliest timetable time is missing.");
                }
            }
            case BEFORE -> {
                if (parseTime(latest) == null) {
                    throw new IllegalArgumentException("Latest timetable time is missing.");
                }
            }
            case COMMERCIAL -> {
                if (parseTime(commercial) == null) {
                    throw new IllegalArgumentException("Commercial timetable time is missing.");
                }
            }
            case NONE -> {
                // handled above
            }
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
        LocalDate date = firstValidityDate(validityDates);
        LocalTime time = firstMeaningfulTime(rows.getFirst(), false);
        return date.atTime(time != null ? time : LocalTime.MIDNIGHT);
    }

    private LocalDateTime resolvePositionEnd(
            List<LocalDate> validityDates, List<TimetableRowData> rows) {
        LocalDate date = lastValidityDate(validityDates);
        LocalTime time = firstMeaningfulTime(rows.getLast(), true);
        return date.atTime(time != null ? time : DEFAULT_END_TIME);
    }

    private LocalDate firstValidityDate(List<LocalDate> validityDates) {
        if (validityDates.isEmpty()) {
            return LocalDate.now();
        }
        return validityDates.stream().sorted().findFirst().orElse(LocalDate.now());
    }

    private LocalDate lastValidityDate(List<LocalDate> validityDates) {
        if (validityDates.isEmpty()) {
            return LocalDate.now();
        }
        return validityDates.stream()
                .sorted()
                .reduce((first, second) -> second)
                .orElse(LocalDate.now());
    }

    private LocalTime firstMeaningfulTime(TimetableRowData row, boolean arrival) {
        if (arrival) {
            return firstMeaningfulArrivalTime(row);
        }
        return firstMeaningfulDepartureTime(row);
    }

    private LocalTime firstMeaningfulArrivalTime(TimetableRowData row) {
        return switch (modeOrNone(row.getArrivalMode())) {
            case EXACT -> parseTime(row.getArrivalExact());
            case WINDOW, BEFORE -> parseTime(row.getArrivalLatest());
            case AFTER -> parseTime(row.getArrivalEarliest());
            case COMMERCIAL -> parseTime(row.getCommercialArrival());
            case NONE -> parseTime(row.getEstimatedArrival());
        };
    }

    private LocalTime firstMeaningfulDepartureTime(TimetableRowData row) {
        return switch (modeOrNone(row.getDepartureMode())) {
            case EXACT -> parseTime(row.getDepartureExact());
            case WINDOW, AFTER -> parseTime(row.getDepartureEarliest());
            case BEFORE -> parseTime(row.getDepartureLatest());
            case COMMERCIAL -> parseTime(row.getCommercialDeparture());
            case NONE -> parseTime(row.getEstimatedDeparture());
        };
    }

    private TimeConstraintMode modeOrNone(TimeConstraintMode mode) {
        return mode == null ? TimeConstraintMode.NONE : mode;
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
