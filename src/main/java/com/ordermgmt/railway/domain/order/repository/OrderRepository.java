package com.ordermgmt.railway.domain.order.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ordermgmt.railway.domain.order.model.Order;
import com.ordermgmt.railway.domain.order.model.ProcessStatus;

/** Repository for orders and their revision history. */
@Repository
public interface OrderRepository
        extends JpaRepository<Order, UUID>, RevisionRepository<Order, UUID, Long> {

    Optional<Order> findByOrderNumber(String orderNumber);

    List<Order> findByProcessStatus(ProcessStatus processStatus);

    List<Order> findByCustomerId(UUID customerId);

    List<Order> findByNameContainingIgnoreCase(String name);

    /** (processStatus, count) buckets for the dashboard — one aggregate instead of a table scan. */
    @Query("SELECT o.processStatus, COUNT(o) FROM Order o GROUP BY o.processStatus")
    List<Object[]> countByProcessStatusGrouped();

    /** Non-final orders whose validity ends on/before the horizon (critical-deadline KPI). */
    @Query(
            "SELECT COUNT(o) FROM Order o WHERE (o.processStatus IS NULL OR o.processStatus <> "
                    + ":finalPhase) AND o.validTo IS NOT NULL AND o.validTo <= :horizon")
    long countCriticalDeadlines(
            @Param("finalPhase") ProcessStatus finalPhase, @Param("horizon") LocalDate horizon);
}
