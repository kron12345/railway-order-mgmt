package com.ordermgmt.railway.ui.component.order;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.BiFunction;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import com.ordermgmt.railway.domain.order.model.CoverageType;
import com.ordermgmt.railway.domain.order.model.OrderPosition;
import com.ordermgmt.railway.domain.order.model.PurchasePosition;
import com.ordermgmt.railway.domain.order.model.PurchaseStatus;
import com.ordermgmt.railway.domain.order.model.ResourceNeed;
import com.ordermgmt.railway.domain.order.model.ResourceType;
import com.ordermgmt.railway.domain.order.repository.PurchasePositionRepository;
import com.ordermgmt.railway.domain.order.repository.ResourceCatalogItemRepository;
import com.ordermgmt.railway.domain.order.service.AuditService;
import com.ordermgmt.railway.domain.order.service.PurchaseOrderService;
import com.ordermgmt.railway.domain.order.service.ResourceNeedService;
import com.ordermgmt.railway.ui.component.AuditHistoryDialog;

/**
 * Collapsible panel showing all resources and their purchases for one OrderPosition. Displays
 * resource type badges, coverage/origin info, and nested purchase positions with TTT status.
 */
public class ResourcePanel extends Div {

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.systemDefault());

    private final OrderPosition position;
    private final ResourceNeedService resourceNeedService;
    private final PurchaseOrderService purchaseOrderService;
    private final ResourceCatalogItemRepository catalogItemRepository;
    private final PurchasePositionRepository purchasePositionRepository;
    private final AuditService auditService;
    private final com.ordermgmt.railway.domain.business.service.BusinessService businessService;
    private final BiFunction<String, Object[], String> translator;
    private final Runnable refreshCallback;

    /**
     * Mutators on an unlocked order may add/trigger/sync; otherwise the panel is read-only (§5.7).
     */
    private final boolean editable;

    private final Div contentSlot = new Div();

    /** Businesses linked per purchase position, batched once per render (avoids one query/row). */
    private java.util.Map<
                    java.util.UUID,
                    java.util.List<com.ordermgmt.railway.domain.business.model.Business>>
            purchaseBizMap = java.util.Map.of();

    public ResourcePanel(
            OrderPosition position,
            ResourceNeedService resourceNeedService,
            PurchaseOrderService purchaseOrderService,
            ResourceCatalogItemRepository catalogItemRepository,
            PurchasePositionRepository purchasePositionRepository,
            AuditService auditService,
            com.ordermgmt.railway.domain.business.service.BusinessService businessService,
            BiFunction<String, Object[], String> translator,
            Runnable refreshCallback,
            boolean editable) {
        this.position = position;
        this.resourceNeedService = resourceNeedService;
        this.purchaseOrderService = purchaseOrderService;
        this.catalogItemRepository = catalogItemRepository;
        this.purchasePositionRepository = purchasePositionRepository;
        this.auditService = auditService;
        this.businessService = businessService;
        this.translator = translator;
        this.refreshCallback = refreshCallback;
        this.editable = editable;

        setWidthFull();
        getStyle()
                .set("background", "var(--rom-bg-primary)")
                .set("border", "1px solid var(--rom-border)")
                .set("border-radius", "4px")
                .set("padding", "8px 12px")
                .set("box-sizing", "border-box");

        contentSlot.setWidthFull();
        add(contentSlot);

        loadResources();
    }

    /** Reload all resources and purchases from DB and rebuild the UI. */
    public void loadResources() {
        contentSlot.removeAll();

        // Batch the linked-business lookup for ALL of this position's purchases (one IN query).
        java.util.List<java.util.UUID> ppIds =
                position.getPurchasePositions() == null
                        ? java.util.List.of()
                        : position.getPurchasePositions().stream()
                                .map(PurchasePosition::getId)
                                .toList();
        purchaseBizMap =
                ppIds.isEmpty()
                        ? java.util.Map.of()
                        : businessService.findByLinkedPurchasePositions(ppIds);

        List<ResourceNeed> resources =
                resourceNeedService.getResourcesForPosition(position.getId());

        Div header = createPanelHeader(resources.size());
        contentSlot.add(header);

        if (resources.isEmpty()) {
            Span empty = new Span(tr("resource.title") + ": —");
            empty.getStyle()
                    .set("color", "var(--rom-text-muted)")
                    .set("font-size", "11px")
                    .set("padding", "4px 0");
            contentSlot.add(empty);
        } else {
            for (ResourceNeed rn : resources) {
                contentSlot.add(createResourceRow(rn));
            }
        }

        contentSlot.add(createFooterButtons());
    }

    /** Reload this panel and notify the parent — used by the purchase action buttons. */
    private void reloadAll() {
        loadResources();
        if (refreshCallback != null) {
            refreshCallback.run();
        }
    }

    private Div createPanelHeader(int count) {
        Div header = new Div();
        header.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("gap", "8px")
                .set("margin-bottom", "6px");

        Span title = new Span(tr("resource.title") + " (" + count + ")");
        title.getStyle()
                .set("font-weight", "600")
                .set("font-size", "12px")
                .set("color", "var(--rom-text-primary)");

        header.add(title);
        return header;
    }

    private Div createResourceRow(ResourceNeed rn) {
        Div row = new Div();
        row.setWidthFull();
        row.getStyle()
                .set(
                        "border-left",
                        "2px solid " + ResourceBadges.resourceColor(rn.getResourceType()))
                .set("padding", "4px 0 4px 10px")
                .set("margin-bottom", "4px");

        // Resource info line
        HorizontalLayout info = new HorizontalLayout();
        info.setSpacing(true);
        info.setPadding(false);
        info.setAlignItems(FlexComponent.Alignment.CENTER);
        info.getStyle().set("flex-wrap", "wrap").set("gap", "4px");

        info.add(createTypeBadge(rn));
        Span description = createDescriptionLabel(rn);
        if (description != null) {
            info.add(description);
        }
        info.add(createCoverageBadge(rn));
        info.add(createOriginBadge(rn));

        if (rn.getQuantity() != null && rn.getQuantity() > 1) {
            info.add(ResourceBadges.small("x" + rn.getQuantity(), "var(--rom-text-secondary)"));
        }

        // Audit history button
        Button histBtn = new Button(VaadinIcon.CLOCK.create());
        histBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        histBtn.getStyle()
                .set("color", "var(--rom-text-muted)")
                .set("min-width", "24px")
                .set("padding", "0");
        histBtn.setTooltipText(tr("audit.button"));
        histBtn.addClickListener(e -> openResourceHistory(rn));
        info.add(histBtn);

        row.add(info);

        // Nested purchase positions
        List<PurchasePosition> purchases = findPurchasesForNeed(rn);
        if (rn.getCoverageType() == CoverageType.INTERNAL && purchases.isEmpty()) {
            Span intern = new Span("(" + tr("resource.coverage.INTERNAL").toLowerCase() + ")");
            intern.getStyle()
                    .set("font-size", "10px")
                    .set("color", "var(--rom-text-muted)")
                    .set("padding-left", "12px");
            row.add(intern);
        } else {
            for (PurchasePosition pp : purchases) {
                row.add(createPurchaseRow(pp, rn));
            }
        }

        // Add Purchase button for EXTERNAL resources (mutators on an unlocked order only)
        if (editable && rn.getCoverageType() == CoverageType.EXTERNAL) {
            row.add(createAddPurchaseButton(rn));
        }

        return row;
    }

    private Div createPurchaseRow(PurchasePosition pp, ResourceNeed need) {
        Div row = new Div();
        row.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("gap", "6px")
                .set("padding", "2px 0 2px 12px")
                .set("flex-wrap", "wrap");

        // Position number
        Span number = new Span(pp.getPositionNumber());
        number.getStyle()
                .set("font-family", "'JetBrains Mono', monospace")
                .set("font-size", "10px")
                .set("font-weight", "600")
                .set("color", "var(--rom-accent)");
        row.add(number);

        // Free-text description (entered in the purchase dialog) — shown muted next to the number.
        if (pp.getDescription() != null && !pp.getDescription().isBlank()) {
            Span desc = new Span(pp.getDescription());
            desc.getStyle().set("font-size", "11px").set("color", "var(--rom-text-secondary)");
            row.add(desc);
        }

        // Status badge
        row.add(createPurchaseStatusBadge(pp));

        // TTT status for CAPACITY purchases
        if (pp.getPmPathRequestId() != null) {
            if (pp.getPmProcessState() != null) {
                row.add(
                        ResourceBadges.small(
                                "TTT: " + pp.getPmProcessState(), "var(--rom-status-info)"));
            }
            if (pp.getPmTtrPhase() != null) {
                row.add(ResourceBadges.small(pp.getPmTtrPhase(), "var(--rom-text-secondary)"));
            }

            // Sync button (mutators on an unlocked order only)
            if (editable) {
                row.add(
                        PurchaseOrderButtons.sync(
                                pp, purchaseOrderService, translator, this::reloadAll));
            }
        }

        // TTT order button for unordered CAPACITY purchases (mutators on an unlocked order only)
        if (editable
                && pp.getPmPathRequestId() == null
                && need.getResourceType() == ResourceType.CAPACITY) {
            row.add(
                    PurchaseOrderButtons.ttt(
                            pp,
                            position.getOperationalTrainNumber(),
                            buildRouteLabel(),
                            purchaseOrderService,
                            translator,
                            this::reloadAll));
        }

        // R²P channel for non-capacity external needs (e.g. Lokführer): mock "Bestellen" →
        // BESTELLT,
        // shown like the TTT flow so it reads the same way.
        if (isR2pPurchase(need)) {
            row.add(ResourceBadges.small("R²P", "var(--rom-status-info)"));
            if (editable && pp.getPurchaseStatus() == PurchaseStatus.OFFEN) {
                row.add(
                        PurchaseOrderButtons.r2p(
                                pp, purchaseOrderService, translator, this::reloadAll));
            }
        }

        // Ordered-at timestamp
        if (pp.getOrderedAt() != null) {
            Span orderedAt = new Span(DT_FMT.format(pp.getOrderedAt()));
            orderedAt
                    .getStyle()
                    .set("font-size", "9px")
                    .set("color", "var(--rom-text-muted)")
                    .set("font-family", "'JetBrains Mono', monospace");
            row.add(orderedAt);
        }

        // Linked businesses for this purchase position (clickable chips → business detail).
        var linkedBusinesses = purchaseBizMap.getOrDefault(pp.getId(), java.util.List.of());
        if (!linkedBusinesses.isEmpty()) {
            row.add(
                    new com.ordermgmt.railway.ui.component.business.BusinessChips(
                            linkedBusinesses, this::tr));
        }

        return row;
    }

    private Div createFooterButtons() {
        Div footer = new Div();
        footer.getStyle()
                .set("display", "flex")
                .set("gap", "8px")
                .set("margin-top", "6px")
                .set("flex-wrap", "wrap");

        // Add-resource / trigger-all are mutations: hidden on a locked order or for non-mutators.
        if (!editable) {
            return footer;
        }

        // + Resource button
        Button addRes = new Button(tr("resource.add"), VaadinIcon.PLUS.create());
        addRes.addThemeVariants(ButtonVariant.LUMO_SMALL);
        addRes.getStyle()
                .set("font-size", "10px")
                .set("color", "var(--rom-accent)")
                .set("border", "1px solid rgba(45,212,191,0.3)")
                .set("background", "rgba(45,212,191,0.06)")
                .set("padding", "2px 8px");
        addRes.addClickListener(
                e -> {
                    ResourceDialog dialog =
                            new ResourceDialog(
                                    position,
                                    resourceNeedService,
                                    catalogItemRepository,
                                    translator);
                    dialog.addSaveListener(ev -> loadResources());
                    dialog.open();
                });
        footer.add(addRes);

        // Trigger all capacity orders button
        Button orderAll = new Button(tr("purchase.triggerAll"), VaadinIcon.CART.create());
        orderAll.addThemeVariants(ButtonVariant.LUMO_SMALL);
        orderAll.getStyle()
                .set("font-size", "10px")
                .set("color", "var(--rom-status-warning)")
                .set("border", "1px solid var(--rom-status-warning)")
                .set("background", "rgba(255,184,0,0.06)")
                .set("padding", "2px 8px");
        orderAll.addClickListener(
                e -> {
                    try {
                        purchaseOrderService.triggerAllCapacityOrders(position.getId());
                        loadResources();
                        if (refreshCallback != null) refreshCallback.run();
                        Notification.show("TTT", 2000, Notification.Position.BOTTOM_END)
                                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    } catch (Exception ex) {
                        Notification.show(ex.getMessage(), 4000, Notification.Position.BOTTOM_END)
                                .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    }
                });
        footer.add(orderAll);

        return footer;
    }

    private Button createAddPurchaseButton(ResourceNeed rn) {
        Button btn = new Button("+ " + tr("purchase.add"));
        btn.addThemeVariants(ButtonVariant.LUMO_SMALL);
        btn.getStyle()
                .set("font-size", "10px")
                .set("color", "var(--rom-text-muted)")
                .set("background", "transparent")
                .set("padding", "1px 6px 1px 12px");
        btn.addClickListener(
                e -> {
                    PurchaseDialog dialog =
                            new PurchaseDialog(
                                    rn.getId(),
                                    tr("resource.type." + rn.getResourceType().name()),
                                    tr("resource.coverage." + rn.getCoverageType().name()),
                                    rn.getResourceType() == ResourceType.CAPACITY,
                                    position.getValidity(),
                                    position.getOperationalTrainNumber(),
                                    buildRouteLabel(),
                                    purchaseOrderService,
                                    translator);
                    dialog.addSaveListener(ev -> loadResources());
                    dialog.open();
                });
        return btn;
    }

    /** A non-capacity external purchase is ordered via the (mock) R²P channel, not TTT. */
    private boolean isR2pPurchase(ResourceNeed need) {
        return need.getCoverageType() == CoverageType.EXTERNAL
                && need.getResourceType() != ResourceType.CAPACITY;
    }

    private List<PurchasePosition> findPurchasesForNeed(ResourceNeed rn) {
        return purchasePositionRepository.findByResourceNeedId(rn.getId());
    }

    private void openResourceHistory(ResourceNeed rn) {
        if (auditService == null) {
            return;
        }
        var entries = auditService.getResourceNeedHistory(rn.getId());
        String title =
                tr("audit.title") + " — " + tr("resource.type." + rn.getResourceType().name());
        var dialog = new AuditHistoryDialog(title, entries, translator);
        dialog.open();
    }

    // --- Badge helpers ---

    private Span createTypeBadge(ResourceNeed rn) {
        String label = tr("resource.type." + rn.getResourceType().name());
        String color = ResourceBadges.resourceColor(rn.getResourceType());
        return ResourceBadges.small(label, color);
    }

    /**
     * Descriptive label next to the type badge. Returns {@code null} when there is nothing to add
     * beyond the badge, so the row no longer prints e.g. "Kapazität · Kapazität". For a CAPACITY
     * need — the Fahrplantrasse that has to be ordered — the route is shown instead, which is more
     * telling than repeating the type.
     */
    private Span createDescriptionLabel(ResourceNeed rn) {
        String text = rn.getDescription() != null ? rn.getDescription() : "";
        if (rn.getCatalogItem() != null) {
            text = rn.getCatalogItem().getName() + (text.isEmpty() ? "" : " " + text);
        }
        if (text.isEmpty() && rn.getResourceType() == ResourceType.CAPACITY) {
            String route = buildRouteLabel();
            if (route != null && !route.isBlank()) {
                text = route;
            }
        }
        if (text.isEmpty()) {
            return null;
        }
        Span label = new Span(text);
        label.getStyle()
                .set("font-size", "11px")
                .set("color", "var(--rom-text-primary)")
                .set("font-weight", "500");
        return label;
    }

    private Span createCoverageBadge(ResourceNeed rn) {
        String label = tr("resource.coverage." + rn.getCoverageType().name());
        String color =
                rn.getCoverageType() == CoverageType.EXTERNAL
                        ? "var(--rom-status-warning)"
                        : "var(--rom-status-active)";
        return ResourceBadges.small(label, color);
    }

    private Span createOriginBadge(ResourceNeed rn) {
        String label = tr("resource.origin." + rn.getOrigin().name());
        return ResourceBadges.small(label, "var(--rom-text-muted)");
    }

    private Span createPurchaseStatusBadge(PurchasePosition pp) {
        String label = tr("purchase.status." + pp.getPurchaseStatus().name());
        String color = ResourceBadges.purchaseStatusColor(pp.getPurchaseStatus());
        return ResourceBadges.small(label, color);
    }

    private String buildRouteLabel() {
        if (position.getFromLocation() != null && position.getToLocation() != null) {
            return position.getFromLocation() + " \u2192 " + position.getToLocation();
        }
        return null;
    }

    private String tr(String key) {
        return translator.apply(key, new Object[0]);
    }
}
