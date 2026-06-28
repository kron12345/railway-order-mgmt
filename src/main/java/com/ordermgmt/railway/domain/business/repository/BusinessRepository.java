package com.ordermgmt.railway.domain.business.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ordermgmt.railway.domain.business.model.Business;
import com.ordermgmt.railway.domain.business.model.BusinessStatus;
import com.ordermgmt.railway.dto.business.BusinessListItem;

/** Repository for persisting and querying businesses. */
@Repository
public interface BusinessRepository extends JpaRepository<Business, UUID> {

    /**
     * Lazy business list (P3): a {@code Slice} of projections (fetch pageSize+1, no total count)
     * with all filters optional and the linked-position counts via {@code SIZE(...)} — never by
     * initializing the n:m collections. The Pageable carries paging + (stable) sort.
     */
    @Query(
            "select new com.ordermgmt.railway.dto.business.BusinessListItem("
                    + "b.id, b.title, b.status, b.validTo, b.dueDate, "
                    + "b.assignmentType, b.assignmentName, b.tags, "
                    + "size(b.orderPositions), size(b.purchasePositions), b.automatic) "
                    + "from Business b "
                    + "where (:text is null "
                    + "  or lower(b.title) like lower(concat('%', cast(:text as string), '%')) "
                    + "  or lower(coalesce(b.description, '')) like lower(concat('%', cast(:text as string), '%')) "
                    + "  or lower(coalesce(b.tags, '')) like lower(concat('%', cast(:text as string), '%'))) "
                    + "and (:status is null or b.status = :status) "
                    + "and (:validFromMin is null or b.validTo is null or b.validTo >= :validFromMin) "
                    + "and (:validToMax is null or b.validFrom is null or b.validFrom <= :validToMax) "
                    + "and (:tags is null or lower(coalesce(b.tags, '')) like lower(concat('%', cast(:tags as string), '%'))) "
                    + "and (:assignee is null "
                    + "  or (b.assignmentType = 'USER' and b.assignmentName = :assignee))")
    Slice<BusinessListItem> searchBusinesses(
            @Param("text") String text,
            @Param("status") BusinessStatus status,
            @Param("validFromMin") LocalDate validFromMin,
            @Param("validToMax") LocalDate validToMax,
            @Param("tags") String tags,
            @Param("assignee") String assignee,
            Pageable pageable);

    /** The automatic business driven by the given deadline rule, if it has been materialized. */
    java.util.Optional<Business> findByFristRegelId(UUID fristRegelId);

    /** All businesses linked to the given OrderPosition via the M2M join table. */
    @Query("SELECT DISTINCT b FROM Business b JOIN b.orderPositions op WHERE op.id = :opId")
    List<Business> findByLinkedOrderPositionId(UUID opId);

    /** (orderPositionId, business) pairs for a batch of positions — one query instead of N. */
    @Query("SELECT op.id, b FROM Business b JOIN b.orderPositions op WHERE op.id IN :opIds")
    List<Object[]> findBusinessesByOrderPositionIds(@Param("opIds") Collection<UUID> opIds);

    /** (purchasePositionId, business) pairs for a batch of purchases — one query instead of N. */
    @Query("SELECT pp.id, b FROM Business b JOIN b.purchasePositions pp WHERE pp.id IN :ppIds")
    List<Object[]> findBusinessesByPurchasePositionIds(@Param("ppIds") Collection<UUID> ppIds);

    /**
     * All businesses that link to anything under the given Order — either an OrderPosition of the
     * order, or a PurchasePosition under one of its OPs.
     */
    @Query(
            "SELECT DISTINCT b FROM Business b "
                    + "LEFT JOIN b.orderPositions op "
                    + "LEFT JOIN b.purchasePositions pp "
                    + "WHERE op.order.id = :orderId OR pp.orderPosition.order.id = :orderId")
    List<Business> findByLinkedOrderId(UUID orderId);
}
