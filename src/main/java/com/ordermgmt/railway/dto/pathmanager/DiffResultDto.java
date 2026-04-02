package com.ordermgmt.railway.dto.pathmanager;

import java.util.List;
import java.util.Map;

/** Result of comparing order-side timetable data with PM journey locations. */
public record DiffResultDto(List<DiffEntryDto> entries) {

    /** A single location entry in the diff result. */
    public record DiffEntryDto(
            String uopid, String name, String changeType, Map<String, String[]> fieldDiffs) {}
}
