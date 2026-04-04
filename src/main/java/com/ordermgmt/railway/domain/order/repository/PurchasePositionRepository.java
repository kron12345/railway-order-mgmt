package com.ordermgmt.railway.domain.order.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ordermgmt.railway.domain.order.model.PurchasePosition;

/** Repository for purchase positions linked to resource needs. */
@Repository
public interface PurchasePositionRepository extends JpaRepository<PurchasePosition, UUID> {

    List<PurchasePosition> findByOrderPositionId(UUID orderPositionId);

    List<PurchasePosition> findByResourceNeedId(UUID resourceNeedId);

    List<PurchasePosition> findByOrderPositionIdAndPmPathRequestIdIsNotNull(UUID orderPositionId);

    long countByPositionNumberStartingWith(String prefix);
}
