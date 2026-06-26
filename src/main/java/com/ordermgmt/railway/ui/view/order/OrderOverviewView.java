package com.ordermgmt.railway.ui.view.order;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import jakarta.annotation.security.PermitAll;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Slice;

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
import com.ordermgmt.railway.domain.order.model.PositionStatus;
import com.ordermgmt.railway.domain.order.model.ProcessStatus;
import com.ordermgmt.railway.domain.order.repository.OrderPositionRepository;
import com.ordermgmt.railway.domain.order.service.OrderService;
import com.ordermgmt.railway.dto.order.OrderListItem;
import com.ordermgmt.railway.dto.order.OrderListQuery;
import com.ordermgmt.railway.infrastructure.keycloak.CurrentUserHelper;
import com.ordermgmt.railway.infrastructure.keycloak.KeycloakUserService;
import com.ordermgmt.railway.ui.component.a11y.SkipLinks;
import com.ordermgmt.railway.ui.component.masterdetail.MasterDetailLayout;
import com.ordermgmt.railway.ui.component.masterdetail.SliceResult;
import com.ordermgmt.railway.ui.component.masterdetail.filter.DateRangeFilterField;
import com.ordermgmt.railway.ui.component.masterdetail.filter.FilterField;
import com.ordermgmt.railway.ui.component.masterdetail.filter.SelectFilterField;
import com.ordermgmt.railway.ui.component.masterdetail.filter.TextFilterField;
import com.ordermgmt.railway.ui.component.masterdetail.filter.ToggleFilterField;
import com.ordermgmt.railway.ui.component.order.OrderCard;
import com.ordermgmt.railway.ui.layout.MainLayout;
import com.ordermgmt.railway.ui.support.OffsetPageable;

/**
 * Master-detail overview for orders. Serves three URL shapes:
 *
 * <ul>
 *   <li>{@code /orders} — list with empty detail
 *   <li>{@code /orders/{orderId}} — list + selected order aggregate detail
 *   <li>{@code /orders/{orderId}/positions/{posId}} — list + single position detail
 * </ul>
 *
 * <p>The list is lazy (P4): {@link MasterDetailLayout} pulls {@link OrderListItem} pages from
 * {@link OrderService#searchOrders} via {@link #lazyLoadOrders}; filters become a server-side
 * {@link OrderListQuery}, so no full order list is ever materialized. {@link OrderDetailView} (the
 * right pane) has many service deps obtained via {@link ObjectProvider}.
 */
@Route(value = "orders/:orderId/positions/:posId", layout = MainLayout.class)
@RouteAlias(value = "orders/:orderId", layout = MainLayout.class)
@RouteAlias(value = "orders", layout = MainLayout.class)
@PageTitle("Aufträge")
@PermitAll
public class OrderOverviewView extends VerticalLayout implements BeforeEnterObserver {

    private static final int PAGE_SIZE = 50;

    private final OrderService orderService;
    private final OrderPositionRepository positionRepository;
    private final BusinessService businessService;
    private final ObjectProvider<OrderDetailView> detailFactory;
    private final KeycloakUserService keycloakUserService;

    // Not final: the cardRenderer lambda (built during the spec chain) reads shell to reloadLazy().
    private MasterDetailLayout<OrderListItem> shell;

