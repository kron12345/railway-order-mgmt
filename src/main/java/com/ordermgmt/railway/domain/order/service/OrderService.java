package com.ordermgmt.railway.domain.order.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ordermgmt.railway.domain.order.expression.OrderExpressionSupport;
import com.ordermgmt.railway.domain.order.model.Order;
import com.ordermgmt.railway.domain.order.model.OrderPosition;
import com.ordermgmt.railway.domain.order.model.OrderPositionVersion;
import com.ordermgmt.railway.domain.order.model.PositionChangeSource;
import com.ordermgmt.railway.domain.order.model.PositionOtnHistory;
import com.ordermgmt.railway.domain.order.model.PositionStatus;
import com.ordermgmt.railway.domain.order.model.PositionVariantType;
import com.ordermgmt.railway.domain.order.model.ProcessStatus;
import com.ordermgmt.railway.domain.order.model.ValidityJsonCodec;
import com.ordermgmt.railway.domain.order.model.Weekdays;
import com.ordermgmt.railway.domain.order.repository.OrderPositionRepository;
import com.ordermgmt.railway.domain.order.repository.OrderPositionVersionRepository;
import com.ordermgmt.railway.domain.order.repository.OrderRepository;
import com.ordermgmt.railway.domain.order.repository.PositionOtnHistoryRepository;
import com.ordermgmt.railway.dto.order.CommandPaletteRow;
import com.ordermgmt.railway.dto.order.OrderListItem;
import com.ordermgmt.railway.dto.order.OrderListQuery;

import lombok.RequiredArgsConstructor;

