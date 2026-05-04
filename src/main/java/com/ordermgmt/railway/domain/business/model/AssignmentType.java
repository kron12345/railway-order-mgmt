package com.ordermgmt.railway.domain.business.model;

/**
 * Discriminator for the {@code assignmentType} column on a {@link Business}: a business
 * can be assigned to either a Keycloak user (USER) or a Keycloak group (GROUP).
 *
 * <p>Stored as a plain {@code VARCHAR(30)} string in the existing column — no migration
 * needed; legacy free-text rows simply do not match either constant and are treated as
 * "unassigned" by the UI.
 */
public enum AssignmentType {
    USER,
    GROUP;

    /** Defensive parse that returns {@code null} for unknown / legacy values. */
    public static AssignmentType fromString(String s) {
        if (s == null) return null;
        try {
            return AssignmentType.valueOf(s);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
