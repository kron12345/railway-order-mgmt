package com.ordermgmt.railway.domain.timetable.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ordermgmt.railway.domain.timetable.model.TimetableArchive;

/** Repository for archived timetable table data. */
@Repository
public interface TimetableArchiveRepository extends JpaRepository<TimetableArchive, UUID> {}
