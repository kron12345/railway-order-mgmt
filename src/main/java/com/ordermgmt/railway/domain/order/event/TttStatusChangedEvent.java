package com.ordermgmt.railway.domain.order.event;

import java.util.UUID;

/**
 * Published when a purchase position's TTT process state actually changes during a sync, so
 * status-triggered deadline rules (Frist-Regeln) can fire immediately instead of waiting for the
 * scheduled pass.
 */
public record TttStatusChangedEvent(UUID orderPositionId, String newProcessState) {}
