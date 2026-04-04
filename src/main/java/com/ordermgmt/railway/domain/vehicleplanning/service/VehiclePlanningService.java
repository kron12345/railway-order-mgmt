package com.ordermgmt.railway.domain.vehicleplanning.service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ordermgmt.railway.domain.pathmanager.model.PmJourneyLocation;
import com.ordermgmt.railway.domain.pathmanager.model.PmReferenceTrain;
import com.ordermgmt.railway.domain.pathmanager.model.PmTimetableYear;
import com.ordermgmt.railway.domain.pathmanager.model.PmTrainVersion;
import com.ordermgmt.railway.domain.pathmanager.repository.PmJourneyLocationRepository;
import com.ordermgmt.railway.domain.pathmanager.repository.PmReferenceTrainRepository;
import com.ordermgmt.railway.domain.pathmanager.repository.PmTimetableYearRepository;
import com.ordermgmt.railway.domain.pathmanager.repository.PmTrainVersionRepository;
import com.ordermgmt.railway.domain.vehicleplanning.model.CouplingPosition;
import com.ordermgmt.railway.domain.vehicleplanning.model.VehicleType;
import com.ordermgmt.railway.domain.vehicleplanning.model.VpRotationEntry;
import com.ordermgmt.railway.domain.vehicleplanning.model.VpRotationSet;
import com.ordermgmt.railway.domain.vehicleplanning.model.VpVehicle;
import com.ordermgmt.railway.domain.vehicleplanning.repository.VpRotationEntryRepository;
import com.ordermgmt.railway.domain.vehicleplanning.repository.VpRotationSetRepository;
import com.ordermgmt.railway.domain.vehicleplanning.repository.VpVehicleRepository;

/** Core service for vehicle rotation planning CRUD operations. */
@Service
@Transactional
public class VehiclePlanningService {

    /** TAF/TAP activity code: vehicle continues from previous train in rotation. */
    private static final String ACTIVITY_FROM_PREVIOUS = "0044";

    /** TAF/TAP activity code: vehicle continues to next train in rotation. */
    private static final String ACTIVITY_TO_NEXT = "0045";

    /** Minimum valid ISO day-of-week (Monday). */
    private static final int MIN_DAY_OF_WEEK = 1;

    /** Maximum valid ISO day-of-week (Sunday). */
    private static final int MAX_DAY_OF_WEEK = 7;

    private final VpRotationSetRepository rotationSetRepo;
    private final VpVehicleRepository vehicleRepo;
    private final VpRotationEntryRepository entryRepo;
    private final PmReferenceTrainRepository trainRepo;
    private final PmTimetableYearRepository timetableYearRepo;
    private final PmTrainVersionRepository trainVersionRepo;
    private final PmJourneyLocationRepository journeyLocationRepo;

    public VehiclePlanningService(
            VpRotationSetRepository rotationSetRepo,
            VpVehicleRepository vehicleRepo,
            VpRotationEntryRepository entryRepo,
            PmReferenceTrainRepository trainRepo,
            PmTimetableYearRepository timetableYearRepo,
            PmTrainVersionRepository trainVersionRepo,
            PmJourneyLocationRepository journeyLocationRepo) {
        this.rotationSetRepo = rotationSetRepo;
        this.vehicleRepo = vehicleRepo;
        this.entryRepo = entryRepo;
        this.trainRepo = trainRepo;
        this.timetableYearRepo = timetableYearRepo;
        this.trainVersionRepo = trainVersionRepo;
        this.journeyLocationRepo = journeyLocationRepo;
    }

    // --- Rotation Set CRUD ---

    /** Returns all rotation sets for the given timetable year, ordered by name. */
    @Transactional(readOnly = true)
    public List<VpRotationSet> getRotationSets(UUID timetableYearId) {
        return rotationSetRepo.findByTimetableYearIdOrderByNameAsc(timetableYearId);
    }

