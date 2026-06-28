package com.ordermgmt.railway.domain.timetable.service;

import static com.ordermgmt.railway.domain.timetable.editing.TimetableAnchors.effectiveArrivalAnchor;
import static com.ordermgmt.railway.domain.timetable.editing.TimetableAnchors.effectiveDepartureAnchor;
import static com.ordermgmt.railway.domain.timetable.editing.TimetableAnchors.hasUserEnteredArrival;
import static com.ordermgmt.railway.domain.timetable.editing.TimetableAnchors.hasUserEnteredDeparture;
import static com.ordermgmt.railway.domain.timetable.editing.TimetableTimeMath.format;
import static com.ordermgmt.railway.domain.timetable.editing.TimetableTimeMath.offsetOrZero;
import static com.ordermgmt.railway.domain.timetable.editing.TimetableTimeMath.parseTime;
import static com.ordermgmt.railway.domain.timetable.editing.TimetableTimeMath.sameInstant;
import static com.ordermgmt.railway.domain.timetable.editing.TimetableTimeMath.toAbsoluteMinutes;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.ordermgmt.railway.domain.infrastructure.repository.OperationalPointRepository;
import com.ordermgmt.railway.domain.timetable.editing.TimetableAnchors;
import com.ordermgmt.railway.domain.timetable.editing.TimetableConstraintEditor;
import com.ordermgmt.railway.domain.timetable.editing.TimetableInterpolator;
import com.ordermgmt.railway.domain.timetable.editing.TimetablePropagator;
import com.ordermgmt.railway.domain.timetable.model.RoutePointRole;
import com.ordermgmt.railway.domain.timetable.model.TimeConstraintMode;
import com.ordermgmt.railway.domain.timetable.model.TimePropagationMode;
import com.ordermgmt.railway.domain.timetable.model.TimetableRowData;

/**
 * Server-side timetable editing facade. The actual logic lives in focused, named helpers so each
 * concern is readable on its own:
 *
 * <ul>
 *   <li>{@link TimetableTimeMath} — HH:mm/offset arithmetic.
 *   <li>{@link TimetableAnchors} — effective arrival/departure anchors + user-entered flags.
 *   <li>{@link TimetableConstraintEditor} — halt rules, mirroring, mode switches, dwell.
 *   <li>{@link TimetableInterpolator} — distance-weighted time interpolation between anchors.
 *   <li>{@link TimetablePropagator} — STRETCH/SHIFT propagation of a changed anchor to neighbours.
 * </ul>
 *
 * <p>This class owns the list mutations (insert/soft-delete/purge with resequencing), the snapshot
 * record, time propagation orchestration, and consistency validation, and delegates the rest.
 */
@Service
public class TimetableEditingService {

    @SuppressWarnings("unused") // reserved for future OP lookups during inserts
    private final OperationalPointRepository operationalPointRepository;

    public TimetableEditingService(OperationalPointRepository operationalPointRepository) {
        this.operationalPointRepository = operationalPointRepository;
    }

    // ── Public mutation API ────────────────────────────────────────────────

    /**
     * Insert a fully-populated {@code newRow} at {@code insertIndex} and propagate the surrounding
     * rows so the schedule stays consistent. The caller fills all relevant time fields; this method
     * does not guess defaults. Propagation: backward from the new arrival anchor, then forward from
     * the new departure anchor.
     */
    public void insertStop(
            List<TimetableRowData> rows,
            int insertIndex,
            TimetableRowData newRow,
            TimePropagationMode mode) {
        if (insertIndex < 0 || insertIndex > rows.size()) {
            throw new IllegalArgumentException("insertIndex out of range: " + insertIndex);
        }
        // Capture neighbour anchors before inserting so propagation knows where the schedule
        // flowed.
        TimeSnapshot prevSnapshot = insertIndex > 0 ? snapshot(rows.get(insertIndex - 1)) : null;
        TimeSnapshot nextSnapshot =
                insertIndex < rows.size() ? snapshot(rows.get(insertIndex)) : null;

        rows.add(insertIndex, newRow);
        resequence(rows);

        LocalTime newArr = effectiveArrivalAnchor(newRow);
        if (prevSnapshot != null && newArr != null && prevSnapshot.departure() != null) {
            long delta = Duration.between(prevSnapshot.departure(), newArr).toMinutes();
            if (delta < 0) {
                TimetablePropagator.propagateBackward(
                        rows, insertIndex, delta, mode, prevSnapshot.departure(), newArr);
            }
        }
        LocalTime newDep = effectiveDepartureAnchor(newRow);
        if (nextSnapshot != null && newDep != null && nextSnapshot.arrival() != null) {
            long delta = Duration.between(nextSnapshot.arrival(), newDep).toMinutes();
            if (delta > 0) {
                TimetablePropagator.propagateForward(
                        rows, insertIndex, delta, mode, nextSnapshot.arrival(), newDep);
            }
        }
    }

