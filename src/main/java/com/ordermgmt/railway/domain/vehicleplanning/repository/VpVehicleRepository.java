package com.ordermgmt.railway.domain.vehicleplanning.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ordermgmt.railway.domain.vehicleplanning.model.VpVehicle;

/** Repository for vehicles within rotation sets. */
@Repository
public interface VpVehicleRepository extends JpaRepository<VpVehicle, UUID> {

    List<VpVehicle> findByRotationSetIdOrderBySequenceAsc(UUID rotationSetId);

    /**
     * Loads vehicles with their rotation entries and the associated reference trains eagerly. This
     * avoids LazyInitializationException when accessing vehicle data outside a transaction (e.g.,
     * from the Vaadin UI thread).
     */
    @Query(
            "SELECT DISTINCT v FROM VpVehicle v"
                    + " LEFT JOIN FETCH v.entries e"
                    + " LEFT JOIN FETCH e.referenceTrain"
                    + " WHERE v.rotationSet.id = :rotationSetId"
                    + " ORDER BY v.sequence ASC")
    List<VpVehicle> findByRotationSetIdWithEntries(@Param("rotationSetId") UUID rotationSetId);
}
