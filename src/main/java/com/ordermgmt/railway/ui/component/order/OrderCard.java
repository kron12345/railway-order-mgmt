package com.ordermgmt.railway.ui.component.order;

import java.time.format.DateTimeFormatter;
import java.util.function.BiConsumer;
import java.util.function.Function;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import com.ordermgmt.railway.domain.business.model.AssignmentType;
import com.ordermgmt.railway.domain.order.model.Order;
import com.ordermgmt.railway.domain.order.model.OrderType;
import com.ordermgmt.railway.domain.order.model.PositionStatus;
import com.ordermgmt.railway.domain.order.model.ProcessStatus;
import com.ordermgmt.railway.infrastructure.keycloak.KeycloakUserService;
import com.ordermgmt.railway.ui.component.StatusBadge;
import com.ordermgmt.railway.ui.component.business.AssigneeComboBox;

/**
 * Bloomberg-style master-list card for an {@link Order}. Status gutter on the left, status pill
 * (icon + text — never colour alone), order number, customer name, position count, validity range.
 */
public class OrderCard extends Div {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yy");

    public OrderCard(
            Order order,
            Function<String, String> tr,
            int positionCount,
            KeycloakUserService keycloakUserService,
            BiConsumer<AssignmentType, String> onAssign) {
        addClassName("order-card-tile");
        if (order.getProcessStatus() != null) {
            addClassName("order-card-tile--" + order.getProcessStatus().name().toLowerCase());
        }

        getElement()
                .setAttribute(
                        "aria-label",
                        tr.apply("order.aria.cardLabel.prefix")
                                + " "
                                + safe(order.getOrderNumber())
                                + ", "
                                + tr.apply("order.processStatus")
                                + " "
                                + (order.getProcessStatus() == null
                                        ? ""
                                        : tr.apply("process." + order.getProcessStatus().name())));

        Div gutter = new Div();
        gutter.addClassName("order-card-tile__gutter");
        gutter.getElement().setAttribute("aria-hidden", "true");
        add(gutter);

        Div body = new Div();
        body.addClassName("order-card-tile__body");

        body.add(buildStatusPill(order.getProcessStatus(), tr));

        if (order.getInternalStatus() != null) {
            body.add(buildInternalStatusBadge(order.getInternalStatus(), tr));
        }

        OrderType orderType = OrderType.of(order);
        if (orderType != null) {
            body.add(
                    new StatusBadge(
                            tr.apply("order.type." + orderType.name()),
                            orderType == OrderType.JAHRESBESTELLUNG
                                    ? StatusBadge.StatusType.INFO
                                    : StatusBadge.StatusType.WARNING));
        }

        Span title =
                new Span(
                        safe(order.getOrderNumber()).isEmpty()
                                ? "—"
                                : safe(order.getOrderNumber()));
        title.addClassName("order-card-tile__number");
        body.add(title);

        if (order.getName() != null && !order.getName().isBlank()) {
            Span name = new Span(order.getName());
            name.addClassName("order-card-tile__name");
            body.add(name);
        }

        body.add(buildMetaRow(order, positionCount, tr));
        body.add(buildAssigneeControl(order, tr, keycloakUserService, onAssign));

        add(body);
    }

    /**
     * Always-on assignee combo (Keycloak person), styled like text — mirrors {@code BusinessCard}.
     */
    private Component buildAssigneeControl(
            Order order,
            Function<String, String> tr,
            KeycloakUserService keycloakUserService,
            BiConsumer<AssignmentType, String> onAssign) {
        var picker = new AssigneeComboBox(keycloakUserService, onAssign);
        picker.addClassName("order-card-tile__assignee-select");
        picker.preset(
                AssignmentType.fromString(order.getAssignmentType()), order.getAssignmentName());
        picker.setPlaceholder("— " + tr.apply("order.unassigned"));
        picker.getElement().setAttribute("aria-label", tr.apply("order.assignment"));
        picker.getElement()
                .addEventListener("click", e -> {})
                .addEventData("event.stopPropagation()");
        picker.getElement()
                .addEventListener("mousedown", e -> {})
                .addEventData("event.stopPropagation()");
        return picker;
    }

    /**
     * Compact internal "Bearbeitungs-Status" chip, shown only when set (SOB §5.6 overview hint).
     */
    private Span buildInternalStatusBadge(PositionStatus status, Function<String, String> tr) {
        Span badge = new Span(tr.apply("position.status." + status.name()));
        badge.addClassName("order-internal-status-badge");
        badge.addClassName("order-internal-status-badge--" + status.name().toLowerCase());
        return badge;
    }

    private HorizontalLayout buildStatusPill(ProcessStatus status, Function<String, String> tr) {
        var pill = new HorizontalLayout();
        pill.addClassName("order-status-pill");
        if (status != null) {
            pill.addClassName("order-status-pill--" + status.name().toLowerCase());
        }
        pill.setPadding(false);
        pill.setSpacing(false);

        VaadinIcon iconSpec =
                status == null
                        ? VaadinIcon.QUESTION_CIRCLE_O
                        : switch (status) {
                            case AUFTRAG -> VaadinIcon.FILE_TEXT_O;
                            case PLANUNG -> VaadinIcon.CALENDAR;
                            case PRODUKT_LEISTUNG -> VaadinIcon.PACKAGE;
                            case PRODUKTION -> VaadinIcon.COG;
                            case ABRECHNUNG_NACHBEREITUNG -> VaadinIcon.CHECK_CIRCLE_O;
                        };
        var icon = iconSpec.create();
        icon.addClassName("order-status-pill__icon");
        icon.getElement().setAttribute("aria-hidden", "true");
        pill.add(icon);

        Span label = new Span(status == null ? "—" : tr.apply("process." + status.name()));
        label.addClassName("order-status-pill__label");
        pill.add(label);

        return pill;
    }

    private Div buildMetaRow(Order order, int positionCount, Function<String, String> tr) {
        Div meta = new Div();
        meta.addClassName("order-card-tile__meta");

        Span counts = new Span(positionCount + " " + tr.apply("order.positionCount.label"));
        counts.addClassName("order-card-tile__counts");
        meta.add(counts);

        Span sep = new Span(" · ");
        sep.getElement().setAttribute("aria-hidden", "true");
        meta.add(sep);

        Span validity = new Span();
        validity.addClassName("order-card-tile__validity");
        if (order.getValidFrom() != null && order.getValidTo() != null) {
            validity.setText(
                    order.getValidFrom().format(DATE_FMT)
                            + " → "
                            + order.getValidTo().format(DATE_FMT));
        } else if (order.getValidTo() != null) {
            validity.setText(tr.apply("order.validTo") + " " + order.getValidTo().format(DATE_FMT));
        } else if (order.getValidFrom() != null) {
            validity.setText(
                    tr.apply("order.validFrom") + " " + order.getValidFrom().format(DATE_FMT));
        } else {
            validity.setText("—");
        }
        meta.add(validity);

        if (order.getCustomer() != null && order.getCustomer().getName() != null) {
            Span sep2 = new Span(" · ");
            sep2.getElement().setAttribute("aria-hidden", "true");
            meta.add(sep2);
            Span customer = new Span(order.getCustomer().getName());
            customer.addClassName("order-card-tile__customer");
            meta.add(customer);
        }

        return meta;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
