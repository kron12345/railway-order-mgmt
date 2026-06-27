package com.ordermgmt.railway.ui.component.order;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import com.ordermgmt.railway.domain.business.model.Business;
import com.ordermgmt.railway.domain.business.service.BusinessService;
import com.ordermgmt.railway.domain.infrastructure.repository.OperationalPointRepository;
import com.ordermgmt.railway.domain.infrastructure.repository.PredefinedTagRepository;
import com.ordermgmt.railway.domain.order.model.Order;
import com.ordermgmt.railway.domain.order.model.OrderPosition;
import com.ordermgmt.railway.domain.order.model.OrderPositionVersion;
import com.ordermgmt.railway.domain.order.model.PositionChangeSource;
import com.ordermgmt.railway.domain.order.model.PositionOtnHistory;
import com.ordermgmt.railway.domain.order.model.PositionType;
import com.ordermgmt.railway.domain.order.model.PositionVariantType;
import com.ordermgmt.railway.domain.order.repository.ResourceCatalogItemRepository;
import com.ordermgmt.railway.domain.order.service.AuditService;
import com.ordermgmt.railway.domain.order.service.FristService;
import com.ordermgmt.railway.domain.order.service.OrderService;
import com.ordermgmt.railway.domain.order.service.PurchaseOrderService;
import com.ordermgmt.railway.domain.order.service.ResourceNeedService;
import com.ordermgmt.railway.domain.pathmanager.model.PmReferenceTrain;
import com.ordermgmt.railway.domain.pathmanager.service.PathManagerService;
import com.ordermgmt.railway.domain.timetable.service.TimetableArchiveService;
import com.ordermgmt.railway.infrastructure.keycloak.CurrentUserHelper;
import com.ordermgmt.railway.ui.component.business.BusinessChips;
import com.ordermgmt.railway.ui.component.masterdetail.filter.FilterPanel;

/** Displays and manages the positions that belong to an order. */
public class OrderPositionPanel extends Div {

    private final Order order;
    private final OrderService orderService;
    private final OperationalPointRepository opRepo;
    private final PredefinedTagRepository tagRepo;
    private final PathManagerService pathManagerService;
    private final ResourceNeedService resourceNeedService;
    private final PurchaseOrderService purchaseOrderService;
    private final ResourceCatalogItemRepository catalogItemRepository;
    private final AuditService auditService;
    private final BusinessService businessService;
    private final FristService fristService;
    private final BiFunction<String, Object[], String> translator;
    private final VerticalLayout rowContainer = new VerticalLayout();
    private Map<UUID, FristService.FristEntry> deadlinesByPosition = Map.of();

    /**
     * SOB §5.7: the content lock is against the Auftraggeber (the non-mutator). Mutators
     * (ADMIN/DISPATCHER = die Planung) keep add/edit/delete during "in Bearbeitung".
     */
    private final boolean editable = CurrentUserHelper.hasAnyRole("ADMIN", "DISPATCHER");

    private final List<OrderPositionRow> rows = new ArrayList<>();
    private boolean compactMode = false;
    private boolean allExpanded = true;
    private Button toggleAllButton;
    private FilterPanel<OrderPosition> filterPanel;
    private Predicate<OrderPosition> positionFilter = p -> true;
    private boolean ready = false;
    private PositionBulkBar bulkBar;
    private OrderPositionActions actions;
    // Per-refresh batched lookups (set at the start of refreshPositions).
    private Map<UUID, List<OrderPositionVersion>> versionsByPosition = Map.of();
    private Map<UUID, List<PositionOtnHistory>> otnByPosition = Map.of();

    public OrderPositionPanel(
            Order order,
            OrderService orderService,
            TimetableArchiveService timetableArchiveService,
            OperationalPointRepository opRepo,
            PredefinedTagRepository tagRepo,
            PathManagerService pathManagerService,
            ResourceNeedService resourceNeedService,
            PurchaseOrderService purchaseOrderService,
            ResourceCatalogItemRepository catalogItemRepository,
            AuditService auditService,
            BusinessService businessService,
            FristService fristService,
            BiFunction<String, Object[], String> translator) {
        this.order = order;
        this.orderService = orderService;
        this.opRepo = opRepo;
        this.tagRepo = tagRepo;
        this.pathManagerService = pathManagerService;
        this.resourceNeedService = resourceNeedService;
        this.purchaseOrderService = purchaseOrderService;
        this.catalogItemRepository = catalogItemRepository;
        this.auditService = auditService;
        this.businessService = businessService;
        this.fristService = fristService;
        this.translator = translator;

        setWidthFull();
        getStyle()
                .set("background", "var(--rom-bg-card)")
                .set("border", "1px solid var(--rom-border)")
                .set("border-radius", "6px")
                .set("padding", "var(--lumo-space-m) var(--lumo-space-l)")
                .set("box-sizing", "border-box");

        filterPanel =
                OrderPositionFilters.build(
                        translator,
                        predicate -> {
                            positionFilter = predicate;
                            refreshPositions();
                        });
        bulkBar = new PositionBulkBar(orderService, translator, this::refreshPositions);
        actions =
                new OrderPositionActions(
                        order,
                        orderService,
                        timetableArchiveService,
                        opRepo,
                        tagRepo,
                        businessService,
                        purchaseOrderService,
                        translator,
                        this::refreshPositions);
        add(createHeader());
        add(filterPanel);
        add(bulkBar);

        rowContainer.setPadding(false);
        rowContainer.setSpacing(false);
        rowContainer.setWidthFull();
        add(rowContainer);

        // The filter panel's initial recompute() fires onChange during construction; guard against
        // that so we render exactly once here (and on every later filter change).
        ready = true;
        refreshPositions();
    }

