package com.ordermgmt.railway.ui.component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;

import com.ordermgmt.railway.domain.order.model.Order;
import com.ordermgmt.railway.domain.order.model.OrderPosition;
import com.ordermgmt.railway.domain.order.model.PositionStatus;
import com.ordermgmt.railway.ui.view.order.OrderListView;

/** Expandable accordion row for a single order inside the order list. */
public class OrderAccordionRow extends Div {

    private final OrderListView parent;
    private final Order order;
    private final Details details;
    private final Div positionsContainer = new Div();
    private final Span visibleCount = new Span();
    private final EnumMap<PositionStatus, Div> statusChips = new EnumMap<>(PositionStatus.class);
    private Div allChip;
    private PositionStatus activeStatus;

    public OrderAccordionRow(Order order, OrderListView parent) {
        this.order = order;
        this.parent = parent;

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
        orderName
                .getStyle()
                .set("font-size", "15px")
                .set("font-weight", "700")
                .set("line-height", "1.3")
                .set("color", "var(--rom-text-primary)");

        identity.add(orderNum, orderName);

        String summaryComment = parent.commentPreview(order.getComment());
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
        editButton.getElement().setAttribute("aria-label", getTranslation("common.edit"));
        editButton.addClickListener(e -> UI.getCurrent().navigate("orders/" + order.getId()));
        parent.preventSummaryToggle(editButton);

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
                parent.createSummaryMetric(
                        getTranslation("order.customer"),
                        order.getCustomer() != null ? order.getCustomer().getName() : "\u2014"),
                parent.createSummaryMetric(
                        getTranslation("order.validity"), parent.formatValidity(order)),
                parent.createSummaryMetric(
                        getTranslation("order.positionCount"),
                        String.valueOf(order.getPositions().size())),
                parent.createSummaryMetric(
                        getTranslation("order.tags"), parent.previewTags(order.getTags())));

        if (order.getInternalStatus() != null && !order.getInternalStatus().isBlank()) {
            meta.add(
                    parent.createSummaryMetric(
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
                            parent.statusColor(status),
                            () -> toggleStatusFilter(status));
            statusChips.put(status, chip);
            chips.add(chip);
        }
        return chips;
    }

    private Div createStatusChip(String label, long count, String color, Runnable onClick) {
        Span text = new Span(label);
        text.getStyle().set("font-size", "10px").set("font-weight", "600").set("line-height", "1");

        Span countBadge = new Span(String.valueOf(count));
        countBadge
                .getStyle()
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
        parent.preventSummaryToggle(chip);
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
        meta.getStyle().set("display", "flex").set("gap", "8px").set("flex-wrap", "wrap");

        meta.add(
                parent.createInfoPill(
                        getTranslation("order.updated") + ": " + parent.formatUpdatedMeta(order),
                        "var(--rom-text-muted)"));

        if (order.getInternalStatus() != null && !order.getInternalStatus().isBlank()) {
            meta.add(
                    parent.createInfoPill(
                            getTranslation("order.internalStatus")
                                    + ": "
                                    + order.getInternalStatus(),
                            "var(--rom-accent)"));
        }

        content.add(meta);

        Div positionsHeader = new Div();
        positionsHeader
                .getStyle()
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

        visibleCount
                .getStyle()
                .set("font-size", "11px")
                .set("font-family", "'JetBrains Mono', monospace")
                .set("color", "var(--rom-text-muted)");

        positionsHeader.add(title, visibleCount);
        content.add(positionsHeader);

        positionsContainer
                .getStyle()
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
            emptyMessage
                    .getStyle()
                    .set("color", "var(--rom-text-muted)")
                    .set("font-size", "12px")
                    .set("padding", "8px 0");
            positionsContainer.add(emptyMessage);
            return;
        }

        for (OrderPosition position : visiblePositions) {
            PositionTile tile = new PositionTile(position, parent::getTranslation);
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
        return parent.sortedPositions(positions);
    }

    private void refreshChipStyles() {
        styleChip(allChip, "var(--rom-text-primary)", activeStatus == null);
        for (Map.Entry<PositionStatus, Div> entry : statusChips.entrySet()) {
            styleChip(
                    entry.getValue(),
                    parent.statusColor(entry.getKey()),
                    activeStatus == entry.getKey());
        }
    }

    private void styleChip(Div chip, String color, boolean active) {
        if (chip == null) {
            return;
        }
        chip.getStyle()
                .set("color", active ? "var(--rom-bg-primary)" : color)
                .set(
                        "background",
                        active ? color : "color-mix(in srgb, " + color + " 8%, transparent)")
                .set(
                        "border",
                        active
                                ? "1px solid " + color
                                : "1px solid color-mix(in srgb, " + color + " 25%, transparent)")
                .set("box-shadow", active ? "0 0 0 1px rgba(15,23,42,0.18)" : "none");
    }
}
