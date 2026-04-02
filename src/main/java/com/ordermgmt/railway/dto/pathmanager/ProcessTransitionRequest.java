package com.ordermgmt.railway.dto.pathmanager;

/** Request body for executing a process state transition. */
public record ProcessTransitionRequest(String action, String comment) {}
