package com.ordermgmt.railway.domain.timetable.service;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.ordermgmt.railway.domain.infrastructure.repository.OperationalPointRepository;
import com.ordermgmt.railway.domain.timetable.model.RoutePointRole;
import com.ordermgmt.railway.domain.timetable.model.TimeConstraintMode;
import com.ordermgmt.railway.domain.timetable.model.TimePropagationMode;
import com.ordermgmt.railway.domain.timetable.model.TimetableRowData;

/**
 * Server-side timetable editing logic. Owns three things that the UI calls into:
 *
 * <ol>
 *   <li>Mutations on the row list — insert/soft-delete/purge with sequence renumbering.
 *   <li>Time propagation — given a row whose arrival or departure anchor changed, push the change
 *       either backwards (arrival → preceding rows) or forwards (departure → following rows).
 *       Either {@link TimePropagationMode#STRETCH} (preserves total schedule length, distorts
 *       travel times proportionally to the next pin) or {@link TimePropagationMode#SHIFT} (rigid
 *       translation that stops at the first pinned row).
 *   <li>Consistency validation — {@link #validate(List)} checks that arrival ≤ departure within
 *       each row and the schedule is monotonic between rows.
 * </ol>
 *
 * <p>The "anchor" of an arrival is the earliest meaningful time the train may arrive (EXACT.value /
 * WINDOW.earliest / COMMERCIAL.value / estimated), and the anchor of a departure is the latest
 * meaningful departure (EXACT.value / WINDOW.latest / COMMERCIAL.value / estimated). That asymmetry
 * matches the semantics the user expects: an earliest-arrival edit is the constraint that pulls the
 * previous travel leg, a latest-departure edit pushes the next leg.
 */
@Service
public class TimetableEditingService {

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    @SuppressWarnings("unused") // reserved for future OP lookups during inserts
    private final OperationalPointRepository operationalPointRepository;

    public TimetableEditingService(OperationalPointRepository operationalPointRepository) {
        this.operationalPointRepository = operationalPointRepository;
    }

    // ── Public mutation API ────────────────────────────────────────────────

    /**
     * Insert a fully-populated {@code newRow} at {@code insertIndex} and propagate the surrounding
     * rows so the schedule stays consistent. The caller is responsible for filling all relevant
     * time fields (arrival, departure, mode markers) on the row; this method does <em>not</em>
     * guess defaults.
     *
     * <p>Propagation: backward from the new arrival anchor, then forward from the new departure
     * anchor. Both directions respect pinned rows and the chosen mode.
     */
    public void insertStop(
            List<TimetableRowData> rows,
            int insertIndex,
            TimetableRowData newRow,
            TimePropagationMode mode) {
        if (insertIndex < 0 || insertIndex > rows.size()) {
            throw new IllegalArgumentException("insertIndex out of range: " + insertIndex);
        }
        // Capture neighbour anchors *before* inserting so the propagation knows where
        // the schedule used to flow. The new row brings its own anchors with it.
        TimeSnapshot prevSnapshot = insertIndex > 0 ? snapshot(rows.get(insertIndex - 1)) : null;
        TimeSnapshot nextSnapshot =
                insertIndex < rows.size() ? snapshot(rows.get(insertIndex)) : null;

        rows.add(insertIndex, newRow);
        resequence(rows);

        // Backwards: previous row's departure must be ≤ new row's arrival anchor.
        LocalTime newArr = effectiveArrivalAnchor(newRow);
        if (prevSnapshot != null && newArr != null && prevSnapshot.departure() != null) {
            long delta = Duration.between(prevSnapshot.departure(), newArr).toMinutes();
            if (delta < 0) {
                propagateBackward(rows, insertIndex, delta, mode, prevSnapshot.departure(), newArr);
            }
        }
        // Forwards: next row's arrival must be ≥ new row's departure anchor.
        LocalTime newDep = effectiveDepartureAnchor(newRow);
        if (nextSnapshot != null && newDep != null && nextSnapshot.arrival() != null) {
            long delta = Duration.between(nextSnapshot.arrival(), newDep).toMinutes();
            if (delta > 0) {
                propagateForward(rows, insertIndex, delta, mode, nextSnapshot.arrival(), newDep);
            }
        }
    }

