package com.ordermgmt.railway.domain.pathmanager.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ordermgmt.railway.domain.pathmanager.model.PmProcessStep;

/** Repository for process step audit records. */
@Repository
public interface PmProcessStepRepository extends JpaRepository<PmProcessStep, UUID> {

    List<PmProcessStep> findByReferenceTrainIdOrderByCreatedAtDesc(UUID referenceTrainId);

    List<PmProcessStep> findByPathRequestIdOrderByCreatedAtDesc(UUID pathRequestId);
}
