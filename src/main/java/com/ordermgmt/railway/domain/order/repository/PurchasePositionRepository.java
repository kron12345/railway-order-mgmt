package com.ordermgmt.railway.domain.order.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ordermgmt.railway.domain.order.model.PurchasePosition;
import com.ordermgmt.railway.domain.order.model.PurchaseStatus;

/** Repository for purchase positions linked to resource needs. */
@Repository
public interface PurchasePositionRepository extends JpaRepository<PurchasePosition, UUID> {

    List<PurchasePosition> findByOrderPositionId(UUID orderPositionId);

    List<PurchasePosition> findByResourceNeedId(UUID resourceNeedId);

    List<PurchasePosition> findByOrderPositionIdAndPmPathRequestIdIsNotNull(UUID orderPositionId);

    long countByPositionNumberStartingWith(String prefix);

    @Query(
            "SELECT DISTINCT pp FROM PurchasePosition pp "
                    + "LEFT JOIN FETCH pp.orderPosition op "
                    + "LEFT JOIN FETCH op.order")
    List<PurchasePosition> findAllWithOrderPosition();

    /**
     * "Open" purchase positions for the open-positions overview: not yet procured ({@code open}) or
     * a TTT path that is not yet booked.
     */
    @Query(
            "SELECT DISTINCT pp FROM PurchasePosition pp "
                    + "LEFT JOIN FETCH pp.orderPosition op "
                    + "LEFT JOIN FETCH op.order "
                    + "WHERE pp.purchaseStatus = :open "
                    + "OR (pp.pmPathRequestId IS NOT NULL "
                    + "    AND (pp.pmProcessState IS NULL OR pp.pmProcessState <> :booked))")
    List<PurchasePosition> findOpenWithOrder(
            @Param("open") PurchaseStatus open, @Param("booked") String booked);
}
