package com.ordermgmt.railway.domain.order.service;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ordermgmt.railway.domain.order.model.Order;
import com.ordermgmt.railway.domain.order.model.OrderPosition;
import com.ordermgmt.railway.domain.order.model.OrderPositionVersion;
import com.ordermgmt.railway.domain.order.model.PositionOtnHistory;
import com.ordermgmt.railway.domain.order.model.PositionStatus;
import com.ordermgmt.railway.domain.order.model.ProcessStatus;
import com.ordermgmt.railway.domain.order.repository.OrderPositionRepository;
import com.ordermgmt.railway.domain.order.repository.OrderPositionVersionRepository;
import com.ordermgmt.railway.domain.order.repository.OrderRepository;
import com.ordermgmt.railway.domain.order.repository.PositionOtnHistoryRepository;

import lombok.RequiredArgsConstructor;

/** Coordinates order and position persistence for the UI layer. */
@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderPositionRepository positionRepository;
    private final OrderPositionVersionRepository versionRepository;
    private final PositionOtnHistoryRepository otnHistoryRepository;

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
        requireCostCenterIfReleased(order);
        return orderRepository.save(order);
    }

    /**
     * Enforces SOB §5.7 on every persist path (not just the status transition): a {@code
     * FREIGEGEBEN} order must always carry a Kostenträger, so it cannot be cleared via a normal
     * edit-save.
     */
    private void requireCostCenterIfReleased(Order order) {
        if (order.getInternalStatus() == PositionStatus.FREIGEGEBEN
                && (order.getCostCenter() == null || order.getCostCenter().isBlank())) {
            throw new CostCenterRequiredException();
        }
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

    /** Thrown when an order is moved to {@code FREIGEGEBEN} without a Kostenträger (SOB §5.7). */
    public static class CostCenterRequiredException extends IllegalStateException {}

    /**
     * Sets the internal "Bearbeitungs-Status" of the order. Enforces SOB §5.7: an order may only
     * become {@code FREIGEGEBEN} once a Kostenträger/PSP-Element is set. No-op when unchanged.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public Order setInternalStatus(UUID id, PositionStatus status) {
        Order order = orderRepository.findById(id).orElseThrow();
        if (order.getInternalStatus() == status) {
            return order;
        }
        if (status == PositionStatus.FREIGEGEBEN
                && (order.getCostCenter() == null || order.getCostCenter().isBlank())) {
            throw new CostCenterRequiredException();
        }
        order.setInternalStatus(status);
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

    /** Batched version trail per position (Map keyed by position id) — avoids one query per row. */
    @Transactional(readOnly = true)
    public Map<UUID, List<OrderPositionVersion>> findVersionsByPositions(List<UUID> positionIds) {
        if (positionIds.isEmpty()) {
            return Map.of();
        }
        return versionRepository.findByOrderPositionIdIn(positionIds).stream()
                .collect(Collectors.groupingBy(v -> v.getOrderPosition().getId()));
    }

    /** Batched OTN history per position (Map keyed by position id). */
    @Transactional(readOnly = true)
    public Map<UUID, List<PositionOtnHistory>> findOtnHistoryByPositions(List<UUID> positionIds) {
        if (positionIds.isEmpty()) {
            return Map.of();
        }
        return otnHistoryRepository.findByOrderPositionIdIn(positionIds).stream()
                .collect(Collectors.groupingBy(h -> h.getOrderPosition().getId()));
    }

    /** Bulk-sets the internal (Bearbeitungs-)status on several positions in one transaction. */
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public int setPositionInternalStatusBulk(Set<UUID> positionIds, PositionStatus status) {
        int updated = 0;
        for (UUID id : positionIds) {
            OrderPosition position = positionRepository.findById(id).orElse(null);
            if (position != null && position.getInternalStatus() != status) {
                position.setInternalStatus(status);
                positionRepository.save(position);
                updated++;
            }
        }
        return updated;
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
        LocalDate horizon = LocalDate.now().plusDays(CRITICAL_DEADLINE_DAYS);

        Map<ProcessStatus, Long> byStatus = new EnumMap<>(ProcessStatus.class);
        for (ProcessStatus s : ProcessStatus.values()) {
            byStatus.put(s, 0L);
        }
        long total = 0;
        for (Object[] row : orderRepository.countByProcessStatusGrouped()) {
            long count = (Long) row[1];
            total += count;
            if (row[0] != null) {
                byStatus.put((ProcessStatus) row[0], count);
            }
        }
        long inFinalPhase = byStatus.getOrDefault(ProcessStatus.ABRECHNUNG_NACHBEREITUNG, 0L);
        long active = total - inFinalPhase;
        long planning = byStatus.getOrDefault(ProcessStatus.PLANUNG, 0L);
        long production = byStatus.getOrDefault(ProcessStatus.PRODUKTION, 0L);
        long critical =
                orderRepository.countCriticalDeadlines(
                        ProcessStatus.ABRECHNUNG_NACHBEREITUNG, horizon);
        return new DashboardStats(total, active, planning, production, critical, byStatus);
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