    /** Toggle a row's soft-deleted flag. Origin and destination cannot be deleted. */
    public void softDeleteStop(List<TimetableRowData> rows, int index) {
        if (index < 0 || index >= rows.size()) return;
        TimetableRowData row = rows.get(index);
        if (row.getRoutePointRole() == RoutePointRole.ORIGIN
                || row.getRoutePointRole() == RoutePointRole.DESTINATION) return;
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
     * {@link #propagate(List, int, TimeSnapshot, TimePropagationMode)} after the mutation so the
     * service knows which side(s) shifted and by how much.
     */
    public record TimeSnapshot(
            LocalTime arrival, int arrivalOffset, LocalTime departure, int departureOffset) {}

    public TimeSnapshot snapshot(TimetableRowData row) {
        int aOff = row.getArrivalOffset() == null ? 0 : row.getArrivalOffset();
        int dOff = row.getDepartureOffset() == null ? 0 : row.getDepartureOffset();
        return new TimeSnapshot(
                effectiveArrivalAnchor(row), aOff, effectiveDepartureAnchor(row), dOff);
    }

    /**
     * Earliest meaningful arrival the train may have at this stop, given the current {@link
     * TimeConstraintMode}. Used as the anchor for backwards propagation.
     */
    public LocalTime effectiveArrivalAnchor(TimetableRowData row) {
        TimeConstraintMode mode = row.getArrivalMode();
        if (mode == null) mode = TimeConstraintMode.NONE;
        return switch (mode) {
            case EXACT ->
                    firstNonNull(
                            parseTime(row.getArrivalExact()), parseTime(row.getEstimatedArrival()));
            case WINDOW, AFTER ->
                    firstNonNull(
                            parseTime(row.getArrivalEarliest()),
                            parseTime(row.getEstimatedArrival()));
            case BEFORE ->
                    firstNonNull(
                            parseTime(row.getArrivalLatest()),
                            parseTime(row.getEstimatedArrival()));
            case COMMERCIAL ->
                    firstNonNull(
                            parseTime(row.getCommercialArrival()),
                            parseTime(row.getEstimatedArrival()));
            case NONE -> parseTime(row.getEstimatedArrival());
        };
    }

    /**
     * Latest meaningful departure the train may have at this stop. Used as the anchor for forwards
     * propagation: the schedule's "max time" semantic — the train is allowed to stay until LLD, so
     * the next station must be reachable from there. This is intentionally LLD (not ELD) on WINDOW
     * mode per Regel 7 + planning constraint: forward computations always assume the largest
     * permitted time range.
     */
    public LocalTime effectiveDepartureAnchor(TimetableRowData row) {
        TimeConstraintMode mode = row.getDepartureMode();
        if (mode == null) mode = TimeConstraintMode.NONE;
        return switch (mode) {
            case EXACT ->
                    firstNonNull(
                            parseTime(row.getDepartureExact()),
                            parseTime(row.getEstimatedDeparture()));
            case WINDOW, BEFORE ->
                    firstNonNull(
                            parseTime(row.getDepartureLatest()),
                            parseTime(row.getEstimatedDeparture()));
            case AFTER ->
                    firstNonNull(
                            parseTime(row.getDepartureEarliest()),
                            parseTime(row.getEstimatedDeparture()));
            case COMMERCIAL ->
                    firstNonNull(
                            parseTime(row.getCommercialDeparture()),
                            parseTime(row.getEstimatedDeparture()));
            case NONE -> parseTime(row.getEstimatedDeparture());
        };
    }

    // ── Public propagation API ─────────────────────────────────────────────

    /**
     * Compare the row's current anchors to the {@code oldSnapshot} and propagate any deltas:
     * arrival changes flow backwards, departure changes flow forwards.
     *
     * <p>Pinned rows (and EXACT-mode rows by virtue of their hard-set times) act as propagation
     * barriers in stretch mode; in shift mode the rigid translation stops at the first pinned row
     * regardless.
     */
    public void propagate(
            List<TimetableRowData> rows,
            int changedIndex,
            TimeSnapshot oldSnapshot,
            TimePropagationMode mode) {
        if (oldSnapshot == null || changedIndex < 0 || changedIndex >= rows.size()) return;
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
            // Keep the row's estimated* in sync with its new effective anchor — neighbours are
            // shifted separately, so the changed row itself needs an explicit sync here so we
            // don't double-shift it.
            row.setEstimatedArrival(now.arrival().format(HH_MM));
            propagateBackward(
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
            row.setEstimatedDeparture(now.departure().format(HH_MM));
            propagateForward(
                    rows, changedIndex, delta, mode, oldSnapshot.departure(), now.departure());
        }
    }

    private boolean sameInstant(LocalTime a, int aOff, LocalTime b, int bOff) {
        return a.equals(b) && aOff == bOff;
    }

    // ── Segmental speed interpolation ──────────────────────────────────────

    /**
     * Distance-weighted time interpolation between consecutive user-entered anchor times.
     *
     * <p>Each user-entered arrival or departure on a row produces an anchor point {@code (rowIndex,
     * distanceFromStart, absoluteMinutes)}. Between consecutive anchors the implied average speed
     * is {@code (Δtime / Δdistance)}, applied to every intermediate row to fill its {@code
     * estimatedArrival} (and {@code estimatedDeparture = arr + dwell}).
     *
     * <p>Rows outside the anchor range (before the first or after the last) are left untouched —
     * the caller can apply a default speed there.
     */
    /** Default cruise speed for fringe rows (before first / after last anchor): 70 km/h. */
    private static final double DEFAULT_SPEED_MIN_PER_METER = 60.0 / 70_000d;

    public void interpolateBetweenAnchors(List<TimetableRowData> rows) {
        if (rows == null || rows.isEmpty()) return;
        List<AnchorPoint> anchors = collectAnchors(rows);
        if (anchors.isEmpty()) return;

        // Segments between consecutive anchors → segmental average speed.
        for (int a = 0; a < anchors.size() - 1; a++) {
            AnchorPoint p1 = anchors.get(a);
            AnchorPoint p2 = anchors.get(a + 1);
            double distSpan = p2.distance() - p1.distance();
            long timeSpan = p2.absMinutes() - p1.absMinutes();
            if (distSpan <= 0 || timeSpan <= 0) continue;
            double minPerMeter = (double) timeSpan / distSpan;

            for (int r = p1.rowIndex() + 1; r < p2.rowIndex(); r++) {
                fillRow(rows.get(r), p1.distance(), p1.absMinutes(), minPerMeter);
            }
        }

        // Fringe fill: rows before the first anchor and after the last anchor get default speed.
        // This makes a single user-entered anchor (e.g. an ELA on one halt) enough to derive an
        // estimated origin departure — without forcing the user to enter a second anchor.
        AnchorPoint first = anchors.get(0);
        for (int r = first.rowIndex() - 1; r >= 0; r--) {
            fillRow(rows.get(r), first.distance(), first.absMinutes(), DEFAULT_SPEED_MIN_PER_METER);
        }
        AnchorPoint last = anchors.get(anchors.size() - 1);
        for (int r = last.rowIndex() + 1; r < rows.size(); r++) {
            fillRow(rows.get(r), last.distance(), last.absMinutes(), DEFAULT_SPEED_MIN_PER_METER);
        }
    }

    /**
     * Compute a row's estimated arrival (and dwell-derived departure) by linear extrapolation from
     * the given anchor at {@code (anchorDist, anchorAbsMin)}, using {@code minPerMeter} as the
     * speed. Works in both directions: rows before the anchor get earlier times (negative delta),
     * rows after get later times.
     */
    private void fillRow(
            TimetableRowData row, double anchorDist, long anchorAbsMin, double minPerMeter) {
        Double dist = row.getDistanceFromStartMeters();
        if (dist == null) return;
        long arrAbs = anchorAbsMin + Math.round((dist - anchorDist) * minPerMeter);
        int dwell = row.getDwellMinutes() != null ? row.getDwellMinutes() : 0;
        long depAbs = arrAbs + dwell;
        row.setEstimatedArrival(wallClock(arrAbs).format(HH_MM));
        row.setEstimatedDeparture(wallClock(depAbs).format(HH_MM));
        row.setArrivalOffset(dayOffset(arrAbs));
        row.setDepartureOffset(dayOffset(depAbs));
    }

    /** A user-entered (rowIndex, distance, absoluteMinutes) interpolation anchor. */
    private record AnchorPoint(int rowIndex, double distance, long absMinutes) {}

    private List<AnchorPoint> collectAnchors(List<TimetableRowData> rows) {
        List<AnchorPoint> anchors = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            TimetableRowData row = rows.get(i);
            Double dist = row.getDistanceFromStartMeters();
            if (dist == null) continue;
            if (hasIntentArrival(row)) {
                LocalTime t = effectiveArrivalAnchor(row);
                int off = row.getArrivalOffset() == null ? 0 : row.getArrivalOffset();
                if (t != null) {
                    anchors.add(new AnchorPoint(i, dist, toAbsoluteMinutes(t, off)));
                }
            }
            if (hasIntentDeparture(row)) {
                LocalTime t = effectiveDepartureAnchor(row);
                int off = row.getDepartureOffset() == null ? 0 : row.getDepartureOffset();
                if (t != null) {
                    anchors.add(new AnchorPoint(i, dist, toAbsoluteMinutes(t, off)));
                }
            }
        }
        return anchors;
    }

    /**
     * "Intent" check: a row's arrival is intentionally fixed when either the user typed it directly
     * OR the user typed the departure side + dwell, in which case the arrival is a
     * derived-but-intended value (Regel 5 mirroring). Same logic for departure side. Used by
     * interpolation so the forward walk after a halt with WINDOW arrival + dwell starts from the
     * mirrored LLD instead of the ELA — i.e. the next row's time is built on top of "the latest the
     * train may leave", which is the planning-relevant figure.
     */
    private boolean hasIntentArrival(TimetableRowData row) {
        if (hasUserEnteredArrival(row)) return true;
        return Boolean.TRUE.equals(row.getHalt())
                && Boolean.TRUE.equals(row.getUserEnteredDwell())
                && hasUserEnteredDeparture(row);
    }

    private boolean hasIntentDeparture(TimetableRowData row) {
        if (hasUserEnteredDeparture(row)) return true;
        return Boolean.TRUE.equals(row.getHalt())
                && Boolean.TRUE.equals(row.getUserEnteredDwell())
                && hasUserEnteredArrival(row);
    }

    // ── Halt-rule enforcement ──────────────────────────────────────────────

    /**
     * Apply the timetable editing rules (Regeln 3 + 4 + 5):
     *
     * <ul>
     *   <li>Halt=false → clear all constraint fields and dwell, reset modes to NONE. Estimated
     *       values (used for interpolation) are preserved. Pin remains user-controlled.
     *   <li>Halt=true with dwell + 1 side filled → mirror the dwell into the other side using the
     *       <em>same</em> mode (rule 5).
     *   <li>Halt=true with both sides filled (no dwell, or contradicting dwell) → drop dwell
     *       (option {@code first} from rule 8).
     * </ul>
     *
     * <p>This method is idempotent and safe to call after every editor sync. It does not delete
     * estimated times — only user-entered constraint fields are mutated.
     */
    public void applyHaltRules(TimetableRowData row) {
        if (row == null) return;

        // Origin and destination are implicit halts with no dwell. They constrain only one side:
        // origin → departure-only, destination → arrival-only. Strip the irrelevant fields so
        // they never sneak into the TTT export.
        if (row.getRoutePointRole() == RoutePointRole.ORIGIN) {
            row.setHalt(true);
            row.setDwellMinutes(null);
            row.setUserEnteredDwell(false);
            clearArrivalConstraints(row);
            return;
        }
        if (row.getRoutePointRole() == RoutePointRole.DESTINATION) {
            row.setHalt(true);
            row.setDwellMinutes(null);
            row.setUserEnteredDwell(false);
            clearDepartureConstraints(row);
            return;
        }

        if (!Boolean.TRUE.equals(row.getHalt())) {
            clearArrivalConstraints(row);
            clearDepartureConstraints(row);
            row.setDwellMinutes(null);
            row.setUserEnteredDwell(false);
            return;
        }

        boolean arrEntered = hasUserEnteredArrival(row);
        boolean depEntered = hasUserEnteredDeparture(row);
        boolean dwellEntered =
                Boolean.TRUE.equals(row.getUserEnteredDwell()) && row.getDwellMinutes() != null;

        if (arrEntered && depEntered) {
            // Both sides explicit → dwell becomes implicit. Drop any user-entered dwell.
            row.setDwellMinutes(null);
            row.setUserEnteredDwell(false);
            return;
        }

        if (dwellEntered && (arrEntered || depEntered)) {
            // Mirror the entered side onto the empty side using the same mode.
            if (arrEntered) {
                mirrorArrivalToDeparture(row);
            } else {
                mirrorDepartureToArrival(row);
            }
        }
        // Else: dwell only, or estimated-only — both legal in step 1, nothing to enforce here.
    }

    /**
     * Whether this row's data is sent in the TTT Path Request. Delegates to {@link
     * TttDraftBuilder#isExportedToTtt} — the same rule the actual export uses — so the UI's
     * exported-indicator and the export can never disagree (e.g. a user-marked TTT-relevant row).
     */
    public boolean isExportedToTtt(TimetableRowData row) {
        return TttDraftBuilder.isExportedToTtt(row);
    }

    /** Returns true when at least one arrival-side constraint field has a user-entered value. */
    public boolean hasUserEnteredArrival(TimetableRowData row) {
        return Boolean.TRUE.equals(row.getUserEnteredArrivalExact())
                || Boolean.TRUE.equals(row.getUserEnteredArrivalEarliest())
                || Boolean.TRUE.equals(row.getUserEnteredArrivalLatest())
                || Boolean.TRUE.equals(row.getUserEnteredCommercialArrival());
    }

    public boolean hasUserEnteredDeparture(TimetableRowData row) {
        return Boolean.TRUE.equals(row.getUserEnteredDepartureExact())
                || Boolean.TRUE.equals(row.getUserEnteredDepartureEarliest())
                || Boolean.TRUE.equals(row.getUserEnteredDepartureLatest())
                || Boolean.TRUE.equals(row.getUserEnteredCommercialDeparture());
    }

    private void clearArrivalConstraints(TimetableRowData row) {
        row.setArrivalMode(TimeConstraintMode.NONE);
        row.setArrivalExact(null);
        row.setArrivalEarliest(null);
        row.setArrivalLatest(null);
        row.setCommercialArrival(null);
        row.setUserEnteredArrivalExact(false);
        row.setUserEnteredArrivalEarliest(false);
        row.setUserEnteredArrivalLatest(false);
        row.setUserEnteredCommercialArrival(false);
    }

    private void clearDepartureConstraints(TimetableRowData row) {
        row.setDepartureMode(TimeConstraintMode.NONE);
        row.setDepartureExact(null);
        row.setDepartureEarliest(null);
        row.setDepartureLatest(null);
        row.setCommercialDeparture(null);
        row.setUserEnteredDepartureExact(false);
        row.setUserEnteredDepartureEarliest(false);
        row.setUserEnteredDepartureLatest(false);
        row.setUserEnteredCommercialDeparture(false);
    }

    private void mirrorArrivalToDeparture(TimetableRowData row) {
        Integer dwell = row.getDwellMinutes();
        if (dwell == null || dwell < 0) return;
        TimeConstraintMode mode = row.getArrivalMode();
        if (mode == null) mode = TimeConstraintMode.NONE;
        // Same-mode rule (Regel 5): force departure mode to match arrival.
        row.setDepartureMode(mode);
        switch (mode) {
            case EXACT -> {
                LocalTime t = parseTime(row.getArrivalExact());
                if (t != null) row.setDepartureExact(t.plusMinutes(dwell).format(HH_MM));
            }
            case WINDOW -> {
                LocalTime e = parseTime(row.getArrivalEarliest());
                LocalTime l = parseTime(row.getArrivalLatest());
                if (e != null) row.setDepartureEarliest(e.plusMinutes(dwell).format(HH_MM));
                if (l != null) row.setDepartureLatest(l.plusMinutes(dwell).format(HH_MM));
            }
            case AFTER -> {
                LocalTime e = parseTime(row.getArrivalEarliest());
                if (e != null) row.setDepartureEarliest(e.plusMinutes(dwell).format(HH_MM));
            }
            case BEFORE -> {
                LocalTime l = parseTime(row.getArrivalLatest());
                if (l != null) row.setDepartureLatest(l.plusMinutes(dwell).format(HH_MM));
            }
            case COMMERCIAL -> {
                LocalTime t = parseTime(row.getCommercialArrival());
                if (t != null) row.setCommercialDeparture(t.plusMinutes(dwell).format(HH_MM));
            }
            case NONE -> {
                /* no-op */
            }
        }
    }

    private void mirrorDepartureToArrival(TimetableRowData row) {
        Integer dwell = row.getDwellMinutes();
        if (dwell == null || dwell < 0) return;
        TimeConstraintMode mode = row.getDepartureMode();
        if (mode == null) mode = TimeConstraintMode.NONE;
        row.setArrivalMode(mode);
        switch (mode) {
            case EXACT -> {
                LocalTime t = parseTime(row.getDepartureExact());
                if (t != null) row.setArrivalExact(t.minusMinutes(dwell).format(HH_MM));
            }
            case WINDOW -> {
                LocalTime e = parseTime(row.getDepartureEarliest());
                LocalTime l = parseTime(row.getDepartureLatest());
                if (e != null) row.setArrivalEarliest(e.minusMinutes(dwell).format(HH_MM));
                if (l != null) row.setArrivalLatest(l.minusMinutes(dwell).format(HH_MM));
            }
            case AFTER -> {
                LocalTime e = parseTime(row.getDepartureEarliest());
                if (e != null) row.setArrivalEarliest(e.minusMinutes(dwell).format(HH_MM));
            }
            case BEFORE -> {
                LocalTime l = parseTime(row.getDepartureLatest());
                if (l != null) row.setArrivalLatest(l.minusMinutes(dwell).format(HH_MM));
            }
            case COMMERCIAL -> {
                LocalTime t = parseTime(row.getCommercialDeparture());
                if (t != null) row.setCommercialArrival(t.minusMinutes(dwell).format(HH_MM));
            }
            case NONE -> {
                /* no-op */
            }
        }
    }

    /**
     * Mode-switch preservation (option a from question 3): when the user changes the constraint
     * mode of one side, copy whatever single value was held in the old mode into the new fields, so
     * no data is silently lost.
     */
    public void preserveOnModeSwitch(
            TimetableRowData row,
            boolean arrivalSide,
            TimeConstraintMode oldMode,
            TimeConstraintMode newMode) {
        if (row == null || oldMode == null || newMode == null || oldMode == newMode) return;

        // Source value: pick the most representative time from the old mode.
        LocalTime source =
                arrivalSide
                        ? oldArrivalRepresentative(row, oldMode)
                        : oldDepartureRepresentative(row, oldMode);

        // Wipe the other-mode fields on this side (without touching userEntered flags yet).
        if (arrivalSide) {
            row.setArrivalExact(null);
            row.setArrivalEarliest(null);
            row.setArrivalLatest(null);
            row.setCommercialArrival(null);
        } else {
            row.setDepartureExact(null);
            row.setDepartureEarliest(null);
            row.setDepartureLatest(null);
            row.setCommercialDeparture(null);
        }

        if (source == null || newMode == TimeConstraintMode.NONE) return;
        String formatted = source.format(HH_MM);
        switch (newMode) {
            case EXACT -> {
                if (arrivalSide) row.setArrivalExact(formatted);
                else row.setDepartureExact(formatted);
            }
            case WINDOW -> {
                if (arrivalSide) {
                    row.setArrivalEarliest(formatted);
                    row.setArrivalLatest(formatted);
                } else {
                    row.setDepartureEarliest(formatted);
                    row.setDepartureLatest(formatted);
                }
            }
            case AFTER -> {
                if (arrivalSide) row.setArrivalEarliest(formatted);
                else row.setDepartureEarliest(formatted);
            }
            case BEFORE -> {
                if (arrivalSide) row.setArrivalLatest(formatted);
                else row.setDepartureLatest(formatted);
            }
            case COMMERCIAL -> {
                if (arrivalSide) row.setCommercialArrival(formatted);
                else row.setCommercialDeparture(formatted);
            }
            case NONE -> {
                /* unreachable */
            }
        }
    }

    private LocalTime oldArrivalRepresentative(TimetableRowData row, TimeConstraintMode oldMode) {
        return switch (oldMode) {
            case EXACT -> parseTime(row.getArrivalExact());
            case WINDOW ->
                    firstNonNull(
                            parseTime(row.getArrivalEarliest()), parseTime(row.getArrivalLatest()));
            case AFTER -> parseTime(row.getArrivalEarliest());
            case BEFORE -> parseTime(row.getArrivalLatest());
            case COMMERCIAL -> parseTime(row.getCommercialArrival());
            case NONE -> null;
        };
    }

    private LocalTime oldDepartureRepresentative(TimetableRowData row, TimeConstraintMode oldMode) {
        return switch (oldMode) {
            case EXACT -> parseTime(row.getDepartureExact());
            case WINDOW ->
                    firstNonNull(
                            parseTime(row.getDepartureLatest()),
                            parseTime(row.getDepartureEarliest()));
            case AFTER -> parseTime(row.getDepartureEarliest());
            case BEFORE -> parseTime(row.getDepartureLatest());
            case COMMERCIAL -> parseTime(row.getCommercialDeparture());
            case NONE -> null;
        };
    }

    // ── Dwell-based departure derivation ───────────────────────────────────

    /**
     * If the row is a halt with a positive {@code dwellMinutes}, mirror its arrival anchors to the
     * departure side, offset by the dwell. Only fires when the user hasn't entered explicit
     * departure values yet — a deliberate guard so we never overwrite intent.
     *
     * <p>Mode mirroring (TTT TimingQualifier mapping per ADR-010 / Anlage 1 §5.7):
     *
     * <ul>
     *   <li>EXACT (ALA) + dwell → EXACT (ALD = ALA + dwell)
     *   <li>WINDOW (ELA, LLA) + dwell → WINDOW (ELD = ELA + dwell, LLD = LLA + dwell)
     *   <li>COMMERCIAL (PLA) + dwell → COMMERCIAL (PLD = PLA + dwell)
     * </ul>
     *
     * <p>Also refreshes {@link TimetableRowData#getEstimatedDeparture()} so the propagate
     * snapshot/diff sees the derived departure as a real anchor change.
     *
     * @return {@code true} if the departure side was filled or refreshed, {@code false} otherwise.
     */
    public boolean deriveDepartureFromDwell(TimetableRowData row) {
        if (row == null || !Boolean.TRUE.equals(row.getHalt())) return false;
        Integer dwell = row.getDwellMinutes();
        if (dwell == null || dwell <= 0) return false;

        TimeConstraintMode arrMode = row.getArrivalMode();
        if (arrMode == null || arrMode == TimeConstraintMode.NONE) return false;
        if (!isDepartureEmpty(row)) return false;

        return switch (arrMode) {
            case EXACT -> mirrorExact(row, dwell);
            case WINDOW -> mirrorWindow(row, dwell);
            case AFTER -> mirrorAfter(row, dwell);
            case BEFORE -> mirrorBefore(row, dwell);
            case COMMERCIAL -> mirrorCommercial(row, dwell);
            case NONE -> false;
        };
    }

    private boolean isDepartureEmpty(TimetableRowData row) {
        TimeConstraintMode m = row.getDepartureMode();
        if (m == null || m == TimeConstraintMode.NONE) return true;
        return switch (m) {
            case EXACT -> row.getDepartureExact() == null || row.getDepartureExact().isBlank();
            case WINDOW ->
                    (row.getDepartureEarliest() == null || row.getDepartureEarliest().isBlank())
                            && (row.getDepartureLatest() == null
                                    || row.getDepartureLatest().isBlank());
            case AFTER ->
                    row.getDepartureEarliest() == null || row.getDepartureEarliest().isBlank();
            case BEFORE -> row.getDepartureLatest() == null || row.getDepartureLatest().isBlank();
            case COMMERCIAL ->
                    row.getCommercialDeparture() == null || row.getCommercialDeparture().isBlank();
            case NONE -> true;
        };
    }

    private boolean mirrorExact(TimetableRowData row, int dwell) {
        LocalTime ala = parseTime(row.getArrivalExact());
        if (ala == null) return false;
        row.setDepartureMode(TimeConstraintMode.EXACT);
        row.setDepartureExact(ala.plusMinutes(dwell).format(HH_MM));
        // Don't touch estimatedDeparture: propagate's shift uses it as the OLD anchor reference,
        // and shiftFollowingTimes' first call updates it to match the new effective anchor.
        return true;
    }

    private boolean mirrorWindow(TimetableRowData row, int dwell) {
        LocalTime ela = parseTime(row.getArrivalEarliest());
        LocalTime lla = parseTime(row.getArrivalLatest());
        if (ela == null || lla == null) return false;
        row.setDepartureMode(TimeConstraintMode.WINDOW);
        row.setDepartureEarliest(ela.plusMinutes(dwell).format(HH_MM));
        row.setDepartureLatest(lla.plusMinutes(dwell).format(HH_MM));
        return true;
    }

    private boolean mirrorCommercial(TimetableRowData row, int dwell) {
        LocalTime pla = parseTime(row.getCommercialArrival());
        if (pla == null) return false;
        row.setDepartureMode(TimeConstraintMode.COMMERCIAL);
        row.setCommercialDeparture(pla.plusMinutes(dwell).format(HH_MM));
        return true;
    }

    private boolean mirrorAfter(TimetableRowData row, int dwell) {
        LocalTime ela = parseTime(row.getArrivalEarliest());
        if (ela == null) return false;
        row.setDepartureMode(TimeConstraintMode.AFTER);
        row.setDepartureEarliest(ela.plusMinutes(dwell).format(HH_MM));
        return true;
    }

    private boolean mirrorBefore(TimetableRowData row, int dwell) {
        LocalTime lla = parseTime(row.getArrivalLatest());
        if (lla == null) return false;
        row.setDepartureMode(TimeConstraintMode.BEFORE);
        row.setDepartureLatest(lla.plusMinutes(dwell).format(HH_MM));
        return true;
    }

    // ── Dwell reconciliation ───────────────────────────────────────────────

    /**
     * Reconcile {@code dwellMinutes} with the row's effective arrival and departure on the
     * <em>earliest</em> side (ALA→ALD for EXACT, ELA→ELD for WINDOW, PLA→PLD for COMMERCIAL). Only
     * acts on halt rows where both anchors exist and the delta is non-negative — and only when the
     * existing {@code dwellMinutes} is missing or contradicts the actual delta.
     *
     * <p>Use case: the user types an explicit departure that's further from arrival than the dwell
     * suggests. Without this, the editor claims "5 min Halt" while the times say 30 min.
     *
     * @return {@code true} if {@code dwellMinutes} was changed.
     */
    public boolean reconcileDwell(TimetableRowData row) {
        if (row == null || !Boolean.TRUE.equals(row.getHalt())) return false;
        LocalTime arr = earliestArrivalForDwell(row);
        LocalTime dep = earliestDepartureForDwell(row);
        if (arr == null || dep == null || dep.isBefore(arr)) return false;
        int actual = (int) Duration.between(arr, dep).toMinutes();
        Integer current = row.getDwellMinutes();
        if (current != null && current == actual) return false;
        row.setDwellMinutes(actual);
        return true;
    }

    private LocalTime earliestArrivalForDwell(TimetableRowData row) {
        TimeConstraintMode mode = row.getArrivalMode();
        if (mode == null) mode = TimeConstraintMode.NONE;
        return switch (mode) {
            case EXACT -> parseTime(row.getArrivalExact());
            case WINDOW, AFTER -> parseTime(row.getArrivalEarliest());
            case BEFORE -> parseTime(row.getArrivalLatest());
            case COMMERCIAL -> parseTime(row.getCommercialArrival());
            case NONE -> parseTime(row.getEstimatedArrival());
        };
    }

    private LocalTime earliestDepartureForDwell(TimetableRowData row) {
        TimeConstraintMode mode = row.getDepartureMode();
        if (mode == null) mode = TimeConstraintMode.NONE;
        return switch (mode) {
            case EXACT -> parseTime(row.getDepartureExact());
            case WINDOW, AFTER -> parseTime(row.getDepartureEarliest());
            case BEFORE -> parseTime(row.getDepartureLatest());
            case COMMERCIAL -> parseTime(row.getCommercialDeparture());
            case NONE -> parseTime(row.getEstimatedDeparture());
        };
    }

    // ── Validation ─────────────────────────────────────────────────────────

    /**
     * Returns a list of human-readable consistency violations. Empty list means the timetable is
     * internally consistent; arrivals are ≤ departures within each row, and travel time between
     * consecutive rows is non-negative.
     */
    public List<String> validate(List<TimetableRowData> rows) {
        List<String> errors = new ArrayList<>();
        if (rows == null || rows.isEmpty()) return errors;

        // Regel 9: at least one halt must exist anywhere on the route. ORIGIN and DESTINATION
        // count as implicit halts (the train stops there by definition), so a pure A→B route
        // already satisfies this even without halt=true on intermediate stops.
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

    // ── Backwards-compatible legacy entry points ───────────────────────────

    public LocalTime resolveRelativeTime(String input, LocalTime baseTime) {
        if (input == null || baseTime == null) return null;
        String trimmed = input.trim();
        if (!trimmed.startsWith("+") && !trimmed.startsWith("-")) return null;
        try {
            return baseTime.plusMinutes(Integer.parseInt(trimmed));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ── Internals ──────────────────────────────────────────────────────────

    /**
     * Propagate a delta forwards (towards higher indices). On STRETCH, scale the times between
     * {@code changedIndex} and the next pinned row so the schedule still ends where it ended
     * before. On SHIFT, rigidly translate every row until the first pinned row.
     */
    private void propagateForward(
            List<TimetableRowData> rows,
            int changedIndex,
            long deltaMinutes,
            TimePropagationMode mode,
            LocalTime oldAnchor,
            LocalTime newAnchor) {
        // Regel 7: forward stretch needs a real anchor ahead — either an explicit pin or a
        // user-entered arrival on the destination. Without one, fall back to SHIFT so the
        // schedule isn't compressed against thin air.
        if (mode == TimePropagationMode.STRETCH && !hasForwardAnchor(rows, changedIndex)) {
            shiftFollowingTimes(rows, changedIndex, deltaMinutes);
            return;
        }
        if (mode == TimePropagationMode.STRETCH) {
            stretchToNextPin(rows, changedIndex, oldAnchor, newAnchor);
        } else {
            shiftFollowingTimes(rows, changedIndex, deltaMinutes);
        }
    }

    private boolean hasForwardAnchor(List<TimetableRowData> rows, int from) {
        for (int i = from + 1; i < rows.size(); i++) {
            if (Boolean.TRUE.equals(rows.get(i).getPinned())) return true;
        }
        // Destination acts as an implicit pin only if its arrival was user-entered.
        if (!rows.isEmpty()) {
            TimetableRowData last = rows.get(rows.size() - 1);
            if (hasUserEnteredArrival(last)) return true;
        }
        return false;
    }

    /**
     * Propagate a delta backwards (towards lower indices), symmetric to {@link #propagateForward}.
     */
    private void propagateBackward(
            List<TimetableRowData> rows,
            int changedIndex,
            long deltaMinutes,
            TimePropagationMode mode,
            LocalTime oldAnchor,
            LocalTime newAnchor) {
        if (mode == TimePropagationMode.STRETCH) {
            stretchToPreviousPin(rows, changedIndex, oldAnchor, newAnchor);
        } else {
            shiftPrecedingTimes(rows, changedIndex, deltaMinutes);
        }
    }

    /**
     * Shift every following row's arrival + departure by {@code deltaMinutes}, stopping at first
     * pin.
     */
    private void shiftFollowingTimes(
            List<TimetableRowData> rows, int fromIndex, long deltaMinutes) {
        // The changed row itself is left untouched — its anchors are already at the new value
        // (set by the caller via the editor or dwell derivation), and propagate() will sync the
        // matching estimated* field separately so we don't double-shift.
        for (int i = fromIndex + 1; i < rows.size(); i++) {
            TimetableRowData row = rows.get(i);
            if (Boolean.TRUE.equals(row.getPinned())) break;
            shiftTimeField(row, true, deltaMinutes);
            shiftTimeField(row, false, deltaMinutes);
        }
    }

    /** Symmetric to shiftFollowingTimes: shift arrival + departure of preceding rows. */
    private void shiftPrecedingTimes(
            List<TimetableRowData> rows, int fromIndex, long deltaMinutes) {
        // The changed row's arrival is already set by caller; only its departure may
        // need help if it was previously linked to the arrival.
        for (int i = fromIndex - 1; i >= 0; i--) {
            TimetableRowData row = rows.get(i);
            if (Boolean.TRUE.equals(row.getPinned())) break;
            shiftTimeField(row, true, deltaMinutes);
            shiftTimeField(row, false, deltaMinutes);
        }
    }

    /**
     * Shift every time field on the given side (estimated, exact, ELA/LLA or ELD/LLD, commercial)
     * by {@code deltaMinutes}. Necessary because the side's "anchor" is derived from one of these
     * depending on mode — leaving the unused fields un-shifted would make {@link
     * #effectiveArrivalAnchor} / {@link #effectiveDepartureAnchor} return stale values for
     * neighbour rows in WINDOW or COMMERCIAL mode.
     */
    private void shiftTimeField(TimetableRowData row, boolean isArrival, long deltaMinutes) {
        shiftFieldWithOffsetUpdate(row, isArrival, deltaMinutes);
    }

    private void stretchToNextPin(
            List<TimetableRowData> rows,
            int changedIndex,
            LocalTime oldAnchor,
            LocalTime newAnchor) {
        int pinIndex = findNextPin(rows, changedIndex);
        if (pinIndex <= changedIndex) return;
        TimetableRowData pinRow = rows.get(pinIndex);
        LocalTime pinTime =
                firstNonNull(
                        parseTime(pinRow.getEstimatedArrival()),
                        parseTime(pinRow.getEstimatedDeparture()));
        if (pinTime == null) return;
        long oldSpan = Duration.between(oldAnchor, pinTime).toMinutes();
        long newSpan = Duration.between(newAnchor, pinTime).toMinutes();
        if (oldSpan <= 0 || newSpan <= 0) return;
        double ratio = (double) newSpan / oldSpan;
        for (int i = changedIndex + 1; i < pinIndex; i++) {
            TimetableRowData row = rows.get(i);
            stretchField(row, true, oldAnchor, newAnchor, ratio);
            stretchField(row, false, oldAnchor, newAnchor, ratio);
        }
    }

    private void stretchToPreviousPin(
            List<TimetableRowData> rows,
            int changedIndex,
            LocalTime oldAnchor,
            LocalTime newAnchor) {
        int pinIndex = findPreviousPin(rows, changedIndex);
        if (pinIndex >= changedIndex) return;
        TimetableRowData pinRow = rows.get(pinIndex);
        LocalTime pinTime =
                firstNonNull(
                        parseTime(pinRow.getEstimatedDeparture()),
                        parseTime(pinRow.getEstimatedArrival()));
        if (pinTime == null) return;
        // Span ist negativ (pin vor changedIndex), rechnen mit absoluten Beträgen.
        long oldSpan = Duration.between(pinTime, oldAnchor).toMinutes();
        long newSpan = Duration.between(pinTime, newAnchor).toMinutes();
        if (oldSpan <= 0 || newSpan <= 0) return;
        double ratio = (double) newSpan / oldSpan;
        for (int i = pinIndex + 1; i < changedIndex; i++) {
            TimetableRowData row = rows.get(i);
            scaleOffsetFromAnchor(row, true, pinTime, ratio);
            scaleOffsetFromAnchor(row, false, pinTime, ratio);
        }
    }

    /** Scale every time field's offset from {@code anchor} by {@code ratio}, in place. */
    private void scaleOffsetFromAnchor(
            TimetableRowData row, boolean isArrival, LocalTime anchor, double ratio) {
        for (TimeFieldAccessor f : timeFields(row, isArrival)) {
            LocalTime t = parseTime(f.getter().get());
            if (t == null) continue;
            long offset = Duration.between(anchor, t).toMinutes();
            long newOffset = Math.round(offset * ratio);
            f.setter().accept(anchor.plusMinutes(newOffset).format(HH_MM));
        }
    }

    /**
     * Stretch every time field on the side: rebase from {@code oldAnchor} to {@code newAnchor},
     * scaling its offset by {@code ratio}. Acts on estimated, exact, window and commercial fields
     * so the neighbour rows' effective anchors stay consistent regardless of their mode.
     */
    private void stretchField(
            TimetableRowData row,
            boolean isArrival,
            LocalTime oldAnchor,
            LocalTime newAnchor,
            double ratio) {
        for (TimeFieldAccessor f : timeFields(row, isArrival)) {
            LocalTime t = parseTime(f.getter().get());
            if (t == null) continue;
            long originalOffset = Duration.between(oldAnchor, t).toMinutes();
            long newOffset = Math.round(originalOffset * ratio);
            f.setter().accept(newAnchor.plusMinutes(newOffset).format(HH_MM));
        }
    }

    /** Lightweight getter/setter pair so the shift/stretch loops can iterate over fields. */
    private record TimeFieldAccessor(
            java.util.function.Supplier<String> getter,
            java.util.function.Consumer<String> setter) {}

    /** All time fields for one side of a row (arrival or departure) in iteration order. */
    private List<TimeFieldAccessor> timeFields(TimetableRowData row, boolean isArrival) {
        if (isArrival) {
            return List.of(
                    new TimeFieldAccessor(row::getEstimatedArrival, row::setEstimatedArrival),
                    new TimeFieldAccessor(row::getArrivalExact, row::setArrivalExact),
                    new TimeFieldAccessor(row::getArrivalEarliest, row::setArrivalEarliest),
                    new TimeFieldAccessor(row::getArrivalLatest, row::setArrivalLatest),
                    new TimeFieldAccessor(row::getCommercialArrival, row::setCommercialArrival));
        }
        return List.of(
                new TimeFieldAccessor(row::getEstimatedDeparture, row::setEstimatedDeparture),
                new TimeFieldAccessor(row::getDepartureExact, row::setDepartureExact),
                new TimeFieldAccessor(row::getDepartureEarliest, row::setDepartureEarliest),
                new TimeFieldAccessor(row::getDepartureLatest, row::setDepartureLatest),
                new TimeFieldAccessor(row::getCommercialDeparture, row::setCommercialDeparture));
    }

    private int findNextPin(List<TimetableRowData> rows, int from) {
        for (int i = from + 1; i < rows.size(); i++) {
            if (Boolean.TRUE.equals(rows.get(i).getPinned())) return i;
        }
        return rows.size() - 1;
    }

    private int findPreviousPin(List<TimetableRowData> rows, int from) {
        for (int i = from - 1; i >= 0; i--) {
            if (Boolean.TRUE.equals(rows.get(i).getPinned())) return i;
        }
        return 0;
    }

    private LocalTime parseTime(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalTime.parse(value, HH_MM);
        } catch (Exception e) {
            return null;
        }
    }

    // ── Offset-aware arithmetic helpers ────────────────────────────────────

    private static final long MINUTES_PER_DAY = 1440L;

    /** Convert a (LocalTime, dayOffset) pair into absolute minutes since day 0. */
    private long toAbsoluteMinutes(LocalTime time, int offsetDays) {
        if (time == null) return 0L;
        return offsetDays * MINUTES_PER_DAY + (time.toSecondOfDay() / 60L);
    }

    /** Modular wall-clock time from absolute minutes — wraps within [00:00, 23:59]. */
    private LocalTime wallClock(long absMinutes) {
        long mod = ((absMinutes % MINUTES_PER_DAY) + MINUTES_PER_DAY) % MINUTES_PER_DAY;
        return LocalTime.of((int) (mod / 60), (int) (mod % 60));
    }

    /** Day offset for absolute minutes (negative if before day 0). */
    private int dayOffset(long absMinutes) {
        return (int) Math.floorDiv(absMinutes, MINUTES_PER_DAY);
    }

    /**
     * Apply a minute delta to an HH:mm string and update the side's day offset on the row when the
     * time wraps past midnight (or before day 0). Both the time field and the row's arrivalOffset /
     * departureOffset are mutated in place.
     */
    private void shiftFieldWithOffsetUpdate(
            TimetableRowData row, boolean isArrival, long deltaMinutes) {
        // Compute new offset ONCE per side from the row's main estimated time, since all fields
        // on the same side share the same offset.
        int currentOffset =
                isArrival
                        ? (row.getArrivalOffset() == null ? 0 : row.getArrivalOffset())
                        : (row.getDepartureOffset() == null ? 0 : row.getDepartureOffset());
        // Track the new offset using any one parsed field as reference.
        Integer newOffset = null;
        for (TimeFieldAccessor f : timeFields(row, isArrival)) {
            LocalTime t = parseTime(f.getter().get());
            if (t == null) continue;
            long abs = toAbsoluteMinutes(t, currentOffset) + deltaMinutes;
            f.setter().accept(wallClock(abs).format(HH_MM));
            if (newOffset == null) newOffset = dayOffset(abs);
        }
        if (newOffset != null) {
            if (isArrival) row.setArrivalOffset(newOffset);
            else row.setDepartureOffset(newOffset);
        }
    }

    @SafeVarargs
    private static <T> T firstNonNull(T... values) {
        for (T v : values) if (v != null) return v;
        return null;
    }
}
