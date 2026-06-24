package com.ordermgmt.railway.domain.order.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ordermgmt.railway.domain.order.model.OrderPosition;

/** Repository for order positions. */
@Repository
public interface OrderPositionRepository extends JpaRepository<OrderPosition, UUID> {

    List<OrderPosition> findByOrderId(UUID orderId);

    /** Positions carrying a given OTN — used to attach inbound R2P orders to an existing train. */
    List<OrderPosition> findByOperationalTrainNumber(String operationalTrainNumber);

    @Query("SELECT DISTINCT op FROM OrderPosition op LEFT JOIN FETCH op.order")
    List<OrderPosition> findAllWithOrder();

    /** Resolves the owning order id for a position without loading the full aggregate. */
    @Query("SELECT op.order.id FROM OrderPosition op WHERE op.id = :id")
    Optional<UUID> findOrderIdById(@Param("id") UUID id);
}
