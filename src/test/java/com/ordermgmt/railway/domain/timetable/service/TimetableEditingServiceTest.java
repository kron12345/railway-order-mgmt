package com.ordermgmt.railway.domain.timetable.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ordermgmt.railway.domain.infrastructure.repository.OperationalPointRepository;
import com.ordermgmt.railway.domain.timetable.model.RoutePointRole;
import com.ordermgmt.railway.domain.timetable.model.TimeConstraintMode;
import com.ordermgmt.railway.domain.timetable.model.TimePropagationMode;
import com.ordermgmt.railway.domain.timetable.model.TimetableRowData;
import com.ordermgmt.railway.domain.timetable.service.TimetableEditingService.TimeSnapshot;

@ExtendWith(MockitoExtension.class)
class TimetableEditingServiceTest {

    @Mock private OperationalPointRepository operationalPointRepository;

    private TimetableEditingService service;

    @BeforeEach
    void setUp() {
        service = new TimetableEditingService(operationalPointRepository);
    }

    // ── resolveRelativeTime ───────────────────────────────────────────

    @Test
    void resolveRelativeTime_plusOffset_returnsCorrectTime() {
        LocalTime result = service.resolveRelativeTime("+5", LocalTime.of(8, 0));

        assertThat(result).isEqualTo(LocalTime.of(8, 5));
    }

    @Test
    void resolveRelativeTime_minusOffset_returnsCorrectTime() {
        LocalTime result = service.resolveRelativeTime("-3", LocalTime.of(8, 0));

        assertThat(result).isEqualTo(LocalTime.of(7, 57));
    }

    @Test
    void resolveRelativeTime_absoluteTime_returnsNull() {
        LocalTime result = service.resolveRelativeTime("08:30", LocalTime.of(8, 0));

        assertThat(result).isNull();
    }

    @Test
    void resolveRelativeTime_nullInput_returnsNull() {
        assertThat(service.resolveRelativeTime(null, LocalTime.of(8, 0))).isNull();
    }

    @Test
    void resolveRelativeTime_nullBase_returnsNull() {
        assertThat(service.resolveRelativeTime("+5", null)).isNull();
    }

    @Test
    void resolveRelativeTime_invalidFormat_returnsNull() {
        assertThat(service.resolveRelativeTime("+abc", LocalTime.of(8, 0))).isNull();
    }

    // ── resequence ────────────────────────────────────────────────────

    @Test
    void resequence_renumbersCorrectly() {
        List<TimetableRowData> rows = new ArrayList<>();
        rows.add(rowWithSequence(10));
        rows.add(rowWithSequence(20));
        rows.add(rowWithSequence(30));

        service.resequence(rows);

        assertThat(rows.get(0).getSequence()).isEqualTo(1);
        assertThat(rows.get(1).getSequence()).isEqualTo(2);
        assertThat(rows.get(2).getSequence()).isEqualTo(3);
    }

    @Test
    void resequence_emptyList_doesNothing() {
        List<TimetableRowData> rows = new ArrayList<>();
        service.resequence(rows);
        assertThat(rows).isEmpty();
    }

    // ── softDeleteStop ────────────────────────────────────────────────

    @Test
    void softDeleteStop_togglesDeletedFlag() {
        List<TimetableRowData> rows = createThreeStopRoute();

        // Middle stop (VIA) should be deletable
        service.softDeleteStop(rows, 1);
        assertThat(rows.get(1).getDeleted()).isTrue();

        // Toggle back
        service.softDeleteStop(rows, 1);
        assertThat(rows.get(1).getDeleted()).isFalse();
    }

    @Test
    void softDeleteStop_protectsOrigin() {
        List<TimetableRowData> rows = createThreeStopRoute();

        service.softDeleteStop(rows, 0);
        assertThat(rows.get(0).getDeleted()).isFalse();
    }

    @Test
    void softDeleteStop_protectsDestination() {
        List<TimetableRowData> rows = createThreeStopRoute();

        service.softDeleteStop(rows, 2);
        assertThat(rows.get(2).getDeleted()).isFalse();
    }

    @Test
    void softDeleteStop_outOfBounds_doesNothing() {
        List<TimetableRowData> rows = createThreeStopRoute();

        service.softDeleteStop(rows, -1);
        service.softDeleteStop(rows, 99);

        // No exceptions and no changes
        assertThat(rows).allMatch(r -> !Boolean.TRUE.equals(r.getDeleted()));
    }

    // ── Helper methods ────────────────────────────────────────────────

    private TimetableRowData rowWithSequence(int seq) {
        TimetableRowData row = new TimetableRowData();
        row.setSequence(seq);
        row.setName("Stop " + seq);
        row.setRoutePointRole(RoutePointRole.VIA);
        return row;
    }

    // ── Propagation matrix (Phase 4) ──────────────────────────────────
    //
    // Five-row reference route: ORIGIN → VIA → VIA → VIA → DESTINATION
    //   0  Bern  dep 08:00
    //   1  Thun  arr 08:20  dep 08:22
    //   2  Spiez arr 08:35  dep 08:37     ← edits target this row
    //   3  Visp  arr 09:00  dep 09:02
    //   4  Brig  arr 09:15
    //
    // Without explicit pins, ORIGIN serves as the previous pin (backward stretch
    // anchor) and DESTINATION as the next pin (forward stretch anchor).