    private HorizontalLayout createHeader() {
        H3 title = new H3(t("position.title"));
        title.getStyle()
                .set("color", "var(--rom-text-primary)")
                .set("margin", "0")
                .set("font-size", "var(--lumo-font-size-l)");

        HorizontalLayout buttons = new HorizontalLayout();
        buttons.setSpacing(true);
        buttons.setAlignItems(FlexComponent.Alignment.CENTER);

        // View controls (for everyone): compact/full toggle + collapse-/expand-all.
        toggleAllButton = new Button();
        toggleAllButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
        applyToggleAllLabel(toggleAllButton);
        toggleAllButton.addClickListener(
                e -> {
                    allExpanded = !allExpanded;
                    rows.forEach(r -> r.setBodyExpanded(allExpanded));
                    applyToggleAllLabel(toggleAllButton);
                });

        Button modeBtn = new Button(VaadinIcon.GRID_SMALL.create());
        modeBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
        applyModeButtonLabel(modeBtn);
        modeBtn.addClickListener(
                e -> {
                    compactMode = !compactMode;
                    allExpanded = !compactMode;
                    applyModeButtonLabel(modeBtn);
                    applyToggleAllLabel(toggleAllButton);
                    refreshPositions();
                });

        buttons.add(modeBtn, toggleAllButton, filterPanel.getToggle());

        // Add-position controls only for mutators on an unlocked order (SOB §5.7).
        if (editable) {
            Button addService =
                    new Button("+ " + t("position.type.LEISTUNG"), VaadinIcon.TOOLS.create());
            addService.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
            addService
                    .getStyle()
                    .set("background", "var(--rom-accent)")
                    .set("color", "var(--rom-bg-primary)");
            addService.addClickListener(e -> actions.openServiceDialog(null));

            Button addTrain =
                    new Button("+ " + t("position.type.FAHRPLAN"), VaadinIcon.TRAIN.create());
            addTrain.addThemeVariants(ButtonVariant.LUMO_SMALL);
            addTrain.getStyle()
                    .set("color", "var(--rom-status-info)")
                    .set("border", "1px solid var(--rom-status-info)")
                    .set("background", "rgba(68,138,255,0.08)");
            addTrain.addClickListener(e -> actions.openTimetableBuilder(null));

            Button addFromPm =
                    new Button("+ " + t("position.fromPm"), VaadinIcon.DOWNLOAD.create());
            addFromPm.addThemeVariants(ButtonVariant.LUMO_SMALL);
            addFromPm
                    .getStyle()
                    .set("color", "var(--rom-text-secondary)")
                    .set("border", "1px solid var(--rom-border)");
            addFromPm.addClickListener(
                    e ->
                            new UnassignedTrainsDialog(
                                            pathManagerService,
                                            order.getId(),
                                            translator,
                                            this::refreshPositions)
                                    .open());

            buttons.add(addService, addTrain, addFromPm);
        }

        HorizontalLayout header = new HorizontalLayout(title, buttons);
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.getStyle().set("margin-bottom", "var(--lumo-space-m)");
        return header;
    }

    private void applyModeButtonLabel(Button b) {
        b.setText(compactMode ? t("position.view.full") : t("position.view.compact"));
    }

    private void applyToggleAllLabel(Button b) {
        b.setText(allExpanded ? t("position.view.collapseAll") : t("position.view.expandAll"));
        b.setIcon((allExpanded ? VaadinIcon.CHEVRON_UP : VaadinIcon.CHEVRON_DOWN).create());
    }