    // Filter controls — held so the lazy loader can read their values into an OrderListQuery.
    private SelectFilterField<OrderListItem, ProcessStatus> processStatusField;
    private SelectFilterField<OrderListItem, PositionStatus> internalStatusField;
    private DateRangeFilterField<OrderListItem> dateRangeField;
    private TextFilterField<OrderListItem> tagsField;
    private ToggleFilterField<OrderListItem> assignedToMeField;

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
                MasterDetailLayout.<OrderListItem>spec()
                        .idExtractor(OrderListItem::id)
                        .cardRenderer(
                                o ->
                                        new OrderCard(
                                                o,
                                                tr,
                                                keycloakUserService,
                                                (t, v) -> {
                                                    try {
                                                        orderService.setAssignee(
                                                                o.id(),
                                                                t == null ? null : t.name(),
                                                                v);
                                                    } catch (RuntimeException ex) {
                                                        // parent refresh shows current state
                                                    }
                                                    shell.reloadLazy();
                                                }))
                        // Inert in lazy mode (applyFilter delegates to the server query); kept so
                        // the
                        // spec stays valid and the legacy in-memory path would still work if
                        // reused.
                        .matcher(
                                (o, q) -> {
                                    String num =
                                            o.orderNumber() == null
                                                    ? ""
                                                    : o.orderNumber().toLowerCase();
                                    String name = o.name() == null ? "" : o.name().toLowerCase();
                                    String tags = o.tags() == null ? "" : o.tags().toLowerCase();
                                    String customer =
                                            o.customerName() == null
                                                    ? ""
                                                    : o.customerName().toLowerCase();
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
                        .readoutLoadedLabel(getTranslation("md.lazy.loaded"))
                        .readoutMoreLabel(getTranslation("md.lazy.more"))
                        .readoutFilteredLabel(getTranslation("md.lazy.filtered"))
                        .sentinelLabel(getTranslation("md.lazy.sentinel", PAGE_SIZE))
                        .announceTemplate(
                                (o, idx, total) ->
                                        getTranslation(
                                                "order.announce.selected",
                                                idx,
                                                total,
                                                o.orderNumber() == null ? "—" : o.orderNumber()))
                        .lazyAnnounceTemplate(
                                (o, idx) ->
                                        getTranslation(
                                                "order.announce.selected.lazy",
                                                idx,
                                                o.orderNumber() == null ? "—" : o.orderNumber()))
                        .extraToolbar(canMutate() ? List.of(buildNewButton()) : List.of())
                        .shortcutNew(
                                canMutate() ? () -> UI.getCurrent().navigate("orders/new") : null)
                        .onSelect(id -> UI.getCurrent().navigate("orders/" + id))
                        .build();
        shell.setSizeFull();
        add(shell);
        setFlexGrow(1, shell);

        shell.setLazyLoader(this::lazyLoadOrders);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String orderParam = event.getRouteParameters().get("orderId").orElse(null);
        String posParam = event.getRouteParameters().get("posId").orElse(null);

        // No order selected — the bare list: (re)load page 1.
        if (orderParam == null) {
            shell.reloadLazy();
            shell.setSelectedId(null);
            shell.setDetail(null);
            return;
        }

        // A specific order/sub-route: load the first page only if nothing is loaded yet, so an
        // already-accumulated list keeps its pages, scroll and selection when a card is clicked.
        shell.ensureLoaded();

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
     * One lazy page of the order list: builds an {@link OrderListQuery} from the search text plus
     * the held filter controls and asks the search service for the slice at {@code offset}.
     *
     * <p>Note: there is no server-side "order type" column (it is a derived badge), so unlike the
     * old in-memory list this lazy filter set has no order-type field.
     */
    private SliceResult<OrderListItem> lazyLoadOrders(String text, int offset) {
        String me = CurrentUserHelper.getUsername();
        OrderListQuery query =
                new OrderListQuery(
                        text,
                        processStatusField.getSelectedValue(),
                        internalStatusField.getSelectedValue(),
                        dateRangeField.getFrom(),
                        dateRangeField.getTo(),
                        tagsField.getTextValue(),
                        assignedToMeField.isToggled() ? me : null);
        Slice<OrderListItem> slice =
                orderService.searchOrders(query, new OffsetPageable(offset, PAGE_SIZE));
        return new SliceResult<>(slice.getContent(), slice.hasNext());
    }

    /**
     * Filter criteria for the order list (status / internal status / validity range / tags /
     * "assigned to me"). "Assigned to me" matches USER-type assignments whose name is the current
     * Keycloak user. Stored as fields so {@link #lazyLoadOrders} can read them.
     */
    private List<FilterField<OrderListItem>> buildFilterFields() {
        String me = CurrentUserHelper.getUsername();
        processStatusField =
                new SelectFilterField<>(
                        getTranslation("filter.field.status"),
                        List.of(ProcessStatus.values()),
                        s -> getTranslation("process." + s.name()),
                        OrderListItem::processStatus);
        internalStatusField =
                new SelectFilterField<>(
                        getTranslation("order.internalStatus"),
                        List.of(PositionStatus.values()),
                        s -> getTranslation("position.status." + s.name()),
                        OrderListItem::internalStatus);
        dateRangeField =
                new DateRangeFilterField<>(
                        getTranslation("filter.field.dateFrom"),
                        getTranslation("filter.field.dateTo"),
                        OrderListItem::validFrom,
                        OrderListItem::validTo);
        tagsField = new TextFilterField<>(getTranslation("filter.field.tags"), OrderListItem::tags);
        assignedToMeField =
                new ToggleFilterField<>(
                        getTranslation("filter.field.assignedToMe"),
                        o ->
                                "USER".equals(o.assignmentType())
                                        && me != null
                                        && me.equals(o.assignmentName()));
        return List.of(
                processStatusField,
                internalStatusField,
                dateRangeField,
                tagsField,
                assignedToMeField);
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
}
