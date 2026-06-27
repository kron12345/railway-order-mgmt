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
import com.vaadin.flow.component.checkbox.Checkbox;
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
    private final Checkbox selectCheckbox = new Checkbox();
    private final Button expandToggle = new Button(VaadinIcon.CHEVRON_DOWN.create());
    private boolean bodyExpanded = true;
    private final List<Supplier<Component>> lazyBody = new ArrayList<>();
    private boolean lazyBuilt = false;
    private HorizontalLayout summaryHeader;
    private HorizontalLayout actionRow;

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

        actionRow =
                new HorizontalLayout(
                        createCalendarButton(t),
                        createViewButton(t),
                        createHistoryButton(t),
                        createEditButton(t, onEdit),
                        createDeleteButton(t, onDelete));
        actionRow.setSpacing(true);
        actionRow.setAlignItems(FlexComponent.Alignment.START);

        expandToggle.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        expandToggle.addClassNames("op-action", "op-action--icon");
        expandToggle.setTooltipText(t.apply("position.view.toggle", new Object[0]));
        expandToggle.getStyle().set("align-self", "center");
        expandToggle.addClickListener(e -> setBodyExpanded(!bodyExpanded));
        expandToggle.setVisible(false); // shown once collapsible body content is added

        selectCheckbox.setVisible(false);
        selectCheckbox.getStyle().set("align-self", "center");

        HorizontalLayout row = new HorizontalLayout(selectCheckbox, expandToggle, info, actionRow);
        row.setWidthFull();
        row.setAlignItems(FlexComponent.Alignment.START);
        row.getStyle().set("padding", "10px 12px").set("gap", "12px").set("cursor", "default");
        row.expand(info);

        return row;
    }

    private Button createCalendarButton(BiFunction<String, Object[], String> t) {
        long purchaseCount = countPurchases();
        String calendarLabel = t.apply("position.action.calendar", new Object[0]);
        Button button =
                new Button(
                        purchaseCount > 0 ? purchaseCount + " " + calendarLabel : calendarLabel,
                        VaadinIcon.CALENDAR.create());
        button.addThemeVariants(ButtonVariant.LUMO_SMALL);
        button.addClassName("op-action");
        if (purchaseCount > 0) {
            button.addClassName("op-action--primary");
        }
        button.addClickListener(event -> toggleCalendar());
        return button;
    }

    private Button createViewButton(BiFunction<String, Object[], String> t) {
        Button button =
                new Button(t.apply("position.action.view", new Object[0]), VaadinIcon.EYE.create());
        button.addThemeVariants(ButtonVariant.LUMO_SMALL);
        button.addClassNames("op-action", "op-action--info");
        button.addClickListener(event -> navigateToDetail());
        return button;
    }

    private Button createHistoryButton(BiFunction<String, Object[], String> t) {
        Button button =
                new Button(t.apply("audit.button", new Object[0]), VaadinIcon.CLOCK.create());
        button.addThemeVariants(ButtonVariant.LUMO_SMALL);
        button.addClassName("op-action");
        button.addClickListener(event -> openPositionHistory());
        return button;
    }

    private Button createEditButton(
            BiFunction<String, Object[], String> t, Consumer<OrderPosition> onEdit) {
        Button button = new Button(t.apply("common.edit", new Object[0]), VaadinIcon.EDIT.create());
        button.addThemeVariants(ButtonVariant.LUMO_SMALL);
        button.addClassName("op-action");
        button.addClickListener(event -> onEdit.accept(position));
        button.setVisible(editable);
        return button;
    }

    private Button createDeleteButton(
            BiFunction<String, Object[], String> t, Consumer<OrderPosition> onDelete) {
        Button button = new Button(VaadinIcon.TRASH.create());
        button.addThemeVariants(ButtonVariant.LUMO_SMALL);
        button.addClassNames("op-action", "op-action--icon", "op-action--danger");
        button.setTooltipText(t.apply("common.delete", new Object[0]));
        button.addClickListener(event -> onDelete.accept(position));
        button.setVisible(editable);
        return button;
    }

    /** Inserts an extra action chip (e.g. Verkehrstage) before the edit/delete buttons. */
    public void addActionChip(String label, VaadinIcon icon, Runnable onClick) {
        if (!editable || actionRow == null) {
            return;
        }
        Button button = new Button(label, icon.create());
        button.addThemeVariants(ButtonVariant.LUMO_SMALL);
        button.addClassName("op-action");
        button.addClickListener(event -> onClick.run());
        int insertionIndex = Math.max(0, actionRow.getComponentCount() - 2);
        actionRow.addComponentAtIndex(insertionIndex, button);
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
        summaryHeader = header;

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
        long resourceCount = countResources();
        // Each purchase position belongs to exactly one resource need of this position, so the
        // total is simply the count of purchases that have a resource need.
        long purchaseCount = countPurchasesWithResourceNeed();
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

    private long countPurchases() {
        return position.getPurchasePositions() != null ? position.getPurchasePositions().size() : 0;
    }

    private long countResources() {
        return position.getResourceNeeds() != null ? position.getResourceNeeds().size() : 0;
    }

    private long countPurchasesWithResourceNeed() {
        if (position.getPurchasePositions() == null) {
            return 0;
        }
        return position.getPurchasePositions().stream()
                .filter(purchase -> purchase.getResourceNeed() != null)
                .count();
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

    /** Flags this position with a ⚠ deviation badge; the tooltip lists the deviation details. */
    public void setDeviations(List<String> deviations) {
        if (deviations == null || deviations.isEmpty() || summaryHeader == null) {
            return;
        }
        StatusBadge badge =
                new StatusBadge(
                        "⚠ " + translator.apply("position.deviation", new Object[0]),
                        StatusBadge.StatusType.DANGER);
        badge.setTitle(String.join("\n", deviations));
        badge.getStyle().set("cursor", "help");
        summaryHeader.add(badge);
    }

    /**
     * Adds a muted "war: …" chip listing the train's past OTNs, so a renamed train stays findable.
     */
    public void setOtnHistory(List<String> pastOtns) {
        if (pastOtns == null || pastOtns.isEmpty() || summaryHeader == null) {
            return;
        }
        Span chip =
                new Span(
                        translator.apply(
                                "position.otnWas", new Object[] {String.join(", ", pastOtns)}));
        chip.getStyle()
                .set("font-size", "11px")
                .set("color", "var(--rom-text-muted)")
                .set("border", "1px dashed var(--rom-border)")
                .set("border-radius", "3px")
                .set("padding", "1px 6px");
        summaryHeader.add(chip);
    }

    /** Shows a selection checkbox (for mutator bulk actions) and reports each toggle. */
    public void enableSelection(Consumer<Boolean> onToggle) {
        selectCheckbox.setVisible(true);
        selectCheckbox.addValueChangeListener(
                e -> onToggle.accept(Boolean.TRUE.equals(e.getValue())));
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
