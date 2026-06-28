package com.ordermgmt.railway.domain.order.expression;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.ordermgmt.railway.domain.order.model.OperatingDays;
import com.ordermgmt.railway.domain.order.model.OrderPosition;
import com.ordermgmt.railway.domain.order.model.PositionStatus;
import com.ordermgmt.railway.domain.order.model.PositionVariantType;

/**
 * Pure helpers for the expression (Ausprägung) feature: cloning a child expression from its parent
 * train identity, and the Verkehrstage date math + handover-summary text. Stateless — extracted
 * from {@code OrderService} so the persistence/transaction orchestration there stays readable while
 * this holds the value-only logic.
 */
public final class OrderExpressionSupport {

    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final int COPIED_NAME_MAX_LENGTH = 255;
    private static final String COPIED_NAME_SUFFIX = " (Kopie)";
    private static final int SHORT_DAY_LIST_LIMIT = 8;

    /** Max length of the persisted change summary (OrderPositionVersion.changeSummary column). */
    private static final int CHANGE_SUMMARY_MAX_LENGTH = 500;

    private OrderExpressionSupport() {}

    /** A new expression cloned from its parent train identity (route/metadata copied, days not). */
    public static OrderPosition newExpression(OrderPosition parent) {
        OrderPosition expression = new OrderPosition();
        expression.setOrder(parent.getOrder());
        expression.setVariantOf(parent);
        expression.setVariantType(PositionVariantType.AUSPRAEGUNG);
        expression.setType(parent.getType());
        expression.setName(copyName(parent.getName()));
        expression.setOperationalTrainNumber(parent.getOperationalTrainNumber());
        expression.setFromLocation(parent.getFromLocation());
        expression.setToLocation(parent.getToLocation());
        // New expressions start unscheduled; copying Verkehrstage would create overlapping
        // siblings before the user assigns date-disjoint days.
        expression.setServiceType(parent.getServiceType());
        expression.setComment(parent.getComment());
        expression.setTags(parent.getTags());
        expression.setInternalStatus(PositionStatus.IN_BEARBEITUNG);
        return expression;
    }

    /**
     * Parent name + " (Kopie)" suffix, clamping the base so the suffix survives the 255-char limit.
     */
    private static String copyName(String parentName) {
        String base = parentName == null ? "" : parentName;
        int maxBase = COPIED_NAME_MAX_LENGTH - COPIED_NAME_SUFFIX.length();
        if (base.length() > maxBase) {
            base = base.substring(0, maxBase);
        }
        return base + COPIED_NAME_SUFFIX;
    }

    /** Earliest start date across the given positions (nulls ignored), or {@code null} if none. */
    public static LocalDate earliestStart(OrderPosition... positions) {
        LocalDate min = null;
        for (OrderPosition position : positions) {
            if (position != null && position.getStart() != null) {
                LocalDate startDate = position.getStart().toLocalDate();
                if (min == null || startDate.isBefore(min)) {
                    min = startDate;
                }
            }
        }
        return min;
    }

    /** Latest end date across the given positions (nulls ignored), or {@code null} if none. */
    public static LocalDate latestEnd(OrderPosition... positions) {
        LocalDate max = null;
        for (OrderPosition position : positions) {
            if (position != null && position.getEnd() != null) {
                LocalDate endDate = position.getEnd().toLocalDate();
                if (max == null || endDate.isAfter(max)) {
                    max = endDate;
                }
            }
        }
        return max;
    }

    /** Distinct weekdays touched by the given dates. */
    public static Set<DayOfWeek> weekdaysOf(List<LocalDate> days) {
        Set<DayOfWeek> weekdays = EnumSet.noneOf(DayOfWeek.class);
        for (LocalDate day : days) {
            weekdays.add(day.getDayOfWeek());
        }
        return weekdays;
    }

    /**
     * Operating days a position actually occupies — delegates to the shared {@link OperatingDays}.
     */
    public static List<LocalDate> effectiveDays(OrderPosition position) {
        return OperatingDays.of(position);
    }

    /** Change-summary text recorded on a sibling that handed Verkehrstage to another expression. */
    public static String dayHandoverSummary(String claimant, List<LocalDate> handedOverDays) {
        String summary =
                "Verkehrstag(e) "
                        + formatHandedOverDays(handedOverDays)
                        + " abgegeben an "
                        + claimant;
        if (summary.length() <= CHANGE_SUMMARY_MAX_LENGTH) {
            return summary;
        }
        return summary.substring(0, CHANGE_SUMMARY_MAX_LENGTH - 1) + "…";
    }

    private static String formatHandedOverDays(List<LocalDate> handedOverDays) {
        if (handedOverDays.size() <= SHORT_DAY_LIST_LIMIT) {
            return handedOverDays.stream()
                    .map(DAY_FORMAT::format)
                    .collect(Collectors.joining(", "));
        }
        return handedOverDays.size()
                + " Tage ("
                + DAY_FORMAT.format(handedOverDays.getFirst())
                + "–"
                + DAY_FORMAT.format(handedOverDays.getLast())
                + ")";
    }
}
