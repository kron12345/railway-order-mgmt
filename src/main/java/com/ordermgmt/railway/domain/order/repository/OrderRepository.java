package com.ordermgmt.railway.domain.order.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.stereotype.Repository;

import com.ordermgmt.railway.domain.order.model.Order;
import com.ordermgmt.railway.domain.order.model.OrderStatus;

@Repository
public interface OrderRepository
        extends JpaRepository<Order, UUID>, RevisionRepository<Order, UUID, Long> {

    Optional<Order> findByOrderNumber(String orderNumber);

    List<Order> findByStatus(OrderStatus status);
}
