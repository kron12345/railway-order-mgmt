package com.ordermgmt.railway.domain.order.model;

/** Single audit revision entry used by the AuditService and displayed in the AuditHistoryDialog. */
public record AuditEntry(
        int revision, String timestamp, String user, String type, String changes) {}
