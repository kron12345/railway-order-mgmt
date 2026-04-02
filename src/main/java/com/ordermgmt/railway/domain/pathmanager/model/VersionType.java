package com.ordermgmt.railway.domain.pathmanager.model;

/** Describes how a train version was created in the path lifecycle. */
public enum VersionType {
    INITIAL,
    MODIFICATION,
    ALTERATION,
    CANCELLATION
}
