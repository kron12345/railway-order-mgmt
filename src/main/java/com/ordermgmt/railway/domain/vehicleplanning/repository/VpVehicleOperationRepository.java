package com.ordermgmt.railway.domain.vehicleplanning.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ordermgmt.railway.domain.vehicleplanning.model.VpVehicleOperation;

/** Repository for vehicle operations attached to rotation entries. */
@Repository
public interface VpVehicleOperationRepository extends JpaRepository<VpVehicleOperation, UUID> {

    List<VpVehicleOperation> findByRotationEntryId(UUID rotationEntryId);
}
