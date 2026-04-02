package com.ordermgmt.railway.domain.pathmanager.model;

/** Result of executing a process transition on a reference train. */
public record ProcessStepResult(
        PmProcessStep processStep, PathProcessState newState, PmTrainVersion newVersion) {}
