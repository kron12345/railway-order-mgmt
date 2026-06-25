package com.ordermgmt.railway.domain.order.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ordermgmt.railway.domain.order.model.AutoOrderLog;

/** Repository for the auto-order idempotency log. */
@Repository
public interface AutoOrderLogRepository extends JpaRepository<AutoOrderLog, UUID> {

    boolean existsByOrderPositionIdAndFristRegelId(UUID orderPositionId, UUID fristRegelId);
}
