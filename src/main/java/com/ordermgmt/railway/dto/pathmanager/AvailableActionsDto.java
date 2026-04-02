package com.ordermgmt.railway.dto.pathmanager;

import java.util.List;

/** Available process actions for a reference train in its current state. */
public record AvailableActionsDto(String currentState, List<String> actions) {}
