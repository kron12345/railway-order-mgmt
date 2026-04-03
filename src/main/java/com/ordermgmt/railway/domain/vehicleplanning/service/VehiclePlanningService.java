package com.ordermgmt.railway.domain.vehicleplanning.service;

import java.util.List;
import java.util.UUID;

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

    @Transactional(readOnly = true)
    public List<VpRotationSet> getRotationSets(UUID timetableYearId) {
        return rotationSetRepo.findByTimetableYearIdOrderByNameAsc(timetableYearId);
    }

    @Transactional(readOnly = true)
    public VpRotationSet getRotationSet(UUID rotationSetId) {
        return rotationSetRepo
                .findById(rotationSetId)
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "Rotation set not found: " + rotationSetId));
    }

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

    public VpRotationSet saveRotationSet(VpRotationSet rotationSet) {
        return rotationSetRepo.save(rotationSet);
    }

    public void deleteRotationSet(UUID rotationSetId) {
        rotationSetRepo.deleteById(rotationSetId);
    }

    // --- Vehicle CRUD ---

    @Transactional(readOnly = true)
    public List<VpVehicle> getVehicles(UUID rotationSetId) {
        return vehicleRepo.findByRotationSetIdOrderBySequenceAsc(rotationSetId);
    }

    public VpVehicle saveVehicle(VpVehicle vehicle) {
        return vehicleRepo.save(vehicle);
    }

    public void deleteVehicle(UUID vehicleId) {
        vehicleRepo.deleteById(vehicleId);
    }

    // --- Available trains ---

    @Transactional(readOnly = true)
    public List<PmReferenceTrain> getAvailableTrains(int year) {
        return trainRepo.findByTimetableYearYearOrderByOperationalTrainNumberAsc(year);
    }

    // --- Entry management ---

    public VpRotationEntry addTrainToVehicle(
            UUID vehicleId, UUID trainId, int dayOfWeek, CouplingPosition coupling) {
        VpVehicle vehicle =
                vehicleRepo
                        .findById(vehicleId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Vehicle not found: " + vehicleId));
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

    public VpRotationEntry moveEntry(UUID entryId, UUID targetVehicleId, int targetDay) {
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

        List<VpRotationEntry> existing =
                entryRepo.findByVehicleIdAndDayOfWeekOrderBySequenceInDayAsc(
                        targetVehicleId, targetDay);
        int nextSeq = existing.isEmpty() ? 0 : existing.getLast().getSequenceInDay() + 1;

        entry.setVehicle(target);
        entry.setDayOfWeek(targetDay);
        entry.setSequenceInDay(nextSeq);
        return entryRepo.save(entry);
    }

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

                setAssociatedOtnOnLastLocation(currentTrain, nextOtn, "0045");
                setAssociatedOtnOnFirstLocation(nextTrain, currentOtn, "0044");
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
}