    private void refreshPositions() {
        if (!ready) {
            return; // still constructing; the constructor renders once after setup
        }
        rowContainer.removeAll();
        rows.clear();
        bulkBar.reset();
        var positions =
                orderService.findPositionsByOrderId(order.getId()).stream()
                        .filter(positionFilter)
                        .toList();

        if (positions.isEmpty()) {
            Span empty = new Span(t("order.positions.empty"));
            empty.getStyle()
                    .set("color", "var(--rom-text-muted)")
                    .set("font-size", "12px")
                    .set("padding", "var(--lumo-space-m) 0");
            rowContainer.add(empty);
            return;
        }

        Map<UUID, List<Business>> businessesByPosition = loadBusinessesByPosition(positions);
        deadlinesByPosition = fristService.mostUrgentByPosition(positions);
        loadPositionHistory(positions);

        PositionGroups groups = groupPositions(positions);
        Set<UUID> topIds =
                groups.topLevel().stream().map(OrderPosition::getId).collect(Collectors.toSet());
        for (OrderPosition top : groups.topLevel()) {
            boolean isZug = top.getVariantType() == PositionVariantType.ZUG;
            // A ZUG identity is a container; its expressions carry the bookings, so it is not
            // directly selectable for bulk actions. Legacy flat positions stay selectable.
            // A ZUG is a header above its expression cards; a flat position is itself one card.
            OrderPositionRow topRow = renderPosition(top, businessesByPosition, !isZug, !isZug);
            List<OrderPositionVersion> trainChanges = new ArrayList<>();
            for (OrderPosition child :
                    groups.childrenByParent().getOrDefault(top.getId(), List.of())) {
                renderPosition(child, businessesByPosition, true, true);
                trainChanges.addAll(versionsByPosition.getOrDefault(child.getId(), List.of()));
            }
            // Train-level Änderungs-Feed: aggregate every expression change under the ZUG.
            if (isZug && !trainChanges.isEmpty()) {
                topRow.addBodyContent(new VersionFeed(trainChanges, translator));
            }
            // Add an expression (Ausprägung) under any FAHRPLAN train (promotes a flat one to ZUG).
            if (editable && top.getType() == PositionType.FAHRPLAN) {
                rowContainer.add(createAddExpressionButton(top));
            }
        }
        // Expressions whose parent train was filtered out still match the filter themselves —
        // render them standalone so a filter (e.g. on status) never hides a matching expression.
        for (var entry : groups.childrenByParent().entrySet()) {
            if (!topIds.contains(entry.getKey())) {
                for (OrderPosition orphan : entry.getValue()) {
                    renderPosition(orphan, businessesByPosition, true, true);
                }
            }
        }
    }

    private Map<UUID, List<Business>> loadBusinessesByPosition(List<OrderPosition> positions) {
        return businessService.findByLinkedOrderPositions(
                positions.stream().map(OrderPosition::getId).toList());
    }

    private void loadPositionHistory(List<OrderPosition> positions) {
        List<UUID> positionIds = positions.stream().map(OrderPosition::getId).toList();
        versionsByPosition = orderService.findVersionsByPositions(positionIds);
        otnByPosition = orderService.findOtnHistoryByPositions(positionIds);
    }

    private PositionGroups groupPositions(List<OrderPosition> positions) {
        Map<UUID, List<OrderPosition>> childrenByParent = new LinkedHashMap<>();
        List<OrderPosition> topLevel = new ArrayList<>();
        for (OrderPosition position : positions) {
            if (isExpressionWithParent(position)) {
                childrenByParent
                        .computeIfAbsent(position.getVariantOf().getId(), key -> new ArrayList<>())
                        .add(position);
                continue;
            }
            topLevel.add(position);
        }
        return new PositionGroups(topLevel, childrenByParent);
    }

    private boolean isExpressionWithParent(OrderPosition position) {
        return position.getVariantType() == PositionVariantType.AUSPRAEGUNG
                && position.getVariantOf() != null;
    }

    private Button createAddExpressionButton(OrderPosition parent) {
        Button addExpression = new Button(t("expression.add.button"), VaadinIcon.PLUS.create());
        addExpression.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        addExpression.getStyle().set("margin", "2px 0 10px 22px");
        if (OrderPositionActions.canSplit(parent)) {
            addExpression.addClickListener(event -> actions.openAddExpression(parent));
            return addExpression;
        }
        addExpression.setEnabled(false);
        addExpression.setTooltipText(t("expression.hasBookings"));
        return addExpression;
    }

