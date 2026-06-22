/**
 * <strong>Timetable</strong> bounded context — routing, builder, and read-only archives of approved
 * timetables.
 *
 * <p>Two responsibilities:
 *
 * <ul>
 *   <li>Routing — given a sequence of operational points (RINF master data) find the shortest path
 *       through the {@code sections_of_line} graph and produce intermediate {@link
 *       com.ordermgmt.railway.domain.timetable.model.TimetableRowData} rows with estimated times.
 *   <li>Archive — every saved timetable lands in {@link
 *       com.ordermgmt.railway.domain.timetable.model.TimetableArchive} with its rows. FAHRPLAN
 *       order positions link to one archive each (ADR-009). Read-only; new versions create new
 *       archives.
 * </ul>
 *
 * <p>Time-handling is documented in ADR-010 (shift / stretch propagation, pinned times,
 * time-constraint modes).
 */
package com.ordermgmt.railway.domain.timetable;
