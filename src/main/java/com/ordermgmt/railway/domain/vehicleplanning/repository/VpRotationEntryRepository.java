package com.ordermgmt.railway.domain.vehicleplanning.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ordermgmt.railway.domain.vehicleplanning.model.VpRotationEntry;

/** Repository for rotation entries mapping trains to vehicles. */
@Repository
public interface VpRotationEntryRepository extends JpaRepository<VpRotationEntry, UUID> {

    List<VpRotationEntry> findByVehicleIdOrderByDayOfWeekAscSequenceInDayAsc(UUID vehicleId);

    List<VpRotationEntry> findByVehicleIdAndDayOfWeekOrderBySequenceInDayAsc(
            UUID vehicleId, int dayOfWeek);

    List<VpRotationEntry> findByVehicleRotationSetId(UUID rotationSetId);
}