    /** Renders one position (train identity, expression, or legacy flat) into the container. */
    private OrderPositionRow renderPosition(
            OrderPosition position,
            Map<UUID, List<Business>> businessesByPosition,
            boolean card,
            boolean selectable) {
        PmReferenceTrain pmTrain = resolveTrain(position);
        OrderPositionRow row =
                new OrderPositionRow(
                        position,
                        pmTrain != null ? pmTrain.getProcessState() : null,
                        pmTrain != null ? pmTrain.getPlanningStatus() : null,
                        translator,
                        actions::editPosition,
                        actions::confirmDelete,
                        p -> actions.respondToAlteration(p, true),
                        p -> actions.respondToAlteration(p, false),
                        auditService,
                        editable);
        OrderPositionDeadlineBadge.apply(
                row, deadlinesByPosition.get(position.getId()), translator);
        if (card) {
            // Expressions (and legacy flat positions) render as full-width cards, no indent.
            row.getStyle()
                    .set("border", "1px solid var(--rom-border)")
                    .set("border-radius", "6px")
                    .set("margin", "6px 0")
                    .set("overflow", "hidden")
                    .set("background", "var(--rom-bg-card)");
            // Verkehrstage editor on each FAHRPLAN card (expression or flat) — set/reassign days.
            if (editable && position.getType() == PositionType.FAHRPLAN) {
                row.addActionChip(
                        t("position.action.verkehrstage"),
                        VaadinIcon.CALENDAR_O,
                        () -> actions.openVerkehrstageDialog(position));
            }
        }
        rows.add(row);
        rowContainer.add(row);
        if (editable && selectable) {
            UUID positionId = position.getId();
            row.enableSelection(selected -> bulkBar.toggle(positionId, selected));
        }
        // Deviations (order ↔ RailOpt) plus any open infrastructure alterations from the versions.
        List<String> deviations =
                new ArrayList<>(DeviationDetector.detect(position, pmTrain, translator));
        List<OrderPositionVersion> versions =
                versionsByPosition.getOrDefault(position.getId(), List.of());
        LocalDate today = LocalDate.now();
        for (OrderPositionVersion v : versions) {
            // Only still-relevant alterations earn a ⚠; expired ones stay in the feed as history.
            boolean active = v.getValidTo() == null || !v.getValidTo().isBefore(today);
            if (v.getSource() == PositionChangeSource.ALTERATION && active) {
                deviations.add(
                        translator.apply(
                                "version.alterationFlag", new Object[] {v.getChangeSummary()}));
            }
        }
        row.setDeviations(deviations);

        // OTN history chip: past numbers, so a renamed train stays recognizable.
        List<String> pastOtns =
                otnByPosition.getOrDefault(position.getId(), List.of()).stream()
                        .filter(h -> h.getValidTo() != null)
                        .map(PositionOtnHistory::getOtn)
                        .distinct()
                        .toList();
        row.setOtnHistory(pastOtns);

        // Linked businesses for this position (clickable chips → business detail).
        var linkedBusinesses = businessesByPosition.getOrDefault(position.getId(), List.of());
        if (!linkedBusinesses.isEmpty()) {
            var chips = new BusinessChips(linkedBusinesses, this::t);
            chips.getStyle().set("margin", "0 12px 6px 12px");
            row.addBodyContent(chips);
        }

        // Per-expression change feed in the body (cheap; only when there are versions).
        if (!versions.isEmpty()) {
            row.addBodyContent(new VersionFeed(versions, translator));
        }

        // Resource panel — lazily built collapsible body. Its constructor loads resources, so a
        // collapsed compact row pays no DB/UI build cost until the user expands it.
        long resourceCount =
                position.getResourceNeeds() != null ? position.getResourceNeeds().size() : 0;
        if (resourceCount > 0) {
            row.addLazyBodyContent(
                    () -> {
                        ResourcePanel resourcePanel =
                                new ResourcePanel(
                                        position,
                                        resourceNeedService,
                                        purchaseOrderService,
                                        catalogItemRepository,
                                        opRepo,
                                        auditService,
                                        businessService,
                                        translator,
                                        this::refreshPositions,
                                        editable);
                        resourcePanel.getStyle().set("margin", "0 12px 8px 12px");
                        return resourcePanel;
                    });
        }

        row.setBodyExpanded(allExpanded);
        return row;
    }

    /**
     * Resolves the linked RailOpt reference train for a transferred FAHRPLAN position so the row
     * can show its lifecycle and planning status. Returns {@code null} when not sent or when the
     * train can no longer be resolved (e.g. cleared mock state).
     */
    private PmReferenceTrain resolveTrain(OrderPosition position) {
        if (position.getType() != PositionType.FAHRPLAN
                || position.getPmReferenceTrainId() == null) {
            return null;
        }
        try {
            return pathManagerService.findByIdWithVersions(position.getPmReferenceTrainId());
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private String t(String key) {
        return translator.apply(key, new Object[0]);
    }

    private record PositionGroups(
            List<OrderPosition> topLevel, Map<UUID, List<OrderPosition>> childrenByParent) {}
}
