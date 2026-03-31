package com.ordermgmt.railway.ui.view.order;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import jakarta.annotation.security.PermitAll;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.combobox.ComboBox;
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
import com.ordermgmt.railway.ui.component.PositionTile;
import com.ordermgmt.railway.ui.layout.MainLayout;

/** Lists all orders and exposes a compact expandable summary for each one. */
@Route(value = "orders", layout = MainLayout.class)
@PageTitle("Orders")
@PermitAll
public class OrderListView extends VerticalLayout {

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
        countLabel.getStyle()
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
            orderList.add(new OrderAccordionRow(order));
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

    private Div createSummaryMetric(String label, String value) {
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
        metricLabel.getStyle()
                .set("font-size", "10px")
                .set("text-transform", "uppercase")
                .set("letter-spacing", "0.06em")
                .set("color", "var(--rom-text-muted)");

        Span metricValue = new Span(value);
        metricValue.getStyle()
                .set("font-size", "12px")
                .set("font-weight", "600")
                .set("color", "var(--rom-text-primary)")
                .set("line-height", "1.3");

        metric.add(metricLabel, metricValue);
        return metric;
    }

    private Div createInfoPill(String label, String color) {
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

    private String formatValidity(Order order) {
        return formatDate(order.getValidFrom()) + " → " + formatDate(order.getValidTo());
    }

    private String formatDate(LocalDate date) {
        return date != null ? date.format(DATE_FMT) : "—";
    }

    private String formatTimestamp(Instant value) {
        return value != null ? TS_FMT.withZone(java.time.ZoneId.systemDefault()).format(value) : "—";
    }

    private List<String> splitTags(String rawTags) {
        List<String> values = new ArrayList<>();
        if (rawTags == null || rawTags.isBlank()) {
            return values;
        }
        for (String token : rawTags.split(",")) {
            String normalized = token.trim();
            if (!normalized.isBlank()) {
                values.add(normalized);
            }
        }
        return values;
    }

    private String previewTags(String rawTags) {
        List<String> tags = splitTags(rawTags);
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

    private String commentPreview(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        return normalized.length() > 180 ? normalized.substring(0, 177) + "..." : normalized;
    }

    private String formatUpdatedMeta(Order order) {
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

    private String statusColor(PositionStatus status) {
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

    private List<OrderPosition> sortedPositions(List<OrderPosition> positions) {
        return positions.stream()
                .sorted(
                        Comparator.comparing(
                                        OrderPosition::getStart,
                                        Comparator.nullsLast(Comparator.naturalOrder()))
                                .thenComparing(OrderPosition::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private void preventSummaryToggle(Component component) {
        component.getElement()
                .executeJs(
                        "this.addEventListener('click', function(event){ event.stopPropagation(); });"
                                + "this.addEventListener('keydown', function(event){"
                                + "  if (event.key === 'Enter' || event.key === ' ') {"
                                + "    event.stopPropagation();"
                                + "  }"
                                + "});");
    }

    private final class OrderAccordionRow extends Div {

        private final Order order;
        private final Details details;
        private final Div positionsContainer = new Div();
        private final Span visibleCount = new Span();
        private final EnumMap<PositionStatus, Div> statusChips =
                new EnumMap<>(PositionStatus.class);
        private Div allChip;
        private PositionStatus activeStatus;

        private OrderAccordionRow(Order order) {
            this.order = order;

            setWidthFull();
            details = new Details(createSummary(), createContent());
            details.setOpened(false);
            details.setWidthFull();
            details.getStyle()
                    .set("border-bottom", "1px solid var(--rom-border)")
                    .set("--vaadin-details-summary-padding", "14px 16px");
            add(details);

            refreshVisiblePositions();
            refreshChipStyles();
        }

        private Component createSummary() {
            Div summary = new Div();
            summary.getStyle()
                    .set("display", "flex")
                    .set("flex-direction", "column")
                    .set("gap", "12px")
                    .set("width", "100%");

            summary.add(createSummaryTopRow(), createSummaryBottomRow());
            return summary;
        }

        private Component createSummaryTopRow() {
            Div identity = new Div();
            identity.getStyle()
                    .set("display", "flex")
                    .set("flex-direction", "column")
                    .set("gap", "2px")
                    .set("flex", "1")
                    .set("min-width", "220px");

            Span orderNum = new Span(order.getOrderNumber());
            orderNum.getStyle()
                    .set("font-family", "'JetBrains Mono', monospace")
                    .set("font-size", "11px")
                    .set("letter-spacing", "0.08em")
                    .set("text-transform", "uppercase")
                    .set("color", "var(--rom-text-secondary)");

            Span orderName = new Span(order.getName());
            orderName.getStyle()
                    .set("font-size", "15px")
                    .set("font-weight", "700")
                    .set("line-height", "1.3")
                    .set("color", "var(--rom-text-primary)");

            identity.add(orderNum, orderName);

            String summaryComment = commentPreview(order.getComment());
            if (summaryComment != null) {
                Span comment = new Span(summaryComment);
                comment.getStyle()
                        .set("font-size", "12px")
                        .set("line-height", "1.35")
                        .set("color", "var(--rom-text-muted)")
                        .set("max-width", "100%")
                        .set("white-space", "nowrap")
                        .set("overflow", "hidden")
                        .set("text-overflow", "ellipsis");
                identity.add(comment);
            }

            Div right = new Div();
            right.getStyle()
                    .set("display", "flex")
                    .set("align-items", "center")
                    .set("gap", "8px")
                    .set("margin-left", "auto");

            Button editButton = new Button(VaadinIcon.EDIT.create());
            editButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            editButton.getStyle().set("color", "var(--rom-text-muted)");
            editButton.addClickListener(e -> UI.getCurrent().navigate("orders/" + order.getId()));
            preventSummaryToggle(editButton);

            right.add(editButton);

            Div row = new Div(identity, right);
            row.getStyle()
                    .set("display", "flex")
                    .set("align-items", "start")
                    .set("gap", "12px")
                    .set("width", "100%")
                    .set("flex-wrap", "wrap");
            return row;
        }

        private Component createSummaryBottomRow() {
            Div meta = new Div();
            meta.getStyle()
                    .set("display", "flex")
                    .set("gap", "8px")
                    .set("flex-wrap", "wrap")
                    .set("flex", "1")
                    .set("min-width", "280px");

            meta.add(
                    createSummaryMetric(getTranslation("order.customer"),
                            order.getCustomer() != null ? order.getCustomer().getName() : "—"),
                    createSummaryMetric(getTranslation("order.validity"), formatValidity(order)),
                    createSummaryMetric(
                            getTranslation("order.positionCount"),
                            String.valueOf(order.getPositions().size())),
                    createSummaryMetric(getTranslation("order.tags"), previewTags(order.getTags())));

            if (order.getInternalStatus() != null && !order.getInternalStatus().isBlank()) {
                meta.add(
                        createSummaryMetric(
                                getTranslation("order.internalStatus"), order.getInternalStatus()));
            }

            Div chips = createStatusChipBar();

            Div row = new Div(meta, chips);
            row.getStyle()
                    .set("display", "flex")
                    .set("align-items", "start")
                    .set("justify-content", "space-between")
                    .set("gap", "12px")
                    .set("width", "100%")
                    .set("flex-wrap", "wrap");
            return row;
        }

        private Div createStatusChipBar() {
            Div chips = new Div();
            chips.getStyle()
                    .set("display", "flex")
                    .set("gap", "6px")
                    .set("flex-wrap", "wrap")
                    .set("justify-content", "flex-end")
                    .set("align-items", "center");

            Map<PositionStatus, Long> counts = countPositionsByStatus();
            allChip =
                    createStatusChip(
                            getTranslation("common.all"),
                            order.getPositions().size(),
                            "var(--rom-text-primary)",
                            this::clearStatusFilter);
            chips.add(allChip);

            for (PositionStatus status : PositionStatus.values()) {
                long count = counts.getOrDefault(status, 0L);
                if (count == 0) {
                    continue;
                }
                Div chip =
                        createStatusChip(
                                getTranslation("position.status." + status.name()),
                                count,
                                statusColor(status),
                                () -> toggleStatusFilter(status));
                statusChips.put(status, chip);
                chips.add(chip);
            }
            return chips;
        }

        private Div createStatusChip(
                String label, long count, String color, Runnable onClick) {
            Span text = new Span(label);
            text.getStyle()
                    .set("font-size", "10px")
                    .set("font-weight", "600")
                    .set("line-height", "1");

            Span countBadge = new Span(String.valueOf(count));
            countBadge.getStyle()
                    .set("font-size", "10px")
                    .set("font-family", "'JetBrains Mono', monospace")
                    .set("font-weight", "700")
                    .set("line-height", "1")
                    .set("padding", "3px 6px")
                    .set("border-radius", "999px")
                    .set("background", "rgba(15,23,42,0.45)");

            Div chip = new Div(text, countBadge);
            chip.getElement().setAttribute("role", "button");
            chip.getElement().setAttribute("tabindex", "0");
            chip.getStyle()
                    .set("display", "inline-flex")
                    .set("align-items", "center")
                    .set("gap", "6px")
                    .set("padding", "5px 9px")
                    .set("border-radius", "999px")
                    .set("border", "1px solid color-mix(in srgb, " + color + " 25%, transparent)")
                    .set("cursor", "pointer")
                    .set("transition", "transform 0.15s, border-color 0.15s, background 0.15s")
                    .set("color", color)
                    .set("background", "color-mix(in srgb, " + color + " 8%, transparent)");
            chip.getElement()
                    .executeJs(
                            "this.addEventListener('keydown', function(event){"
                                    + "  if (event.key === 'Enter' || event.key === ' ') {"
                                    + "    event.preventDefault();"
                                    + "    event.stopPropagation();"
                                    + "    this.click();"
                                    + "  }"
                                    + "});");
            chip.addClickListener(e -> onClick.run());
            preventSummaryToggle(chip);
            return chip;
        }

        private Component createContent() {
            Div content = new Div();
            content.getStyle()
                    .set("display", "flex")
                    .set("flex-direction", "column")
                    .set("gap", "12px")
                    .set("padding", "4px 0 0 0");

            Div meta = new Div();
            meta.getStyle()
                    .set("display", "flex")
                    .set("gap", "8px")
                    .set("flex-wrap", "wrap");

            meta.add(
                    createInfoPill(
                            getTranslation("order.updated")
                                    + ": "
                                    + formatUpdatedMeta(order),
                            "var(--rom-text-muted)"));

            if (order.getInternalStatus() != null && !order.getInternalStatus().isBlank()) {
                meta.add(
                        createInfoPill(
                                getTranslation("order.internalStatus")
                                        + ": "
                                        + order.getInternalStatus(),
                                "var(--rom-accent)"));
            }

            content.add(meta);

            Div positionsHeader = new Div();
            positionsHeader.getStyle()
                    .set("display", "flex")
                    .set("justify-content", "space-between")
                    .set("align-items", "center")
                    .set("gap", "12px")
                    .set("flex-wrap", "wrap");

            Span title = new Span(getTranslation("order.positions"));
            title.getStyle()
                    .set("font-size", "12px")
                    .set("font-weight", "700")
                    .set("text-transform", "uppercase")
                    .set("letter-spacing", "0.06em")
                    .set("color", "var(--rom-text-primary)");

            visibleCount.getStyle()
                    .set("font-size", "11px")
                    .set("font-family", "'JetBrains Mono', monospace")
                    .set("color", "var(--rom-text-muted)");

            positionsHeader.add(title, visibleCount);
            content.add(positionsHeader);

            positionsContainer.getStyle()
                    .set("display", "grid")
                    .set("grid-template-columns", "repeat(auto-fill, minmax(340px, 1fr))")
                    .set("gap", "10px")
                    .set("padding", "0 0 8px 0");
            content.add(positionsContainer);

            return content;
        }

        private Map<PositionStatus, Long> countPositionsByStatus() {
            EnumMap<PositionStatus, Long> counts = new EnumMap<>(PositionStatus.class);
            for (OrderPosition position : order.getPositions()) {
                PositionStatus status = position.getInternalStatus();
                if (status == null) {
                    continue;
                }
                counts.put(status, counts.getOrDefault(status, 0L) + 1);
            }
            return counts;
        }

        private void toggleStatusFilter(PositionStatus status) {
            activeStatus = activeStatus == status ? null : status;
            details.setOpened(true);
            refreshVisiblePositions();
            refreshChipStyles();
        }

        private void clearStatusFilter() {
            activeStatus = null;
            details.setOpened(true);
            refreshVisiblePositions();
            refreshChipStyles();
        }

        private void refreshVisiblePositions() {
            positionsContainer.removeAll();

            List<OrderPosition> visiblePositions = visiblePositions();
            visibleCount.setText(
                    visiblePositions.size()
                            + "/"
                            + order.getPositions().size()
                            + " "
                            + getTranslation("order.positionCount"));

            if (visiblePositions.isEmpty()) {
                Span emptyMessage =
                        new Span(
                                activeStatus == null
                                        ? getTranslation("order.positions.empty")
                                        : getTranslation("order.positions.filtered.empty"));
                emptyMessage.getStyle()
                        .set("color", "var(--rom-text-muted)")
                        .set("font-size", "12px")
                        .set("padding", "8px 0");
                positionsContainer.add(emptyMessage);
                return;
            }

            for (OrderPosition position : visiblePositions) {
                PositionTile tile = new PositionTile(position, OrderListView.this::getTranslation);
                tile.addClickListener(e -> UI.getCurrent().navigate("orders/" + order.getId()));
                positionsContainer.add(tile);
            }
        }

        private List<OrderPosition> visiblePositions() {
            List<OrderPosition> positions =
                    activeStatus == null
                            ? order.getPositions()
                            : order.getPositions().stream()
                                    .filter(position -> position.getInternalStatus() == activeStatus)
                                    .toList();
            return sortedPositions(positions);
        }

        private void refreshChipStyles() {
            styleChip(allChip, "var(--rom-text-primary)", activeStatus == null);
            for (Map.Entry<PositionStatus, Div> entry : statusChips.entrySet()) {
                styleChip(entry.getValue(), statusColor(entry.getKey()), activeStatus == entry.getKey());
            }
        }

        private void styleChip(Div chip, String color, boolean active) {
            if (chip == null) {
                return;
            }
            chip.getStyle()
                    .set("color", active ? "var(--rom-bg-primary)" : color)
                    .set("background",
                            active
                                    ? color
                                    : "color-mix(in srgb, " + color + " 8%, transparent)")
                    .set("border",
                            active
                                    ? "1px solid " + color
                                    : "1px solid color-mix(in srgb, "
                                            + color
                                            + " 25%, transparent)")
                    .set("box-shadow", active ? "0 0 0 1px rgba(15,23,42,0.18)" : "none");
        }
    }
}