    @Test
    void propagate_exactArrival_shift_pushesPrecedingRowsBackwards() {
        List<TimetableRowData> rows = createFiveStopRoute();
        TimeSnapshot before = service.snapshot(rows.get(2));

        // User sets EXACT arrival 08:40 on Spiez (was 08:35 → +5 min)
        TimetableRowData spiez = rows.get(2);
        spiez.setArrivalMode(TimeConstraintMode.EXACT);
        spiez.setArrivalExact("08:40");
        spiez.setEstimatedArrival("08:40");

        service.propagate(rows, 2, before, TimePropagationMode.SHIFT);

        // Origin departure shifted by +5
        assertThat(rows.get(0).getEstimatedDeparture()).isEqualTo("08:05");
        // Thun arrival/departure shifted by +5
        assertThat(rows.get(1).getEstimatedArrival()).isEqualTo("08:25");
        assertThat(rows.get(1).getEstimatedDeparture()).isEqualTo("08:27");
        // Following rows untouched (only departure side propagates forward)
        assertThat(rows.get(3).getEstimatedArrival()).isEqualTo("09:00");
    }

    @Test
    void propagate_exactArrival_stretch_scalesPrecedingTravelToOrigin() {
        List<TimetableRowData> rows = createFiveStopRoute();
        TimeSnapshot before = service.snapshot(rows.get(2));

        TimetableRowData spiez = rows.get(2);
        spiez.setArrivalMode(TimeConstraintMode.EXACT);
        spiez.setArrivalExact("08:40");
        spiez.setEstimatedArrival("08:40");

        service.propagate(rows, 2, before, TimePropagationMode.STRETCH);

        // Origin (pin) stays put
        assertThat(rows.get(0).getEstimatedDeparture()).isEqualTo("08:00");
        // Thun: offsets from origin (20, 22 min) scaled by 40/35 ≈ 1.143
        //   arrival  20 * 1.143 = 22.86 → 23 → 08:23
        //   departure 22 * 1.143 = 25.14 → 25 → 08:25
        assertThat(rows.get(1).getEstimatedArrival()).isEqualTo("08:23");
        assertThat(rows.get(1).getEstimatedDeparture()).isEqualTo("08:25");
    }

    @Test
    void propagate_exactDeparture_shift_pushesFollowingRowsForward() {
        List<TimetableRowData> rows = createFiveStopRoute();
        TimeSnapshot before = service.snapshot(rows.get(2));

        TimetableRowData spiez = rows.get(2);
        spiez.setDepartureMode(TimeConstraintMode.EXACT);
        spiez.setDepartureExact("08:42");
        spiez.setEstimatedDeparture("08:42");

        service.propagate(rows, 2, before, TimePropagationMode.SHIFT);

        // Visp shifted by +5
        assertThat(rows.get(3).getEstimatedArrival()).isEqualTo("09:05");
        assertThat(rows.get(3).getEstimatedDeparture()).isEqualTo("09:07");
        // Brig (destination) shifted by +5
        assertThat(rows.get(4).getEstimatedArrival()).isEqualTo("09:20");
        // Preceding rows untouched
        assertThat(rows.get(1).getEstimatedArrival()).isEqualTo("08:20");
    }

    @Test
    void propagate_exactDeparture_stretch_compressesFollowingTravelToDestination() {
        List<TimetableRowData> rows = createFiveStopRoute();
        // Pin Brig so STRETCH has a real forward anchor (Regel 7: stretch needs a pin or
        // user-entered arrival on destination, otherwise it falls back to SHIFT).
        rows.get(4).setPinned(true);
        TimeSnapshot before = service.snapshot(rows.get(2));

        // Spiez departs 5 min later: 08:37 → 08:42, but Brig still arrives 09:15
        TimetableRowData spiez = rows.get(2);
        spiez.setDepartureMode(TimeConstraintMode.EXACT);
        spiez.setDepartureExact("08:42");
        spiez.setEstimatedDeparture("08:42");

        service.propagate(rows, 2, before, TimePropagationMode.STRETCH);

        // Visp offsets shifted+scaled: old anchor 08:37 → new 08:42
        //   arrival 09:00 was at offset +23 from old anchor
        //     new offset = round(23 * 33/38) = round(19.97) = 20 → 08:42 + 20 = 09:02
        //   departure 09:02 was at offset +25
        //     new offset = round(25 * 33/38) = round(21.71) = 22 → 09:04
        assertThat(rows.get(3).getEstimatedArrival()).isEqualTo("09:02");
        assertThat(rows.get(3).getEstimatedDeparture()).isEqualTo("09:04");
        // Brig (pin) stays at 09:15
        assertThat(rows.get(4).getEstimatedArrival()).isEqualTo("09:15");
    }

    @Test
    void propagate_forwardStretch_withoutPinOrExplicitDestArrival_fallsBackToShift() {
        // Regel 7 regression: destination has only an estimated arrival (no user-entered),
        // so STRETCH must degrade to SHIFT or the schedule gets stretched against thin air.
        List<TimetableRowData> rows = createFiveStopRoute();
        TimeSnapshot before = service.snapshot(rows.get(2));

        TimetableRowData spiez = rows.get(2);
        spiez.setDepartureMode(TimeConstraintMode.EXACT);
        spiez.setDepartureExact("08:42");
        spiez.setEstimatedDeparture("08:42");

        service.propagate(rows, 2, before, TimePropagationMode.STRETCH);

        // Falls back to SHIFT: every following row shifted by +5
        assertThat(rows.get(3).getEstimatedArrival()).isEqualTo("09:05");
        assertThat(rows.get(4).getEstimatedArrival()).isEqualTo("09:20");
    }

