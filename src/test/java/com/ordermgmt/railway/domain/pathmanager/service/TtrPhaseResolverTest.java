package com.ordermgmt.railway.domain.pathmanager.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ordermgmt.railway.domain.pathmanager.model.PathProcessType;
import com.ordermgmt.railway.domain.pathmanager.model.PmTimetableYear;
import com.ordermgmt.railway.domain.pathmanager.model.TtrPhase;

class TtrPhaseResolverTest {

    private TtrPhaseResolver resolver;
    private PmTimetableYear year2026;

    @BeforeEach
    void setUp() {
        resolver = new TtrPhaseResolver();
        year2026 = new PmTimetableYear();
        year2026.setYear(2026);
        year2026.setStartDate(LocalDate.of(2025, 12, 14));
        year2026.setEndDate(LocalDate.of(2026, 12, 12));
    }

    // ── CAPACITY_STRATEGY ────────────────────────────────────────────

    @Test
    void resolvePhase_beforeX36_returnsCapacityStrategy() {
        // X-60 months = Jun 2021
        LocalDate date = LocalDate.of(2021, 6, 1);
        assertThat(resolver.resolvePhase(year2026, date)).isEqualTo(TtrPhase.CAPACITY_STRATEGY);
    }

    @Test
    void resolvePhase_justBeforeX36_returnsCapacityStrategy() {
        // X-36 = 2025-12-14 minus 36 months = 2022-12-14
        LocalDate date = LocalDate.of(2022, 12, 13);
        assertThat(resolver.resolvePhase(year2026, date)).isEqualTo(TtrPhase.CAPACITY_STRATEGY);
    }

    // ── CAPACITY_MODEL ───────────────────────────────────────────────

    @Test
    void resolvePhase_atX36_returnsCapacityModel() {
        // X-36 = 2022-12-14
        LocalDate date = LocalDate.of(2022, 12, 14);
        assertThat(resolver.resolvePhase(year2026, date)).isEqualTo(TtrPhase.CAPACITY_MODEL);
    }

    @Test
    void resolvePhase_dec2023_returnsCapacityModel() {
        LocalDate date = LocalDate.of(2023, 12, 1);
        assertThat(resolver.resolvePhase(year2026, date)).isEqualTo(TtrPhase.CAPACITY_MODEL);
    }

    // ── CAPACITY_SUPPLY ──────────────────────────────────────────────

    @Test
    void resolvePhase_atX18_returnsCapacitySupply() {
        // X-18 = 2025-12-14 minus 18 months = 2024-06-14
        LocalDate date = LocalDate.of(2024, 6, 14);
        assertThat(resolver.resolvePhase(year2026, date)).isEqualTo(TtrPhase.CAPACITY_SUPPLY);
    }

    @Test
    void resolvePhase_jun2025_returnsCapacitySupply() {
        LocalDate date = LocalDate.of(2025, 1, 1);
        assertThat(resolver.resolvePhase(year2026, date)).isEqualTo(TtrPhase.CAPACITY_SUPPLY);
    }

    // ── ANNUAL_ORDERING ──────────────────────────────────────────────

    @Test
    void resolvePhase_atX11_returnsAnnualOrdering() {
        // X-11 = 2025-12-14 minus 11 months = 2025-01-14
        LocalDate date = LocalDate.of(2025, 1, 14);
        assertThat(resolver.resolvePhase(year2026, date)).isEqualTo(TtrPhase.ANNUAL_ORDERING);
    }

    @Test
    void resolvePhase_annualOrdering_processTypeIsAnnualNew() {
        LocalDate date = LocalDate.of(2025, 2, 1);
        assertThat(resolver.resolveProcessType(year2026, date))
                .isEqualTo(PathProcessType.ANNUAL_NEW);
    }

    @Test
    void resolvePhase_annualOrdering_draftAllowed() {
        LocalDate date = LocalDate.of(2025, 2, 1);
        assertThat(resolver.isDraftOfferAllowed(year2026, date)).isTrue();
    }

    // ── LATE_ORDERING ────────────────────────────────────────────────

    @Test
    void resolvePhase_atX8_5_returnsLateOrdering() {
        // X-8.5 = 2025-12-14 minus 8 months minus 15 days = 2025-03-30
        LocalDate date = LocalDate.of(2025, 3, 30);
        assertThat(resolver.resolvePhase(year2026, date)).isEqualTo(TtrPhase.LATE_ORDERING);
    }

