package com.ordermgmt.railway.domain.order.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.ordermgmt.railway.domain.order.model.OrderPosition;

/** Repository for order positions. */
@Repository
public interface OrderPositionRepository extends JpaRepository<OrderPosition, UUID> {

    List<OrderPosition> findByOrderId(UUID orderId);

    @Query("SELECT DISTINCT op FROM OrderPosition op LEFT JOIN FETCH op.order")
    List<OrderPosition> findAllWithOrder();
}