    @Test
    void propagate_windowEarliestArrival_actsAsBackwardAnchor() {
        List<TimetableRowData> rows = createFiveStopRoute();
        // Switch row 2 to WINDOW mode for arrival
        TimetableRowData spiez = rows.get(2);
        spiez.setArrivalMode(TimeConstraintMode.WINDOW);
        spiez.setArrivalEarliest("08:35");
        spiez.setArrivalLatest("08:40");

        TimeSnapshot before = service.snapshot(spiez);
        // User pushes earliest arrival to 08:40
        spiez.setArrivalEarliest("08:40");

        service.propagate(rows, 2, before, TimePropagationMode.SHIFT);

        // Same effect as EXACT-arrival shift: preceding rows +5
        assertThat(rows.get(0).getEstimatedDeparture()).isEqualTo("08:05");
        assertThat(rows.get(1).getEstimatedArrival()).isEqualTo("08:25");
    }

    @Test
    void propagate_windowLatestDeparture_actsAsForwardAnchor() {
        List<TimetableRowData> rows = createFiveStopRoute();
        TimetableRowData spiez = rows.get(2);
        spiez.setDepartureMode(TimeConstraintMode.WINDOW);
        spiez.setDepartureEarliest("08:35");
        spiez.setDepartureLatest("08:37");

        TimeSnapshot before = service.snapshot(spiez);
        // User pushes latest departure to 08:42
        spiez.setDepartureLatest("08:42");

        service.propagate(rows, 2, before, TimePropagationMode.SHIFT);

        assertThat(rows.get(3).getEstimatedArrival()).isEqualTo("09:05");
        assertThat(rows.get(4).getEstimatedArrival()).isEqualTo("09:20");
    }

    @Test
    void propagate_windowBothSides_propagatesBothDirections() {
        List<TimetableRowData> rows = createFiveStopRoute();
        TimetableRowData spiez = rows.get(2);
        spiez.setArrivalMode(TimeConstraintMode.WINDOW);
        spiez.setArrivalEarliest("08:35");
        spiez.setArrivalLatest("08:40");
        spiez.setDepartureMode(TimeConstraintMode.WINDOW);
        spiez.setDepartureEarliest("08:35");
        spiez.setDepartureLatest("08:37");

        TimeSnapshot before = service.snapshot(spiez);
        spiez.setArrivalEarliest("08:40");
        spiez.setDepartureLatest("08:42");

        service.propagate(rows, 2, before, TimePropagationMode.SHIFT);

        // Backward
        assertThat(rows.get(1).getEstimatedArrival()).isEqualTo("08:25");
        // Forward
        assertThat(rows.get(3).getEstimatedDeparture()).isEqualTo("09:07");
    }

    @Test
    void propagate_noneMode_withoutEstimateChange_isNoOp() {
        List<TimetableRowData> rows = createFiveStopRoute();
        TimetableRowData spiez = rows.get(2);
        TimeSnapshot before = service.snapshot(spiez);

        // No edits; propagate should not move any row
        service.propagate(rows, 2, before, TimePropagationMode.SHIFT);

        assertThat(rows.get(0).getEstimatedDeparture()).isEqualTo("08:00");
        assertThat(rows.get(1).getEstimatedArrival()).isEqualTo("08:20");
        assertThat(rows.get(3).getEstimatedDeparture()).isEqualTo("09:02");
        assertThat(rows.get(4).getEstimatedArrival()).isEqualTo("09:15");
    }

    // ── insertStop with explicit times (no default dwell) ─────────────

    @Test
    void insertStop_fittingBetweenNeighbours_doesNotShiftThem() {
        List<TimetableRowData> rows = createFiveStopRoute();
        // Insert between Thun (idx 1, dep 08:22) and Spiez (idx 2, arr 08:35)
        TimetableRowData newRow = stop("Burgdorf", "08:30", "08:32");
        service.insertStop(rows, 2, newRow, TimePropagationMode.SHIFT);

        // Neighbours untouched
        assertThat(rows.get(1).getEstimatedDeparture()).isEqualTo("08:22");
        assertThat(rows.get(3).getEstimatedArrival()).isEqualTo("08:35");
        // New row is at index 2
        assertThat(rows.get(2).getName()).isEqualTo("Burgdorf");
        // Resequenced
        assertThat(rows.get(2).getSequence()).isEqualTo(3);
    }

    @Test
    void insertStop_arrivalEarlierThanPreviousDeparture_shiftsPrecedingBackwards() {
        List<TimetableRowData> rows = createFiveStopRoute();
        // Insert with arrival 08:18 — earlier than Thun's departure 08:22 (delta -4)
        TimetableRowData newRow = stop("ZwischenStop", "08:18", "08:20");
        service.insertStop(rows, 2, newRow, TimePropagationMode.SHIFT);

        // Bern + Thun pulled back by 4
        assertThat(rows.get(0).getEstimatedDeparture()).isEqualTo("07:56");
        assertThat(rows.get(1).getEstimatedArrival()).isEqualTo("08:16");
        assertThat(rows.get(1).getEstimatedDeparture()).isEqualTo("08:18");
    }

    @Test
    void insertStop_departureLaterThanNextArrival_shiftsFollowingForward() {
        List<TimetableRowData> rows = createFiveStopRoute();
        // Insert with departure 08:40 — later than Spiez arr 08:35 (delta +5)
        TimetableRowData newRow = stop("ZwischenStop", "08:30", "08:40");
        service.insertStop(rows, 2, newRow, TimePropagationMode.SHIFT);

        // Spiez (now idx 3) and following pushed by +5
        assertThat(rows.get(3).getEstimatedArrival()).isEqualTo("08:40");
        assertThat(rows.get(3).getEstimatedDeparture()).isEqualTo("08:42");
        assertThat(rows.get(4).getEstimatedArrival()).isEqualTo("09:05");
    }

    // ── deriveDepartureFromDwell ──────────────────────────────────────

