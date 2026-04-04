package com.ordermgmt.railway.ui.component;

import java.util.List;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;

import com.ordermgmt.railway.domain.order.model.Order;
import com.ordermgmt.railway.domain.order.model.OrderPosition;
import com.ordermgmt.railway.domain.order.model.PositionStatus;

/**
 * Callback interface for {@link OrderAccordionRow} to access presentation helpers without a direct
 * dependency on {@link com.ordermgmt.railway.ui.view.order.OrderListView}.
 *
 * <p>This breaks the circular reference between the view and its row component.
 */
public interface OrderRowCallbacks {

    String commentPreview(String comment);

    void preventSummaryToggle(Component component);

    Div createSummaryMetric(String label, String value);

    String formatValidity(Order order);

    String previewTags(String rawTags);

    String statusColor(PositionStatus status);

    Div createInfoPill(String label, String color);

    String formatUpdatedMeta(Order order);

    List<OrderPosition> sortedPositions(List<OrderPosition> positions);
}
