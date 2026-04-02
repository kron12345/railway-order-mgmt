package com.ordermgmt.railway.api.pathmanager;

import java.util.List;
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

        List<TimetableRowData> orderSide =
                orderLocations.stream().map(PathManagerDiffController::toTimetableRowData).toList();

        List<PmJourneyLocation> pmSide = latestVersion.getJourneyLocations();

        DiffResult result = diffService.diff(orderSide, pmSide);
        return PathManagerDtoMapper.toDiffResultDto(result);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleNotFound(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    // ── Private helpers ────────────────────────────────────────────────

    private static TimetableRowData toTimetableRowData(JourneyLocationDto dto) {
        TimetableRowData row = new TimetableRowData();
        row.setSequence(dto.sequence());
        row.setUopid(dto.uopid());
        row.setName(dto.primaryLocationName());
        row.setCountry(dto.countryCodeIso());
        row.setJourneyLocationType(dto.journeyLocationType());
        row.setEstimatedArrival(dto.arrivalTime());
        row.setEstimatedDeparture(dto.departureTime());
        row.setDwellMinutes(dto.dwellTime());
        return row;
    }
}
