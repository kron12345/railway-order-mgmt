package com.ordermgmt.railway.api.pathmanager;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ordermgmt.railway.domain.pathmanager.model.PathProcessState;
import com.ordermgmt.railway.domain.pathmanager.model.PmJourneyLocation;
import com.ordermgmt.railway.domain.pathmanager.model.PmReferenceTrain;
import com.ordermgmt.railway.domain.pathmanager.model.PmTimetableYear;
import com.ordermgmt.railway.domain.pathmanager.model.PmTrainVersion;
import com.ordermgmt.railway.domain.pathmanager.model.TrainHeaderUpdate;
import com.ordermgmt.railway.domain.pathmanager.model.VersionType;
import com.ordermgmt.railway.domain.pathmanager.repository.PmJourneyLocationRepository;
import com.ordermgmt.railway.domain.pathmanager.repository.PmProcessStepRepository;
import com.ordermgmt.railway.domain.pathmanager.repository.PmReferenceTrainRepository;
import com.ordermgmt.railway.domain.pathmanager.repository.PmTimetableYearRepository;
import com.ordermgmt.railway.domain.pathmanager.repository.PmTrainVersionRepository;
import com.ordermgmt.railway.domain.pathmanager.service.IdentifierGenerator;
import com.ordermgmt.railway.domain.pathmanager.service.PathManagerService;
import com.ordermgmt.railway.dto.pathmanager.JourneyLocationDto;
import com.ordermgmt.railway.dto.pathmanager.TrainDetailDto;
import com.ordermgmt.railway.dto.pathmanager.TrainSubmitRequest;
import com.ordermgmt.railway.dto.pathmanager.TrainSummaryDto;
import com.ordermgmt.railway.dto.pathmanager.TrainVersionDto;
import com.ordermgmt.railway.mapper.pathmanager.PathManagerDtoMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * REST API for the Path Manager domain.
 *
 * <p>Provides CRUD operations for reference trains, train versions, and journey locations. Trains
 * are submitted from order management and progress through the path-request lifecycle.
 *
 * <p>Ownership rules:
 *
 * <ul>
 *   <li>A {@code TrainVersion} always belongs to exactly one {@code ReferenceTrain}.
 *   <li>A {@code JourneyLocation} always belongs to exactly one {@code TrainVersion}.
 *   <li>All update/read endpoints validate the parent chain (train-&gt;version-&gt;location).
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/pathmanager")
@Tag(name = "Path Manager")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PathManagerController {

    private static final String ERROR_KEY = "error";
    private static final String RESOURCE_NOT_FOUND_ERROR = "Resource not found";
    private static final String INVALID_REQUEST_ERROR = "Invalid request";
    private static final String TIMETABLE_YEAR_LABEL_PREFIX = "Fahrplanjahr ";
    private static final int INITIAL_VERSION_NUMBER = 1;
    private static final String INITIAL_VERSION_LABEL = "Initial v1";
    private static final int TIMETABLE_YEAR_START_MONTH = 12;
    private static final int TIMETABLE_YEAR_START_DAY = 14;
    private static final int TIMETABLE_YEAR_END_MONTH = 12;
    private static final int TIMETABLE_YEAR_END_DAY = 12;

    private final PathManagerService pathManagerService;
    private final IdentifierGenerator identifierGenerator;
    private final PmReferenceTrainRepository referenceTrainRepository;
    private final PmTrainVersionRepository trainVersionRepository;
    private final PmJourneyLocationRepository journeyLocationRepository;
    private final PmProcessStepRepository processStepRepository;
    private final PmTimetableYearRepository timetableYearRepository;

    /**
     * Submits a new reference train from order management. Creates the train, an initial version
     * with journey locations (if provided), and resolves the timetable year from the calendar start
     * date.
     */
    @PostMapping("/trains")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    @Operation(summary = "Submit a new train from order management")
    @ApiResponse(responseCode = "201", description = "Train created successfully")
    public TrainDetailDto submitTrain(@Valid @RequestBody TrainSubmitRequest request) {
        PmReferenceTrain train = createTrainFromRequest(request);
        return loadTrainDetail(train.getId());
    }

    /** Lists all reference trains, optionally filtered by timetable year. */
    @GetMapping("/trains")
    @Operation(summary = "List all reference trains, optionally filtered by timetable year")
    @ApiResponse(responseCode = "200", description = "List of train summaries")
    public List<TrainSummaryDto> listTrains(@RequestParam(required = false) Integer year) {
        List<PmReferenceTrain> trains =
                year != null
                        ? pathManagerService.findByYear(year)
                        : referenceTrainRepository.findAll();
        return trains.stream().map(PathManagerDtoMapper::toSummary).toList();
    }

    @GetMapping("/trains/{trainId}")
    @Operation(summary = "Get full detail of a reference train")
    @ApiResponse(responseCode = "200", description = "Train detail")
    @ApiResponse(responseCode = "404", description = "Train not found")
    public ResponseEntity<TrainDetailDto> getTrain(@PathVariable UUID trainId) {
        return ResponseEntity.ok(loadTrainDetail(trainId));
    }

    @GetMapping("/trains/{trainId}/versions")
    @Operation(summary = "List all versions of a reference train")
    @ApiResponse(responseCode = "200", description = "List of train versions")
    public List<TrainVersionDto> listVersions(@PathVariable UUID trainId) {
        validateTrainExists(trainId);
        return trainVersionRepository.findWithLocationsByReferenceTrainId(trainId).stream()
                .map(PathManagerDtoMapper::toVersion)
                .toList();
    }

    @GetMapping("/trains/{trainId}/versions/{versionId}/locations")
    @Operation(summary = "Get journey locations for a specific train version")
    @ApiResponse(responseCode = "200", description = "List of journey locations")
    public List<JourneyLocationDto> listLocations(
            @PathVariable UUID trainId, @PathVariable UUID versionId) {
        PmTrainVersion version = validateVersionBelongsToTrain(trainId, versionId);
        return journeyLocationRepository
                .findByTrainVersionIdOrderBySequenceAsc(version.getId())
                .stream()
                .map(PathManagerDtoMapper::toLocationDto)
                .toList();
    }

    @PutMapping("/trains/{trainId}")
    @Transactional
    @Operation(summary = "Update train header fields")
    @ApiResponse(responseCode = "200", description = "Train updated")
    @ApiResponse(responseCode = "404", description = "Train not found")
    public ResponseEntity<TrainDetailDto> updateTrain(
            @PathVariable UUID trainId, @RequestBody TrainSubmitRequest request) {
        pathManagerService.updateTrainHeader(
                new TrainHeaderUpdate(
                        trainId,
                        request.operationalTrainNumber(),
                        request.trainType(),
                        request.trafficTypeCode(),
                        null,
                        null,
                        null,
                        null));
        return ResponseEntity.ok(loadTrainDetail(trainId));
    }

    /**
     * Updates a single journey location. Validates that the location belongs to the specified
     * version and that the version belongs to the specified train.
     */
    @PutMapping("/trains/{trainId}/versions/{versionId}/locations/{locationId}")
    @Transactional
    @Operation(summary = "Update a single journey location")
    @ApiResponse(responseCode = "200", description = "Location updated")
    @ApiResponse(responseCode = "404", description = "Location not found")
    public ResponseEntity<JourneyLocationDto> updateLocation(
            @PathVariable UUID trainId,
            @PathVariable UUID versionId,
            @PathVariable UUID locationId,
            @RequestBody JourneyLocationDto request) {
        validateVersionBelongsToTrain(trainId, versionId);
        validateLocationBelongsToVersion(locationId, versionId);
        pathManagerService.updateJourneyLocation(
                locationId,
                request.arrivalTime(),
                request.departureTime(),
                request.dwellTime(),
                request.arrivalQualifier(),
                request.departureQualifier(),
                joinActivityCodes(request.activityCodes()),
                request.journeyLocationType(),
                request.subsidiaryCode(),
                request.associatedTrainOtn());
        PmJourneyLocation updated =
                journeyLocationRepository
                        .findById(locationId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Journey location not found: " + locationId));
        return ResponseEntity.ok(PathManagerDtoMapper.toLocationDto(updated));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(errorBody(RESOURCE_NOT_FOUND_ERROR));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalStateException ex) {
        return ResponseEntity.badRequest().body(errorBody(INVALID_REQUEST_ERROR));
    }

    private PmReferenceTrain createTrainFromRequest(TrainSubmitRequest request) {
        PmReferenceTrain train = new PmReferenceTrain();
        applySubmittedHeader(train, request);
        applySubmittedCalendar(train, request);
        train.setProcessState(PathProcessState.NEW);
        assignTimetableYear(train);
        assignTrainIdentifier(train);

        train = referenceTrainRepository.save(train);

        if (request.locations() != null && !request.locations().isEmpty()) {
            createInitialVersionFromDto(train, request.locations());
        }

        return train;
    }

    private void applySubmittedHeader(PmReferenceTrain train, TrainSubmitRequest request) {
        train.setOperationalTrainNumber(request.operationalTrainNumber());
        train.setTrainType(request.trainType());
        train.setTrafficTypeCode(request.trafficTypeCode());
        train.setSourcePositionId(request.sourcePositionId());
    }

    private void applySubmittedCalendar(PmReferenceTrain train, TrainSubmitRequest request) {
        if (request.calendarStart() != null) {
            train.setCalendarStart(LocalDate.parse(request.calendarStart()));
        }
        if (request.calendarEnd() != null) {
            train.setCalendarEnd(LocalDate.parse(request.calendarEnd()));
        }
        train.setCalendarBitmap(request.calendarBitmap());
    }

    private void assignTimetableYear(PmReferenceTrain train) {
        int year = timetableYearFrom(train);
        PmTimetableYear timetableYear =
                timetableYearRepository.findByYear(year).orElseGet(() -> createTimetableYear(year));

        train.setTimetableYear(timetableYear);
        train.setTridTimetableYear(year);
    }

    private int timetableYearFrom(PmReferenceTrain train) {
        return train.getCalendarStart() != null
                ? train.getCalendarStart().getYear()
                : LocalDate.now().getYear();
    }

    private void assignTrainIdentifier(PmReferenceTrain train) {
        train.setTridCompany(identifierGenerator.company());
        train.setTridCore(identifierGenerator.generateCore());
        train.setTridVariant(identifierGenerator.initialVariant());
    }

    private PmTimetableYear createTimetableYear(int year) {
        PmTimetableYear timetableYear = new PmTimetableYear();
        timetableYear.setYear(year);
        timetableYear.setLabel(TIMETABLE_YEAR_LABEL_PREFIX + year);
        timetableYear.setStartDate(
                LocalDate.of(year - 1, TIMETABLE_YEAR_START_MONTH, TIMETABLE_YEAR_START_DAY));
        timetableYear.setEndDate(
                LocalDate.of(year, TIMETABLE_YEAR_END_MONTH, TIMETABLE_YEAR_END_DAY));
        return timetableYearRepository.save(timetableYear);
    }

    private void createInitialVersionFromDto(
            PmReferenceTrain train, List<JourneyLocationDto> locations) {
        PmTrainVersion version = new PmTrainVersion();
        version.setReferenceTrain(train);
        version.setVersionNumber(INITIAL_VERSION_NUMBER);
        version.setVersionType(VersionType.INITIAL);
        version.setLabel(INITIAL_VERSION_LABEL);
        version.setOperationalTrainNumber(train.getOperationalTrainNumber());
        version.setTrainType(train.getTrainType());
        version.setTrafficTypeCode(train.getTrafficTypeCode());
        version.setCalendarStart(train.getCalendarStart());
        version.setCalendarEnd(train.getCalendarEnd());
        version.setCalendarBitmap(train.getCalendarBitmap());

        for (JourneyLocationDto locationDto : locations) {
            version.getJourneyLocations().add(toJourneyLocation(version, locationDto));
        }

        trainVersionRepository.save(version);
    }

    private PmJourneyLocation toJourneyLocation(
            PmTrainVersion version, JourneyLocationDto locationDto) {
        PmJourneyLocation location = new PmJourneyLocation();
        location.setTrainVersion(version);
        location.setSequence(locationDto.sequence());
        location.setCountryCodeIso(locationDto.countryCodeIso());
        location.setLocationPrimaryCode(locationDto.locationPrimaryCode());
        location.setPrimaryLocationName(locationDto.primaryLocationName());
        location.setUopid(locationDto.uopid());
        location.setJourneyLocationType(locationDto.journeyLocationType());
        location.setArrivalTime(locationDto.arrivalTime());
        location.setDepartureTime(locationDto.departureTime());
        location.setDwellTime(locationDto.dwellTime());
        location.setArrivalQualifier(locationDto.arrivalQualifier());
        location.setDepartureQualifier(locationDto.departureQualifier());
        location.setSubsidiaryCode(locationDto.subsidiaryCode());
        location.setAssociatedTrainOtn(locationDto.associatedTrainOtn());
        location.setActivities(toActivityCodesJson(locationDto.activityCodes()));
        return location;
    }

    private String toActivityCodesJson(List<String> activityCodes) {
        if (activityCodes == null || activityCodes.isEmpty()) {
            return null;
        }
        return activityCodes.stream()
                .map(code -> "\"" + code + "\"")
                .collect(Collectors.joining(",", "[", "]"));
    }

    private TrainDetailDto loadTrainDetail(UUID trainId) {
        PmReferenceTrain train = pathManagerService.findById(trainId);
        var trainVersions = trainVersionRepository.findWithLocationsByReferenceTrainId(trainId);
        var processSteps = processStepRepository.findByReferenceTrainIdOrderByCreatedAtDesc(trainId);
        return PathManagerDtoMapper.toDetail(train, trainVersions, processSteps);
    }

    private void validateTrainExists(UUID trainId) {
        pathManagerService.findById(trainId);
    }

    private PmTrainVersion validateVersionBelongsToTrain(UUID trainId, UUID versionId) {
        validateTrainExists(trainId);
        PmTrainVersion version =
                trainVersionRepository
                        .findById(versionId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Train version not found: " + versionId));
        if (!version.getReferenceTrain().getId().equals(trainId)) {
            throw new IllegalArgumentException("Version does not belong to train");
        }
        return version;
    }

    private void validateLocationBelongsToVersion(UUID locationId, UUID versionId) {
        PmJourneyLocation location =
                journeyLocationRepository
                        .findById(locationId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Journey location not found: " + locationId));
        if (!location.getTrainVersion().getId().equals(versionId)) {
            throw new IllegalArgumentException("Location does not belong to version");
        }
    }

    private String joinActivityCodes(List<String> activityCodes) {
        return activityCodes != null ? String.join(",", activityCodes) : null;
    }

    private Map<String, String> errorBody(String message) {
        return Map.of(ERROR_KEY, message);
    }
}
