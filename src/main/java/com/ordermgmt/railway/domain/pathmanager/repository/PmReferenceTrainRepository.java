package com.ordermgmt.railway.domain.pathmanager.repository;

import java.util.List;
import java.util.UUID;

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

    List<PmReferenceTrain> findBySourcePositionId(UUID sourcePositionId);
}
