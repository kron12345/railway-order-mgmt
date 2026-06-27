package com.ordermgmt.railway.domain.order.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ordermgmt.railway.domain.order.model.FristRegel;
import com.ordermgmt.railway.domain.order.model.OperatingDays;
import com.ordermgmt.railway.domain.order.model.OrderPosition;
import com.ordermgmt.railway.domain.order.model.OrderType;
import com.ordermgmt.railway.domain.order.model.PositionType;
import com.ordermgmt.railway.domain.order.model.PositionVariantType;
import com.ordermgmt.railway.domain.order.model.PurchaseStatus;
import com.ordermgmt.railway.domain.order.model.ResourceType;
import com.ordermgmt.railway.domain.order.model.ValidityJsonCodec;
import com.ordermgmt.railway.domain.order.model.Weekdays;
import com.ordermgmt.railway.domain.order.repository.FristRegelRepository;
import com.ordermgmt.railway.domain.order.repository.OrderPositionRepository;

import lombok.RequiredArgsConstructor;

/** Evaluates configurable deadline rules (Frist-Regeln) against bookable FAHRPLAN positions. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FristService {

    private static final int DEFAULT_OFFSET_DAYS = 0;
    private static final int DEFAULT_WARN_THRESHOLD_DAYS = 0;

    /** Cap on the per-operating-day breakdown so a year-long weekday template stays bounded. */
    private static final int MAX_PER_DAY = 200;

    private final FristRegelRepository regelRepository;
    private final OrderPositionRepository positionRepository;

    public enum Status {
        UEBERFAELLIG,
        FAELLIG_BALD,
        OK
    }

    /** One operating day of a position with its rolling deadline (FAHRT anchor) and urgency. */
    public record DayDeadline(LocalDate operatingDay, LocalDate deadline, Status status) {}

    /**
     * A position's deadline under a rule. {@code deadline}/{@code status} is the aggregate shown in
     * the overview (for FAHRT: the next open trip); {@code perDay} carries the per-Verkehrstag
     * breakdown for rolling anchors (empty for absolute/year anchors, whose single date applies to
     * the whole position).
     */
    public record FristEntry(
            OrderPosition position,
            FristRegel regel,
            LocalDate deadline,
            Status status,
            List<DayDeadline> perDay) {}

    /** A rule seen as an automatic business: its dynamic members plus an urgency breakdown. */
    public record AutoBusiness(FristRegel regel, int total, long overdue, long dueSoon) {}

    /** One evaluation: the per-position entries and the per-rule automatic-business summary. */
    public record Overview(List<FristEntry> entries, List<AutoBusiness> businesses) {}

    public List<FristRegel> rules() {
        return regelRepository.findAll();
    }

    /** Evaluates deadlines once and derives both the entry list and the automatic businesses. */
    public Overview overview() {
        List<FristEntry> entries = evaluate();
        return new Overview(entries, summarize(entries));
    }

    private List<AutoBusiness> summarize(List<FristEntry> entries) {
        Map<UUID, List<FristEntry>> byRule = new LinkedHashMap<>();
        for (FristEntry entry : entries) {
            byRule.computeIfAbsent(entry.regel().getId(), ruleId -> new ArrayList<>()).add(entry);
        }
        List<AutoBusiness> businesses = new ArrayList<>();
        for (FristRegel rule : regelRepository.findByEnabledTrue()) {
            List<FristEntry> ruleEntries = byRule.getOrDefault(rule.getId(), List.of());
            long overdue =
                    ruleEntries.stream()
                            .filter(entry -> entry.status() == Status.UEBERFAELLIG)
                            .count();
            long dueSoon =
                    ruleEntries.stream()
                            .filter(entry -> entry.status() == Status.FAELLIG_BALD)
                            .count();
            businesses.add(new AutoBusiness(rule, ruleEntries.size(), overdue, dueSoon));
        }
        return businesses;
    }

    /** All deadline entries for enabled rules, sorted by deadline (most urgent first). */
    public List<FristEntry> evaluate() {
        List<FristRegel> rules = regelRepository.findByEnabledTrue();
        List<OrderPosition> bookablePositions =
                positionRepository.findByType(PositionType.FAHRPLAN).stream()
                        .filter(position -> position.getVariantType() != PositionVariantType.ZUG)
                        .filter(this::hasOperatingDays)
                        .toList();
        LocalDate today = LocalDate.now();
        List<FristEntry> entries = new ArrayList<>();
        for (FristRegel rule : rules) {
            for (OrderPosition position : bookablePositions) {
                if (!isMember(rule, position)) {
                    continue;
                }
                LocalDate deadline = deadline(rule, position, today);
                if (deadline == null) {
                    continue;
                }
                entries.add(
                        new FristEntry(
                                position,
                                rule,
                                deadline,
                                status(deadline, today, rule.getWarnThresholdDays()),
                                perDayDeadlines(rule, position, today)));
            }
        }
        entries.sort(Comparator.comparing(FristEntry::deadline));
        return entries;
    }

    /**
     * For a rolling FAHRT rule, the deadline of each operating day of the position (day + offset),
     * so the overview can expand a per-Verkehrstag detail. Empty for absolute/year anchors — their
     * single deadline already applies to the whole position — and capped at {@value #MAX_PER_DAY}.
     */
    private List<DayDeadline> perDayDeadlines(
            FristRegel rule, OrderPosition position, LocalDate today) {
        if (rule.getAnchor() != FristRegel.Anchor.FAHRT) {
            return List.of();
        }
        int offset = rule.getOffsetDays() != null ? rule.getOffsetDays() : DEFAULT_OFFSET_DAYS;
        List<LocalDate> operatingDays = OperatingDays.of(position);
        List<DayDeadline> perDay = new ArrayList<>();
        for (LocalDate day : operatingDays) {
            LocalDate deadline = day.plusDays(offset);
            perDay.add(
                    new DayDeadline(
                            day, deadline, status(deadline, today, rule.getWarnThresholdDays())));
            if (perDay.size() >= MAX_PER_DAY) {
                break;
            }
        }
        perDay.sort(Comparator.comparing(DayDeadline::operatingDay));
        return perDay;
    }

    /**
     * The single most urgent deadline entry per position (for the order-detail Frist-chip), if any.
     * Mirrors {@link #evaluate()} but scoped to the given positions and keeping only the earliest
     * deadline per position across all enabled rules.
     */
    public Map<UUID, FristEntry> mostUrgentByPosition(Collection<OrderPosition> positions) {
        List<FristRegel> rules = regelRepository.findByEnabledTrue();
        LocalDate today = LocalDate.now();
        Map<UUID, FristEntry> result = new HashMap<>();
        for (OrderPosition position : positions) {
            if (position.getType() != PositionType.FAHRPLAN
                    || position.getVariantType() == PositionVariantType.ZUG
                    || !hasOperatingDays(position)) {
                continue;
            }
            FristEntry best = null;
            for (FristRegel rule : rules) {
                if (!isMember(rule, position)) {
                    continue;
                }
                LocalDate deadline = deadline(rule, position, today);
                if (deadline == null) {
                    continue;
                }
                if (best == null || deadline.isBefore(best.deadline())) {
                    best =
                            new FristEntry(
                                    position,
                                    rule,
                                    deadline,
                                    status(deadline, today, rule.getWarnThresholdDays()),
                                    perDayDeadlines(rule, position, today));
                }
            }
            if (best != null) {
                result.put(position.getId(), best);
            }
        }
        return result;
    }

    /**
     * A position is a deadline candidate only once it carries some temporal anchor: an explicit
     * validity date-set, a weekday template, or at least a start/end range (legacy flat Fahrplan
     * rows). A freshly cloned, not-yet-assigned expression (A-S5) has none of these and must not
     * surface a phantom deadline.
     */
    private boolean hasOperatingDays(OrderPosition position) {
        if (!ValidityJsonCodec.fromJson(position.getValidity()).isEmpty()
                || !Weekdays.parse(position.getWeekdays()).isEmpty()) {
            return true;
        }
        // Legacy flat Fahrplan rows are scheduled only via start/end; an expression's schedule must
        // come from its validity/weekday set, so a bare start does not make a clone a candidate.
        return position.getVariantType() == null && position.getStart() != null;
    }

    private boolean isMember(FristRegel rule, OrderPosition position) {
        return switch (rule.getMemberFilter()) {
            case ALLE_FAHRPLAN -> true;
            case NICHT_BESTELLT -> isNotYetBooked(position);
            case KEIN_FAHRZEUG -> hasNoVehicleNeed(position);
            case AUFTRAGSTYP_JAHRES -> orderTypeIs(position, OrderType.JAHRESBESTELLUNG);
            case AUFTRAGSTYP_ADHOC -> orderTypeIs(position, OrderType.EINZELBESTELLUNG);
        };
    }

    /** No VEHICLE resource need attached → no vehicle assigned to this position yet. */
    private boolean hasNoVehicleNeed(OrderPosition position) {
        if (position.getResourceNeeds() == null) {
            return true;
        }
        return position.getResourceNeeds().stream()
                .noneMatch(need -> need.getResourceType() == ResourceType.VEHICLE);
    }

    private boolean orderTypeIs(OrderPosition position, OrderType type) {
        return OrderType.of(position.getOrder()) == type;
    }

    private boolean isNotYetBooked(OrderPosition position) {
        if (position.getPurchasePositions() == null || position.getPurchasePositions().isEmpty()) {
            return true;
        }
        return position.getPurchasePositions().stream()
                .anyMatch(
                        purchasePosition ->
                                purchasePosition.getPurchaseStatus() == PurchaseStatus.OFFEN);
    }

    /** Effective deadline of a member; {@code null} when the anchor date is unknown. */
    public LocalDate deadline(FristRegel rule, OrderPosition position, LocalDate today) {
        int offset = rule.getOffsetDays() != null ? rule.getOffsetDays() : DEFAULT_OFFSET_DAYS;
        return switch (rule.getAnchor()) {
            case ABSOLUT -> rule.getAbsoluteDate();
            case FAHRT -> {
                LocalDate trip = referenceTripDay(position, today);
                yield trip != null ? trip.plusDays(offset) : null;
            }
            case FAHRPLANJAHR_START ->
                    position.getOrder() != null && position.getOrder().getValidFrom() != null
                            ? position.getOrder().getValidFrom().plusDays(offset)
                            : null;
        };
    }

    /**
     * The trip day a rolling Fahrt deadline anchors on: the next operating day (matching the
     * expression's Verkehrstage) on or after today within the validity range, or the last operating
     * day when the range is already past. So "2 days before the trip" tracks the next actual run.
     */
    private LocalDate referenceTripDay(OrderPosition position, LocalDate today) {
        if (position.getStart() == null) {
            return null;
        }
        LocalDate from = position.getStart().toLocalDate();
        LocalDate to =
                position.getEnd() != null ? position.getEnd().toLocalDate() : from.plusYears(1);
        if (to.isBefore(from)) {
            to = from;
        }
        Set<DayOfWeek> weekdays = Weekdays.parse(position.getWeekdays());
        LocalDate start = today.isAfter(from) ? today : from;
        for (LocalDate day = start; !day.isAfter(to); day = day.plusDays(1)) {
            if (weekdays.isEmpty() || weekdays.contains(day.getDayOfWeek())) {
                return day;
            }
        }
        for (LocalDate day = to; !day.isBefore(from); day = day.minusDays(1)) {
            if (weekdays.isEmpty() || weekdays.contains(day.getDayOfWeek())) {
                return day;
            }
        }
        return from;
    }

    private Status status(LocalDate deadline, LocalDate today, Integer warnThresholdDays) {
        int warn = warnThresholdDays != null ? warnThresholdDays : DEFAULT_WARN_THRESHOLD_DAYS;
        if (deadline.isBefore(today)) {
            return Status.UEBERFAELLIG;
        }
        if (!deadline.isAfter(today.plusDays(warn))) {
            return Status.FAELLIG_BALD;
        }
        return Status.OK;
    }
}
