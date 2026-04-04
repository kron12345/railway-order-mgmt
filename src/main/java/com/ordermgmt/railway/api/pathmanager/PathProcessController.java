package com.ordermgmt.railway.api.pathmanager;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ordermgmt.railway.domain.pathmanager.model.PathAction;
import com.ordermgmt.railway.domain.pathmanager.model.PmProcessStep;
import com.ordermgmt.railway.domain.pathmanager.model.PmReferenceTrain;
import com.ordermgmt.railway.domain.pathmanager.model.ProcessStepResult;
import com.ordermgmt.railway.domain.pathmanager.repository.PmProcessStepRepository;
import com.ordermgmt.railway.domain.pathmanager.service.PathManagerService;
import com.ordermgmt.railway.domain.pathmanager.service.PathProcessEngine;
import com.ordermgmt.railway.dto.pathmanager.AvailableActionsDto;
import com.ordermgmt.railway.dto.pathmanager.ProcessStepDto;
import com.ordermgmt.railway.dto.pathmanager.ProcessTransitionRequest;
import com.ordermgmt.railway.mapper.pathmanager.PathManagerDtoMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** REST endpoints for TTT process simulation (state transitions on reference trains). */
@RestController
@RequestMapping("/api/v1/pathmanager/process")
@Tag(name = "TTT Process Simulation")
@RequiredArgsConstructor
public class PathProcessController {

    private final PathProcessEngine pathProcessEngine;
    private final PathManagerService pathManagerService;
    private final PmProcessStepRepository processStepRepository;

    @PostMapping("/{referenceTrainId}/step")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Execute a process state transition")
    @ApiResponse(responseCode = "201", description = "Transition executed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid action for current state")
    @ApiResponse(responseCode = "404", description = "Reference train not found")
    public ProcessStepDto executeTransition(
            @PathVariable UUID referenceTrainId,
            @Valid @RequestBody ProcessTransitionRequest request) {
        PathAction action = PathAction.valueOf(request.action());
        ProcessStepResult result =
                pathProcessEngine.executeTransition(referenceTrainId, action, request.comment());
        return PathManagerDtoMapper.toStepDto(result.processStep());
    }

    @GetMapping("/{referenceTrainId}/available-actions")
    @Operation(summary = "Get available actions for the current process state")
    @ApiResponse(responseCode = "200", description = "Available actions returned")
    @ApiResponse(responseCode = "404", description = "Reference train not found")
    public AvailableActionsDto getAvailableActions(@PathVariable UUID referenceTrainId) {
        PmReferenceTrain train = pathManagerService.findById(referenceTrainId);
        Set<PathAction> actions = pathProcessEngine.getAvailableActions(referenceTrainId);
        List<String> actionNames = actions.stream().map(PathAction::name).sorted().toList();
        return new AvailableActionsDto(train.getProcessState().name(), actionNames);
    }

    @GetMapping("/{referenceTrainId}/history")
    @Operation(summary = "Get process step history for a reference train")
    @ApiResponse(responseCode = "200", description = "Process history returned")
    public List<ProcessStepDto> getHistory(@PathVariable UUID referenceTrainId) {
        pathManagerService.findById(referenceTrainId); // validate existence
        List<PmProcessStep> steps =
                processStepRepository.findByReferenceTrainIdOrderByCreatedAtDesc(referenceTrainId);
        return steps.stream().map(PathManagerDtoMapper::toStepDto).toList();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Resource not found"));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleBadState(IllegalStateException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", "Invalid request"));
    }
}
