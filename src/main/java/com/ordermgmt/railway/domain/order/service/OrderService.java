package com.ordermgmt.railway.domain.order.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
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
import com.ordermgmt.railway.domain.order.model.PositionChangeSource;
import com.ordermgmt.railway.domain.order.model.PositionOtnHistory;
import com.ordermgmt.railway.domain.order.model.PositionStatus;
import com.ordermgmt.railway.domain.order.model.PositionType;
import com.ordermgmt.railway.domain.order.model.PositionVariantType;
import com.ordermgmt.railway.domain.order.model.ProcessStatus;
import com.ordermgmt.railway.domain.order.model.ValidityJsonCodec;
import com.ordermgmt.railway.domain.order.model.Weekdays;
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

    /** Data for a new expression (Ausprägung) under a train identity. */
    public record ExpressionDraft(
            String name,
            String otn,
            String weekdays,
            String fromLocation,
            String toLocation,
            LocalDateTime start,
            LocalDateTime end) {}

    /** Thrown when a flat position that already has bookings is split into expressions. */
    public static class PositionHasBookingsException extends IllegalStateException {}

    /** Thrown when a new expression shares Verkehrstage with a sibling on overlapping dates. */
    public static class ExpressionConflictException extends IllegalStateException {
        private final String conflictName;

        public ExpressionConflictException(String conflictName) {
            this.conflictName = conflictName;
        }

        public String getConflictName() {
            return conflictName;
        }
    }

    /**
     * Adds an expression (Ausprägung) under a train identity. Promotes a flat position to a ZUG the
     * first time an expression is added. Enforces Verkehrstage disjointness: no sibling may share a
     * weekday on overlapping dates (TTT: not two paths with the same TRID at the same time).
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public OrderPosition addExpression(UUID parentId, ExpressionDraft draft) {
        OrderPosition parent = positionRepository.findById(parentId).orElseThrow();
        if (parent.getVariantType() == null) {
            // A flat position's own purchase orders hang on it directly; splitting it would orphan
            // them, so only a position without real bookings may be promoted. (An intrinsic
            // CAPACITY
            // resource need — the timetable link every FAHRPLAN position carries — does not count.)
            if (parent.getPurchasePositions() != null && !parent.getPurchasePositions().isEmpty()) {
                throw new PositionHasBookingsException();
            }
            parent.setVariantType(PositionVariantType.ZUG);
            positionRepository.save(parent);
        }
        for (OrderPosition sibling : positionRepository.findByVariantOfId(parentId)) {
            if (Weekdays.overlaps(sibling.getWeekdays(), draft.weekdays())
                    && datesOverlap(sibling, draft.start(), draft.end())) {
                throw new ExpressionConflictException(sibling.getName());
            }
        }
        OrderPosition child = new OrderPosition();
        child.setOrder(parent.getOrder());
        child.setVariantOf(parent);
        child.setVariantType(PositionVariantType.AUSPRAEGUNG);
        child.setType(PositionType.FAHRPLAN);
        child.setName(draft.name());
        child.setOperationalTrainNumber(draft.otn());
        child.setWeekdays(draft.weekdays());
        child.setFromLocation(draft.fromLocation());
        child.setToLocation(draft.toLocation());
        child.setStart(draft.start());
        child.setEnd(draft.end());
        return positionRepository.save(child);
    }

    private static boolean datesOverlap(OrderPosition a, LocalDateTime start, LocalDateTime end) {
        if (a.getStart() == null || a.getEnd() == null || start == null || end == null) {
            return true; // unknown range → assume overlap (conservative)
        }
        return !a.getEnd().isBefore(start) && !end.isBefore(a.getStart());
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
        OrderPosition expr = positionRepository.findById(expressionId).orElseThrow();
        OrderPosition parent = expr.getVariantOf();

        List<OrderPosition> siblings =
                parent != null ? positionRepository.findByVariantOfId(parent.getId()) : List.of();

        // Span the whole train window (parent + this expression + every sibling) so a day owned by
        // a sibling outside this expression's own range is still visible and reassignable.
        List<OrderPosition> windowSources = new java.util.ArrayList<>(siblings);
        windowSources.add(expr);
        if (parent != null) {
            windowSources.add(parent);
        }
        OrderPosition[] sources = windowSources.toArray(new OrderPosition[0]);
        LocalDate min = earliestStart(sources);
        LocalDate max = latestEnd(sources);
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
            for (LocalDate day : ValidityJsonCodec.fromJson(sibling.getValidity())) {
                occupied.putIfAbsent(day, sibling.getName());
            }
        }
        return new VerkehrstageContext(
                min, max, occupied, ValidityJsonCodec.fromJson(expr.getValidity()));
    }

    /**
     * Sets an expression's Verkehrstage (date-set). Any day newly claimed from a sibling is removed
     * there and recorded as a {@code MODIFICATION} version on that sibling — so disjointness is
     * enforced date-precisely by reassignment instead of a hard reject.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public void setExpressionVerkehrstage(UUID expressionId, List<LocalDate> days) {
        OrderPosition expr = positionRepository.findById(expressionId).orElseThrow();
        Set<LocalDate> claimed = new HashSet<>(days);

        OrderPosition parent = expr.getVariantOf();
        if (parent != null) {
            for (OrderPosition sibling : positionRepository.findByVariantOfId(parent.getId())) {
                if (sibling.getId().equals(expressionId)) {
                    continue;
                }
                List<LocalDate> siblingDays = ValidityJsonCodec.fromJson(sibling.getValidity());
                List<LocalDate> given =
                        siblingDays.stream().filter(claimed::contains).sorted().toList();
                if (given.isEmpty()) {
                    continue; // this sibling keeps all its days
                }
                List<LocalDate> kept =
                        siblingDays.stream().filter(d -> !claimed.contains(d)).sorted().toList();
                sibling.setValidity(kept.isEmpty() ? null : ValidityJsonCodec.toJson(kept));
                sibling.setWeekdays(Weekdays.format(weekdaysOf(kept)));
                positionRepository.save(sibling);
                recordDayHandover(sibling, expr.getName(), given);
            }
        }

        List<LocalDate> sorted = days.stream().sorted().toList();
        expr.setValidity(sorted.isEmpty() ? null : ValidityJsonCodec.toJson(sorted));
        expr.setWeekdays(Weekdays.format(weekdaysOf(sorted)));
        positionRepository.save(expr);
    }

    private static LocalDate earliestStart(OrderPosition... positions) {
        LocalDate min = null;
        for (OrderPosition p : positions) {
            if (p != null && p.getStart() != null) {
                LocalDate d = p.getStart().toLocalDate();
                if (min == null || d.isBefore(min)) {
                    min = d;
                }
            }
        }
        return min;
    }

    private static LocalDate latestEnd(OrderPosition... positions) {
        LocalDate max = null;
        for (OrderPosition p : positions) {
            if (p != null && p.getEnd() != null) {
                LocalDate d = p.getEnd().toLocalDate();
                if (max == null || d.isAfter(max)) {
                    max = d;
                }
            }
        }
        return max;
    }

    private static Set<DayOfWeek> weekdaysOf(List<LocalDate> days) {
        Set<DayOfWeek> set = EnumSet.noneOf(DayOfWeek.class);
        for (LocalDate d : days) {
            set.add(d.getDayOfWeek());
        }
        return set;
    }

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    /** Records on the shortened sibling that it handed Verkehrstage to another expression. */
    private void recordDayHandover(
            OrderPosition sibling, String claimant, List<LocalDate> givenDays) {
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
        String dayList = givenDays.stream().map(DAY_FMT::format).collect(Collectors.joining(", "));
        version.setChangeSummary("Verkehrstag(e) " + dayList + " abgegeben an " + claimant);
        versionRepository.save(version);
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
