package com.ordermgmt.railway.ui.component.order;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import com.ordermgmt.railway.domain.order.model.OrderPosition;
import com.ordermgmt.railway.domain.order.model.PositionType;
import com.ordermgmt.railway.domain.order.model.PurchasePosition;
import com.ordermgmt.railway.ui.component.PurchaseCalendarPanel;
import com.ordermgmt.railway.ui.component.StatusBadge;
import com.ordermgmt.railway.ui.util.StringUtils;

/** Single position row with summary info and toggleable purchase calendar. */
public class OrderPositionRow extends Div {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd.MM. HH:mm");
    private static final int MAX_TAGS = 3;
    private final OrderPosition position;
    private final BiFunction<String, Object[], String> translator;
    private final Div calendarSlot = new Div();
    private boolean calendarOpen = false;

    public OrderPositionRow(
            OrderPosition position,
            BiFunction<String, Object[], String> translator,
            Consumer<OrderPosition> onEdit,
            Consumer<OrderPosition> onDelete,
            Consumer<OrderPosition> onSendToPm) {
        this.position = position;
        this.translator = translator;

        setWidthFull();
        getStyle()
                .set("border-bottom", "1px solid var(--rom-border)")
                .set("padding", "0")
                .set("box-sizing", "border-box");

        add(createSummary(translator, onEdit, onDelete, onSendToPm));

        calendarSlot.setWidthFull();
        calendarSlot
                .getStyle()
                .set("padding", "0 12px 12px 12px")
                .set("overflow-x", "auto")
                .set("box-sizing", "border-box");
        calendarSlot.setVisible(false);
        add(calendarSlot);
    }

    private HorizontalLayout createSummary(
            BiFunction<String, Object[], String> t,
            Consumer<OrderPosition> onEdit,
            Consumer<OrderPosition> onDelete,
            Consumer<OrderPosition> onSendToPm) {

        Div info = createInfoBlock(t);

        // Purchase calendar toggle — prominent button
        long purchaseCount =
                position.getPurchasePositions() != null
                        ? position.getPurchasePositions().size()
                        : 0;
        String btnText = translator.apply("purchase.calendar.btn", new Object[0]);
        String calLabel = purchaseCount > 0 ? purchaseCount + " " + btnText : btnText;
        Button calBtn = new Button(calLabel, VaadinIcon.CALENDAR.create());
        calBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
        calBtn.getStyle()
                .set("font-family", "'JetBrains Mono', monospace")
                .set("font-size", "10px")
                .set("font-weight", "600")
                .set("border-radius", "4px")
                .set("min-width", "90px");
        if (purchaseCount > 0) {
            calBtn.getStyle()
                    .set("background", "rgba(45,212,191,0.1)")
                    .set("color", "var(--rom-accent)")
                    .set("border", "1px solid rgba(45,212,191,0.3)");
        } else {
            calBtn.getStyle()
                    .set("background", "rgba(148,163,184,0.06)")
                    .set("color", "var(--rom-text-muted)")
                    .set("border", "1px solid var(--rom-border)");
        }
        calBtn.addClickListener(e -> toggleCalendar());

        // View button for FAHRPLAN positions (eye icon -> archive view)
        Button viewBtn = new Button(VaadinIcon.EYE.create());
        viewBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        viewBtn.getStyle().set("color", "var(--rom-status-info)");
        viewBtn.addClickListener(e -> navigateToArchiveView());
        viewBtn.setVisible(position.getType() == PositionType.FAHRPLAN);

        // Send to / View in Path Manager button for FAHRPLAN positions
        Button pmBtn = createPathManagerButton(onSendToPm);

        // Edit + Delete (smaller, secondary)
        Button editBtn = new Button(VaadinIcon.EDIT.create());
        editBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        editBtn.getStyle().set("color", "var(--rom-text-muted)");
        editBtn.addClickListener(e -> onEdit.accept(position));

        Button delBtn = new Button(VaadinIcon.TRASH.create());
        delBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        delBtn.getStyle().set("color", "var(--rom-status-danger)");
        delBtn.addClickListener(e -> onDelete.accept(position));

        HorizontalLayout actions = new HorizontalLayout(calBtn, viewBtn, pmBtn, editBtn, delBtn);
        actions.setSpacing(true);
        actions.setAlignItems(FlexComponent.Alignment.START);

        HorizontalLayout row = new HorizontalLayout(info, actions);
        row.setWidthFull();
        row.setAlignItems(FlexComponent.Alignment.START);
        row.getStyle().set("padding", "10px 12px").set("gap", "12px").set("cursor", "default");
        row.expand(info);

        return row;
    }

    private Div createInfoBlock(BiFunction<String, Object[], String> t) {
        Div info = new Div();
        info.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "6px")
                .set("min-width", "0");

        HorizontalLayout header = new HorizontalLayout();
        header.setSpacing(true);
        header.setPadding(false);
        header.setMargin(false);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.getStyle().set("flex-wrap", "wrap");

        Span name = new Span(position.getName());
        name.getStyle()
                .set("font-weight", "600")
                .set("font-size", "13px")
                .set("color", "var(--rom-text-primary)")
                .set("min-width", "150px");

        header.add(name, createTypeBadge(t), createStatusBadge(t));
        info.add(header);

        Div meta = new Div();
        meta.getStyle().set("display", "flex").set("flex-wrap", "wrap").set("gap", "6px");

