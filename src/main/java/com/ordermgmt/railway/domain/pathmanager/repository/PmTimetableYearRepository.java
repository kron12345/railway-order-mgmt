package com.ordermgmt.railway.domain.pathmanager.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ordermgmt.railway.domain.pathmanager.model.PmTimetableYear;

/** Repository for timetable year periods. */
@Repository
public interface PmTimetableYearRepository extends JpaRepository<PmTimetableYear, UUID> {

    Optional<PmTimetableYear> findByYear(int year);
}
