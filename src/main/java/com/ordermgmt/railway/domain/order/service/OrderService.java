package com.ordermgmt.railway.domain.order.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ordermgmt.railway.domain.order.model.Order;
import com.ordermgmt.railway.domain.order.model.OrderPosition;
import com.ordermgmt.railway.domain.order.model.ProcessStatus;
import com.ordermgmt.railway.domain.order.repository.OrderPositionRepository;
import com.ordermgmt.railway.domain.order.repository.OrderRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderPositionRepository positionRepository;

    @Transactional(readOnly = true)
    public List<Order> findAll() {
        return orderRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Order> findAllWithPositions() {
        List<Order> orders = orderRepository.findAll();
        orders.forEach(o -> o.getPositions().size());
        return orders;
    }

    public Optional<Order> findById(UUID id) {
        return orderRepository.findById(id);
    }

    public List<Order> findByProcessStatus(ProcessStatus status) {
        return orderRepository.findByProcessStatus(status);
    }

    public List<Order> search(String query) {
        return orderRepository.findByNameContainingIgnoreCase(query);
    }

    public Order save(Order order) {
        return orderRepository.save(order);
    }

    public void delete(UUID id) {
        orderRepository.deleteById(id);
    }

    public List<OrderPosition> findPositionsByOrderId(UUID orderId) {
        return positionRepository.findByOrderId(orderId);
    }

    public OrderPosition savePosition(OrderPosition position) {
        return positionRepository.save(position);
    }

    public void deletePosition(UUID positionId) {
        positionRepository.deleteById(positionId);
    }

    public long count() {
        return orderRepository.count();
    }

    public long countByStatus(ProcessStatus status) {
        return orderRepository.findByProcessStatus(status).size();
    }
}
