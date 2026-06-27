package com.ordermgmt.railway.domain.pathmanager.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ordermgmt.railway.domain.pathmanager.model.PmJourneyLocation;

/** Repository for journey locations within train versions. */
@Repository
public interface PmJourneyLocationRepository extends JpaRepository<PmJourneyLocation, UUID> {

    List<PmJourneyLocation> findByTrainVersionIdOrderBySequenceAsc(UUID trainVersionId);

    /** One page of a version's journey locations for paged TreeGrid loading. */
    List<PmJourneyLocation> findByTrainVersionIdOrderBySequenceAsc(
            UUID trainVersionId, Pageable pageable);

    long countByTrainVersionId(UUID trainVersionId);
}
