package com.ordermgmt.railway.domain.pathmanager.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ordermgmt.railway.domain.pathmanager.model.PmTrainVersion;

/** Repository for train version snapshots. */
@Repository
public interface PmTrainVersionRepository extends JpaRepository<PmTrainVersion, UUID> {

    List<PmTrainVersion> findByReferenceTrainIdOrderByVersionNumberDesc(UUID referenceTrainId);

    /** Eagerly fetches journey locations to avoid LazyInitializationException in DTOs. */
    @EntityGraph(attributePaths = "journeyLocations")
    @Query(
            "SELECT v FROM PmTrainVersion v WHERE v.referenceTrain.id = :trainId"
                    + " ORDER BY v.versionNumber DESC")
    List<PmTrainVersion> findWithLocationsByReferenceTrainId(@Param("trainId") UUID trainId);

    Optional<PmTrainVersion> findFirstByReferenceTrainIdOrderByVersionNumberDesc(
            UUID referenceTrainId);
}
