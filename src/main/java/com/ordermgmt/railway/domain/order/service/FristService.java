package com.ordermgmt.railway.domain.order.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ordermgmt.railway.domain.order.model.FristRegel;
import com.ordermgmt.railway.domain.order.model.OrderPosition;
import com.ordermgmt.railway.domain.order.model.PositionType;
import com.ordermgmt.railway.domain.order.model.PositionVariantType;
import com.ordermgmt.railway.domain.order.model.PurchaseStatus;
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

    private final FristRegelRepository regelRepository;
    private final OrderPositionRepository positionRepository;

    public enum Status {
        UEBERFAELLIG,
        FAELLIG_BALD,
        OK
    }

    public record FristEntry(
            OrderPosition position, FristRegel regel, LocalDate deadline, Status status) {}

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
        for (FristEntry e : entries) {
            byRule.computeIfAbsent(e.regel().getId(), k -> new ArrayList<>()).add(e);
        }
        List<AutoBusiness> result = new ArrayList<>();
        for (FristRegel rule : regelRepository.findByEnabledTrue()) {
            List<FristEntry> rEntries = byRule.getOrDefault(rule.getId(), List.of());
            long overdue = rEntries.stream().filter(e -> e.status() == Status.UEBERFAELLIG).count();
            long dueSoon = rEntries.stream().filter(e -> e.status() == Status.FAELLIG_BALD).count();
            result.add(new AutoBusiness(rule, rEntries.size(), overdue, dueSoon));
        }
        return result;
    }

    /** All deadline entries for enabled rules, sorted by deadline (most urgent first). */
    public List<FristEntry> evaluate() {
        List<FristRegel> rules = regelRepository.findByEnabledTrue();
        List<OrderPosition> bookable =
                positionRepository.findByType(PositionType.FAHRPLAN).stream()
                        .filter(p -> p.getVariantType() != PositionVariantType.ZUG)
                        .filter(this::hasOperatingDays)
                        .toList();
        LocalDate today = LocalDate.now();
        List<FristEntry> entries = new ArrayList<>();
        for (FristRegel rule : rules) {
            for (OrderPosition pos : bookable) {
                if (!isMember(rule, pos)) {
                    continue;
                }
                LocalDate deadline = deadline(rule, pos, today);
                if (deadline == null) {
                    continue;
                }
                entries.add(
                        new FristEntry(
                                pos,
                                rule,
                                deadline,
                                status(deadline, today, rule.getWarnThresholdDays())));
            }
        }
        entries.sort(Comparator.comparing(FristEntry::deadline));
        return entries;
    }

    /**
     * An expression is a deadline candidate only once it is actually scheduled — it has operating
     * days, either an explicit validity date-set or a weekday template. A freshly cloned, not-yet-
     * assigned expression (A-S5) has neither and must not surface a phantom deadline.
     */
    private boolean hasOperatingDays(OrderPosition pos) {
        return !ValidityJsonCodec.fromJson(pos.getValidity()).isEmpty()
                || !Weekdays.parse(pos.getWeekdays()).isEmpty();
    }

    private boolean isMember(FristRegel rule, OrderPosition pos) {
        return switch (rule.getMemberFilter()) {
            case ALLE_FAHRPLAN -> true;
            case NICHT_BESTELLT -> isNotYetBooked(pos);
        };
    }

    private boolean isNotYetBooked(OrderPosition pos) {
        if (pos.getPurchasePositions() == null || pos.getPurchasePositions().isEmpty()) {
            return true;
        }
        return pos.getPurchasePositions().stream()
                .anyMatch(pp -> pp.getPurchaseStatus() == PurchaseStatus.OFFEN);
    }

    /** Effective deadline of a member; {@code null} when the anchor date is unknown. */
    public LocalDate deadline(FristRegel rule, OrderPosition pos, LocalDate today) {
        int offset = rule.getOffsetDays() != null ? rule.getOffsetDays() : 0;
        return switch (rule.getAnchor()) {
            case ABSOLUT -> rule.getAbsoluteDate();
            case FAHRT -> {
                LocalDate trip = referenceTripDay(pos, today);
                yield trip != null ? trip.plusDays(offset) : null;
            }
            case FAHRPLANJAHR_START ->
                    pos.getOrder() != null && pos.getOrder().getValidFrom() != null
                            ? pos.getOrder().getValidFrom().plusDays(offset)
                            : null;
        };
    }

    /**
     * The trip day a rolling Fahrt deadline anchors on: the next operating day (matching the
     * expression's Verkehrstage) on or after today within the validity range, or the last operating
     * day when the range is already past. So "2 days before the trip" tracks the next actual run.
     */
    private LocalDate referenceTripDay(OrderPosition pos, LocalDate today) {
        if (pos.getStart() == null) {
            return null;
        }
        LocalDate from = pos.getStart().toLocalDate();
        LocalDate to = pos.getEnd() != null ? pos.getEnd().toLocalDate() : from.plusYears(1);
        if (to.isBefore(from)) {
            to = from;
        }
        Set<DayOfWeek> days = Weekdays.parse(pos.getWeekdays());
        LocalDate start = today.isAfter(from) ? today : from;
        for (LocalDate d = start; !d.isAfter(to); d = d.plusDays(1)) {
            if (days.isEmpty() || days.contains(d.getDayOfWeek())) {
                return d;
            }
        }
        for (LocalDate d = to; !d.isBefore(from); d = d.minusDays(1)) {
            if (days.isEmpty() || days.contains(d.getDayOfWeek())) {
                return d;
            }
        }
        return from;
    }

    private Status status(LocalDate deadline, LocalDate today, Integer warnThresholdDays) {
        int warn = warnThresholdDays != null ? warnThresholdDays : 0;
        if (deadline.isBefore(today)) {
            return Status.UEBERFAELLIG;
        }
        if (!deadline.isAfter(today.plusDays(warn))) {
            return Status.FAELLIG_BALD;
        }
        return Status.OK;
    }
}
