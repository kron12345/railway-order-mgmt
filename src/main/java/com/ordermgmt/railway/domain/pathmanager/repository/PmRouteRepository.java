package com.ordermgmt.railway.domain.pathmanager.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ordermgmt.railway.domain.pathmanager.model.PmRoute;

/** Repository for TTT routes. */
@Repository
public interface PmRouteRepository extends JpaRepository<PmRoute, UUID> {

    List<PmRoute> findByReferenceTrainId(UUID referenceTrainId);
}
