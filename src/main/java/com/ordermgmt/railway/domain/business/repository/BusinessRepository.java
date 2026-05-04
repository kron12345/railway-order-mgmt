package com.ordermgmt.railway.domain.business.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.ordermgmt.railway.domain.business.model.Business;

/** Repository for persisting and querying businesses. */
@Repository
public interface BusinessRepository extends JpaRepository<Business, UUID> {

    @Query("SELECT b.id, SIZE(b.orderPositions), SIZE(b.purchasePositions) FROM Business b")
    List<Object[]> findAllLinkCounts();

    /** All businesses linked to the given OrderPosition via the M2M join table. */
    @Query("SELECT DISTINCT b FROM Business b JOIN b.orderPositions op WHERE op.id = :opId")
    List<Business> findByLinkedOrderPositionId(UUID opId);

    /** All businesses linked to the given PurchasePosition via the M2M join table. */
    @Query("SELECT DISTINCT b FROM Business b JOIN b.purchasePositions pp WHERE pp.id = :ppId")
    List<Business> findByLinkedPurchasePositionId(UUID ppId);

    /**
     * All businesses that link to anything under the given Order — either an
     * OrderPosition of the order, or a PurchasePosition under one of its OPs.
     */
    @Query("SELECT DISTINCT b FROM Business b "
            + "LEFT JOIN b.orderPositions op "
            + "LEFT JOIN b.purchasePositions pp "
            + "WHERE op.order.id = :orderId OR pp.orderPosition.order.id = :orderId")
    List<Business> findByLinkedOrderId(UUID orderId);
}