/**
 * Coordinates order and position persistence for the UI layer. The expression (Ausprägung) cloning
 * and Verkehrstage date/text math live in {@link OrderExpressionSupport}; this service owns the
 * repositories, transactions and authorization around them.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    /** Number of days within which an order's end of validity counts as a "critical deadline". */
    private static final int CRITICAL_DEADLINE_DAYS = 30;

    private final OrderRepository orderRepository;
    private final OrderPositionRepository positionRepository;
    private final OrderPositionVersionRepository versionRepository;
    private final PositionOtnHistoryRepository otnHistoryRepository;

    /**
     * Flat (order, position) label rows for the ⌃K command palette — no collection initialization.
     */
    @Transactional(readOnly = true)
    public List<CommandPaletteRow> commandPaletteRows() {
        return orderRepository.findCommandPaletteRows();
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

    /**
     * Lazy order list (P3): a {@code Slice} of {@link OrderListItem} projections for the given
     * filter, with the sort made stable via an id tie-breaker so paging never skips/repeats. No
     * total count (Slice fetches pageSize+1) — the list shows "loaded / more", not a total.
     */
    @Transactional(readOnly = true)
    public Slice<OrderListItem> searchOrders(OrderListQuery query, Pageable pageable) {
        return orderRepository.searchOrders(
                blankToNull(query.text()),
                query.processStatus(),
                query.internalStatus(),
                query.validFromMin(),
                query.validToMax(),
                blankToNull(query.tags()),
                blankToNull(query.assignee()),
                query.orderType() == null ? null : query.orderType().name(),
                query.incompleteOnly(),
                stableSort(pageable, "orderNumber"));
    }

    /**
     * Appends an id tie-breaker (or a default field + id when unsorted) for stable paging. Rebuilds
     * the Pageable via {@code PageRequest.of(getPageNumber(), getPageSize())}; this preserves the
     * exact offset ONLY for page-aligned offsets (multiples of the page size). The lazy list
     * callers always append whole pages, so their offsets stay aligned — do not feed this a
     * non-aligned {@code OffsetPageable} or the reconstructed page will skip rows.
     */
    static Pageable stableSort(Pageable pageable, String defaultField) {
        Sort sort =
                pageable.getSort().isSorted()
                        ? pageable.getSort().and(Sort.by(Sort.Direction.ASC, "id"))
                        : Sort.by(Sort.Direction.ASC, defaultField)
                                .and(Sort.by(Sort.Direction.ASC, "id"));
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
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
        requireCostCenter(order.getInternalStatus(), order.getCostCenter());
    }

    /**
     * SOB §5.7 invariant in one place: a FREIGEGEBEN order must carry a Kostenträger/PSP-Element.
     */
    private void requireCostCenter(PositionStatus status, String costCenter) {
        if (status == PositionStatus.FREIGEGEBEN && (costCenter == null || costCenter.isBlank())) {
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
        requireCostCenter(status, order.getCostCenter());
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

    /** Thrown when a flat position that already has bookings is split into expressions. */
    public static class PositionHasBookingsException extends IllegalStateException {}

    /**
     * Creates a new expression (Ausprägung) as a clone of its parent train identity: copies the
     * parent's route/metadata so the type-appropriate editor (Fahrplan-Builder for FAHRPLAN,
     * service dialog for LEISTUNG) opens pre-filled and the user only edits the deviation. Promotes
     * a flat position to a ZUG on the first split. The timetable archive (FAHRPLAN) is cloned
     * separately by the caller ({@code TimetableArchiveService.cloneArchiveTo}) to avoid an
     * order→timetable cycle.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public OrderPosition addExpressionFromParent(UUID parentId) {
        OrderPosition parent = positionRepository.findById(parentId).orElseThrow();
        promoteFlatParent(parent);
        return positionRepository.save(OrderExpressionSupport.newExpression(parent));
    }

    private void promoteFlatParent(OrderPosition parent) {
        if (parent.getVariantType() != null) {
            return;
        }
        // A flat position's own purchase orders hang on it directly; splitting it would orphan
        // them, so only a position without real bookings may be promoted.
        if (parent.getPurchasePositions() != null && !parent.getPurchasePositions().isEmpty()) {
            throw new PositionHasBookingsException();
        }
        parent.setVariantType(PositionVariantType.ZUG);
        positionRepository.save(parent);
    }

    /** Picker context for an expression's Verkehrstage: calendar bounds, occupied days, current. */
    public record VerkehrstageContext(
            LocalDate min,
            LocalDate max,
            Map<LocalDate, String> occupied,
            List<LocalDate> current) {}

    /**
     * Builds the Verkehrstage-Picker context for an expression: calendar bounds from its train's
     * window plus the days already taken by sibling expressions (date → owning expression name), so
     * the picker can mark them and offer reassignment.
     */
    @Transactional(readOnly = true)
    public VerkehrstageContext verkehrstageContext(UUID expressionId) {
        OrderPosition expression = positionRepository.findById(expressionId).orElseThrow();
        OrderPosition parent = expression.getVariantOf();

        List<OrderPosition> siblings =
                parent != null ? positionRepository.findByVariantOfId(parent.getId()) : List.of();

        // Span the whole train window (parent + this expression + every sibling) so a day owned by
        // a sibling outside this expression's own range is still visible and reassignable.
        List<OrderPosition> windowSources = new ArrayList<>(siblings);
        windowSources.add(expression);
        if (parent != null) {
            windowSources.add(parent);
        }
        OrderPosition[] sources = windowSources.toArray(new OrderPosition[0]);
        LocalDate min = OrderExpressionSupport.earliestStart(sources);
        LocalDate max = OrderExpressionSupport.latestEnd(sources);
        if (min == null) {
            min = LocalDate.now();
        }
        if (max == null || max.isBefore(min)) {
            max = min.plusYears(1);
        }

        Map<LocalDate, String> occupied = new HashMap<>();
        for (OrderPosition sibling : siblings) {
            if (sibling.getId().equals(expressionId)) {
                continue;
            }
            for (LocalDate day : OrderExpressionSupport.effectiveDays(sibling)) {
                occupied.putIfAbsent(day, sibling.getName());
            }
        }
        // Pre-fill the edited expression's own effective days (validity, or weekday-derived when
        // unset) — symmetric with the sibling occupancy above, so opening the picker never shows an
        // empty selection for a weekday-template expression and a save can't silently drop it.
        return new VerkehrstageContext(
                min, max, occupied, OrderExpressionSupport.effectiveDays(expression));
    }

    /**
     * Sets an expression's Verkehrstage (date-set). Any day newly claimed from a sibling is removed
     * there and recorded as a {@code MODIFICATION} version on that sibling — so disjointness is
     * enforced date-precisely by reassignment instead of a hard reject.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public void setExpressionVerkehrstage(UUID expressionId, List<LocalDate> days) {
        OrderPosition expression = positionRepository.findById(expressionId).orElseThrow();
        Set<LocalDate> claimed = new HashSet<>(days);

        OrderPosition parent = expression.getVariantOf();
        if (parent != null) {
            reassignClaimedSiblingDays(parent, expression, claimed);
        }

        List<LocalDate> sorted = days.stream().sorted().toList();
        expression.setValidity(sorted.isEmpty() ? null : ValidityJsonCodec.toJson(sorted));
        expression.setWeekdays(Weekdays.format(OrderExpressionSupport.weekdaysOf(sorted)));
        positionRepository.save(expression);
    }

    private void reassignClaimedSiblingDays(
            OrderPosition parent, OrderPosition expression, Set<LocalDate> claimedDays) {
        for (OrderPosition sibling : positionRepository.findByVariantOfId(parent.getId())) {
            if (sibling.getId().equals(expression.getId())) {
                continue;
            }
            List<LocalDate> siblingDays = OrderExpressionSupport.effectiveDays(sibling);
            List<LocalDate> handedOverDays =
                    siblingDays.stream().filter(claimedDays::contains).sorted().toList();
            if (handedOverDays.isEmpty()) {
                continue;
            }
            List<LocalDate> remainingDays =
                    siblingDays.stream()
                            .filter(day -> !claimedDays.contains(day))
                            .sorted()
                            .toList();
            sibling.setValidity(
                    remainingDays.isEmpty() ? null : ValidityJsonCodec.toJson(remainingDays));
            sibling.setWeekdays(Weekdays.format(OrderExpressionSupport.weekdaysOf(remainingDays)));
            positionRepository.save(sibling);
            recordDayHandover(sibling, expression.getName(), handedOverDays);
        }
    }

    /** Records on the shortened sibling that it handed Verkehrstage to another expression. */
    private void recordDayHandover(
            OrderPosition sibling, String claimant, List<LocalDate> handedOverDays) {
        int next =
                versionRepository
                                .findByOrderPositionIdOrderByVersionNumberAsc(sibling.getId())
                                .stream()
                                .mapToInt(OrderPositionVersion::getVersionNumber)
                                .max()
                                .orElse(0)
                        + 1;
        OrderPositionVersion version = new OrderPositionVersion();
        version.setOrderPosition(sibling);
        version.setVersionNumber(next);
        version.setSource(PositionChangeSource.MODIFICATION);
        version.setChangeSummary(
                OrderExpressionSupport.dayHandoverSummary(claimant, handedOverDays));
        versionRepository.save(version);
    }

    /** Batched version trail per position (Map keyed by position id) — avoids one query per row. */
    @Transactional(readOnly = true)
    public Map<UUID, List<OrderPositionVersion>> findVersionsByPositions(List<UUID> positionIds) {
        if (positionIds.isEmpty()) {
            return Map.of();
        }
        return versionRepository.findByOrderPositionIdIn(positionIds).stream()
                .collect(Collectors.groupingBy(version -> version.getOrderPosition().getId()));
    }

    /** Batched OTN history per position (Map keyed by position id). */
    @Transactional(readOnly = true)
    public Map<UUID, List<PositionOtnHistory>> findOtnHistoryByPositions(List<UUID> positionIds) {
        if (positionIds.isEmpty()) {
            return Map.of();
        }
        return otnHistoryRepository.findByOrderPositionIdIn(positionIds).stream()
                .collect(Collectors.groupingBy(history -> history.getOrderPosition().getId()));
    }

    /** Bulk-sets the internal (Bearbeitungs-)status on several positions in one transaction. */
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public int setPositionInternalStatusBulk(Set<UUID> positionIds, PositionStatus status) {
        int updated = 0;
        for (UUID positionId : positionIds) {
            OrderPosition position = positionRepository.findById(positionId).orElse(null);
            if (position == null || position.getInternalStatus() == status) {
                continue;
            }
            position.setInternalStatus(status);
            positionRepository.save(position);
            updated++;
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
        for (ProcessStatus status : ProcessStatus.values()) {
            byStatus.put(status, 0L);
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
