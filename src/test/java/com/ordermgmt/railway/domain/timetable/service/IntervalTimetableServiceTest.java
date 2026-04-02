package com.ordermgmt.railway.domain.timetable.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

/** Pure unit tests for IntervalTimetableService calculation methods. */
class IntervalTimetableServiceTest {

    private final IntervalTimetableService service =
            new IntervalTimetableService(
                    null /* archiveService not needed for calculation tests */);

    // ── calculateDepartureTimes ───────────────────────────────────────

    @Test
    void calculateDepartureTimes_normalRange_returnsExpected() {
        List<LocalTime> times =
                service.calculateDepartureTimes(LocalTime.of(6, 0), LocalTime.of(8, 0), false, 30);

        assertThat(times)
                .containsExactly(
                        LocalTime.of(6, 0),
                        LocalTime.of(6, 30),
                        LocalTime.of(7, 0),
                        LocalTime.of(7, 30),
                        LocalTime.of(8, 0));
        assertThat(times).hasSize(5);
    }

    @Test
    void calculateDepartureTimes_midnightCrossing_returnsExpected() {
        List<LocalTime> times =
                service.calculateDepartureTimes(LocalTime.of(23, 0), LocalTime.of(1, 0), true, 30);

        assertThat(times)
                .containsExactly(
                        LocalTime.of(23, 0),
                        LocalTime.of(23, 30),
                        LocalTime.of(0, 0),
                        LocalTime.of(0, 30),
                        LocalTime.of(1, 0));
        assertThat(times).hasSize(5);
    }

    @Test
    void calculateDepartureTimes_sameStartAndEnd_returnsSingleTime() {
        List<LocalTime> times =
                service.calculateDepartureTimes(LocalTime.of(8, 0), LocalTime.of(8, 0), false, 30);

        assertThat(times).containsExactly(LocalTime.of(8, 0));
        assertThat(times).hasSize(1);
    }

    @Test
    void calculateDepartureTimes_hourlyInterval_returnsCorrectCount() {
        List<LocalTime> times =
                service.calculateDepartureTimes(LocalTime.of(6, 0), LocalTime.of(10, 0), false, 60);

        assertThat(times)
                .containsExactly(
                        LocalTime.of(6, 0),
                        LocalTime.of(7, 0),
                        LocalTime.of(8, 0),
                        LocalTime.of(9, 0),
                        LocalTime.of(10, 0));
    }

    @Test
    void calculateDepartureTimes_15minInterval_returnsCorrectCount() {
        List<LocalTime> times =
                service.calculateDepartureTimes(
                        LocalTime.of(12, 0), LocalTime.of(13, 0), false, 15);

        assertThat(times)
                .containsExactly(
                        LocalTime.of(12, 0),
                        LocalTime.of(12, 15),
                        LocalTime.of(12, 30),
                        LocalTime.of(12, 45),
                        LocalTime.of(13, 0));
    }

    // ── incrementOtn ──────────────────────────────────────────────────

    @Test
    void incrementOtn_numericBase_returnsIncremented() {
        assertThat(service.incrementOtn("95001", 3)).isEqualTo("95004");
    }

    @Test
    void incrementOtn_nonNumericBase_returnsUnchanged() {
        assertThat(service.incrementOtn("95xxx", 3)).isEqualTo("95xxx");
    }

    @Test
    void incrementOtn_null_returnsNull() {
        assertThat(service.incrementOtn(null, 0)).isNull();
    }

    @Test
    void incrementOtn_blank_returnsNull() {
        assertThat(service.incrementOtn("  ", 5)).isNull();
    }

    @Test
    void incrementOtn_zeroOffset_returnsSameNumber() {
        assertThat(service.incrementOtn("95001", 0)).isEqualTo("95001");
    }

    @Test
    void incrementOtn_numericWithWhitespace_trimsAndIncrements() {
        assertThat(service.incrementOtn(" 100 ", 2)).isEqualTo("102");
    }
}
