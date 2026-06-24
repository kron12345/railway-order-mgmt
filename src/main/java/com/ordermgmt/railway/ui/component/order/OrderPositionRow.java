package com.ordermgmt.railway.ui.component.order;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import com.ordermgmt.railway.domain.order.model.OrderPosition;
import com.ordermgmt.railway.domain.order.model.PositionType;
import com.ordermgmt.railway.domain.order.model.PurchasePosition;
import com.ordermgmt.railway.domain.order.service.AuditService;
import com.ordermgmt.railway.domain.pathmanager.model.PathProcessState;
import com.ordermgmt.railway.domain.pathmanager.model.PmPlanningStatus;
import com.ordermgmt.railway.ui.component.AuditHistoryDialog;
import com.ordermgmt.railway.ui.component.PurchaseCalendarPanel;
import com.ordermgmt.railway.ui.component.StatusBadge;
import com.ordermgmt.railway.ui.util.StringUtils;

/** Single position row with summary info and toggleable purchase calendar. */
public class OrderPositionRow extends Div {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd.MM. HH:mm");
    private static final int MAX_TAGS = 3;
    private final OrderPosition position;
    private final PathProcessState pmState;
    private final PmPlanningStatus planningStatus;
    private final Consumer<OrderPosition> onAcceptAlteration;
    private final Consumer<OrderPosition> onRejectAlteration;
    private final BiFunction<String, Object[], String> translator;
    private final AuditService auditService;
    private final boolean editable;
    private final Div calendarSlot = new Div();
    private boolean calendarOpen = false;
    private final Div bodySlot = new Div();
    private final Button expandToggle = new Button(VaadinIcon.CHEVRON_DOWN.create());
    private boolean bodyExpanded = true;
    private final List<Supplier<Component>> lazyBody = new ArrayList<>();
    private boolean lazyBuilt = false;

