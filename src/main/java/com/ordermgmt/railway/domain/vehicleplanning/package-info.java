/**
 * <strong>Vehicle Planning</strong> bounded context — assigns reference trains
 * (from the path manager) to physical vehicles across weekdays.
 *
 * <p>{@link com.ordermgmt.railway.domain.vehicleplanning.model.VpRotationSet} is
 * scoped to a Fahrplanjahr; it owns
 * {@link com.ordermgmt.railway.domain.vehicleplanning.model.VpVehicle}s, which in
 * turn own {@link com.ordermgmt.railway.domain.vehicleplanning.model.VpRotationEntry}s
 * (one entry = one train assigned for one weekday). Multiple traction is modelled
 * via {@code CouplingPosition} (FRONT / REAR) on the entry.
 *
 * <p>{@link com.ordermgmt.railway.domain.vehicleplanning.service.VehiclePlanningService}
 * handles CRUD, drag-and-drop moves, and adding trains to vehicles.
 * {@link com.ordermgmt.railway.domain.vehicleplanning.service.ConflictDetectionService}
 * surfaces time-overlap, location-mismatch, and turnaround-time conflicts —
 * shown in the Gantt view's conflict panel.
 *
 * <p>Planning data is <em>not</em> Envers-audited: it is reshufflable working
 * state, not a contractual log (ADR-012).
 */
package com.ordermgmt.railway.domain.vehicleplanning;