    /** Toggle a row's soft-deleted flag. Origin and destination cannot be deleted. */
    public void softDeleteStop(List<TimetableRowData> rows, int index) {
        if (index < 0 || index >= rows.size()) {
            return;
        }
        TimetableRowData row = rows.get(index);
        if (isRouteEndpoint(row)) {
            return;
        }
        row.setDeleted(!Boolean.TRUE.equals(row.getDeleted()));
    }

    /** Permanently remove soft-deleted rows and renumber. */
    public void purgeDeleted(List<TimetableRowData> rows) {
        rows.removeIf(row -> Boolean.TRUE.equals(row.getDeleted()));
        resequence(rows);
    }

    /** Renumber rows starting from 1. */
    public void resequence(List<TimetableRowData> rows) {
        for (int i = 0; i < rows.size(); i++) {
            rows.get(i).setSequence(i + 1);
        }
    }

    // ── Anchor extraction ──────────────────────────────────────────────────

    /**
     * Snapshot of a row's effective time anchors. Take one before mutating the row, pass it to
     * {@link #propagate} after the mutation so the service knows which side(s) shifted and by how
     * much.
     */
    public record TimeSnapshot(
            LocalTime arrival, int arrivalOffset, LocalTime departure, int departureOffset) {}

    public TimeSnapshot snapshot(TimetableRowData row) {
        return new TimeSnapshot(
                effectiveArrivalAnchor(row),
                offsetOrZero(row.getArrivalOffset()),
                effectiveDepartureAnchor(row),
                offsetOrZero(row.getDepartureOffset()));
    }

    public LocalTime effectiveArrivalAnchor(TimetableRowData row) {
        return TimetableAnchors.effectiveArrivalAnchor(row);
    }

    public LocalTime effectiveDepartureAnchor(TimetableRowData row) {
        return TimetableAnchors.effectiveDepartureAnchor(row);
    }

    // ── Public propagation API ─────────────────────────────────────────────

    /**
     * Compare the row's current anchors to {@code oldSnapshot} and propagate any deltas: arrival
     * changes flow backwards, departure changes flow forwards. Pinned rows (and EXACT-mode rows by
     * their hard-set times) act as propagation barriers.
     */
    public void propagate(
            List<TimetableRowData> rows,
            int changedIndex,
            TimeSnapshot oldSnapshot,
            TimePropagationMode mode) {
        if (oldSnapshot == null || changedIndex < 0 || changedIndex >= rows.size()) {
            return;
        }
        TimetableRowData row = rows.get(changedIndex);
        TimeSnapshot now = snapshot(row);

        if (now.arrival() != null
                && oldSnapshot.arrival() != null
                && !sameInstant(
                        oldSnapshot.arrival(),
                        oldSnapshot.arrivalOffset(),
                        now.arrival(),
                        now.arrivalOffset())) {
            long delta =
                    toAbsoluteMinutes(now.arrival(), now.arrivalOffset())
                            - toAbsoluteMinutes(oldSnapshot.arrival(), oldSnapshot.arrivalOffset());
            // Keep the row's estimated* in sync with its new effective anchor so we don't
            // double-shift it (neighbours are shifted separately).
            row.setEstimatedArrival(format(now.arrival()));
            TimetablePropagator.propagateBackward(
                    rows, changedIndex, delta, mode, oldSnapshot.arrival(), now.arrival());
        }
        if (now.departure() != null
                && oldSnapshot.departure() != null
                && !sameInstant(
                        oldSnapshot.departure(),
                        oldSnapshot.departureOffset(),
                        now.departure(),
                        now.departureOffset())) {
            long delta =
                    toAbsoluteMinutes(now.departure(), now.departureOffset())
                            - toAbsoluteMinutes(
                                    oldSnapshot.departure(), oldSnapshot.departureOffset());
            row.setEstimatedDeparture(format(now.departure()));
            TimetablePropagator.propagateForward(
                    rows, changedIndex, delta, mode, oldSnapshot.departure(), now.departure());
        }
    }

    /** Distance-weighted time interpolation between consecutive user-entered anchor times. */
    public void interpolateBetweenAnchors(List<TimetableRowData> rows) {
        TimetableInterpolator.interpolateBetweenAnchors(rows);
    }

    // ── Halt rules / constraints (delegated) ────────────────────────────────

    public void applyHaltRules(TimetableRowData row) {
        TimetableConstraintEditor.applyHaltRules(row);
    }

    public void preserveOnModeSwitch(
            TimetableRowData row,
            boolean arrivalSide,
            TimeConstraintMode oldMode,
            TimeConstraintMode newMode) {
        TimetableConstraintEditor.preserveOnModeSwitch(row, arrivalSide, oldMode, newMode);
    }

