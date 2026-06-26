package com.ordermgmt.railway.domain.order.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ordermgmt.railway.domain.order.model.Order;
import com.ordermgmt.railway.domain.order.model.PositionStatus;
import com.ordermgmt.railway.domain.order.model.ProcessStatus;
import com.ordermgmt.railway.dto.order.OrderListItem;

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

    /**
     * Lazy order list (P3): a {@code Slice} of projections (fetch pageSize+1, no total count) with
     * all filters optional and an aggregate position count via a correlated subquery — never by
     * initializing the positions collection. The Pageable carries paging + (stable) sort.
     */
    @Query(
            "select new com.ordermgmt.railway.dto.order.OrderListItem("
                    + "o.id, o.orderNumber, o.name, c.name, o.validFrom, o.validTo, "
                    + "o.processStatus, o.internalStatus, o.assignmentType, o.assignmentName, "
                    + "o.tags, o.createdAt, "
                    + "(select count(p) from OrderPosition p where p.order = o)) "
                    + "from Order o left join o.customer c "
                    + "where (:text is null "
                    + "  or lower(o.orderNumber) like lower(concat('%', :text, '%')) "
                    + "  or lower(o.name) like lower(concat('%', :text, '%')) "
                    + "  or lower(coalesce(o.tags, '')) like lower(concat('%', :text, '%')) "
                    + "  or lower(coalesce(c.name, '')) like lower(concat('%', :text, '%'))) "
                    + "and (:processStatus is null or o.processStatus = :processStatus) "
                    + "and (:internalStatus is null or o.internalStatus = :internalStatus) "
                    + "and (:validFromMin is null or o.validTo >= :validFromMin) "
                    + "and (:validToMax is null or o.validFrom <= :validToMax) "
                    + "and (:tags is null or lower(coalesce(o.tags, '')) like lower(concat('%', :tags, '%'))) "
                    + "and (:assignee is null "
                    + "  or (o.assignmentType = 'USER' and o.assignmentName = :assignee))")
    Slice<OrderListItem> searchOrders(
            @Param("text") String text,
            @Param("processStatus") ProcessStatus processStatus,
            @Param("internalStatus") PositionStatus internalStatus,
            @Param("validFromMin") LocalDate validFromMin,
            @Param("validToMax") LocalDate validToMax,
            @Param("tags") String tags,
            @Param("assignee") String assignee,
            Pageable pageable);
}
