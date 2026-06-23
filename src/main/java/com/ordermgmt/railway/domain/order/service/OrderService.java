package com.ordermgmt.railway.domain.order.service;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ordermgmt.railway.domain.order.model.Order;
import com.ordermgmt.railway.domain.order.model.OrderPosition;
import com.ordermgmt.railway.domain.order.model.ProcessStatus;
import com.ordermgmt.railway.domain.order.repository.OrderPositionRepository;
import com.ordermgmt.railway.domain.order.repository.OrderRepository;

import lombok.RequiredArgsConstructor;

/** Coordinates order and position persistence for the UI layer. */
@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderPositionRepository positionRepository;

    @Transactional(readOnly = true)
    public List<Order> findAllWithPositions() {
        List<Order> orders = orderRepository.findAll();
        initializePositions(orders);
        return orders;
    }

    @Transactional(readOnly = true)
    public Optional<Order> findById(UUID id) {
        return orderRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<OrderPosition> findPositionById(UUID id) {
        return positionRepository
                .findById(id)
                .map(
                        position -> {
                            position.getResourceNeeds().size();
                            initializePurchasePositions(List.of(position));
                            return position;
                        });
    }

    @Transactional(readOnly = true)
    public List<Order> findByProcessStatus(ProcessStatus status) {
        List<Order> orders = orderRepository.findByProcessStatus(status);
        initializePositions(orders);
        return orders;
    }

    @Transactional(readOnly = true)
    public List<Order> search(String query) {
        List<Order> orders = orderRepository.findByNameContainingIgnoreCase(query);
        initializePositions(orders);
        return orders;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public Order save(Order order) {
        return orderRepository.save(order);
    }

    /**
     * Sets the assignee for an order (mirrors {@code BusinessService.setAssignee}). {@code type} is
     * the {@code AssignmentType} name (USER/GROUP) or {@code null}; {@code name} the canonical
     * value. No-op when unchanged so re-renders do not churn the audit log.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public Order setAssignee(UUID id, String type, String name) {
        Order order = orderRepository.findById(id).orElseThrow();
        if (Objects.equals(order.getAssignmentType(), type)
                && Objects.equals(order.getAssignmentName(), name)) {
            return order;
        }
        order.setAssignmentType(type);
        order.setAssignmentName(name);
        return orderRepository.save(order);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public void delete(UUID id) {
        orderRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<OrderPosition> findPositionsByOrderId(UUID orderId) {
        List<OrderPosition> positions = positionRepository.findByOrderId(orderId);
        initializePurchasePositions(positions);
        return positions;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public OrderPosition savePosition(OrderPosition position) {
        return positionRepository.save(position);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public void deletePosition(UUID positionId) {
        positionRepository.deleteById(positionId);
    }

    @Transactional(readOnly = true)
    public long count() {
        return orderRepository.count();
    }

    /** Number of days within which an order's end of validity counts as a "critical deadline". */
    private static final int CRITICAL_DEADLINE_DAYS = 30;

    /**
     * Aggregated counts for the dashboard, computed from a single order scan to avoid one query per
     * KPI. "Active" excludes orders already in the final billing phase; a deadline is "critical"
     * when a still-active order's validity ends within {@value #CRITICAL_DEADLINE_DAYS} days.
     */
    public record DashboardStats(
            long total,
            long active,
            long inPlanning,
            long inProduction,
            long criticalDeadlines,
            Map<ProcessStatus, Long> byStatus) {}

    @Transactional(readOnly = true)
    public DashboardStats dashboardStats() {
        List<Order> all = orderRepository.findAll();
        LocalDate horizon = LocalDate.now().plusDays(CRITICAL_DEADLINE_DAYS);

        Map<ProcessStatus, Long> byStatus = new EnumMap<>(ProcessStatus.class);
        for (ProcessStatus s : ProcessStatus.values()) {
            byStatus.put(s, 0L);
        }

        long active = 0;
        long planning = 0;
        long production = 0;
        long critical = 0;
        for (Order order : all) {
            ProcessStatus status = order.getProcessStatus();
            if (status != null) {
                byStatus.merge(status, 1L, Long::sum);
            }
            boolean finalPhase = status == ProcessStatus.ABRECHNUNG_NACHBEREITUNG;
            if (!finalPhase) {
                active++;
            }
            if (status == ProcessStatus.PLANUNG) {
                planning++;
            }
            if (status == ProcessStatus.PRODUKTION) {
                production++;
            }
            if (!finalPhase && order.getValidTo() != null && !order.getValidTo().isAfter(horizon)) {
                critical++;
            }
        }
        return new DashboardStats(all.size(), active, planning, production, critical, byStatus);
    }

    private void initializePositions(List<Order> orders) {
        orders.forEach(
                order -> {
                    order.getPositions().size();
                    initializePurchasePositions(order.getPositions());
                });
    }

    private void initializePurchasePositions(List<OrderPosition> positions) {
        positions.forEach(
                position -> {
                    position.getPurchasePositions().size();
                    position.getResourceNeeds().size();
                });
    }
}
