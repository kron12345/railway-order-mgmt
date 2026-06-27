package com.ordermgmt.railway.domain.business.model;

import java.util.Collections;
import java.util.Set;

/** Lifecycle states for a business work item. */
public enum BusinessStatus {
    IN_BEARBEITUNG,
    FREIGEGEBEN,
    UEBERARBEITEN,
    ABGESCHLOSSEN,
    ANNULLIERT;

    /**
     * Returns the valid next status targets for this status. Closed/annulled states are absorbing
     * (no transitions allowed).
     */
    public Set<BusinessStatus> nextTargets() {
        return switch (this) {
            case ABGESCHLOSSEN, ANNULLIERT -> Collections.emptySet();
            case IN_BEARBEITUNG -> Set.of(FREIGEGEBEN);
            case FREIGEGEBEN -> Set.of(IN_BEARBEITUNG, UEBERARBEITEN, ABGESCHLOSSEN, ANNULLIERT);
            case UEBERARBEITEN -> Set.of(ABGESCHLOSSEN, ANNULLIERT);
        };
    }

    /** Check if a transition from this status to the target is allowed. */
    public boolean canTransitionTo(BusinessStatus target) {
        return nextTargets().contains(target);
    }
}