    @Test
    void deriveDepartureFromDwell_windowArrival_mirrorsToWindowDepartureWithDwell() {
        TimetableRowData row = haltRow();
        row.setArrivalMode(TimeConstraintMode.WINDOW);
        row.setArrivalEarliest("08:30");
        row.setArrivalLatest("08:35");
        row.setDwellMinutes(5);

        boolean changed = service.deriveDepartureFromDwell(row);

        assertThat(changed).isTrue();
        assertThat(row.getDepartureMode()).isEqualTo(TimeConstraintMode.WINDOW);
        assertThat(row.getDepartureEarliest()).isEqualTo("08:35");
        assertThat(row.getDepartureLatest()).isEqualTo("08:40");
    }

    @Test
    void deriveDepartureFromDwell_exactArrival_mirrorsToExactDeparture() {
        TimetableRowData row = haltRow();
        row.setArrivalMode(TimeConstraintMode.EXACT);
        row.setArrivalExact("08:30");
        row.setDwellMinutes(3);

        boolean changed = service.deriveDepartureFromDwell(row);

        assertThat(changed).isTrue();
        assertThat(row.getDepartureMode()).isEqualTo(TimeConstraintMode.EXACT);
        assertThat(row.getDepartureExact()).isEqualTo("08:33");
    }

    @Test
    void deriveDepartureFromDwell_commercialArrival_mirrorsToCommercialDeparture() {
        TimetableRowData row = haltRow();
        row.setArrivalMode(TimeConstraintMode.COMMERCIAL);
        row.setCommercialArrival("12:00");
        row.setDwellMinutes(2);

        boolean changed = service.deriveDepartureFromDwell(row);

        assertThat(changed).isTrue();
        assertThat(row.getDepartureMode()).isEqualTo(TimeConstraintMode.COMMERCIAL);
        assertThat(row.getCommercialDeparture()).isEqualTo("12:02");
    }

    @Test
    void deriveDepartureFromDwell_doesNotOverrideExplicitDeparture() {
        TimetableRowData row = haltRow();
        row.setArrivalMode(TimeConstraintMode.EXACT);
        row.setArrivalExact("08:30");
        row.setDwellMinutes(5);
        // User already entered an explicit departure
        row.setDepartureMode(TimeConstraintMode.EXACT);
        row.setDepartureExact("08:42");

        boolean changed = service.deriveDepartureFromDwell(row);

        assertThat(changed).isFalse();
        assertThat(row.getDepartureExact()).isEqualTo("08:42");
    }

    @Test
    void deriveDepartureFromDwell_skipsWhenNotHalt() {
        TimetableRowData row = haltRow();
        row.setHalt(false);
        row.setArrivalMode(TimeConstraintMode.EXACT);
        row.setArrivalExact("08:30");
        row.setDwellMinutes(5);

        assertThat(service.deriveDepartureFromDwell(row)).isFalse();
    }

    @Test
    void deriveDepartureFromDwell_skipsWhenDwellNullOrZero() {
        TimetableRowData row = haltRow();
        row.setArrivalMode(TimeConstraintMode.EXACT);
        row.setArrivalExact("08:30");
        row.setDwellMinutes(null);
        assertThat(service.deriveDepartureFromDwell(row)).isFalse();
        row.setDwellMinutes(0);
        assertThat(service.deriveDepartureFromDwell(row)).isFalse();
    }

    @Test
    void propagate_forward_shiftsAllTimeFieldsOnWindowNeighbour() {
        // Regression: previously shiftTimeField only touched estimated + exact, leaving
        // ELA/LLA on a WINDOW-mode neighbour stale. validate() then complained about
        // negative travel time even though the user expected propagation to fix it.
        List<TimetableRowData> rows = createFiveStopRoute();
        // Make Visp (idx 3) a WINDOW row with concrete earliest/latest values
        TimetableRowData visp = rows.get(3);
        visp.setArrivalMode(TimeConstraintMode.WINDOW);
        visp.setArrivalEarliest("09:00");
        visp.setArrivalLatest("09:05");

        // Spiez (idx 2) gets a +5min EXACT-departure edit
        TimetableRowData spiez = rows.get(2);
        TimeSnapshot before = service.snapshot(spiez);
        spiez.setDepartureMode(TimeConstraintMode.EXACT);
        spiez.setDepartureExact("08:42");
        spiez.setEstimatedDeparture("08:42");

        service.propagate(rows, 2, before, TimePropagationMode.SHIFT);

        // All Visp time fields shifted by +5
        assertThat(visp.getEstimatedArrival()).isEqualTo("09:05");
        assertThat(visp.getArrivalEarliest()).isEqualTo("09:05");
        assertThat(visp.getArrivalLatest()).isEqualTo("09:10");
        // Validator must now be clean — no negative travel time
        assertThat(service.validate(rows)).isEmpty();
    }

    @Test
    void deriveDepartureFromDwell_thenPropagateForward_shiftsFollowingRows() {
        // End-to-end: user puts WINDOW arrival + 5min dwell on Spiez (idx 2),
        // service derives departure window 08:35–08:40. Forward propagation must
        // detect the new LLD anchor (08:40) vs the old estimatedDeparture (08:37)
        // and shift Visp + Brig by +3 minutes.
        List<TimetableRowData> rows = createFiveStopRoute();
        TimetableRowData spiez = rows.get(2);
        TimeSnapshot before = service.snapshot(spiez);

        spiez.setHalt(true);
        spiez.setArrivalMode(TimeConstraintMode.WINDOW);
        spiez.setArrivalEarliest("08:30");
        spiez.setArrivalLatest("08:35");
        spiez.setDwellMinutes(5);
        // Departure stays empty — derive it
        spiez.setDepartureMode(TimeConstraintMode.NONE);

        service.deriveDepartureFromDwell(spiez);
        service.propagate(rows, 2, before, TimePropagationMode.SHIFT);

        assertThat(spiez.getDepartureLatest()).isEqualTo("08:40");
        // Visp shifted by +3 (08:40 - 08:37)
        assertThat(rows.get(3).getEstimatedArrival()).isEqualTo("09:03");
        assertThat(rows.get(3).getEstimatedDeparture()).isEqualTo("09:05");
        assertThat(rows.get(4).getEstimatedArrival()).isEqualTo("09:18");
    }

