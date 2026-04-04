package com.ordermgmt.railway.ui.view.order;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import jakarta.annotation.security.PermitAll;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
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
import com.ordermgmt.railway.domain.order.model.OrderPosition;
import com.ordermgmt.railway.domain.order.model.PositionStatus;
import com.ordermgmt.railway.domain.order.model.ProcessStatus;
import com.ordermgmt.railway.domain.order.service.OrderService;
import com.ordermgmt.railway.ui.component.OrderAccordionRow;
import com.ordermgmt.railway.ui.component.OrderRowCallbacks;
import com.ordermgmt.railway.ui.layout.MainLayout;
import com.ordermgmt.railway.ui.util.StringUtils;

/** Lists all orders and exposes a compact expandable summary for each one. */
@Route(value = "orders", layout = MainLayout.class)
@PageTitle("Orders")
@PermitAll
public class OrderListView extends VerticalLayout implements OrderRowCallbacks {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final int MAX_SUMMARY_TAGS = 2;

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
        searchField.setWidth("250px");
        searchField.addValueChangeListener(e -> refreshList());

        statusFilter = new ComboBox<>();
        statusFilter.setPlaceholder(getTranslation("order.processStatus"));
        statusFilter.setItems(ProcessStatus.values());
        statusFilter.setItemLabelGenerator(s -> getTranslation("process." + s.name()));
        statusFilter.setClearButtonVisible(true);
        statusFilter.setWidth("160px");
        statusFilter.addValueChangeListener(e -> refreshList());

        HorizontalLayout toolbar = new HorizontalLayout(searchField, statusFilter);
        toolbar.setAlignItems(FlexComponent.Alignment.END);
        toolbar.getStyle()
                .set("margin-bottom", "var(--lumo-space-s)")
                .set("gap", "var(--lumo-space-s)")
                .set("flex-wrap", "wrap");
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
            orderList.add(new OrderAccordionRow(order, this));
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

        List<Order> orders;
        if (selectedStatus != null) {
            orders = orderService.findByProcessStatus(selectedStatus);
        } else if (query != null && !query.isBlank()) {
            orders = orderService.search(query);
        } else {
            orders = orderService.findAllWithPositions();
        }

        List<Order> sorted = new ArrayList<>(orders);
        sorted.sort(
                Comparator.comparing(
                                Order::getUpdatedAt,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Order::getOrderNumber, String.CASE_INSENSITIVE_ORDER));
        return sorted;
    }

    @Override
    public Div createSummaryMetric(String label, String value) {
        Div metric = new Div();
        metric.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "2px")
                .set("padding", "6px 10px")
                .set("border-radius", "6px")
                .set("background", "rgba(15,23,42,0.35)")
                .set("border", "1px solid rgba(148,163,184,0.12)")
                .set("min-width", "120px");

        Span metricLabel = new Span(label);
        metricLabel
                .getStyle()
                .set("font-size", "10px")
                .set("text-transform", "uppercase")
                .set("letter-spacing", "0.06em")
                .set("color", "var(--rom-text-muted)");

        Span metricValue = new Span(value);
        metricValue
                .getStyle()
                .set("font-size", "12px")
                .set("font-weight", "600")
                .set("color", "var(--rom-text-primary)")
                .set("line-height", "1.3");

        metric.add(metricLabel, metricValue);
        return metric;
    }

    @Override
    public Div createInfoPill(String label, String color) {
        Div pill = new Div();
        pill.setText(label);
        pill.getStyle()
                .set("font-size", "10px")
                .set("font-family", "'JetBrains Mono', monospace")
                .set("font-weight", "500")
                .set("padding", "3px 8px")
                .set("border-radius", "999px")
                .set("color", color)
                .set("background", "color-mix(in srgb, " + color + " 10%, transparent)")
                .set("border", "1px solid color-mix(in srgb, " + color + " 25%, transparent)");
        return pill;
    }

    @Override
    public String formatValidity(Order order) {
        return formatDate(order.getValidFrom()) + " → " + formatDate(order.getValidTo());
    }

    private String formatDate(LocalDate date) {
        return date != null ? date.format(DATE_FMT) : "—";
    }

    private String formatTimestamp(Instant value) {
        return value != null
                ? TS_FMT.withZone(java.time.ZoneId.systemDefault()).format(value)
                : "—";
    }

    @Override
    public String previewTags(String rawTags) {
        List<String> tags = StringUtils.splitTags(rawTags);
        if (tags.isEmpty()) {
            return "—";
        }
        int visible = Math.min(tags.size(), MAX_SUMMARY_TAGS);
        String preview = String.join(", ", tags.subList(0, visible));
        if (tags.size() > visible) {
            preview += " +" + (tags.size() - visible);
        }
        return preview;
    }

    @Override
    public String commentPreview(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        return normalized.length() > 180 ? normalized.substring(0, 177) + "..." : normalized;
    }

    @Override
    public String formatUpdatedMeta(Order order) {
        String timestamp = formatTimestamp(order.getUpdatedAt());
        String actor = order.getUpdatedBy();
        if (actor == null || actor.isBlank()) {
            actor = order.getCreatedBy();
        }
        if (actor == null || actor.isBlank()) {
            return timestamp;
        }
        return timestamp + " · " + actor;
    }

    @Override
    public String statusColor(PositionStatus status) {
        if (status == null) {
            return "var(--rom-text-muted)";
        }
        return switch (status) {
            case FREIGEGEBEN, ABGESCHLOSSEN -> "var(--rom-status-active)";
            case IN_BEARBEITUNG, UEBERMITTELT -> "var(--rom-status-info)";
            case UEBERARBEITEN, BEANTRAGT -> "var(--rom-status-warning)";
            case ANNULLIERT -> "var(--rom-status-danger)";
        };
    }

    @Override
    public List<OrderPosition> sortedPositions(List<OrderPosition> positions) {
        return positions.stream()
                .sorted(
                        Comparator.comparing(
                                        OrderPosition::getStart,
                                        Comparator.nullsLast(Comparator.naturalOrder()))
                                .thenComparing(
                                        OrderPosition::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    public void preventSummaryToggle(Component component) {
        component
                .getElement()
                .executeJs(
                        "this.addEventListener('click', function(event){ event.stopPropagation(); });"
                                + "this.addEventListener('keydown', function(event){"
                                + "  if (event.key === 'Enter' || event.key === ' ') {"
                                + "    event.stopPropagation();"
                                + "  }"
                                + "});");
    }
}
