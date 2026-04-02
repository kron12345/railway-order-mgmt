package com.ordermgmt.railway.domain.pathmanager.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ordermgmt.railway.domain.pathmanager.model.PmJourneyLocation;

/** Repository for journey locations within train versions. */
@Repository
public interface PmJourneyLocationRepository extends JpaRepository<PmJourneyLocation, UUID> {

    List<PmJourneyLocation> findByTrainVersionIdOrderBySequenceAsc(UUID trainVersionId);
}
