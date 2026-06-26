package com.ordermgmt.railway.domain.pathmanager.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.stereotype.Repository;

import com.ordermgmt.railway.domain.pathmanager.model.PmReferenceTrain;

/** Repository for TTT reference trains and their revision history. */
@Repository
public interface PmReferenceTrainRepository
        extends JpaRepository<PmReferenceTrain, UUID>,
                RevisionRepository<PmReferenceTrain, UUID, Long> {

    List<PmReferenceTrain> findByTimetableYearYearOrderByOperationalTrainNumberAsc(int year);

    /**
     * One page of a year's trains (P6: the TreeGrid fetches a page from the DB, not the full list).
     */
    List<PmReferenceTrain> findByTimetableYearYearOrderByOperationalTrainNumberAsc(
            int year, Pageable pageable);

    long countByTimetableYearYear(int year);

    List<PmReferenceTrain> findBySourcePositionId(UUID sourcePositionId);

    /** Reference trains not yet captured as an order position ("nicht zugewiesen"). */
    List<PmReferenceTrain> findBySourcePositionIdIsNull();
}
