package com.ordermgmt.railway.api.pathmanager;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ordermgmt.railway.domain.pathmanager.model.DiffResult;
import com.ordermgmt.railway.domain.pathmanager.model.PmJourneyLocation;
import com.ordermgmt.railway.domain.pathmanager.model.PmTrainVersion;
import com.ordermgmt.railway.domain.pathmanager.repository.PmTrainVersionRepository;
import com.ordermgmt.railway.domain.pathmanager.service.DiffService;
import com.ordermgmt.railway.domain.timetable.model.TimetableRowData;
import com.ordermgmt.railway.dto.pathmanager.DiffResultDto;
import com.ordermgmt.railway.dto.pathmanager.JourneyLocationDto;
import com.ordermgmt.railway.mapper.pathmanager.PathManagerDtoMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** REST endpoint for comparing order-side timetable data with PM journey locations. */
@RestController
@RequestMapping("/api/v1/pathmanager/diff")
@Tag(name = "Timetable Diff")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PathManagerDiffController {

    private static final String ERROR_KEY = "error";
    private static final String RESOURCE_NOT_FOUND_ERROR = "Resource not found";
    private static final String INVALID_REQUEST_ERROR = "Invalid request";

    private final DiffService diffService;
    private final PmTrainVersionRepository trainVersionRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Compare order-side locations with the latest PM train version")
    @ApiResponse(responseCode = "201", description = "Diff computed successfully")
    @ApiResponse(responseCode = "404", description = "Reference train not found or has no versions")
    public DiffResultDto computeDiff(
            @RequestBody List<JourneyLocationDto> orderLocations,
            @RequestParam UUID referenceTrainId) {

        PmTrainVersion latestVersion =
                trainVersionRepository
                        .findFirstByReferenceTrainIdOrderByVersionNumberDesc(referenceTrainId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "No versions found for reference train: "
                                                        + referenceTrainId));

        List<TimetableRowData> orderSideRows =
                orderLocations.stream().map(PathManagerDiffController::toTimetableRowData).toList();

        List<PmJourneyLocation> pathManagerLocations = latestVersion.getJourneyLocations();

        DiffResult diffResult = diffService.diff(orderSideRows, pathManagerLocations);
        return PathManagerDtoMapper.toDiffResultDto(diffResult);
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

    private static TimetableRowData toTimetableRowData(JourneyLocationDto location) {
        TimetableRowData row = new TimetableRowData();
        row.setSequence(location.sequence());
        row.setUopid(location.uopid());
        row.setName(location.primaryLocationName());
        row.setCountry(location.countryCodeIso());
        row.setJourneyLocationType(location.journeyLocationType());
        row.setEstimatedArrival(location.arrivalTime());
        row.setEstimatedDeparture(location.departureTime());
        row.setDwellMinutes(location.dwellTime());
        return row;
    }

    private Map<String, String> errorBody(String message) {
        return Map.of(ERROR_KEY, message);
    }
}
