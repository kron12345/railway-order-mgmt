package com.ordermgmt.railway.domain.pathmanager.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ordermgmt.railway.domain.pathmanager.model.PmTrainVersion;

/** Repository for train version snapshots. */
@Repository
public interface PmTrainVersionRepository extends JpaRepository<PmTrainVersion, UUID> {

    List<PmTrainVersion> findByReferenceTrainIdOrderByVersionNumberDesc(UUID referenceTrainId);

    Optional<PmTrainVersion> findFirstByReferenceTrainIdOrderByVersionNumberDesc(
            UUID referenceTrainId);
}