    @Test
    void reconcileDwell_explicitDeparture_overridesDwellWithActualDelta() {
        // User typed dwell=5 first, then changed departure 06:00 (= actually 30min from arrival)
        TimetableRowData row = haltRow();
        row.setArrivalMode(TimeConstraintMode.EXACT);
        row.setArrivalExact("05:30");
        row.setDepartureMode(TimeConstraintMode.EXACT);
        row.setDepartureExact("06:00");
        row.setDwellMinutes(5);

        boolean changed = service.reconcileDwell(row);

        assertThat(changed).isTrue();
        assertThat(row.getDwellMinutes()).isEqualTo(30);
    }

    @Test
    void reconcileDwell_consistentValues_doesNothing() {
        TimetableRowData row = haltRow();
        row.setArrivalMode(TimeConstraintMode.WINDOW);
        row.setArrivalEarliest("08:30");
        row.setArrivalLatest("08:35");
        row.setDepartureMode(TimeConstraintMode.WINDOW);
        row.setDepartureEarliest("08:35");
        row.setDepartureLatest("08:40");
        row.setDwellMinutes(5);

        assertThat(service.reconcileDwell(row)).isFalse();
        assertThat(row.getDwellMinutes()).isEqualTo(5);
    }

    @Test
    void reconcileDwell_skipsWhenNotHalt() {
        TimetableRowData row = haltRow();
        row.setHalt(false);
        row.setArrivalMode(TimeConstraintMode.EXACT);
        row.setArrivalExact("05:30");
        row.setDepartureMode(TimeConstraintMode.EXACT);
        row.setDepartureExact("06:00");
        row.setDwellMinutes(null);

        assertThat(service.reconcileDwell(row)).isFalse();
    }

    // ── New rule-set tests (Regeln 1-7 + edge cases) ──────────────────

    @Test
    void applyHaltRules_afterArrivalPlusDwell_mirrorsToAfterDeparture() {
        TimetableRowData row = haltRow();
        row.setArrivalMode(TimeConstraintMode.AFTER);
        row.setArrivalEarliest("08:00"); // ≥ 08:00
        row.setUserEnteredArrivalEarliest(true);
        row.setDwellMinutes(5);
        row.setUserEnteredDwell(true);

        service.applyHaltRules(row);

        // Mirror: ELD = ELA + dwell, no LLD
        assertThat(row.getDepartureMode()).isEqualTo(TimeConstraintMode.AFTER);
        assertThat(row.getDepartureEarliest()).isEqualTo("08:05");
        assertThat(row.getDepartureLatest()).isNull();
    }

    @Test
    void applyHaltRules_beforeArrivalPlusDwell_mirrorsToBeforeDeparture() {
        TimetableRowData row = haltRow();
        row.setArrivalMode(TimeConstraintMode.BEFORE);
        row.setArrivalLatest("08:30"); // ≤ 08:30
        row.setUserEnteredArrivalLatest(true);
        row.setDwellMinutes(5);
        row.setUserEnteredDwell(true);

        service.applyHaltRules(row);

        assertThat(row.getDepartureMode()).isEqualTo(TimeConstraintMode.BEFORE);
        assertThat(row.getDepartureLatest()).isEqualTo("08:35");
        assertThat(row.getDepartureEarliest()).isNull();
    }

    @Test
    void effectiveArrivalAnchor_afterMode_returnsELA() {
        TimetableRowData row = haltRow();
        row.setArrivalMode(TimeConstraintMode.AFTER);
        row.setArrivalEarliest("08:00");

        assertThat(service.effectiveArrivalAnchor(row)).isEqualTo(LocalTime.of(8, 0));
    }

    @Test
    void effectiveArrivalAnchor_beforeMode_returnsLLA() {
        TimetableRowData row = haltRow();
        row.setArrivalMode(TimeConstraintMode.BEFORE);
        row.setArrivalLatest("08:30");

        assertThat(service.effectiveArrivalAnchor(row)).isEqualTo(LocalTime.of(8, 30));
    }

    @Test
    void effectiveDepartureAnchor_beforeMode_returnsLLD() {
        TimetableRowData row = haltRow();
        row.setDepartureMode(TimeConstraintMode.BEFORE);
        row.setDepartureLatest("11:00");

        assertThat(service.effectiveDepartureAnchor(row)).isEqualTo(LocalTime.of(11, 0));
    }

    @Test
    void effectiveDepartureAnchor_afterMode_returnsELD() {
        TimetableRowData row = haltRow();
        row.setDepartureMode(TimeConstraintMode.AFTER);
        row.setDepartureEarliest("10:00");

        assertThat(service.effectiveDepartureAnchor(row)).isEqualTo(LocalTime.of(10, 0));
    }

