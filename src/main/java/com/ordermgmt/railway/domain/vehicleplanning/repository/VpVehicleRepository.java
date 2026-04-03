package com.ordermgmt.railway.domain.vehicleplanning.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ordermgmt.railway.domain.vehicleplanning.model.VpVehicle;

/** Repository for vehicles within rotation sets. */
@Repository
public interface VpVehicleRepository extends JpaRepository<VpVehicle, UUID> {

    List<VpVehicle> findByRotationSetIdOrderBySequenceAsc(UUID rotationSetId);
}
