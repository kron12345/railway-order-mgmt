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
            OrderPosition position,
            PmReferenceTrain train,
            BiFunction<String, Object[], String> translator) {
        List<String> deviations = new ArrayList<>();
        if (position == null || train == null) {
            return deviations;
        }
        compareOrderWithTrain(position, train, deviations, translator);
        compareVersions(train, deviations, translator);
        return deviations;
    }

    private static void compareOrderWithTrain(
            OrderPosition position,
            PmReferenceTrain train,
            List<String> deviations,
            BiFunction<String, Object[], String> translator) {
        List<PmJourneyLocation> locations = locations(latestVersion(train));
        if (!locations.isEmpty()) {
            String firstLocation = locations.get(0).getPrimaryLocationName();
            String lastLocation = locations.get(locations.size() - 1).getPrimaryLocationName();
            if (differs(position.getFromLocation(), firstLocation)) {
                deviations.add(
                        translator.apply(
                                "deviation.from",
                                new Object[] {nz(position.getFromLocation()), nz(firstLocation)}));
            }
            if (differs(position.getToLocation(), lastLocation)) {
                deviations.add(
                        translator.apply(
                                "deviation.to",
                                new Object[] {nz(position.getToLocation()), nz(lastLocation)}));
            }
        }
        if (position.getStart() != null
                && train.getCalendarStart() != null
                && !position.getStart().toLocalDate().equals(train.getCalendarStart())) {
            deviations.add(
                    translator.apply(
                            "deviation.start",
                            new Object[] {
                                position.getStart().toLocalDate(), train.getCalendarStart()
                            }));
        }
        if (position.getEnd() != null
                && train.getCalendarEnd() != null
                && !position.getEnd().toLocalDate().equals(train.getCalendarEnd())) {
            deviations.add(
                    translator.apply(
                            "deviation.end",
                            new Object[] {
                                position.getEnd().toLocalDate(), train.getCalendarEnd()
                            }));
        }
    }

    private static void compareVersions(
            PmReferenceTrain train,
            List<String> deviations,
            BiFunction<String, Object[], String> translator) {
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
        List<PmJourneyLocation> initialLocations = locations(initial);
        List<PmJourneyLocation> latestLocations = locations(latest);
        Object[] versions = {initial.getVersionNumber(), latest.getVersionNumber()};
        boolean stopsChanged = initialLocations.size() != latestLocations.size();
        int compareCount = Math.min(initialLocations.size(), latestLocations.size());
        int changedTimeCount = 0;
        for (int i = 0; i < compareCount; i++) {
            PmJourneyLocation initialLocation = initialLocations.get(i);
            PmJourneyLocation latestLocation = latestLocations.get(i);
            // A stop swapped at the same sequence is a route change even when the count is equal.
            if (differs(
                    initialLocation.getPrimaryLocationName(),
                    latestLocation.getPrimaryLocationName())) {
                stopsChanged = true;
            }
            // Compare the day offset too, so a time that rolls past midnight is not seen as equal.
            if (differs(initialLocation.getArrivalTime(), latestLocation.getArrivalTime())
                    || differs(
                            initialLocation.getDepartureTime(), latestLocation.getDepartureTime())
                    || !Objects.equals(
                            initialLocation.getArrivalOffset(), latestLocation.getArrivalOffset())
                    || !Objects.equals(
                            initialLocation.getDepartureOffset(),
                            latestLocation.getDepartureOffset())) {
                changedTimeCount++;
            }
        }
        if (stopsChanged) {
            deviations.add(translator.apply("deviation.versionStops", versions));
        }
        if (changedTimeCount > 0) {
            deviations.add(
                    translator.apply(
                            "deviation.versionTimes",
                            new Object[] {
                                initial.getVersionNumber(),
                                latest.getVersionNumber(),
                                changedTimeCount
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
