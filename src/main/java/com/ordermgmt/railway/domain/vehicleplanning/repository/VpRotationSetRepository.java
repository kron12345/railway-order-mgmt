package com.ordermgmt.railway.domain.vehicleplanning.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ordermgmt.railway.domain.vehicleplanning.model.VpRotationSet;

/** Repository for vehicle rotation sets. */
@Repository
public interface VpRotationSetRepository extends JpaRepository<VpRotationSet, UUID> {

    List<VpRotationSet> findByTimetableYearIdOrderByNameAsc(UUID timetableYearId);
}
