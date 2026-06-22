/**
 * <strong>Infrastructure</strong> bounded context — railway infrastructure master data sourced from
 * the European RINF register.
 *
 * <p>Two main entities:
 *
 * <ul>
 *   <li>{@code OperationalPoint} — stations, junctions, switches; identified by UOPID (Unique
 *       Operational Point Identifier).
 *   <li>{@code SectionOfLine} — track segments connecting two operational points; form the graph
 *       used by the timetable router.
 * </ul>
 *
 * <p>Plus {@code PredefinedTag} — the schlagwort catalogue imported via CSV (ADR-008). Imports are
 * atomic-replace: a failed import leaves the previous data intact.
 */
package com.ordermgmt.railway.domain.infrastructure;
