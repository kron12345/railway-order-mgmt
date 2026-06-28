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
import com.ordermgmt.railway.dto.order.CommandPaletteRow;
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

    /**
     * Flat (order, position) rows for the ⌃K command palette — only the searchable labels, via a
     * left join, so the positions/purchase/resource collections are never initialized.
     */
    @Query(
            "select new com.ordermgmt.railway.dto.order.CommandPaletteRow("
                    + "o.id, o.orderNumber, o.name, c.name, p.id, p.name) "
                    + "from Order o left join o.customer c left join o.positions p "
                    + "order by o.orderNumber")
    List<CommandPaletteRow> findCommandPaletteRows();

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
                    + "  or lower(o.orderNumber) like lower(concat('%', cast(:text as string), '%')) "
                    + "  or lower(o.name) like lower(concat('%', cast(:text as string), '%')) "
                    + "  or lower(coalesce(o.tags, '')) like lower(concat('%', cast(:text as string), '%')) "
                    + "  or lower(coalesce(c.name, '')) like lower(concat('%', cast(:text as string), '%'))) "
                    + "and (:processStatus is null or o.processStatus = :processStatus) "
                    + "and (:internalStatus is null or o.internalStatus = :internalStatus) "
                    // Cast the null-guard occurrence so PostgreSQL can infer the bind type: a bare
                    // `? is null` for a date param has no type context and fails parse with 42P18
                    // ("could not determine data type"). Mirrors the cast(:text as string) above.
                    + "and (cast(:validFromMin as LocalDate) is null or o.validTo >= :validFromMin) "
                    + "and (cast(:validToMax as LocalDate) is null or o.validFrom <= :validToMax) "
                    + "and (:tags is null or lower(coalesce(o.tags, '')) like lower(concat('%', cast(:tags as string), '%'))) "
                    + "and (:assignee is null "
                    + "  or (o.assignmentType = 'USER' and o.assignmentName = :assignee)) "
                    // B2 derived order type (no column): re-derive Jahres/Einzel from the lead time
                    // between order date and first validity day, mirroring OrderType.ofDates.
                    + "and (:orderType is null or (o.createdAt is not null and o.validFrom is not null "
                    + "  and ((:orderType = 'EINZELBESTELLUNG' "
                    + "         and cast(o.createdAt as LocalDate) > (o.validFrom - 2 month)) "
                    + "    or (:orderType = 'JAHRESBESTELLUNG' "
                    + "         and cast(o.createdAt as LocalDate) <= (o.validFrom - 2 month))))) "
                    // B2 "order incomplete" (SOB §5.7): at least one purchase position not yet
                    // BOOKED.
                    + "and (:incompleteOnly = false or exists ("
                    + "  select 1 from PurchasePosition pp where pp.orderPosition.order = o "
                    + "  and (pp.pmProcessState is null or pp.pmProcessState <> 'BOOKED')))")
    Slice<OrderListItem> searchOrders(
            @Param("text") String text,
            @Param("processStatus") ProcessStatus processStatus,
            @Param("internalStatus") PositionStatus internalStatus,
            @Param("validFromMin") LocalDate validFromMin,
            @Param("validToMax") LocalDate validToMax,
            @Param("tags") String tags,
            @Param("assignee") String assignee,
            @Param("orderType") String orderType,
            @Param("incompleteOnly") boolean incompleteOnly,
            Pageable pageable);
}
