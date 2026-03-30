package com.ordermgmt.railway.domain.infrastructure.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ordermgmt.railway.domain.infrastructure.model.ImportLog;

@Repository
public interface ImportLogRepository extends JpaRepository<ImportLog, UUID> {

    List<ImportLog> findAllByOrderByStartedAtDesc();
}
