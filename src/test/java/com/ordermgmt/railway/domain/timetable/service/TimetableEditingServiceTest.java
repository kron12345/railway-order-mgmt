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
import com.ordermgmt.railway.domain.timetable.model.TimetableRowData;

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