    @Test
    void applyHaltRules_origin_stripsArrivalAndDwell() {
        TimetableRowData row = new TimetableRowData();
        row.setName("A");
        row.setRoutePointRole(RoutePointRole.ORIGIN);
        // User accidentally typed arrival on origin — must be wiped
        row.setArrivalMode(TimeConstraintMode.EXACT);
        row.setArrivalExact("08:00");
        row.setUserEnteredArrivalExact(true);
        row.setDwellMinutes(5);
        row.setUserEnteredDwell(true);
        // Departure is the legitimate side — must be preserved
        row.setDepartureMode(TimeConstraintMode.EXACT);
        row.setDepartureExact("08:30");
        row.setUserEnteredDepartureExact(true);

        service.applyHaltRules(row);

        assertThat(row.getHalt()).isTrue();
        assertThat(row.getArrivalExact()).isNull();
        assertThat(row.getDwellMinutes()).isNull();
        assertThat(row.getDepartureExact()).isEqualTo("08:30");
    }

    @Test
    void applyHaltRules_destination_stripsDepartureAndDwell() {
        TimetableRowData row = new TimetableRowData();
        row.setName("Z");
        row.setRoutePointRole(RoutePointRole.DESTINATION);
        row.setDepartureMode(TimeConstraintMode.EXACT);
        row.setDepartureExact("17:00");
        row.setUserEnteredDepartureExact(true);
        row.setDwellMinutes(3);
        row.setUserEnteredDwell(true);
        row.setArrivalMode(TimeConstraintMode.EXACT);
        row.setArrivalExact("16:30");
        row.setUserEnteredArrivalExact(true);

        service.applyHaltRules(row);

        assertThat(row.getHalt()).isTrue();
        assertThat(row.getDepartureExact()).isNull();
        assertThat(row.getDwellMinutes()).isNull();
        assertThat(row.getArrivalExact()).isEqualTo("16:30");
    }

    @Test
    void applyHaltRules_haltFalse_stripsAllConstraintFieldsAndDwell() {
        TimetableRowData row = haltRow();
        row.setHalt(false);
        row.setArrivalMode(TimeConstraintMode.EXACT);
        row.setArrivalExact("08:00");
        row.setUserEnteredArrivalExact(true);
        row.setDwellMinutes(5);
        row.setUserEnteredDwell(true);

        service.applyHaltRules(row);

        assertThat(row.getArrivalMode()).isEqualTo(TimeConstraintMode.NONE);
        assertThat(row.getArrivalExact()).isNull();
        assertThat(row.getDwellMinutes()).isNull();
        assertThat(row.getUserEnteredArrivalExact()).isFalse();
        assertThat(row.getUserEnteredDwell()).isFalse();
    }

    @Test
    void applyHaltRules_dwellPlusArrival_mirrorsToDeparture_sameMode() {
        TimetableRowData row = haltRow();
        row.setArrivalMode(TimeConstraintMode.WINDOW);
        row.setArrivalEarliest("08:30");
        row.setArrivalLatest("08:35");
        row.setUserEnteredArrivalEarliest(true);
        row.setUserEnteredArrivalLatest(true);
        row.setDwellMinutes(5);
        row.setUserEnteredDwell(true);

        service.applyHaltRules(row);

        assertThat(row.getDepartureMode()).isEqualTo(TimeConstraintMode.WINDOW);
        assertThat(row.getDepartureEarliest()).isEqualTo("08:35");
        assertThat(row.getDepartureLatest()).isEqualTo("08:40");
        // Departure side is derived, not user-entered → no flags
        assertThat(row.getUserEnteredDepartureEarliest()).isFalse();
    }

    @Test
    void applyHaltRules_bothSidesEntered_dropsDwell() {
        TimetableRowData row = haltRow();
        row.setArrivalMode(TimeConstraintMode.EXACT);
        row.setArrivalExact("08:00");
        row.setUserEnteredArrivalExact(true);
        row.setDepartureMode(TimeConstraintMode.EXACT);
        row.setDepartureExact("08:30");
        row.setUserEnteredDepartureExact(true);
        row.setDwellMinutes(5);
        row.setUserEnteredDwell(true);

        service.applyHaltRules(row);

        assertThat(row.getDwellMinutes()).isNull();
        assertThat(row.getUserEnteredDwell()).isFalse();
        // Constraints preserved on both sides
        assertThat(row.getArrivalExact()).isEqualTo("08:00");
        assertThat(row.getDepartureExact()).isEqualTo("08:30");
    }

    @Test
    void preserveOnModeSwitch_exactToWindow_copiesValueIntoBothFields() {
        TimetableRowData row = haltRow();
        row.setArrivalMode(TimeConstraintMode.EXACT);
        row.setArrivalExact("08:30");

        service.preserveOnModeSwitch(
                row, true, TimeConstraintMode.EXACT, TimeConstraintMode.WINDOW);

        assertThat(row.getArrivalExact()).isNull();
        assertThat(row.getArrivalEarliest()).isEqualTo("08:30");
        assertThat(row.getArrivalLatest()).isEqualTo("08:30");
    }

    @Test
    void preserveOnModeSwitch_windowToExact_copiesEarliestAsExact() {
        TimetableRowData row = haltRow();
        row.setArrivalMode(TimeConstraintMode.WINDOW);
        row.setArrivalEarliest("08:30");
        row.setArrivalLatest("08:35");

        service.preserveOnModeSwitch(
                row, true, TimeConstraintMode.WINDOW, TimeConstraintMode.EXACT);

        assertThat(row.getArrivalEarliest()).isNull();
        assertThat(row.getArrivalLatest()).isNull();
        assertThat(row.getArrivalExact()).isEqualTo("08:30");
    }