    public boolean deriveDepartureFromDwell(TimetableRowData row) {
        return TimetableConstraintEditor.deriveDepartureFromDwell(row);
    }

    public boolean reconcileDwell(TimetableRowData row) {
        return TimetableConstraintEditor.reconcileDwell(row);
    }

    public boolean hasUserEnteredArrival(TimetableRowData row) {
        return TimetableAnchors.hasUserEnteredArrival(row);
    }

    public boolean hasUserEnteredDeparture(TimetableRowData row) {
        return TimetableAnchors.hasUserEnteredDeparture(row);
    }

    /**
     * Whether this row's data is sent in the TTT Path Request. Delegates to {@link
     * TttDraftBuilder#isExportedToTtt} — the same rule the export uses — so the UI indicator and
     * the export can never disagree.
     */
    public boolean isExportedToTtt(TimetableRowData row) {
        return TttDraftBuilder.isExportedToTtt(row);
    }

    // ── Validation ─────────────────────────────────────────────────────────

    /**
     * Human-readable consistency violations. Empty means consistent: arrivals ≤ departures within
     * each row and non-negative travel time between consecutive rows.
     */
    public List<String> validate(List<TimetableRowData> rows) {
        List<String> errors = new ArrayList<>();
        if (rows == null || rows.isEmpty()) {
            return errors;
        }

        // Regel 9: at least one halt must exist (ORIGIN/DESTINATION count as implicit halts).
        boolean hasAnyHalt =
                rows.stream()
                        .anyMatch(
                                r ->
                                        Boolean.TRUE.equals(r.getHalt())
                                                || r.getRoutePointRole() == RoutePointRole.ORIGIN
                                                || r.getRoutePointRole()
                                                        == RoutePointRole.DESTINATION);
        if (!hasAnyHalt) {
            errors.add("Mindestens ein Halt erforderlich (Origin, Destination oder Zwischenhalt)");
        }

        for (int i = 0; i < rows.size(); i++) {
            TimetableRowData r = rows.get(i);

            // Regel 3: pass-through rows must not have user-entered times.
            if (!Boolean.TRUE.equals(r.getHalt())
                    && (hasUserEnteredArrival(r) || hasUserEnteredDeparture(r))) {
                errors.add(
                        "Row "
                                + (i + 1)
                                + " ("
                                + r.getName()
                                + "): Durchfahrt darf keine expliziten Zeiten haben");
            }

            // WINDOW order on each side: ELA ≤ LLA, ELD ≤ LLD
            if (r.getArrivalMode() == TimeConstraintMode.WINDOW) {
                LocalTime ela = parseTime(r.getArrivalEarliest());
                LocalTime lla = parseTime(r.getArrivalLatest());
                if (ela != null && lla != null && ela.isAfter(lla)) {
                    errors.add("Row " + (i + 1) + " (" + r.getName() + "): ELA > LLA");
                }
            }
            if (r.getDepartureMode() == TimeConstraintMode.WINDOW) {
                LocalTime eld = parseTime(r.getDepartureEarliest());
                LocalTime lld = parseTime(r.getDepartureLatest());
                if (eld != null && lld != null && eld.isAfter(lld)) {
                    errors.add("Row " + (i + 1) + " (" + r.getName() + "): ELD > LLD");
                }
            }

            LocalTime arr = effectiveArrivalAnchor(r);
            LocalTime dep = effectiveDepartureAnchor(r);
            if (arr != null && dep != null && arr.isAfter(dep)) {
                errors.add(
                        "Row "
                                + (i + 1)
                                + " ("
                                + r.getName()
                                + "): arrival "
                                + arr
                                + " is after departure "
                                + dep);
            }
            if (i > 0) {
                TimetableRowData prev = rows.get(i - 1);
                LocalTime prevDep = effectiveDepartureAnchor(prev);
                if (prevDep != null && arr != null && prevDep.isAfter(arr)) {
                    errors.add(
                            "Row "
                                    + (i + 1)
                                    + " ("
                                    + r.getName()
                                    + "): arrival "
                                    + arr
                                    + " is before previous departure "
                                    + prevDep
                                    + " (negative travel time)");
                }
            }
        }
        return errors;
    }

    // ── Backwards-compatible legacy entry point ─────────────────────────────

    public LocalTime resolveRelativeTime(String input, LocalTime baseTime) {
        if (input == null || baseTime == null) {
            return null;
        }
        String trimmed = input.trim();
        if (!trimmed.startsWith("+") && !trimmed.startsWith("-")) {
            return null;
        }
        try {
            return baseTime.plusMinutes(Integer.parseInt(trimmed));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean isRouteEndpoint(TimetableRowData row) {
        return row.getRoutePointRole() == RoutePointRole.ORIGIN
                || row.getRoutePointRole() == RoutePointRole.DESTINATION;
    }
}