    /** Returns a single rotation set by ID, or throws if not found. */
    @Transactional(readOnly = true)
    public VpRotationSet getRotationSet(UUID rotationSetId) {
        return rotationSetRepo
                .findById(rotationSetId)
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "Rotation set not found: " + rotationSetId));
    }

    /** Creates a new rotation set with the given name and description for the specified year. */
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public VpRotationSet createRotationSet(String name, String description, int year) {
        PmTimetableYear tty =
                timetableYearRepo
                        .findByYear(year)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Timetable year not found: " + year));
        VpRotationSet rs = new VpRotationSet();
        rs.setName(name);
        rs.setDescription(description);
        rs.setTimetableYear(tty);
        return rotationSetRepo.save(rs);
    }

    /** Persists changes to an existing rotation set. */
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public VpRotationSet saveRotationSet(VpRotationSet rotationSet) {
        return rotationSetRepo.save(rotationSet);
    }

    /** Deletes a rotation set and all its vehicles and entries. */
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public void deleteRotationSet(UUID rotationSetId) {
        rotationSetRepo.deleteById(rotationSetId);
    }

    // --- Vehicle CRUD ---

    /**
     * Returns all vehicles for the given rotation set with entries and reference trains eagerly
     * loaded. This ensures the data is available outside the transaction boundary (Vaadin UI
     * thread).
     */
    @Transactional(readOnly = true)
    public List<VpVehicle> getVehicles(UUID rotationSetId) {
        return vehicleRepo.findByRotationSetIdWithEntries(rotationSetId);
    }

    /** Persists changes to a vehicle. */
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public VpVehicle saveVehicle(VpVehicle vehicle) {
        return vehicleRepo.save(vehicle);
    }

    /** Deletes a vehicle and all its rotation entries. */
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public void deleteVehicle(UUID vehicleId) {
        vehicleRepo.deleteById(vehicleId);
    }

    // --- Vehicle creation ---

    /**
     * Creates a new vehicle (duty) in the given rotation set.
     *
     * @param rotationSetId the rotation set to add the vehicle to
     * @param label display label for the vehicle
     * @param vehicleType type of the vehicle
     * @return the persisted vehicle
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public VpVehicle addVehicle(UUID rotationSetId, String label, VehicleType vehicleType) {
        VpRotationSet rs =
                rotationSetRepo
                        .findById(rotationSetId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Rotation set not found: " + rotationSetId));
        List<VpVehicle> existing = vehicleRepo.findByRotationSetIdOrderBySequenceAsc(rotationSetId);
        int nextSeq = existing.isEmpty() ? 0 : existing.getLast().getSequence() + 1;

        VpVehicle vehicle = new VpVehicle();
        vehicle.setLabel(label);
        vehicle.setVehicleType(vehicleType);
        vehicle.setSequence(nextSeq);
        vehicle.setRotationSet(rs);
        return vehicleRepo.save(vehicle);
    }

    // --- Available trains ---

    /** Returns all reference trains for the given timetable year, ordered by OTN. */
    @Transactional(readOnly = true)
    public List<PmReferenceTrain> getAvailableTrains(int year) {
        return trainRepo.findByTimetableYearYearOrderByOperationalTrainNumberAsc(year);
    }

    /**
     * Returns trains not yet assigned to the given rotation set in the specified year. Useful for
     * populating the "shelf" rows in the Gantt chart.
     *
     * @param rotationSetId the rotation set to check assignments against
     * @param year the timetable year
     * @return unassigned reference trains ordered by OTN
     */
    @Transactional(readOnly = true)
    public List<PmReferenceTrain> getUnassignedTrains(UUID rotationSetId, int year) {
        // Use the eager-fetching query so that trainVersions are available
        // outside the transaction (Vaadin UI thread has no open session).
        // We only fetch trainVersions (not pathRequests) to avoid MultipleBagFetchException.
        List<PmReferenceTrain> allTrains = trainRepo.findByYearWithVersions(year);
        Set<UUID> assignedTrainIds =
                entryRepo.findByVehicleRotationSetId(rotationSetId).stream()
                        .map(e -> e.getReferenceTrain().getId())
                        .collect(Collectors.toSet());
        return allTrains.stream().filter(t -> !assignedTrainIds.contains(t.getId())).toList();
    }

    // --- Entry management ---

    /**
     * Assigns a reference train to a vehicle on a specific day of the week.
     *
     * @param vehicleId the target vehicle (must belong to a rotation set)
     * @param trainId the PM reference train to assign
     * @param dayOfWeek ISO day-of-week (1=Monday .. 7=Sunday)
     * @param coupling the coupling position for this entry
     * @return the persisted rotation entry
     * @throws IllegalArgumentException if dayOfWeek is out of range or entities not found
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public VpRotationEntry addTrainToVehicle(
            UUID vehicleId, UUID trainId, int dayOfWeek, CouplingPosition coupling) {
        validateDayOfWeek(dayOfWeek);

        VpVehicle vehicle =
                vehicleRepo
                        .findById(vehicleId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Vehicle not found: " + vehicleId));
        if (vehicle.getRotationSet() == null) {
            throw new IllegalArgumentException(
                    "Vehicle does not belong to a rotation set: " + vehicleId);
        }

        PmReferenceTrain train =
                trainRepo
                        .findById(trainId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Reference train not found: " + trainId));

        List<VpRotationEntry> existing =
                entryRepo.findByVehicleIdAndDayOfWeekOrderBySequenceInDayAsc(vehicleId, dayOfWeek);
        int nextSeq = existing.isEmpty() ? 0 : existing.getLast().getSequenceInDay() + 1;

        VpRotationEntry entry = new VpRotationEntry();
        entry.setVehicle(vehicle);
        entry.setReferenceTrain(train);
        entry.setDayOfWeek(dayOfWeek);
        entry.setSequenceInDay(nextSeq);
        entry.setCouplingType(coupling);
        return entryRepo.save(entry);
    }

    /**
     * Moves an existing rotation entry to a different vehicle and/or day.
     *
     * @param entryId the entry to move
     * @param targetVehicleId the target vehicle (must belong to the same rotation set)
     * @param targetDay ISO day-of-week (1=Monday .. 7=Sunday)
     * @return the updated entry
     * @throws IllegalArgumentException if validation fails
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public VpRotationEntry moveEntry(UUID entryId, UUID targetVehicleId, int targetDay) {
        validateDayOfWeek(targetDay);

        VpRotationEntry entry =
                entryRepo
                        .findById(entryId)
                        .orElseThrow(
                                () -> new IllegalArgumentException("Entry not found: " + entryId));
        VpVehicle target =
                vehicleRepo
                        .findById(targetVehicleId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Target vehicle not found: " + targetVehicleId));

        UUID sourceSetId = entry.getVehicle().getRotationSet().getId();
        UUID targetSetId = target.getRotationSet().getId();
        if (!sourceSetId.equals(targetSetId)) {
            throw new IllegalArgumentException(
                    "Cannot move entry across rotation sets (source="
                            + sourceSetId
                            + ", target="
                            + targetSetId
                            + ")");
        }

        List<VpRotationEntry> existing =
                entryRepo.findByVehicleIdAndDayOfWeekOrderBySequenceInDayAsc(
                        targetVehicleId, targetDay);
        int nextSeq = existing.isEmpty() ? 0 : existing.getLast().getSequenceInDay() + 1;

        entry.setVehicle(target);
        entry.setDayOfWeek(targetDay);
        entry.setSequenceInDay(nextSeq);
        return entryRepo.save(entry);
    }

    /** Removes a rotation entry by ID. */
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public void removeEntry(UUID entryId) {
        entryRepo.deleteById(entryId);
    }

    // --- Vehicle linking (0044/0045) ---

    /**
     * Writes 0044/0045 activity codes and associated OTN to the Path Manager journey locations
     * based on the vehicle rotation sequence. For each consecutive pair of trains in a vehicle's
     * rotation, the last location of the current train gets 0045 (to next), and the first location
     * of the next train gets 0044 (from previous).
     *
     * @param rotationSetId the rotation set to process
     * @return number of journey locations updated
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Transactional
    public int writeVehicleLinksToPathManager(UUID rotationSetId) {
        rotationSetRepo
                .findById(rotationSetId)
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "Rotation set not found: " + rotationSetId));
        List<VpVehicle> vehicles = vehicleRepo.findByRotationSetIdOrderBySequenceAsc(rotationSetId);
        int updatedCount = 0;

        for (VpVehicle vehicle : vehicles) {
            List<VpRotationEntry> entries =
                    entryRepo.findByVehicleIdOrderByDayOfWeekAscSequenceInDayAsc(vehicle.getId());

            for (int i = 0; i < entries.size() - 1; i++) {
                VpRotationEntry current = entries.get(i);
                VpRotationEntry next = entries.get(i + 1);

                PmReferenceTrain currentTrain = current.getReferenceTrain();
                PmReferenceTrain nextTrain = next.getReferenceTrain();

                String currentOtn = currentTrain.getOperationalTrainNumber();
                String nextOtn = nextTrain.getOperationalTrainNumber();

                setAssociatedOtnOnLastLocation(currentTrain, nextOtn, ACTIVITY_TO_NEXT);
                setAssociatedOtnOnFirstLocation(nextTrain, currentOtn, ACTIVITY_FROM_PREVIOUS);
                updatedCount += 2;
            }
        }
        return updatedCount;
    }

    private void setAssociatedOtnOnLastLocation(
            PmReferenceTrain train, String otn, String activityCode) {
        PmTrainVersion latestVersion =
                trainVersionRepo
                        .findFirstByReferenceTrainIdOrderByVersionNumberDesc(train.getId())
                        .orElse(null);
        if (latestVersion == null) {
            return;
        }
        List<PmJourneyLocation> locations =
                journeyLocationRepo.findByTrainVersionIdOrderBySequenceAsc(latestVersion.getId());
        if (locations.isEmpty()) {
            return;
        }
        PmJourneyLocation last = locations.getLast();
        last.setAssociatedTrainOtn(otn);
        appendActivityCode(last, activityCode);
        journeyLocationRepo.save(last);
    }

    private void setAssociatedOtnOnFirstLocation(
            PmReferenceTrain train, String otn, String activityCode) {
        PmTrainVersion latestVersion =
                trainVersionRepo
                        .findFirstByReferenceTrainIdOrderByVersionNumberDesc(train.getId())
                        .orElse(null);
        if (latestVersion == null) {
            return;
        }
        List<PmJourneyLocation> locations =
                journeyLocationRepo.findByTrainVersionIdOrderBySequenceAsc(latestVersion.getId());
        if (locations.isEmpty()) {
            return;
        }
        PmJourneyLocation first = locations.getFirst();
        first.setAssociatedTrainOtn(otn);
        appendActivityCode(first, activityCode);
        journeyLocationRepo.save(first);
    }

    private void appendActivityCode(PmJourneyLocation location, String activityCode) {
        String activities = location.getActivities();
        if (activities == null || activities.isBlank()) {
            location.setActivities(activityCode);
        } else if (!activities.contains(activityCode)) {
            location.setActivities(activities + "," + activityCode);
        }
    }

    private void validateDayOfWeek(int dayOfWeek) {
        if (dayOfWeek < MIN_DAY_OF_WEEK || dayOfWeek > MAX_DAY_OF_WEEK) {
            throw new IllegalArgumentException(
                    "dayOfWeek must be between "
                            + MIN_DAY_OF_WEEK
                            + " and "
                            + MAX_DAY_OF_WEEK
                            + ", got: "
                            + dayOfWeek);
        }
    }
}
