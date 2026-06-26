package com.ordermgmt.railway.domain.order.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ordermgmt.railway.domain.order.model.OrderPosition;
import com.ordermgmt.railway.domain.order.model.PositionVariantType;

/** Repository for order positions. */
@Repository
public interface OrderPositionRepository extends JpaRepository<OrderPosition, UUID> {

    List<OrderPosition> findByOrderId(UUID orderId);

    /**
     * All positions of a type (e.g. FAHRPLAN), with the owning order AND purchase positions fetched
     * — deadline evaluation reads both per position, so fetching them avoids an N+1 over purchases.
     */
    @Query(
            "SELECT DISTINCT op FROM OrderPosition op "
                    + "LEFT JOIN FETCH op.order "
                    + "LEFT JOIN FETCH op.purchasePositions "
                    + "WHERE op.type = :type")
    List<OrderPosition> findByType(
            @Param("type") com.ordermgmt.railway.domain.order.model.PositionType type);

    /** Child expressions (Ausprägungen) of a train-identity position. */
    List<OrderPosition> findByVariantOfId(UUID variantOfId);

    /** Positions of a given hierarchy role (e.g. ZUG); pass null for legacy flat positions. */
    List<OrderPosition> findByVariantType(PositionVariantType variantType);

    /** Positions carrying a given OTN — used to attach inbound R2P orders to an existing train. */
    List<OrderPosition> findByOperationalTrainNumber(String operationalTrainNumber);

    @Query("SELECT DISTINCT op FROM OrderPosition op LEFT JOIN FETCH op.order")
    List<OrderPosition> findAllWithOrder();

    /** Resolves the owning order id for a position without loading the full aggregate. */
    @Query("SELECT op.order.id FROM OrderPosition op WHERE op.id = :id")
    Optional<UUID> findOrderIdById(@Param("id") UUID id);
}
