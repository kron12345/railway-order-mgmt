package com.ordermgmt.railway.dto.pathmanager;

import jakarta.validation.constraints.NotBlank;

/** Request body for executing a process state transition. */
public record ProcessTransitionRequest(
        @NotBlank(message = "Action is required") String action, String comment) {}
