package com.ordermgmt.railway.domain.pathmanager.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ordermgmt.railway.domain.pathmanager.model.PmPath;

/** Repository for TTT paths (offers and bookings). */
@Repository
public interface PmPathRepository extends JpaRepository<PmPath, UUID> {

    List<PmPath> findByReferenceTrainId(UUID referenceTrainId);

    List<PmPath> findByPathRequestId(UUID pathRequestId);
}
