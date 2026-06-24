package com.ordermgmt.railway.ui.component.order;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;

import com.ordermgmt.railway.domain.order.model.OrderPosition;
import com.ordermgmt.railway.domain.pathmanager.model.PmJourneyLocation;
import com.ordermgmt.railway.domain.pathmanager.model.PmReferenceTrain;
import com.ordermgmt.railway.domain.pathmanager.model.PmTrainVersion;

/**
 * Detects deviations of an order position from its linked RailOpt reference train (SOB S.26): both
 * the order ↔ RailOpt mismatch (position von/nach/Start/Ende vs. the train) and the version ↔
 * original drift (latest train version vs. the INITIAL one = a path modification/alteration).
 */
final class DeviationDetector {

    private DeviationDetector() {}

    /** Human-readable deviation messages; empty when the position is consistent / has no train. */
    static List<String> detect(
            OrderPosition pos, PmReferenceTrain train, BiFunction<String, Object[], String> t) {
        List<String> devs = new ArrayList<>();
        if (pos == null || train == null) {
            return devs;
        }
        compareOrderVsTrain(pos, train, devs, t);
        compareVersions(train, devs, t);
        return devs;
    }

    private static void compareOrderVsTrain(
            OrderPosition pos,
            PmReferenceTrain train,
            List<String> devs,
            BiFunction<String, Object[], String> t) {
        List<PmJourneyLocation> locs = locations(latestVersion(train));
        if (!locs.isEmpty()) {
            String first = locs.get(0).getPrimaryLocationName();
            String last = locs.get(locs.size() - 1).getPrimaryLocationName();
            if (differs(pos.getFromLocation(), first)) {
                devs.add(
                        t.apply(
                                "deviation.from",
                                new Object[] {nz(pos.getFromLocation()), nz(first)}));
            }
            if (differs(pos.getToLocation(), last)) {
                devs.add(t.apply("deviation.to", new Object[] {nz(pos.getToLocation()), nz(last)}));
            }
        }
        if (pos.getStart() != null
                && train.getCalendarStart() != null
                && !pos.getStart().toLocalDate().equals(train.getCalendarStart())) {
            devs.add(
                    t.apply(
                            "deviation.start",
                            new Object[] {pos.getStart().toLocalDate(), train.getCalendarStart()}));
        }
        if (pos.getEnd() != null
                && train.getCalendarEnd() != null
                && !pos.getEnd().toLocalDate().equals(train.getCalendarEnd())) {
            devs.add(
                    t.apply(
                            "deviation.end",
                            new Object[] {pos.getEnd().toLocalDate(), train.getCalendarEnd()}));
        }
    }

    private static void compareVersions(
            PmReferenceTrain train, List<String> devs, BiFunction<String, Object[], String> t) {
        if (train.getTrainVersions() == null || train.getTrainVersions().size() < 2) {
            return;
        }
        PmTrainVersion initial =
                train.getTrainVersions().stream()
                        .min(Comparator.comparing(PmTrainVersion::getVersionNumber))
                        .orElse(null);
        PmTrainVersion latest = latestVersion(train);
        if (initial == null
                || latest == null
                || Objects.equals(initial.getVersionNumber(), latest.getVersionNumber())) {
            return;
        }
        List<PmJourneyLocation> initLocs = locations(initial);
        List<PmJourneyLocation> latestLocs = locations(latest);
        Object[] versions = {initial.getVersionNumber(), latest.getVersionNumber()};
        boolean stopsChanged = initLocs.size() != latestLocs.size();
        int compareCount = Math.min(initLocs.size(), latestLocs.size());
        int timeChanged = 0;
        for (int i = 0; i < compareCount; i++) {
            PmJourneyLocation a = initLocs.get(i);
            PmJourneyLocation b = latestLocs.get(i);
            // A stop swapped at the same sequence is a route change even when the count is equal.
            if (differs(a.getPrimaryLocationName(), b.getPrimaryLocationName())) {
                stopsChanged = true;
            }
            // Compare the day offset too, so a time that rolls past midnight is not seen as equal.
            if (differs(a.getArrivalTime(), b.getArrivalTime())
                    || differs(a.getDepartureTime(), b.getDepartureTime())
                    || !Objects.equals(a.getArrivalOffset(), b.getArrivalOffset())
                    || !Objects.equals(a.getDepartureOffset(), b.getDepartureOffset())) {
                timeChanged++;
            }
        }
        if (stopsChanged) {
            devs.add(t.apply("deviation.versionStops", versions));
        }
        if (timeChanged > 0) {
            devs.add(
                    t.apply(
                            "deviation.versionTimes",
                            new Object[] {
                                initial.getVersionNumber(), latest.getVersionNumber(), timeChanged
                            }));
        }
    }

    private static PmTrainVersion latestVersion(PmReferenceTrain train) {
        if (train.getTrainVersions() == null) {
            return null;
        }
        return train.getTrainVersions().stream()
                .max(Comparator.comparing(PmTrainVersion::getVersionNumber))
                .orElse(null);
    }

    private static List<PmJourneyLocation> locations(PmTrainVersion version) {
        if (version == null || version.getJourneyLocations() == null) {
            return List.of();
        }
        return version.getJourneyLocations().stream()
                .sorted(Comparator.comparing(PmJourneyLocation::getSequence))
                .toList();
    }

    private static boolean differs(String a, String b) {
        return !Objects.equals(nz(a), nz(b));
    }

    private static String nz(String value) {
        return value == null ? "" : value;
    }
}