        if (hasText(position.getOperationalTrainNumber())) {
            meta.add(
                    createMetaBadge(
                            "OTN: " + position.getOperationalTrainNumber(), "var(--rom-accent)"));
        }
        String route = formatRoute();
        if (!"—".equals(route)) {
            meta.add(createMetaBadge(route, "var(--rom-text-secondary)"));
        }
        String timeWindow = formatTimeWindow();
        if (timeWindow != null) {
            meta.add(createMetaBadge(timeWindow, "var(--rom-status-info)"));
        }
        if (hasText(position.getServiceType())) {
            meta.add(createMetaBadge(position.getServiceType(), "var(--rom-status-warning)"));
        }

        List<String> tags = StringUtils.splitTags(position.getTags());
        for (int i = 0; i < Math.min(tags.size(), MAX_TAGS); i++) {
            meta.add(createMetaBadge("#" + tags.get(i), "var(--rom-text-muted)"));
        }
        if (tags.size() > MAX_TAGS) {
            meta.add(createMetaBadge("+" + (tags.size() - MAX_TAGS), "var(--rom-text-muted)"));
        }

        if (meta.getComponentCount() > 0) {
            info.add(meta);
        }

        if (hasText(position.getComment())) {
            Span comment = new Span(position.getComment());
            comment.getStyle()
                    .set("display", "block")
                    .set("color", "var(--rom-text-muted)")
                    .set("font-size", "11px")
                    .set("line-height", "1.35")
                    .set("white-space", "nowrap")
                    .set("overflow", "hidden")
                    .set("text-overflow", "ellipsis")
                    .set("max-width", "100%");
            info.add(comment);
        }

        return info;
    }

    private void toggleCalendar() {
        calendarOpen = !calendarOpen;
        calendarSlot.setVisible(calendarOpen);

        if (calendarOpen && calendarSlot.getComponentCount() == 0) {
            List<PurchasePosition> purchases =
                    position.getPurchasePositions() != null
                            ? new ArrayList<>(position.getPurchasePositions())
                            : List.of();
            calendarSlot.add(new PurchaseCalendarPanel(position, purchases, translator));
        }
    }

    private StatusBadge createTypeBadge(BiFunction<String, Object[], String> t) {
        if (position.getType() == null) {
            return new StatusBadge("—", StatusBadge.StatusType.NEUTRAL);
        }
        String label = t.apply("position.type." + position.getType().name(), new Object[0]);
        return switch (position.getType()) {
            case FAHRPLAN -> new StatusBadge(label, StatusBadge.StatusType.INFO);
            case LEISTUNG -> new StatusBadge(label, StatusBadge.StatusType.WARNING);
        };
    }

    private StatusBadge createStatusBadge(BiFunction<String, Object[], String> t) {
        if (position.getInternalStatus() == null) {
            return new StatusBadge("—", StatusBadge.StatusType.NEUTRAL);
        }
        String label =
                t.apply("position.status." + position.getInternalStatus().name(), new Object[0]);
        return switch (position.getInternalStatus()) {
            case IN_BEARBEITUNG, UEBERMITTELT ->
                    new StatusBadge(label, StatusBadge.StatusType.INFO);
            case FREIGEGEBEN, ABGESCHLOSSEN ->
                    new StatusBadge(label, StatusBadge.StatusType.SUCCESS);
            case UEBERARBEITEN, BEANTRAGT -> new StatusBadge(label, StatusBadge.StatusType.WARNING);
            case ANNULLIERT -> new StatusBadge(label, StatusBadge.StatusType.DANGER);
        };
    }

    private String formatRoute() {
        String from = position.getFromLocation();
        String to = position.getToLocation();
        if (from == null && to == null) return "—";
        return (from != null ? from : "?") + " → " + (to != null ? to : "?");
    }

    private String formatTimeWindow() {
        if (position.getStart() == null && position.getEnd() == null) {
            return null;
        }
        String start = position.getStart() != null ? position.getStart().format(DT_FMT) : "—";
        String end = position.getEnd() != null ? position.getEnd().format(DT_FMT) : "—";
        if (position.getStart() != null && position.getEnd() != null) {
            return start + " → " + end;
        }
        return position.getStart() != null ? start : end;
    }

    private Span createMetaBadge(String text, String color) {
        Span badge = new Span(text);
        badge.getStyle()
                .set("font-size", "10px")
                .set("font-family", "'JetBrains Mono', monospace")
                .set("font-weight", "500")
                .set("padding", "2px 6px")
                .set("border-radius", "4px")
                .set("color", color)
                .set("background", "color-mix(in srgb, " + color + " 10%, transparent)")
                .set("border", "1px solid color-mix(in srgb, " + color + " 20%, transparent)");
        return badge;
    }

    private Button createPathManagerButton(Consumer<OrderPosition> onSendToPm) {
        boolean isFahrplan = position.getType() == PositionType.FAHRPLAN;
        boolean alreadySent = position.getPmReferenceTrainId() != null;

        Icon icon = VaadinIcon.TRAIN.create();
        Button btn = new Button(icon);
        btn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        btn.setVisible(isFahrplan);

        if (alreadySent) {
            btn.setTooltipText(translator.apply("position.viewInPm", new Object[0]));
            btn.getStyle().set("color", "var(--rom-accent)");
            btn.addClickListener(e -> UI.getCurrent().navigate("pathmanager"));
        } else {
            btn.setTooltipText(translator.apply("position.sendToPm", new Object[0]));
            btn.getStyle().set("color", "var(--rom-status-warning)");
            btn.addClickListener(e -> onSendToPm.accept(position));
        }

        return btn;
    }

    private void navigateToArchiveView() {
        if (position.getOrder() != null) {
            UI.getCurrent()
                    .navigate(
                            "orders/"
                                    + position.getOrder().getId()
                                    + "/timetable/"
                                    + position.getId());
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
