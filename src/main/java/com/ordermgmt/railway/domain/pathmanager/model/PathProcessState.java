package com.ordermgmt.railway.domain.pathmanager.model;

/** Process states of a reference train in the TTT path lifecycle. */
public enum PathProcessState {
    NEW,
    CREATED,
    MODIFIED,
    WITHDRAWN,
    RECEIPT_CONFIRMED,
    DRAFT_OFFERED,
    NO_ALTERNATIVE,
    REVISION_REQUESTED,
    FINAL_OFFERED,
    BOOKED,
    MODIFICATION_REQUESTED,
    ALTERATION_ANNOUNCED,
    ALTERATION_OFFERED,
    CANCELED,
    SUPERSEDED
}
