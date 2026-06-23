package com.ordermgmt.railway.ui.view.order;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import jakarta.annotation.security.PermitAll;

import org.springframework.beans.factory.ObjectProvider;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;

import com.ordermgmt.railway.domain.business.service.BusinessService;
import com.ordermgmt.railway.domain.order.model.Order;
import com.ordermgmt.railway.domain.order.model.ProcessStatus;
import com.ordermgmt.railway.domain.order.repository.OrderPositionRepository;
import com.ordermgmt.railway.domain.order.service.OrderService;
import com.ordermgmt.railway.infrastructure.keycloak.CurrentUserHelper;
import com.ordermgmt.railway.infrastructure.keycloak.KeycloakUserService;
import com.ordermgmt.railway.ui.component.a11y.SkipLinks;
import com.ordermgmt.railway.ui.component.masterdetail.MasterDetailLayout;
import com.ordermgmt.railway.ui.component.masterdetail.filter.DateRangeFilterField;
import com.ordermgmt.railway.ui.component.masterdetail.filter.FilterField;
import com.ordermgmt.railway.ui.component.masterdetail.filter.SelectFilterField;
import com.ordermgmt.railway.ui.component.masterdetail.filter.TextFilterField;
import com.ordermgmt.railway.ui.component.masterdetail.filter.ToggleFilterField;
import com.ordermgmt.railway.ui.component.order.OrderCard;
import com.ordermgmt.railway.ui.layout.MainLayout;

/**
 * Master-detail overview for orders. Serves three URL shapes:
 *
 * <ul>
 *   <li>{@code /orders} — list with empty detail
 *   <li>{@code /orders/{orderId}} — list + selected order aggregate detail
 *   <li>{@code /orders/{orderId}/positions/{posId}} — list + single position detail
 * </ul>
 *
 * <p>{@link OrderDetailView} (the right pane for an order) has 11 service deps; we obtain instances
 * via {@link ObjectProvider} so Spring autowires them.
 */
@Route(value = "orders/:orderId/positions/:posId", layout = MainLayout.class)
@RouteAlias(value = "orders/:orderId", layout = MainLayout.class)
@RouteAlias(value = "orders", layout = MainLayout.class)
@PageTitle("Aufträge")
@PermitAll
public class OrderOverviewView extends VerticalLayout implements BeforeEnterObserver {

    private final OrderService orderService;
    private final OrderPositionRepository positionRepository;
    private final BusinessService businessService;
    private final ObjectProvider<OrderDetailView> detailFactory;
    private final KeycloakUserService keycloakUserService;
    private final MasterDetailLayout<Order> shell;
    private final Map<UUID, Integer> positionCounts = new HashMap<>();

