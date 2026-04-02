package com.ordermgmt.railway.domain.pathmanager.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ordermgmt.railway.domain.pathmanager.model.PmPathRequest;

/** Repository for TTT path requests. */
@Repository
public interface PmPathRequestRepository extends JpaRepository<PmPathRequest, UUID> {

    List<PmPathRequest> findByReferenceTrainId(UUID referenceTrainId);
}