    @Test
    void resolvePhase_apr2025_returnsLateOrdering() {
        LocalDate date = LocalDate.of(2025, 4, 15);
        assertThat(resolver.resolvePhase(year2026, date)).isEqualTo(TtrPhase.LATE_ORDERING);
    }

    @Test
    void resolvePhase_lateOrdering_processTypeIsAnnualLate() {
        LocalDate date = LocalDate.of(2025, 4, 15);
        assertThat(resolver.resolveProcessType(year2026, date))
                .isEqualTo(PathProcessType.ANNUAL_LATE);
    }

    @Test
    void resolvePhase_lateOrdering_draftNotAllowed() {
        LocalDate date = LocalDate.of(2025, 4, 15);
        assertThat(resolver.isDraftOfferAllowed(year2026, date)).isFalse();
    }

    // ── AD_HOC_ORDERING ──────────────────────────────────────────────

    @Test
    void resolvePhase_atX2_returnsAdHocOrdering() {
        // X-2 = 2025-12-14 minus 2 months = 2025-10-14
        LocalDate date = LocalDate.of(2025, 10, 14);
        assertThat(resolver.resolvePhase(year2026, date)).isEqualTo(TtrPhase.AD_HOC_ORDERING);
    }

    @Test
    void resolvePhase_adHocOrdering_processTypeIsAdHoc() {
        LocalDate date = LocalDate.of(2025, 11, 1);
        assertThat(resolver.resolveProcessType(year2026, date)).isEqualTo(PathProcessType.AD_HOC);
    }

    @Test
    void resolvePhase_adHocOrdering_draftNotAllowed() {
        LocalDate date = LocalDate.of(2025, 11, 1);
        assertThat(resolver.isDraftOfferAllowed(year2026, date)).isFalse();
    }

    // ── PAST ─────────────────────────────────────────────────────────

    @Test
    void resolvePhase_afterEndDate_returnsPast() {
        LocalDate date = LocalDate.of(2026, 12, 13);
        assertThat(resolver.resolvePhase(year2026, date)).isEqualTo(TtrPhase.PAST);
    }

    @Test
    void resolvePhase_past_processTypeIsNull() {
        LocalDate date = LocalDate.of(2027, 1, 1);
        assertThat(resolver.resolveProcessType(year2026, date)).isNull();
    }

    @Test
    void resolvePhase_past_draftNotAllowed() {
        LocalDate date = LocalDate.of(2027, 1, 1);
        assertThat(resolver.isDraftOfferAllowed(year2026, date)).isFalse();
    }

    // ── Boundary checks ──────────────────────────────────────────────

    @Test
    void resolvePhase_onEndDate_returnsAdHocOrdering() {
        // End date itself should NOT be PAST (isAfter is strict)
        LocalDate date = LocalDate.of(2026, 12, 12);
        assertThat(resolver.resolvePhase(year2026, date)).isEqualTo(TtrPhase.AD_HOC_ORDERING);
    }

    @Test
    void resolvePhase_capacityStrategy_processTypeIsNull() {
        LocalDate date = LocalDate.of(2021, 1, 1);
        assertThat(resolver.resolveProcessType(year2026, date)).isNull();
    }

    @Test
    void resolvePhase_capacityModel_processTypeIsNull() {
        LocalDate date = LocalDate.of(2023, 6, 1);
        assertThat(resolver.resolveProcessType(year2026, date)).isNull();
    }

    @Test
    void resolvePhase_capacitySupply_processTypeIsNull() {
        LocalDate date = LocalDate.of(2024, 12, 1);
        assertThat(resolver.resolveProcessType(year2026, date)).isNull();
    }

    // ── phaseDescription ─────────────────────────────────────────────

    @Test
    void phaseDescription_annualOrdering_includesProcessType() {
        LocalDate date = LocalDate.of(2025, 2, 1);
        String desc = resolver.phaseDescription(year2026, date);
        assertThat(desc).contains("FPJ 2026").contains("Annual Ordering").contains("ProcessType=0");
    }

    @Test
    void phaseDescription_past_noProcessType() {
        LocalDate date = LocalDate.of(2027, 1, 1);
        String desc = resolver.phaseDescription(year2026, date);
        assertThat(desc).contains("FPJ 2026").contains("ended").doesNotContain("ProcessType");
    }
}