    public OrderOverviewView(
            OrderService orderService,
            OrderPositionRepository positionRepository,
            BusinessService businessService,
            ObjectProvider<OrderDetailView> detailFactory,
            KeycloakUserService keycloakUserService) {
        this.orderService = orderService;
        this.positionRepository = positionRepository;
        this.businessService = businessService;
        this.detailFactory = detailFactory;
        this.keycloakUserService = keycloakUserService;

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        getStyle().set("background", "var(--rom-bg-primary)");
        addClassName("order-overview");

        add(buildSkipLinks());

        Function<String, String> tr = this::getTranslation;
        shell =
                MasterDetailLayout.<Order>spec()
                        .idExtractor(Order::getId)
                        .cardRenderer(
                                o ->
                                        new OrderCard(
                                                o,
                                                tr,
                                                positionCounts.getOrDefault(o.getId(), 0),
                                                keycloakUserService,
                                                (t, v) -> {
                                                    try {
                                                        orderService.setAssignee(
                                                                o.getId(),
                                                                t == null ? null : t.name(),
                                                                v);
                                                    } catch (RuntimeException ex) {
                                                        // parent refresh shows current state
                                                    }
                                                    loadOrders();
                                                }))
                        .matcher(
                                (o, q) -> {
                                    String num =
                                            o.getOrderNumber() == null
                                                    ? ""
                                                    : o.getOrderNumber().toLowerCase();
                                    String name =
                                            o.getName() == null ? "" : o.getName().toLowerCase();
                                    String tags =
                                            o.getTags() == null ? "" : o.getTags().toLowerCase();
                                    String customer =
                                            o.getCustomer() != null
                                                            && o.getCustomer().getName() != null
                                                    ? o.getCustomer().getName().toLowerCase()
                                                    : "";
                                    return num.contains(q)
                                            || name.contains(q)
                                            || tags.contains(q)
                                            || customer.contains(q);
                                })
                        .filterPlaceholder(getTranslation("order.filterPlaceholder"))
                        .filterAriaLabel(getTranslation("order.search"))
                        .filterId("order-filter")
                        .listId("order-list")
                        .detailId("order-detail")
                        .listAriaLabel(getTranslation("order.list.aria"))
                        .detailAriaLabel(getTranslation("order.detail.aria"))
                        .toolbarAriaLabel(getTranslation("order.toolbar.aria"))
                        .filterFields(buildFilterFields())
                        .filterToggleLabel(getTranslation("filter.toggle"))
                        .filterClearAllLabel(getTranslation("filter.clearAll"))
                        .filterChipClearAria(getTranslation("filter.chip.clearAria"))
                        .filterPanelAria(getTranslation("filter.panel.aria"))
                        .emptyText(getTranslation("order.empty"))
                        .detailEmptyText(getTranslation("order.detail.empty"))
                        .announceTemplate(
                                (o, idx, total) ->
                                        getTranslation(
                                                "order.announce.selected",
                                                idx,
                                                total,
                                                o.getOrderNumber() == null
                                                        ? "—"
                                                        : o.getOrderNumber()))
                        .extraToolbar(canMutate() ? List.of(buildNewButton()) : List.of())
                        .shortcutNew(
                                canMutate() ? () -> UI.getCurrent().navigate("orders/new") : null)
                        .onSelect(id -> UI.getCurrent().navigate("orders/" + id))
                        .build();
        shell.setSizeFull();
        add(shell);
        setFlexGrow(1, shell);

        loadOrders();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String orderParam = event.getRouteParameters().get("orderId").orElse(null);
        String posParam = event.getRouteParameters().get("posId").orElse(null);

        loadOrders();

        // No order selected.
        if (orderParam == null) {
            shell.setSelectedId(null);
            shell.setDetail(null);
            return;
        }

        // New-order flow.
        if ("new".equals(orderParam)) {
            shell.setSelectedId(null);
            OrderDetailView detail = detailFactory.getObject();
            detail.setMode(null, true);
            shell.setDetail(detail);
            return;
        }

        UUID orderId;
        try {
            orderId = UUID.fromString(orderParam);
        } catch (IllegalArgumentException ex) {
            UI.getCurrent().navigate("orders");
            return;
        }
        Order order = orderService.findById(orderId).orElse(null);
        if (order == null) {
            UI.getCurrent().navigate("orders");
            return;
        }
        shell.setSelectedId(orderId);

        // Sub-route: single position detail.
        if (posParam != null) {
            try {
                UUID posId = UUID.fromString(posParam);
                shell.setDetail(
                        new OrderPositionDetailView(
                                positionRepository,
                                businessService,
                                orderId,
                                posId,
                                this::getTranslation));
            } catch (IllegalArgumentException ex) {
                UI.getCurrent().navigate("orders/" + orderId);
            }
            return;
        }

        // Order aggregate detail.
        OrderDetailView detail = detailFactory.getObject();
        if (!detail.setMode(orderId, false)) {
            UI.getCurrent().navigate("orders");
            return;
        }
        shell.setDetail(detail);
    }

    /**
     * Filter criteria for the order list, identical in shape to the business list (status /
     * validity range / tags / "assigned to me"). "Assigned to me" matches USER-type assignments
     * whose name is the current Keycloak user.
     */
    private List<FilterField<Order>> buildFilterFields() {
        String me = CurrentUserHelper.getUsername();
        return List.of(
                new SelectFilterField<>(
                        getTranslation("filter.field.status"),
                        List.of(ProcessStatus.values()),
                        s -> getTranslation("process." + s.name()),
                        Order::getProcessStatus),
                new DateRangeFilterField<>(
                        getTranslation("filter.field.dateFrom"),
                        getTranslation("filter.field.dateTo"),
                        Order::getValidFrom,
                        Order::getValidTo),
                new TextFilterField<>(getTranslation("filter.field.tags"), Order::getTags),
                new ToggleFilterField<>(
                        getTranslation("filter.field.assignedToMe"),
                        o ->
                                "USER".equals(o.getAssignmentType())
                                        && me != null
                                        && me.equals(o.getAssignmentName())));
    }

    private Component buildSkipLinks() {
        return new SkipLinks(
                List.of(
                        new SkipLinks.SkipTarget(
                                "order-filter", getTranslation("a11y.skip.filter")),
                        new SkipLinks.SkipTarget("order-list", getTranslation("a11y.skip.list")),
                        new SkipLinks.SkipTarget(
                                "order-detail", getTranslation("a11y.skip.detail"))));
    }

    private boolean canMutate() {
        return CurrentUserHelper.hasAnyRole("ADMIN", "DISPATCHER");
    }

    private Component buildNewButton() {
        var btn = new Button(getTranslation("order.new"), VaadinIcon.PLUS.create());
        btn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        btn.getElement().setAttribute("aria-keyshortcuts", "n");
        btn.addClickListener(e -> UI.getCurrent().navigate("orders/new"));
        return btn;
    }

    private void loadOrders() {
        List<Order> orders = orderService.findAllWithPositions();
        positionCounts.clear();
        for (Order o : orders) {
            int n = o.getPositions() == null ? 0 : o.getPositions().size();
            positionCounts.put(o.getId(), n);
        }
        shell.setItems(orders);
    }
}
