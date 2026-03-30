package com.ordermgmt.railway.ui.view.order;

import java.util.List;

import jakarta.annotation.security.PermitAll;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import com.ordermgmt.railway.domain.order.model.Order;
import com.ordermgmt.railway.domain.order.model.ProcessStatus;
import com.ordermgmt.railway.domain.order.service.OrderService;
import com.ordermgmt.railway.ui.component.PositionTile;
import com.ordermgmt.railway.ui.component.StatusBadge;
import com.ordermgmt.railway.ui.component.StatusHeatmap;
import com.ordermgmt.railway.ui.layout.MainLayout;

/** Lists all orders and exposes a compact expandable summary for each one. */
@Route(value = "orders", layout = MainLayout.class)
@PageTitle("Orders")
@PermitAll
public class OrderListView extends VerticalLayout {

    private final OrderService orderService;
    private final VerticalLayout orderList = new VerticalLayout();
    private TextField searchField;
    private ComboBox<ProcessStatus> statusFilter;
    private Span countLabel;

    public OrderListView(OrderService orderService) {
        this.orderService = orderService;
        setPadding(false);
        setSpacing(false);
        setWidthFull();
        getStyle()
                .set("background", "var(--rom-bg-primary)")
                .set("padding", "var(--lumo-space-xs) var(--lumo-space-m)")
                .set("overflow-x", "hidden")
                .set("box-sizing", "border-box");

        add(createHeader());
        add(createToolbar());
        add(createOrderList());
        refreshList();
    }

    private Component createHeader() {
        H2 title = new H2(getTranslation("order.title"));
        title.getStyle()
                .set("color", "var(--rom-text-primary)")
                .set("font-weight", "600")
                .set("margin", "0");

        countLabel = new Span();
        countLabel
                .getStyle()
                .set("color", "var(--rom-text-muted)")
                .set("font-size", "var(--lumo-font-size-s)")
                .set("margin-left", "var(--lumo-space-s)");

        HorizontalLayout left = new HorizontalLayout(title, countLabel);
        left.setAlignItems(FlexComponent.Alignment.BASELINE);

        Button newOrder = new Button(getTranslation("order.new"), VaadinIcon.PLUS.create());
        newOrder.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        newOrder.getStyle()
                .set("background", "var(--rom-accent)")
                .set("color", "var(--rom-bg-primary)")
                .set("font-weight", "600");
        newOrder.addClickListener(e -> UI.getCurrent().navigate("orders/new"));

        HorizontalLayout header = new HorizontalLayout(left, newOrder);
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.getStyle().set("margin-bottom", "var(--lumo-space-m)");
        return header;
    }

    private Component createToolbar() {
        searchField = new TextField();
        searchField.setPlaceholder(getTranslation("common.search") + "...");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.setWidth("300px");
        searchField.addValueChangeListener(e -> refreshList());

        statusFilter = new ComboBox<>();
        statusFilter.setPlaceholder(getTranslation("order.processStatus"));
        statusFilter.setItems(ProcessStatus.values());
        statusFilter.setItemLabelGenerator(s -> getTranslation("process." + s.name()));
        statusFilter.setClearButtonVisible(true);
        statusFilter.setWidth("200px");
        statusFilter.addValueChangeListener(e -> refreshList());

        HorizontalLayout toolbar = new HorizontalLayout(searchField, statusFilter);
        toolbar.setAlignItems(FlexComponent.Alignment.END);
        toolbar.getStyle().set("margin-bottom", "var(--lumo-space-m)");
        return toolbar;
    }

    private Component createOrderList() {
        orderList.setPadding(false);
        orderList.setSpacing(false);
        orderList.setWidthFull();

        Div wrapper = new Div(orderList);
        wrapper.setSizeFull();
        wrapper.getStyle()
                .set("background", "var(--rom-bg-card)")
                .set("border", "1px solid var(--rom-border)")
                .set("border-radius", "6px")
                .set("overflow", "auto");
        return wrapper;
    }

    private void refreshList() {
        orderList.removeAll();
        List<Order> orders = loadOrders();

        countLabel.setText(orders.size() + " " + getTranslation("order.title"));

        for (Order order : orders) {
            orderList.add(createAccordionRow(order));
        }

        if (orders.isEmpty()) {
            Span empty = new Span(getTranslation("dashboard.overview.placeholder"));
            empty.getStyle()
                    .set("color", "var(--rom-text-muted)")
                    .set("padding", "var(--lumo-space-xl)")
                    .set("text-align", "center")
                    .set("display", "block");
            orderList.add(empty);
        }
    }

