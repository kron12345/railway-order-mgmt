package com.ordermgmt.railway.ui.component.order;

import java.util.function.BiFunction;

import com.ordermgmt.railway.domain.order.service.FristService;
import com.ordermgmt.railway.ui.component.StatusBadge;

/**
 * Renders the most-urgent deadline chip (next due date + triggering rule) on a position row. Split
 * out of {@link OrderPositionPanel} to keep that view under the size limit.
 */
final class OrderPositionDeadlineBadge {

    private OrderPositionDeadlineBadge() {}

    /** Adds a deadline chip for the given entry; no-op when the position has no deadline. */
    static void apply(
            OrderPositionRow row,
            FristService.FristEntry entry,
            BiFunction<String, Object[], String> translator) {
        if (entry == null) {
            return;
        }
        String text =
                translator.apply(
                        "fristen.chip", new Object[] {entry.deadline(), entry.regel().getName()});
        row.addDeadlineBadge(text, badgeType(entry.status()));
    }

    private static StatusBadge.StatusType badgeType(FristService.Status status) {
        return switch (status) {
            case UEBERFAELLIG -> StatusBadge.StatusType.DANGER;
            case FAELLIG_BALD -> StatusBadge.StatusType.WARNING;
            case OK -> StatusBadge.StatusType.NEUTRAL;
        };
    }
}
