package com.ordermgmt.railway.domain.business.model;

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
    public java.util.Set<BusinessStatus> nextTargets() {
        return switch (this) {
            case ABGESCHLOSSEN, ANNULLIERT -> java.util.Collections.emptySet();
            case IN_BEARBEITUNG -> java.util.Set.of(FREIGEGEBEN);
            case FREIGEGEBEN ->
                    java.util.Set.of(IN_BEARBEITUNG, UEBERARBEITEN, ABGESCHLOSSEN, ANNULLIERT);
            case UEBERARBEITEN -> java.util.Set.of(ABGESCHLOSSEN, ANNULLIERT);
        };
    }

    /** Check if a transition from this status to the target is allowed. */
    public boolean canTransitionTo(BusinessStatus target) {
        return nextTargets().contains(target);
    }
}
