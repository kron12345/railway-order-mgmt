package com.ordermgmt.railway.ui.component.order;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.ordermgmt.railway.domain.order.model.OrderPosition;
import com.ordermgmt.railway.domain.order.model.PositionStatus;
import com.ordermgmt.railway.domain.order.model.PurchaseStatus;
import com.ordermgmt.railway.domain.pathmanager.model.PathProcessState;
import com.ordermgmt.railway.ui.component.masterdetail.filter.FilterField;
import com.ordermgmt.railway.ui.component.masterdetail.filter.FilterPanel;
import com.ordermgmt.railway.ui.component.masterdetail.filter.PredicateSelectFilterField;
import com.ordermgmt.railway.ui.component.masterdetail.filter.SelectFilterField;

/**
 * Builds the collapsible position filter for an order — by internal (Bearbeitungs-) status, TTT
 * process state of the purchase positions, and procurement status. Split out of {@link
 * OrderPositionPanel} to keep that view under the size limit; pure construction over a translator
 * and a filter callback.
 */
final class OrderPositionFilters {

    private OrderPositionFilters() {}

    static FilterPanel<OrderPosition> build(
            BiFunction<String, Object[], String> translator,
            Consumer<Predicate<OrderPosition>> onFilter) {
        List<FilterField<OrderPosition>> fields =
                List.of(
                        new SelectFilterField<>(
                                t(translator, "position.filter.internalStatus"),
                                List.of(PositionStatus.values()),
                                value -> t(translator, "position.status." + value.name()),
                                OrderPosition::getInternalStatus),
                        new PredicateSelectFilterField<OrderPosition, PathProcessState>(
                                t(translator, "position.filter.tttStatus"),
                                List.of(PathProcessState.values()),
                                value -> t(translator, "pm.state." + value.name()),
                                OrderPositionFilters::hasPurchaseWithTtt),
                        new PredicateSelectFilterField<OrderPosition, PurchaseStatus>(
                                t(translator, "position.filter.purchaseStatus"),
                                List.of(PurchaseStatus.values()),
                                value -> t(translator, "purchase.status." + value.name()),
                                OrderPositionFilters::hasPurchaseWithStatus));
        FilterPanel.Labels labels =
                new FilterPanel.Labels(
                        t(translator, "filter.toggle"),
                        t(translator, "filter.clearAll"),
                        t(translator, "filter.chip.clearAria"),
                        t(translator, "filter.panel.aria"));
        return new FilterPanel<>(fields, onFilter, labels);
    }

    private static boolean hasPurchaseWithTtt(OrderPosition position, PathProcessState state) {
        return position.getPurchasePositions() != null
                && position.getPurchasePositions().stream()
                        .anyMatch(purchase -> state.name().equals(purchase.getPmProcessState()));
    }

    private static boolean hasPurchaseWithStatus(OrderPosition position, PurchaseStatus status) {
        return position.getPurchasePositions() != null
                && position.getPurchasePositions().stream()
                        .anyMatch(purchase -> purchase.getPurchaseStatus() == status);
    }

    private static String t(BiFunction<String, Object[], String> translator, String key) {
        return translator.apply(key, new Object[0]);
    }
}
