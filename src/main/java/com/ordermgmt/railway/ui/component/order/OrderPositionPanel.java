package com.ordermgmt.railway.ui.component.order;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.select.SelectVariant;

import com.ordermgmt.railway.domain.infrastructure.repository.OperationalPointRepository;
import com.ordermgmt.railway.domain.infrastructure.repository.PredefinedTagRepository;
import com.ordermgmt.railway.domain.order.model.Order;
import com.ordermgmt.railway.domain.order.model.OrderPosition;
import com.ordermgmt.railway.domain.order.model.OrderPositionVersion;
import com.ordermgmt.railway.domain.order.model.PositionChangeSource;
import com.ordermgmt.railway.domain.order.model.PositionOtnHistory;
import com.ordermgmt.railway.domain.order.model.PositionStatus;
import com.ordermgmt.railway.domain.order.model.PositionType;
import com.ordermgmt.railway.domain.order.model.PositionVariantType;
import com.ordermgmt.railway.domain.order.model.PurchaseStatus;
import com.ordermgmt.railway.domain.order.repository.PurchasePositionRepository;
import com.ordermgmt.railway.domain.order.repository.ResourceCatalogItemRepository;
import com.ordermgmt.railway.domain.order.service.AuditService;
import com.ordermgmt.railway.domain.order.service.OrderService;
import com.ordermgmt.railway.domain.order.service.PurchaseOrderService;
import com.ordermgmt.railway.domain.order.service.ResourceNeedService;
import com.ordermgmt.railway.domain.pathmanager.model.PathProcessState;
import com.ordermgmt.railway.domain.pathmanager.model.PmReferenceTrain;
import com.ordermgmt.railway.domain.pathmanager.service.PathManagerService;
import com.ordermgmt.railway.infrastructure.keycloak.CurrentUserHelper;
import com.ordermgmt.railway.ui.component.masterdetail.filter.FilterField;
import com.ordermgmt.railway.ui.component.masterdetail.filter.FilterPanel;
import com.ordermgmt.railway.ui.component.masterdetail.filter.PredicateSelectFilterField;
import com.ordermgmt.railway.ui.component.masterdetail.filter.SelectFilterField;

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
    private final PurchasePositionRepository purchasePositionRepository;
    private final AuditService auditService;
    private final com.ordermgmt.railway.domain.business.service.BusinessService businessService;
    private final BiFunction<String, Object[], String> translator;
    private final VerticalLayout rowContainer = new VerticalLayout();

    /**
     * SOB §5.7: the content lock is against the Auftraggeber (the non-mutator). Mutators
     * (ADMIN/DISPATCHER = die Planung) keep add/edit/delete during "in Bearbeitung".
     */
    private final boolean editable = CurrentUserHelper.hasAnyRole("ADMIN", "DISPATCHER");

    private final java.util.List<OrderPositionRow> rows = new java.util.ArrayList<>();
    private boolean compactMode = false;
    private boolean allExpanded = true;
    private Button toggleAllButton;
    private FilterPanel<OrderPosition> filterPanel;
    private Predicate<OrderPosition> positionFilter = p -> true;
    private boolean ready = false;
    private final java.util.Set<java.util.UUID> selectedPositionIds =
            new java.util.LinkedHashSet<>();
    private HorizontalLayout bulkBar;
    private Span bulkCountLabel;
    private Select<PositionStatus> bulkStatusSelect;
    // Per-refresh batched lookups (set at the start of refreshPositions).
    private java.util.Map<java.util.UUID, java.util.List<OrderPositionVersion>> versionsByPosition =
            java.util.Map.of();
    private java.util.Map<java.util.UUID, java.util.List<PositionOtnHistory>> otnByPosition =
            java.util.Map.of();

    public OrderPositionPanel(
            Order order,
            OrderService orderService,
            OperationalPointRepository opRepo,
            PredefinedTagRepository tagRepo,
            PathManagerService pathManagerService,
            ResourceNeedService resourceNeedService,
            PurchaseOrderService purchaseOrderService,
            ResourceCatalogItemRepository catalogItemRepository,
            PurchasePositionRepository purchasePositionRepository,
            AuditService auditService,
            com.ordermgmt.railway.domain.business.service.BusinessService businessService,
            BiFunction<String, Object[], String> translator) {
        this.order = order;
        this.orderService = orderService;
        this.opRepo = opRepo;
        this.tagRepo = tagRepo;
        this.pathManagerService = pathManagerService;
        this.resourceNeedService = resourceNeedService;
        this.purchaseOrderService = purchaseOrderService;
        this.catalogItemRepository = catalogItemRepository;
        this.purchasePositionRepository = purchasePositionRepository;
        this.auditService = auditService;
        this.businessService = businessService;
        this.translator = translator;

        setWidthFull();
        getStyle()
                .set("background", "var(--rom-bg-card)")
                .set("border", "1px solid var(--rom-border)")
                .set("border-radius", "6px")
                .set("padding", "var(--lumo-space-m) var(--lumo-space-l)")
                .set("box-sizing", "border-box");

        filterPanel = buildFilterPanel();
        add(createHeader());
        add(filterPanel);
        add(buildBulkBar());

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
            addService.addClickListener(e -> openServiceDialog(null));

            Button addTrain =
                    new Button("+ " + t("position.type.FAHRPLAN"), VaadinIcon.TRAIN.create());
            addTrain.addThemeVariants(ButtonVariant.LUMO_SMALL);
            addTrain.getStyle()
                    .set("color", "var(--rom-status-info)")
                    .set("border", "1px solid var(--rom-status-info)")
                    .set("background", "rgba(68,138,255,0.08)");
            addTrain.addClickListener(e -> openTimetableBuilder(null));

            Button addFromPm =
                    new Button("+ " + t("position.fromPm"), VaadinIcon.DOWNLOAD.create());
            addFromPm.addThemeVariants(ButtonVariant.LUMO_SMALL);
            addFromPm
                    .getStyle()
                    .set("color", "var(--rom-text-secondary)")
                    .set("border", "1px solid var(--rom-border)");
            addFromPm.addClickListener(e -> openUnassignedTrainsDialog());

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
        selectedPositionIds.clear();
        updateBulkBar();
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

        // One batched query for all positions' linked businesses (instead of one per row).
        java.util.Map<
                        java.util.UUID,
                        java.util.List<com.ordermgmt.railway.domain.business.model.Business>>
                businessesByPosition =
                        businessService.findByLinkedOrderPositions(
                                positions.stream().map(OrderPosition::getId).toList());

        // Batched version trail + OTN history for all positions (one query each, not per row).
        java.util.List<java.util.UUID> posIds =
                positions.stream().map(OrderPosition::getId).toList();
        versionsByPosition = orderService.findVersionsByPositions(posIds);
        otnByPosition = orderService.findOtnHistoryByPositions(posIds);

        // Group expressions (Ausprägungen) under their parent train identity; top-level rows =
        // ZUG identities + legacy flat positions. Children render indented beneath their parent.
        java.util.Map<java.util.UUID, java.util.List<OrderPosition>> childrenByParent =
                new java.util.LinkedHashMap<>();
        java.util.List<OrderPosition> tops = new java.util.ArrayList<>();
        for (OrderPosition pos : positions) {
            if (pos.getVariantType() == PositionVariantType.AUSPRAEGUNG
                    && pos.getVariantOf() != null) {
                childrenByParent
                        .computeIfAbsent(
                                pos.getVariantOf().getId(), k -> new java.util.ArrayList<>())
                        .add(pos);
            } else {
                tops.add(pos);
            }
        }

        java.util.Set<java.util.UUID> topIds = new java.util.HashSet<>();
        for (OrderPosition top : tops) {
            topIds.add(top.getId());
        }
        for (OrderPosition top : tops) {
            boolean isZug = top.getVariantType() == PositionVariantType.ZUG;
            // A ZUG identity is a container; its expressions carry the bookings, so it is not
            // directly selectable for bulk actions. Legacy flat positions stay selectable.
            OrderPositionRow topRow = renderPosition(top, businessesByPosition, false, !isZug);
            java.util.List<OrderPositionVersion> trainChanges = new java.util.ArrayList<>();
            for (OrderPosition child :
                    childrenByParent.getOrDefault(top.getId(), java.util.List.of())) {
                renderPosition(child, businessesByPosition, true, true);
                trainChanges.addAll(
                        versionsByPosition.getOrDefault(child.getId(), java.util.List.of()));
            }
            // Train-level Änderungs-Feed: aggregate every expression change under the ZUG.
            if (isZug && !trainChanges.isEmpty()) {
                topRow.addBodyContent(new VersionFeed(trainChanges, translator));
            }
        }
        // Expressions whose parent train was filtered out still match the filter themselves —
        // render them standalone so a filter (e.g. on status) never hides a matching expression.
        for (var entry : childrenByParent.entrySet()) {
            if (!topIds.contains(entry.getKey())) {
                for (OrderPosition orphan : entry.getValue()) {
                    renderPosition(orphan, businessesByPosition, false, true);
                }
            }
        }
    }

    /** Renders one position (train identity, expression, or legacy flat) into the container. */
    private OrderPositionRow renderPosition(
            OrderPosition pos,
            java.util.Map<
                            java.util.UUID,
                            java.util.List<com.ordermgmt.railway.domain.business.model.Business>>
                    businessesByPosition,
            boolean indented,
            boolean selectable) {
        PmReferenceTrain pmTrain = resolveTrain(pos);
        OrderPositionRow row =
                new OrderPositionRow(
                        pos,
                        pmTrain != null ? pmTrain.getProcessState() : null,
                        pmTrain != null ? pmTrain.getPlanningStatus() : null,
                        translator,
                        this::openPositionForEdit,
                        this::confirmDeletePosition,
                        p -> respondToAlteration(p, true),
                        p -> respondToAlteration(p, false),
                        auditService,
                        editable);
        if (indented) {
            row.getStyle()
                    .set("margin-left", "22px")
                    .set("border-left", "2px solid var(--rom-border)")
                    .set("padding-left", "8px");
        }
        rows.add(row);
        rowContainer.add(row);
        if (editable && selectable) {
            java.util.UUID pid = pos.getId();
            row.enableSelection(selected -> toggleSelection(pid, selected));
        }
        // Deviations (order ↔ RailOpt) plus any open infrastructure alterations from the versions.
        java.util.List<String> deviations =
                new java.util.ArrayList<>(DeviationDetector.detect(pos, pmTrain, translator));
        java.util.List<OrderPositionVersion> versions =
                versionsByPosition.getOrDefault(pos.getId(), java.util.List.of());
        java.time.LocalDate today = java.time.LocalDate.now();
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
        java.util.List<String> pastOtns =
                otnByPosition.getOrDefault(pos.getId(), java.util.List.of()).stream()
                        .filter(h -> h.getValidTo() != null)
                        .map(PositionOtnHistory::getOtn)
                        .distinct()
                        .toList();
        row.setOtnHistory(pastOtns);

        // Linked businesses for this position (clickable chips → business detail).
        var linkedBusinesses = businessesByPosition.getOrDefault(pos.getId(), java.util.List.of());
        if (!linkedBusinesses.isEmpty()) {
            var chips =
                    new com.ordermgmt.railway.ui.component.business.BusinessChips(
                            linkedBusinesses, this::t);
            chips.getStyle().set("margin", "0 12px 6px 12px");
            row.addBodyContent(chips);
        }

        // Per-expression change feed in the body (cheap; only when there are versions).
        if (!versions.isEmpty()) {
            row.addBodyContent(new VersionFeed(versions, translator));
        }

        // Resource panel — lazily built collapsible body. Its constructor loads resources, so a
        // collapsed compact row pays no DB/UI build cost until the user expands it.
        long resCount = pos.getResourceNeeds() != null ? pos.getResourceNeeds().size() : 0;
        if (resCount > 0) {
            row.addLazyBodyContent(
                    () -> {
                        ResourcePanel resourcePanel =
                                new ResourcePanel(
                                        pos,
                                        resourceNeedService,
                                        purchaseOrderService,
                                        catalogItemRepository,
                                        purchasePositionRepository,
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
    private PmReferenceTrain resolveTrain(OrderPosition pos) {
        if (pos.getType() != PositionType.FAHRPLAN || pos.getPmReferenceTrainId() == null) {
            return null;
        }
        try {
            return pathManagerService.findByIdWithVersions(pos.getPmReferenceTrainId());
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private void respondToAlteration(OrderPosition pos, boolean accept) {
        try {
            purchaseOrderService.respondToAlteration(pos.getId(), accept);
            Notification.show(
                            t(accept ? "order.alteration.accepted" : "order.alteration.rejected"),
                            2500,
                            Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (RuntimeException ex) {
            Notification.show(t("common.errorGeneric"), 3000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } finally {
            refreshPositions();
        }
    }

    private void openPositionForEdit(OrderPosition pos) {
        if (pos.getType() == PositionType.LEISTUNG) {
            openServiceDialog(pos);
        } else {
            openTimetableBuilder(pos);
        }
    }

    private void openServiceDialog(OrderPosition existing) {
        ServicePositionDialog dialog =
                new ServicePositionDialog(
                        order,
                        existing,
                        orderService,
                        opRepo,
                        tagRepo,
                        businessService,
                        translator);
        dialog.addSaveListener(e -> refreshPositions());
        dialog.open();
    }

    private void confirmDeletePosition(OrderPosition pos) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader(t("common.delete") + ": " + pos.getName() + "?");
        dialog.setCancelable(true);
        dialog.setCancelText(t("common.cancel"));
        dialog.setConfirmText(t("common.delete"));
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(
                e -> {
                    orderService.deletePosition(pos.getId());
                    refreshPositions();
                });
        dialog.open();
    }

    private void openTimetableBuilder(OrderPosition existing) {
        String target = "orders/" + order.getId() + "/timetable-builder";
        if (existing != null) {
            target += "?positionId=" + existing.getId();
        }
        UI.getCurrent().navigate(target);
    }

    /** Lists RailOpt reference trains not yet captured here and lets the user attach one. */
    private void openUnassignedTrainsDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(t("position.fromPm.title"));

        VerticalLayout list = new VerticalLayout();
        list.setPadding(false);
        list.setSpacing(true);
        list.setWidth("440px");

        var trains = pathManagerService.findUnassignedTrains();
        if (trains.isEmpty()) {
            Span empty = new Span(t("position.fromPm.empty"));
            empty.getStyle().set("color", "var(--rom-text-muted)").set("font-size", "13px");
            list.add(empty);
        } else {
            for (PmReferenceTrain train : trains) {
                Span info = new Span(unassignedLabel(train));
                info.getStyle().set("font-size", "13px");
                Button take =
                        new Button(t("position.fromPm.capture"), VaadinIcon.DOWNLOAD.create());
                take.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
                take.addClickListener(e -> captureUnassignedTrain(train, dialog));

                HorizontalLayout row = new HorizontalLayout(info, take);
                row.setWidthFull();
                row.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
                row.setAlignItems(FlexComponent.Alignment.CENTER);
                list.add(row);
            }
        }

        dialog.add(list);
        dialog.getFooter().add(new Button(t("common.cancel"), e -> dialog.close()));
        dialog.open();
    }

    private void captureUnassignedTrain(PmReferenceTrain train, Dialog dialog) {
        try {
            pathManagerService.captureUnassignedTrainAsPosition(train.getId(), order.getId());
            Notification.show(t("position.fromPm.captured"), 2500, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            dialog.close();
            refreshPositions();
        } catch (RuntimeException ex) {
            Notification.show(t("common.errorGeneric"), 3000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private String unassignedLabel(PmReferenceTrain train) {
        StringBuilder label = new StringBuilder();
        label.append(
                train.getOperationalTrainNumber() != null
                        ? "OTN " + train.getOperationalTrainNumber()
                        : train.getTridCore());
        if (train.getCalendarStart() != null && train.getCalendarEnd() != null) {
            label.append("  ·  ")
                    .append(train.getCalendarStart())
                    .append(" – ")
                    .append(train.getCalendarEnd());
        }
        return label.toString();
    }

    /**
     * Reusable, collapsible filter for this order's positions, by their Bestellpositions-status.
     */
    private FilterPanel<OrderPosition> buildFilterPanel() {
        List<FilterField<OrderPosition>> fields =
                List.of(
                        new SelectFilterField<>(
                                t("position.filter.internalStatus"),
                                List.of(PositionStatus.values()),
                                v -> t("position.status." + v.name()),
                                OrderPosition::getInternalStatus),
                        new PredicateSelectFilterField<OrderPosition, PathProcessState>(
                                t("position.filter.tttStatus"),
                                List.of(PathProcessState.values()),
                                v -> t("pm.state." + v.name()),
                                this::hasPurchaseWithTtt),
                        new PredicateSelectFilterField<OrderPosition, PurchaseStatus>(
                                t("position.filter.purchaseStatus"),
                                List.of(PurchaseStatus.values()),
                                v -> t("purchase.status." + v.name()),
                                this::hasPurchaseWithStatus));
        FilterPanel.Labels labels =
                new FilterPanel.Labels(
                        t("filter.toggle"),
                        t("filter.clearAll"),
                        t("filter.chip.clearAria"),
                        t("filter.panel.aria"));
        return new FilterPanel<>(
                fields,
                predicate -> {
                    positionFilter = predicate;
                    refreshPositions();
                },
                labels);
    }

    private boolean hasPurchaseWithTtt(OrderPosition pos, PathProcessState state) {
        return pos.getPurchasePositions() != null
                && pos.getPurchasePositions().stream()
                        .anyMatch(pp -> state.name().equals(pp.getPmProcessState()));
    }

    private boolean hasPurchaseWithStatus(OrderPosition pos, PurchaseStatus status) {
        return pos.getPurchasePositions() != null
                && pos.getPurchasePositions().stream()
                        .anyMatch(pp -> pp.getPurchaseStatus() == status);
    }

    private HorizontalLayout buildBulkBar() {
        bulkCountLabel = new Span();
        bulkStatusSelect = new Select<>();
        bulkStatusSelect.setItems(PositionStatus.values());
        bulkStatusSelect.setItemLabelGenerator(s -> t("position.status." + s.name()));
        bulkStatusSelect.setPlaceholder(t("bulk.status.placeholder"));
        bulkStatusSelect.addThemeVariants(SelectVariant.LUMO_SMALL);

        Button apply = new Button(t("bulk.apply"), VaadinIcon.CHECK.create());
        apply.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
        apply.addClickListener(e -> applyBulkStatus());

        bulkBar = new HorizontalLayout(bulkCountLabel, bulkStatusSelect, apply);
        bulkBar.setAlignItems(FlexComponent.Alignment.CENTER);
        bulkBar.setSpacing(true);
        bulkBar.getStyle()
                .set("margin", "4px 0 8px 0")
                .set("padding", "6px 10px")
                .set("background", "var(--rom-bg-card)")
                .set("border", "1px solid var(--rom-border)")
                .set("border-radius", "6px");
        bulkBar.setVisible(false);
        return bulkBar;
    }

    private void toggleSelection(java.util.UUID positionId, boolean selected) {
        if (selected) {
            selectedPositionIds.add(positionId);
        } else {
            selectedPositionIds.remove(positionId);
        }
        updateBulkBar();
    }

    private void updateBulkBar() {
        if (bulkBar == null) {
            return;
        }
        boolean anySelected = !selectedPositionIds.isEmpty();
        bulkBar.setVisible(anySelected);
        if (!anySelected) {
            bulkStatusSelect.clear(); // don't carry a stale status into the next selection
        }
        bulkCountLabel.setText(
                translator.apply("bulk.selected", new Object[] {selectedPositionIds.size()}));
    }

    private void applyBulkStatus() {
        PositionStatus status = bulkStatusSelect.getValue();
        if (status == null || selectedPositionIds.isEmpty()) {
            return;
        }
        int n =
                orderService.setPositionInternalStatusBulk(
                        new java.util.HashSet<>(selectedPositionIds), status);
        Notification.show(
                        translator.apply("bulk.done", new Object[] {n}),
                        2500,
                        Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        selectedPositionIds.clear();
        refreshPositions();
    }

    private String t(String key) {
        return translator.apply(key, new Object[0]);
    }
}