    public OrderPositionRow(
            OrderPosition position,
            PathProcessState pmState,
            PmPlanningStatus planningStatus,
            BiFunction<String, Object[], String> translator,
            Consumer<OrderPosition> onEdit,
            Consumer<OrderPosition> onDelete,
            Consumer<OrderPosition> onAcceptAlteration,
            Consumer<OrderPosition> onRejectAlteration,
            AuditService auditService,
            boolean editable) {
        this.position = position;
        this.pmState = pmState;
        this.planningStatus = planningStatus;
        this.onAcceptAlteration = onAcceptAlteration;
        this.onRejectAlteration = onRejectAlteration;
        this.translator = translator;
        this.auditService = auditService;
        this.editable = editable;

        setWidthFull();
        addClassName("order-position-row");
        getStyle()
                .set("border-bottom", "1px solid var(--rom-border)")
                .set("padding", "0")
                .set("box-sizing", "border-box")
                .set("transition", "background-color 120ms ease, box-shadow 120ms ease");

        add(createSummary(translator, onEdit, onDelete));

        bodySlot.setWidthFull();
        bodySlot.getStyle().set("box-sizing", "border-box");
        bodySlot.setVisible(bodyExpanded);
        add(bodySlot);

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
            Consumer<OrderPosition> onDelete) {

        Div info = createInfoBlock(t);

        // Action chips: icon + label, bordered so they clearly read as buttons (op-action* CSS).
        // Purchase calendar — the primary action, carrying the order count.
        long purchaseCount =
                position.getPurchasePositions() != null
                        ? position.getPurchasePositions().size()
                        : 0;
        String calText = t.apply("position.action.calendar", new Object[0]);
        Button calBtn =
                new Button(
                        purchaseCount > 0 ? purchaseCount + " " + calText : calText,
                        VaadinIcon.CALENDAR.create());
        calBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
        calBtn.addClassName("op-action");
        if (purchaseCount > 0) {
            calBtn.addClassName("op-action--primary");
        }
        calBtn.addClickListener(e -> toggleCalendar());

        // View archive (FAHRPLAN only).
        Button viewBtn =
                new Button(t.apply("position.action.view", new Object[0]), VaadinIcon.EYE.create());
        viewBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
        viewBtn.addClassNames("op-action", "op-action--info");
        // Canonical position detail for ALL types (fields + linked businesses + timetable for
        // FAHRPLAN). The same place the business links point to.
        viewBtn.addClickListener(e -> navigateToDetail());

        // History.
        Button histBtn =
                new Button(t.apply("audit.button", new Object[0]), VaadinIcon.CLOCK.create());
        histBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
        histBtn.addClassName("op-action");
        histBtn.addClickListener(e -> openPositionHistory());

        // Edit + Delete only for mutators on an unlocked order (SOB §5.7); content is frozen
        // while the order is "in Bearbeitung".
        Button editBtn =
                new Button(t.apply("common.edit", new Object[0]), VaadinIcon.EDIT.create());
        editBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
        editBtn.addClassName("op-action");
        editBtn.addClickListener(e -> onEdit.accept(position));
        editBtn.setVisible(editable);

        // Delete stays icon-only and turns danger-red on hover, so it reads as the secondary
        // action.
        Button delBtn = new Button(VaadinIcon.TRASH.create());
        delBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
        delBtn.addClassNames("op-action", "op-action--icon", "op-action--danger");
        delBtn.setTooltipText(t.apply("common.delete", new Object[0]));
        delBtn.addClickListener(e -> onDelete.accept(position));
        delBtn.setVisible(editable);

        HorizontalLayout actions = new HorizontalLayout(calBtn, viewBtn, histBtn, editBtn, delBtn);
        actions.setSpacing(true);
        actions.setAlignItems(FlexComponent.Alignment.START);

        expandToggle.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        expandToggle.addClassNames("op-action", "op-action--icon");
        expandToggle.setTooltipText(t.apply("position.view.toggle", new Object[0]));
        expandToggle.getStyle().set("align-self", "center");
        expandToggle.addClickListener(e -> setBodyExpanded(!bodyExpanded));
        expandToggle.setVisible(false); // shown once collapsible body content is added

        HorizontalLayout row = new HorizontalLayout(expandToggle, info, actions);
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

        header.add(name);
        if (position.getType() == PositionType.FAHRPLAN
                && position.getPmReferenceTrainId() != null) {
            if (planningStatus != null && planningStatus != PmPlanningStatus.UNPLANNED) {
                header.add(createPlanningBadge(t));
            }
            // Alteration response mutates path/purchase state → frozen on a locked order too.
            if (pmState == PathProcessState.ALTERATION_OFFERED && editable) {
                header.add(createAlterationActions(t));
            }
        }
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

        // Resource summary badge
        long resourceCount =
                position.getResourceNeeds() != null ? position.getResourceNeeds().size() : 0;
        // Each purchase position belongs to exactly one resource need of this position, so the
        // total is simply the count of purchases that have a resource need.
        long purchaseCount =
                position.getPurchasePositions() == null
                        ? 0
                        : position.getPurchasePositions().stream()
                                .filter(pp -> pp.getResourceNeed() != null)
                                .count();
        if (resourceCount > 0) {
            String resLabel =
                    translator.apply(
                            "resource.summary",
                            new Object[] {
                                String.valueOf(resourceCount), String.valueOf(purchaseCount)
                            });
            meta.add(createMetaBadge(resLabel, "var(--rom-text-secondary)"));
        }

        if (meta.getComponentCount() > 0) {
            info.add(meta);
        }

        // Combined Bestellpositions-Status rollup (procurement + external TTT) — the at-a-glance
        // status shown in compact/collapsed mode.
        Div rollup = PurchaseStatusRollup.build(position, translator);
        if (rollup.getComponentCount() > 0) {
            rollup.getStyle().set("margin-top", "2px");
            info.add(rollup);
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

    /** Planning status from RailOpt (planned / on shelf / on physical resource). */
    private StatusBadge createPlanningBadge(BiFunction<String, Object[], String> t) {
        String label = t.apply("pm.planning." + planningStatus.name(), new Object[0]);
        StatusBadge.StatusType type =
                switch (planningStatus) {
                    case ON_PHYSICAL_RESOURCE -> StatusBadge.StatusType.SUCCESS;
                    case ON_SHELF -> StatusBadge.StatusType.WARNING;
                    case PLANNED -> StatusBadge.StatusType.INFO;
                    case UNPLANNED -> StatusBadge.StatusType.NEUTRAL;
                };
        return new StatusBadge(t.apply("pm.planning.label", new Object[0]) + " · " + label, type);
    }

    /** Accept/reject buttons shown on the order when RailOpt has offered a path alteration. */
    private HorizontalLayout createAlterationActions(BiFunction<String, Object[], String> t) {
        Button accept = new Button(t.apply("order.alteration.accept", new Object[0]));
        accept.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_SUCCESS);
        accept.addClickListener(e -> onAcceptAlteration.accept(position));

        Button reject = new Button(t.apply("order.alteration.reject", new Object[0]));
        reject.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
        reject.addClickListener(e -> onRejectAlteration.accept(position));

        HorizontalLayout box = new HorizontalLayout(accept, reject);
        box.setSpacing(true);
        box.setPadding(false);
        return box;
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

    private void openPositionHistory() {
        if (auditService == null) {
            return;
        }
        var entries = auditService.getPositionHistory(position.getId());
        var dialog =
                new AuditHistoryDialog(
                        translator.apply("audit.title", new Object[0]) + " — " + position.getName(),
                        entries,
                        translator);
        dialog.open();
    }

    private void navigateToDetail() {
        if (position.getOrder() != null) {
            UI.getCurrent()
                    .navigate(
                            "orders/"
                                    + position.getOrder().getId()
                                    + "/positions/"
                                    + position.getId());
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /** Adds eagerly-built collapsible body content (e.g. cheap linked-business chips). */
    public void addBodyContent(Component component) {
        bodySlot.add(component);
        expandToggle.setVisible(true);
    }

    /**
     * Registers expensive body content (e.g. the resource panel) built only on first expansion, so
     * a collapsed position in compact mode pays no DB/UI build cost until the user opens it.
     */
    public void addLazyBodyContent(Supplier<Component> supplier) {
        lazyBody.add(supplier);
        expandToggle.setVisible(true);
        // Built on first setBodyExpanded(true) — the panel always calls that after adding content.
    }

    /** Expands or collapses the body (resources / chips). The chevron only shows with content. */
    public void setBodyExpanded(boolean expanded) {
        this.bodyExpanded = expanded;
        if (expanded) {
            buildLazyBody();
        } else if (calendarOpen) {
            // A collapsed position hides its purchase calendar too, so "compact" stays compact.
            calendarOpen = false;
            calendarSlot.setVisible(false);
        }
        bodySlot.setVisible(expanded);
        expandToggle.setIcon(
                (expanded ? VaadinIcon.CHEVRON_DOWN : VaadinIcon.CHEVRON_RIGHT).create());
    }

    private void buildLazyBody() {
        if (lazyBuilt) {
            return;
        }
        lazyBuilt = true;
        lazyBody.forEach(s -> bodySlot.add(s.get()));
    }
}