    private List<Order> loadOrders() {
        ProcessStatus selectedStatus = statusFilter.getValue();
        String query = searchField.getValue();

        if (selectedStatus != null) {
            return orderService.findByProcessStatus(selectedStatus);
        }
        if (query != null && !query.isBlank()) {
            return orderService.search(query);
        }
        return orderService.findAllWithPositions();
    }

    private Component createAccordionRow(Order order) {
        Details details = new Details(createSummary(order), createTilesContainer(order));
        details.setOpened(false);
        details.setWidthFull();
        details.getStyle()
                .set("border-bottom", "1px solid var(--rom-border)")
                .set("--vaadin-details-summary-padding", "12px 16px");
        return details;
    }

    private Div createSummary(Order order) {
        Div summary = new Div();
        summary.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("gap", "16px")
                .set("width", "100%")
                .set("flex-wrap", "wrap");

        Span orderNum = new Span(order.getOrderNumber());
        orderNum.getStyle()
                .set("font-family", "'JetBrains Mono', monospace")
                .set("font-size", "12px")
                .set("color", "var(--rom-text-secondary)")
                .set("min-width", "110px");

        Span orderName = new Span(order.getName());
        orderName
                .getStyle()
                .set("font-weight", "600")
                .set("font-size", "14px")
                .set("color", "var(--rom-text-primary)")
                .set("flex", "1")
                .set("min-width", "150px")
                .set("overflow", "hidden")
                .set("text-overflow", "ellipsis")
                .set("white-space", "nowrap");

        Span customer = new Span(order.getCustomer() != null ? order.getCustomer().getName() : "—");
        customer.getStyle()
                .set("color", "var(--rom-text-muted)")
                .set("font-size", "13px")
                .set("min-width", "100px")
                .set("flex", "0.6");

        StatusHeatmap heatmap = new StatusHeatmap(order.getPositions());
        heatmap.getStyle().set("min-width", "80px");

        StatusBadge processBadge = createProcessBadge(order.getProcessStatus());
        summary.add(orderNum, orderName, customer, heatmap, processBadge, createEditButton(order));
        return summary;
    }

    private Button createEditButton(Order order) {
        Button editButton = new Button(VaadinIcon.EDIT.create());
        editButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        editButton.getStyle().set("color", "var(--rom-text-muted)");
        editButton.addClickListener(e -> UI.getCurrent().navigate("orders/" + order.getId()));
        return editButton;
    }

    private Div createTilesContainer(Order order) {
        Div tilesContainer = new Div();
        tilesContainer
                .getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fill, minmax(300px, 1fr))")
                .set("gap", "10px")
                .set("padding", "8px 0");

        for (var pos : order.getPositions()) {
            var tile = new PositionTile(pos);
            tile.addClickListener(e -> UI.getCurrent().navigate("orders/" + order.getId()));
            tilesContainer.add(tile);
        }

        if (order.getPositions().isEmpty()) {
            Span emptyMessage = new Span(getTranslation("order.positions.empty"));
            emptyMessage
                    .getStyle()
                    .set("color", "var(--rom-text-muted)")
                    .set("font-size", "12px")
                    .set("padding", "8px 0");
            tilesContainer.add(emptyMessage);
        }
        return tilesContainer;
    }

    private StatusBadge createProcessBadge(ProcessStatus status) {
        if (status == null) {
            return new StatusBadge("—", StatusBadge.StatusType.NEUTRAL);
        }
        String label = getTranslation("process." + status.name());
        StatusBadge.StatusType type =
                switch (status) {
                    case AUFTRAG -> StatusBadge.StatusType.INFO;
                    case PLANUNG -> StatusBadge.StatusType.WARNING;
                    case PRODUKT_LEISTUNG -> StatusBadge.StatusType.INFO;
                    case PRODUKTION -> StatusBadge.StatusType.SUCCESS;
                    case ABRECHNUNG_NACHBEREITUNG -> StatusBadge.StatusType.NEUTRAL;
                };
        return new StatusBadge(label, type);
    }
}