    @Test
    void interpolateBetweenAnchors_windowArrivalPlusDwell_forwardWalkUsesLLD() {
        // Regression: halt with WINDOW arrival + dwell → applyHaltRules mirrors departure to
        // (ELD, LLD). Forward-walking the next row must start from LLD (latest possible
        // departure), not ELA. Otherwise position 12 ends up at the same time as ELA at 11.
        List<TimetableRowData> rows = new ArrayList<>();
        TimetableRowData origin = new TimetableRowData();
        origin.setName("A");
        origin.setRoutePointRole(RoutePointRole.ORIGIN);
        origin.setDistanceFromStartMeters(0.0);
        rows.add(origin);

        TimetableRowData mid = new TimetableRowData();
        mid.setName("Mid");
        mid.setRoutePointRole(RoutePointRole.VIA);
        mid.setDistanceFromStartMeters(35_000.0);
        mid.setHalt(true);
        mid.setArrivalMode(TimeConstraintMode.WINDOW);
        mid.setArrivalEarliest("06:00");
        mid.setArrivalLatest("06:30");
        mid.setUserEnteredArrivalEarliest(true);
        mid.setUserEnteredArrivalLatest(true);
        mid.setDwellMinutes(5);
        mid.setUserEnteredDwell(true);
        rows.add(mid);

        TimetableRowData dest = new TimetableRowData();
        dest.setName("B");
        dest.setRoutePointRole(RoutePointRole.DESTINATION);
        dest.setDistanceFromStartMeters(70_000.0);
        rows.add(dest);

        // Mirror dwell → (ELD, LLD), then interpolate
        service.applyHaltRules(mid);
        service.interpolateBetweenAnchors(rows);

        // Mid LLD = 06:35; B is 35km away at 70 km/h = 30 min → B estimated arrival 07:05
        assertThat(mid.getDepartureLatest()).isEqualTo("06:35");
        assertThat(dest.getEstimatedArrival()).isEqualTo("07:05");
    }

    @Test
    void interpolateBetweenAnchors_singleAnchor_walksBackwardAtDefaultSpeed() {
        // Only ONE user-entered anchor (ELA on the destination). With 70 km/h default the
        // origin departure must be back-computed; otherwise the user has no estimated start.
        List<TimetableRowData> rows = new ArrayList<>();
        TimetableRowData origin = new TimetableRowData();
        origin.setName("A");
        origin.setRoutePointRole(RoutePointRole.ORIGIN);
        origin.setDistanceFromStartMeters(0.0);
        rows.add(origin);

        TimetableRowData dest = new TimetableRowData();
        dest.setName("B");
        dest.setRoutePointRole(RoutePointRole.DESTINATION);
        dest.setDistanceFromStartMeters(70_000.0); // 70 km
        dest.setEstimatedArrival("09:00");
        dest.setArrivalMode(TimeConstraintMode.WINDOW);
        dest.setArrivalEarliest("09:00");
        dest.setArrivalLatest("09:05");
        dest.setUserEnteredArrivalEarliest(true);
        dest.setUserEnteredArrivalLatest(true);
        rows.add(dest);

        service.interpolateBetweenAnchors(rows);

        // 70 km @ 70 km/h = 60 min back from 09:00 → origin departure 08:00
        assertThat(origin.getEstimatedDeparture()).isEqualTo("08:00");
    }

    @Test
    void interpolateBetweenAnchors_segmentalSpeed_fillsIntermediateRows() {
        // Three rows: A (origin, dep 08:00, distance 0) → B (via, distance 60km) → C (dest, arr
        // 09:00, distance 120km)
        // Average speed: 120km / 60min = 120 km/h. B's interpolated arrival = 08:30.
        List<TimetableRowData> rows = new ArrayList<>();
        TimetableRowData a = new TimetableRowData();
        a.setName("A");
        a.setRoutePointRole(RoutePointRole.ORIGIN);
        a.setDistanceFromStartMeters(0.0);
        a.setEstimatedDeparture("08:00");
        a.setDepartureMode(TimeConstraintMode.EXACT);
        a.setDepartureExact("08:00");
        a.setUserEnteredDepartureExact(true);
        rows.add(a);

        TimetableRowData b = new TimetableRowData();
        b.setName("B");
        b.setRoutePointRole(RoutePointRole.VIA);
        b.setDistanceFromStartMeters(60_000.0);
        rows.add(b);

        TimetableRowData c = new TimetableRowData();
        c.setName("C");
        c.setRoutePointRole(RoutePointRole.DESTINATION);
        c.setDistanceFromStartMeters(120_000.0);
        c.setEstimatedArrival("09:00");
        c.setArrivalMode(TimeConstraintMode.EXACT);
        c.setArrivalExact("09:00");
        c.setUserEnteredArrivalExact(true);
        rows.add(c);

        service.interpolateBetweenAnchors(rows);

        assertThat(b.getEstimatedArrival()).isEqualTo("08:30");
        assertThat(b.getEstimatedDeparture()).isEqualTo("08:30");
    }

    @Test
    void propagate_forwardShift_acrossMidnight_updatesDayOffset() {
        // Two-row route, departure shift pushes following row past midnight.
        List<TimetableRowData> rows = new ArrayList<>();
        TimetableRowData first = new TimetableRowData();
        first.setName("A");
        first.setRoutePointRole(RoutePointRole.ORIGIN);
        first.setEstimatedDeparture("23:00");
        first.setDepartureMode(TimeConstraintMode.EXACT);
        first.setDepartureExact("23:00");
        rows.add(first);

        TimetableRowData second = new TimetableRowData();
        second.setName("B");
        second.setRoutePointRole(RoutePointRole.DESTINATION);
        second.setEstimatedArrival("23:55");
        rows.add(second);

        // Shift A's departure to 23:50 (→ +50min). B's arrival 23:55 → 00:45 next day.
        TimeSnapshot before = service.snapshot(first);
        first.setDepartureExact("23:50");
        first.setEstimatedDeparture("23:50");
        first.setUserEnteredDepartureExact(true);

        service.propagate(rows, 0, before, TimePropagationMode.SHIFT);

        assertThat(second.getEstimatedArrival()).isEqualTo("00:45");
        assertThat(second.getArrivalOffset()).isEqualTo(1);
    }

