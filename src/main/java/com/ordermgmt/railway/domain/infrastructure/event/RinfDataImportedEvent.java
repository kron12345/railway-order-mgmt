package com.ordermgmt.railway.domain.infrastructure.event;

/**
 * Published after a successful RINF import (operational points or sections of line) so downstream
 * caches — such as the routing graph in the timetable context — can invalidate stale data instead
 * of waiting for the time-based eviction.
 *
 * <p>Using an application event keeps the infrastructure (import) context decoupled from the
 * timetable (routing) context: the importer does not depend on the routing cache, it only announces
 * that the underlying topology changed.
 *
 * @param dataset the imported dataset marker ("RINF_OP" or "RINF_SOL")
 * @param country the ISO country the import targeted
 */
public record RinfDataImportedEvent(String dataset, String country) {}
