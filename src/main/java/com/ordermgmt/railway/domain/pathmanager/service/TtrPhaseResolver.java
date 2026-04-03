package com.ordermgmt.railway.domain.pathmanager.service;

import java.time.LocalDate;

import org.springframework.stereotype.Service;

import com.ordermgmt.railway.domain.pathmanager.model.PathProcessType;
import com.ordermgmt.railway.domain.pathmanager.model.PmTimetableYear;
import com.ordermgmt.railway.domain.pathmanager.model.TtrPhase;

/**
 * Resolves the current TTR phase and the corresponding TTT process type for a given timetable year.
 *
 * <p>Phase boundaries are defined relative to the timetable year start date (X):
 *
 * <ul>
 *   <li>X-60 months: Capacity Strategy
 *   <li>X-36 months: Capacity Model
 *   <li>X-18 months: Capacity Supply
 *   <li>X-11 months: Annual Ordering (Bestellphase 2)
 *   <li>X-8.5 months: Late Ordering (Bestellphase 3)
 *   <li>X-2 months: Ad Hoc Ordering
 *   <li>After end date: Past
 * </ul>
 */
@Service
public class TtrPhaseResolver {

    /**
     * Determines the current TTR phase for a timetable year at the given date.
     *
     * @param year the timetable year with start/end dates
     * @param today the reference date
     * @return the TTR phase
     */
    public TtrPhase resolvePhase(PmTimetableYear year, LocalDate today) {
        LocalDate x = year.getStartDate();

        if (today.isAfter(year.getEndDate())) {
            return TtrPhase.PAST;
        }
        if (today.isBefore(x.minusMonths(36))) {
            return TtrPhase.CAPACITY_STRATEGY;
        }
        if (today.isBefore(x.minusMonths(18))) {
            return TtrPhase.CAPACITY_MODEL;
        }
        if (today.isBefore(x.minusMonths(11))) {
            return TtrPhase.CAPACITY_SUPPLY;
        }
        // X-8.5 months = X minus 8 months minus 15 days
        if (today.isBefore(x.minusMonths(8).minusDays(15))) {
            return TtrPhase.ANNUAL_ORDERING;
        }
        if (today.isBefore(x.minusMonths(2))) {
            return TtrPhase.LATE_ORDERING;
        }
        return TtrPhase.AD_HOC_ORDERING;
    }

    /**
     * Returns the TTT process type for new path requests at the given date.
     *
     * @param year the timetable year
     * @param today the reference date
     * @return the process type, or {@code null} if ordering is not yet possible
     */
    public PathProcessType resolveProcessType(PmTimetableYear year, LocalDate today) {
        TtrPhase phase = resolvePhase(year, today);
        return switch (phase) {
            case ANNUAL_ORDERING -> PathProcessType.ANNUAL_NEW;
            case LATE_ORDERING -> PathProcessType.ANNUAL_LATE;
            case AD_HOC_ORDERING -> PathProcessType.AD_HOC;
            default -> null;
        };
    }

    /**
     * Returns {@code true} if draft offers are allowed in the current phase (Bestellphase 2 only).
     *
     * @param year the timetable year
     * @param today the reference date
     * @return whether draft offers are permitted
     */
    public boolean isDraftOfferAllowed(PmTimetableYear year, LocalDate today) {
        return resolvePhase(year, today) == TtrPhase.ANNUAL_ORDERING;
    }

    /**
     * Returns a human-readable description of the current phase.
     *
     * @param year the timetable year
     * @param today the reference date
     * @return description string
     */
    public String phaseDescription(PmTimetableYear year, LocalDate today) {
        TtrPhase phase = resolvePhase(year, today);
        PathProcessType processType = resolveProcessType(year, today);
        String desc = "FPJ " + year.getYear() + " — " + phase.getLabel();
        if (processType != null) {
            desc += " (ProcessType=" + processType.code() + ")";
        }
        return desc;
    }
}