    @Test
    void validate_passThroughWithExplicitTime_reportsError() {
        List<TimetableRowData> rows = createFiveStopRoute();
        TimetableRowData spiez = rows.get(2);
        spiez.setHalt(false); // pass-through
        spiez.setArrivalMode(TimeConstraintMode.EXACT);
        spiez.setArrivalExact("08:35");
        spiez.setUserEnteredArrivalExact(true);

        List<String> errors = service.validate(rows);

        assertThat(errors).anySatisfy(e -> assertThat(e).contains("Spiez").contains("Durchfahrt"));
    }

    @Test
    void hasUserEntered_reflectsFlagsCorrectly() {
        TimetableRowData row = haltRow();
        assertThat(service.hasUserEnteredArrival(row)).isFalse();
        row.setArrivalExact("08:00");
        row.setUserEnteredArrivalExact(true);
        assertThat(service.hasUserEnteredArrival(row)).isTrue();
        assertThat(service.hasUserEnteredDeparture(row)).isFalse();
    }

    private TimetableRowData haltRow() {
        TimetableRowData r = new TimetableRowData();
        r.setName("Bern");
        r.setRoutePointRole(RoutePointRole.VIA);
        r.setHalt(true);
        return r;
    }

    // ── validate() ────────────────────────────────────────────────────

    @Test
    void validate_arrivalAfterDeparture_reportsError() {
        List<TimetableRowData> rows = createFiveStopRoute();
        // Force Spiez arrival 08:50 with departure still 08:37
        TimetableRowData spiez = rows.get(2);
        spiez.setArrivalMode(TimeConstraintMode.EXACT);
        spiez.setArrivalExact("08:50");
        spiez.setEstimatedArrival("08:50");

        List<String> errors = service.validate(rows);

        assertThat(errors)
                .anySatisfy(e -> assertThat(e).contains("Spiez").contains("after departure"));
    }

    @Test
    void validate_negativeTravelTime_reportsError() {
        List<TimetableRowData> rows = createFiveStopRoute();
        // Visp arrives 08:30 — before Spiez departs 08:37 (negative travel)
        TimetableRowData visp = rows.get(3);
        visp.setEstimatedArrival("08:30");

        List<String> errors = service.validate(rows);

        assertThat(errors)
                .anySatisfy(e -> assertThat(e).contains("Visp").contains("negative travel"));
    }

    @Test
    void validate_consistentTimetable_returnsEmpty() {
        List<TimetableRowData> rows = createFiveStopRoute();

        assertThat(service.validate(rows)).isEmpty();
    }

    // ── Helper ────────────────────────────────────────────────────────

    private TimetableRowData stop(String name, String arr, String dep) {
        TimetableRowData r = new TimetableRowData();
        r.setName(name);
        r.setRoutePointRole(RoutePointRole.VIA);
        r.setArrivalMode(TimeConstraintMode.NONE);
        r.setDepartureMode(TimeConstraintMode.NONE);
        r.setEstimatedArrival(arr);
        r.setEstimatedDeparture(dep);
        return r;
    }

    private List<TimetableRowData> createFiveStopRoute() {
        List<TimetableRowData> rows = new ArrayList<>();
        TimetableRowData bern = new TimetableRowData();
        bern.setSequence(1);
        bern.setName("Bern");
        bern.setRoutePointRole(RoutePointRole.ORIGIN);
        bern.setEstimatedDeparture("08:00");
        rows.add(bern);

        TimetableRowData thun = new TimetableRowData();
        thun.setSequence(2);
        thun.setName("Thun");
        thun.setRoutePointRole(RoutePointRole.VIA);
        thun.setEstimatedArrival("08:20");
        thun.setEstimatedDeparture("08:22");
        rows.add(thun);

        TimetableRowData spiez = new TimetableRowData();
        spiez.setSequence(3);
        spiez.setName("Spiez");
        spiez.setRoutePointRole(RoutePointRole.VIA);
        spiez.setEstimatedArrival("08:35");
        spiez.setEstimatedDeparture("08:37");
        rows.add(spiez);

        TimetableRowData visp = new TimetableRowData();
        visp.setSequence(4);
        visp.setName("Visp");
        visp.setRoutePointRole(RoutePointRole.VIA);
        visp.setEstimatedArrival("09:00");
        visp.setEstimatedDeparture("09:02");
        rows.add(visp);

        TimetableRowData brig = new TimetableRowData();
        brig.setSequence(5);
        brig.setName("Brig");
        brig.setRoutePointRole(RoutePointRole.DESTINATION);
        brig.setEstimatedArrival("09:15");
        rows.add(brig);

        return rows;
    }

    private List<TimetableRowData> createThreeStopRoute() {
        List<TimetableRowData> rows = new ArrayList<>();

        TimetableRowData origin = new TimetableRowData();
        origin.setSequence(1);
        origin.setName("Bern");
        origin.setRoutePointRole(RoutePointRole.ORIGIN);
        origin.setEstimatedDeparture("08:00");
        rows.add(origin);

        TimetableRowData via = new TimetableRowData();
        via.setSequence(2);
        via.setName("Thun");
        via.setRoutePointRole(RoutePointRole.VIA);
        via.setEstimatedArrival("08:20");
        via.setEstimatedDeparture("08:22");
        rows.add(via);

        TimetableRowData destination = new TimetableRowData();
        destination.setSequence(3);
        destination.setName("Spiez");
        destination.setRoutePointRole(RoutePointRole.DESTINATION);
        destination.setEstimatedArrival("08:35");
        rows.add(destination);

        return rows;
    }
}
