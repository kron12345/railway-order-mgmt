package com.ordermgmt.railway.domain.vehicleplanning.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ordermgmt.railway.domain.pathmanager.model.PmJourneyLocation;
import com.ordermgmt.railway.domain.pathmanager.model.PmTrainVersion;
import com.ordermgmt.railway.domain.pathmanager.repository.PmJourneyLocationRepository;
import com.ordermgmt.railway.domain.vehicleplanning.model.Conflict;
import com.ordermgmt.railway.domain.vehicleplanning.model.Conflict.Severity;
import com.ordermgmt.railway.domain.vehicleplanning.model.VpRotationEntry;
import com.ordermgmt.railway.domain.vehicleplanning.model.VpRotationSet;
import com.ordermgmt.railway.domain.vehicleplanning.model.VpVehicle;
import com.ordermgmt.railway.domain.vehicleplanning.repository.VpRotationSetRepository;

/** Detects time overlaps and location mismatches within vehicle rotations. */
@Service
@Transactional(readOnly = true)
public class ConflictDetectionService {

    private final VpRotationSetRepository rotationSetRepo;
    private final PmJourneyLocationRepository journeyLocationRepo;

    public ConflictDetectionService(
            VpRotationSetRepository rotationSetRepo,
            PmJourneyLocationRepository journeyLocationRepo) {
        this.rotationSetRepo = rotationSetRepo;
        this.journeyLocationRepo = journeyLocationRepo;
    }

    /**
     * Detects scheduling conflicts for all vehicles in the given rotation set.
     *
     * <p>Checks each vehicle and day for: (1) time overlap between consecutive trains, (2) location
     * mismatch (arrival location of one train vs. departure location of the next).
     */
    public List<Conflict> detectConflicts(UUID rotationSetId) {
        VpRotationSet rs =
                rotationSetRepo
                        .findById(rotationSetId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Rotation set not found: " + rotationSetId));

        List<Conflict> conflicts = new ArrayList<>();
        for (VpVehicle vehicle : rs.getVehicles()) {
            for (int dow = 1; dow <= 7; dow++) {
                detectConflictsForVehicleDay(vehicle, dow, conflicts);
            }
        }
        return conflicts;
    }

    private void detectConflictsForVehicleDay(
            VpVehicle vehicle, int dayOfWeek, List<Conflict> conflicts) {
        List<VpRotationEntry> entries =
                vehicle.getEntries().stream()
                        .filter(e -> e.getDayOfWeek() == dayOfWeek)
                        .sorted(Comparator.comparingInt(VpRotationEntry::getSequenceInDay))
                        .toList();

        if (entries.size() < 2) {
            return;
        }

        for (int i = 0; i < entries.size() - 1; i++) {
            VpRotationEntry current = entries.get(i);
            VpRotationEntry next = entries.get(i + 1);

            TrainTimes currentTimes = resolveTrainTimes(current);
            TrainTimes nextTimes = resolveTrainTimes(next);

            if (currentTimes == null || nextTimes == null) {
                continue;
            }

            // Check time overlap
            if (currentTimes.arrivalTime() != null
                    && nextTimes.departureTime() != null
                    && currentTimes.arrivalTime().compareTo(nextTimes.departureTime()) > 0) {
                conflicts.add(
                        new Conflict(
                                vehicle.getId(),
                                vehicle.getLabel(),
                                dayOfWeek,
                                String.format(
                                        "Time overlap: %s (arr %s) vs %s (dep %s)",
                                        trainLabel(current),
                                        currentTimes.arrivalTime(),
                                        trainLabel(next),
                                        nextTimes.departureTime()),
                                Severity.ERROR));
            }

            // Check location mismatch
            if (currentTimes.arrivalLocation() != null
                    && nextTimes.departureLocation() != null
                    && !currentTimes.arrivalLocation().equals(nextTimes.departureLocation())) {
                conflicts.add(
                        new Conflict(
                                vehicle.getId(),
                                vehicle.getLabel(),
                                dayOfWeek,
                                String.format(
                                        "Location mismatch: %s arrives at %s but %s departs"
                                                + " from %s",
                                        trainLabel(current),
                                        currentTimes.arrivalLocation(),
                                        trainLabel(next),
                                        nextTimes.departureLocation()),
                                Severity.WARNING));
            }
        }
    }

    /**
     * Resolves departure and arrival times from the latest version's journey locations. Uses first
     * location's departure and last location's arrival.
     */
    private TrainTimes resolveTrainTimes(VpRotationEntry entry) {
        List<PmTrainVersion> versions = entry.getReferenceTrain().getTrainVersions();
        if (versions == null || versions.isEmpty()) {
            return null;
        }

        // Use the latest version (highest version number)
        PmTrainVersion latestVersion =
                versions.stream()
                        .max(Comparator.comparingInt(PmTrainVersion::getVersionNumber))
                        .orElse(null);

        if (latestVersion == null) {
            return null;
        }

        List<PmJourneyLocation> locations =
                journeyLocationRepo.findByTrainVersionIdOrderBySequenceAsc(latestVersion.getId());

        if (locations.isEmpty()) {
            return null;
        }

        PmJourneyLocation first = locations.getFirst();
        PmJourneyLocation last = locations.getLast();

        return new TrainTimes(
                first.getDepartureTime(),
                last.getArrivalTime(),
                first.getPrimaryLocationName(),
                last.getPrimaryLocationName());
    }

    private String trainLabel(VpRotationEntry entry) {
        String otn = entry.getReferenceTrain().getOperationalTrainNumber();
        return otn != null ? otn : entry.getReferenceTrain().getTridCore();
    }

    private record TrainTimes(
            String departureTime,
            String arrivalTime,
            String departureLocation,
            String arrivalLocation) {}
}
