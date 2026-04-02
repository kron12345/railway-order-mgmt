package com.ordermgmt.railway.api.pathmanager;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
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

/** REST endpoints for managing reference trains and their journey locations. */
@RestController
@RequestMapping("/api/v1/pathmanager")
@Tag(name = "Path Manager")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PathManagerController {

    private final PathManagerService pathManagerService;
    private final IdentifierGenerator identifierGenerator;
    private final PmReferenceTrainRepository referenceTrainRepository;
    private final PmTrainVersionRepository trainVersionRepository;
    private final PmJourneyLocationRepository journeyLocationRepository;
    private final PmProcessStepRepository processStepRepository;
    private final PmTimetableYearRepository timetableYearRepository;

    @PostMapping("/trains")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    @Operation(summary = "Submit a new train from order management")
    @ApiResponse(responseCode = "201", description = "Train created successfully")
    public TrainDetailDto submitTrain(@RequestBody TrainSubmitRequest request) {
        validateSubmitRequest(request);
        PmReferenceTrain train = createTrainFromRequest(request);
        return loadTrainDetail(train.getId());
    }

    @GetMapping("/trains")
    @Operation(summary = "List all reference trains, optionally filtered by timetable year")
    @ApiResponse(responseCode = "200", description = "List of train summaries")
    public List<TrainSummaryDto> listTrains(@RequestParam(required = false) Integer year) {
        List<PmReferenceTrain> trains;
        if (year != null) {
            trains = pathManagerService.findByYear(year);
        } else {
            trains = referenceTrainRepository.findAll();
        }
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
        pathManagerService.findById(trainId); // validate existence
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
                trainId,
                request.operationalTrainNumber(),
                request.trainType(),
                request.trafficTypeCode(),
                null,
                null,
                null,
                null);
        return ResponseEntity.ok(loadTrainDetail(trainId));
    }

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
                request.activityCodes() != null ? String.join(",", request.activityCodes()) : null,
                request.journeyLocationType(),
                request.subsidiaryCode());
        PmJourneyLocation updated =
                journeyLocationRepository
                        .findById(locationId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Journey location not found: " + locationId));
        return ResponseEntity.ok(PathManagerDtoMapper.toLocationDto(updated));
    }

    // ── Exception handling ─────────────────────────────────────────────

    @org.springframework.web.bind.annotation.ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException ex) {
        String message = ex.getMessage();
        if (message != null && message.contains("not found")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(message);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(message);
    }

    // ── Private helpers ────────────────────────────────────────────────

    private void validateSubmitRequest(TrainSubmitRequest request) {
        if (request.operationalTrainNumber() == null
                || request.operationalTrainNumber().isBlank()) {
            throw new IllegalArgumentException("Operational train number is required");
        }
        if (request.operationalTrainNumber().length() > 20) {
            throw new IllegalArgumentException("Operational train number too long (max 20)");
        }
    }

    private PmReferenceTrain createTrainFromRequest(TrainSubmitRequest request) {
        PmReferenceTrain train = new PmReferenceTrain();
        train.setOperationalTrainNumber(request.operationalTrainNumber());
        train.setTrainType(request.trainType());
        train.setTrafficTypeCode(request.trafficTypeCode());
        train.setSourcePositionId(request.sourcePositionId());
        train.setProcessState(PathProcessState.NEW);

        if (request.calendarStart() != null) {
            train.setCalendarStart(LocalDate.parse(request.calendarStart()));
        }
        if (request.calendarEnd() != null) {
            train.setCalendarEnd(LocalDate.parse(request.calendarEnd()));
        }
        train.setCalendarBitmap(request.calendarBitmap());

        // Resolve timetable year from calendar start
        int year =
                train.getCalendarStart() != null
                        ? train.getCalendarStart().getYear()
                        : LocalDate.now().getYear();
        PmTimetableYear ttYear =
                timetableYearRepository.findByYear(year).orElseGet(() -> createTimetableYear(year));
        train.setTimetableYear(ttYear);
        train.setTridTimetableYear(year);
        train.setTridCompany(identifierGenerator.company());
        train.setTridCore(identifierGenerator.generateCore());
        train.setTridVariant(identifierGenerator.initialVariant());

        train = referenceTrainRepository.save(train);

        if (request.locations() != null && !request.locations().isEmpty()) {
            createInitialVersionFromDto(train, request.locations());
        }

        return train;
    }

    private PmTimetableYear createTimetableYear(int year) {
        PmTimetableYear ttYear = new PmTimetableYear();
        ttYear.setYear(year);
        ttYear.setLabel("Fahrplanjahr " + year);
        ttYear.setStartDate(LocalDate.of(year - 1, 12, 14));
        ttYear.setEndDate(LocalDate.of(year, 12, 12));
        return timetableYearRepository.save(ttYear);
    }

    private void createInitialVersionFromDto(
            PmReferenceTrain train, List<JourneyLocationDto> locations) {
        PmTrainVersion version = new PmTrainVersion();
        version.setReferenceTrain(train);
        version.setVersionNumber(1);
        version.setVersionType(VersionType.INITIAL);
        version.setLabel("Initial v1");
        version.setOperationalTrainNumber(train.getOperationalTrainNumber());
        version.setTrainType(train.getTrainType());
        version.setTrafficTypeCode(train.getTrafficTypeCode());
        version.setCalendarStart(train.getCalendarStart());
        version.setCalendarEnd(train.getCalendarEnd());
        version.setCalendarBitmap(train.getCalendarBitmap());

        for (JourneyLocationDto dto : locations) {
            PmJourneyLocation loc = new PmJourneyLocation();
            loc.setTrainVersion(version);
            loc.setSequence(dto.sequence());
            loc.setCountryCodeIso(dto.countryCodeIso());
            loc.setLocationPrimaryCode(dto.locationPrimaryCode());
            loc.setPrimaryLocationName(dto.primaryLocationName());
            loc.setUopid(dto.uopid());
            loc.setJourneyLocationType(dto.journeyLocationType());
            loc.setArrivalTime(dto.arrivalTime());
            loc.setDepartureTime(dto.departureTime());
            loc.setDwellTime(dto.dwellTime());
            loc.setArrivalQualifier(dto.arrivalQualifier());
            loc.setDepartureQualifier(dto.departureQualifier());
            loc.setSubsidiaryCode(dto.subsidiaryCode());
            version.getJourneyLocations().add(loc);
        }

        trainVersionRepository.save(version);
    }

    private TrainDetailDto loadTrainDetail(UUID trainId) {
        PmReferenceTrain train = pathManagerService.findById(trainId);
        var versions = trainVersionRepository.findWithLocationsByReferenceTrainId(trainId);
        var steps = processStepRepository.findByReferenceTrainIdOrderByCreatedAtDesc(trainId);
        return PathManagerDtoMapper.toDetail(train, versions, steps);
    }

    private PmTrainVersion validateVersionBelongsToTrain(UUID trainId, UUID versionId) {
        pathManagerService.findById(trainId);
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
}
