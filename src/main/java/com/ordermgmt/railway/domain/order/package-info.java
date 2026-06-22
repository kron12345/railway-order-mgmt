/**
 * <strong>Order</strong> bounded context — the heart of the application.
 *
 * <p>An {@link com.ordermgmt.railway.domain.order.model.Order} contains many {@link
 * com.ordermgmt.railway.domain.order.model.OrderPosition}s. Positions split into types (FAHRPLAN,
 * LEISTUNG …) and have demand-side {@link com.ordermgmt.railway.domain.order.model.ResourceNeed}s
 * (Bedarfe). Each need can be fulfilled internally or via a {@link
 * com.ordermgmt.railway.domain.order.model.PurchasePosition} (Bestellposition, extern bestellte
 * Kapazität / Personal / Fahrzeug).
 *
 * <p>FAHRPLAN positions link 1:1 to a {@link
 * com.ordermgmt.railway.domain.timetable.model.TimetableArchive} via a CAPACITY resource need
 * (ADR-009).
 *
 * <p>{@link com.ordermgmt.railway.domain.order.service.OrderService} is the only mutation entry
 * point and is guarded by {@code @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")} on save /
 * delete paths. {@link com.ordermgmt.railway.domain.order.service.AuditService} provides the
 * cross-cutting Envers history readout used by the UI.
 */
package com.ordermgmt.railway.domain.order;
