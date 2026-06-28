package com.ordermgmt.railway.ui.component.order;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import com.ordermgmt.railway.domain.business.model.Business;
import com.ordermgmt.railway.domain.business.service.BusinessService;
import com.ordermgmt.railway.domain.infrastructure.repository.OperationalPointRepository;
import com.ordermgmt.railway.domain.order.model.CoverageType;
import com.ordermgmt.railway.domain.order.model.OrderPosition;
import com.ordermgmt.railway.domain.order.model.PurchasePosition;
import com.ordermgmt.railway.domain.order.model.ResourceNeed;
import com.ordermgmt.railway.domain.order.model.ResourceType;
import com.ordermgmt.railway.domain.order.model.ValidityJsonCodec;
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

    private final OrderPosition position;
    private final ResourceNeedService resourceNeedService;
    private final PurchaseOrderService purchaseOrderService;
    private final ResourceCatalogItemRepository catalogItemRepository;
    private final OperationalPointRepository opRepo;
    private final AuditService auditService;
    private final BusinessService businessService;
    private final BiFunction<String, Object[], String> translator;
    private final Runnable refreshCallback;

    /**
     * Mutators on an unlocked order may add/trigger/sync; otherwise the panel is read-only (§5.7).
     */
    private final boolean editable;

    private final Div contentSlot = new Div();

    /** Businesses linked per purchase position, batched once per render (avoids one query/row). */
    private Map<UUID, List<Business>> purchaseBizMap = Map.of();

    /** Purchases grouped by resource-need id, built once per render from the loaded collection. */
    private Map<UUID, List<PurchasePosition>> purchasesByNeed = Map.of();

    public ResourcePanel(
            OrderPosition position,
            ResourceNeedService resourceNeedService,
            PurchaseOrderService purchaseOrderService,
            ResourceCatalogItemRepository catalogItemRepository,
            OperationalPointRepository opRepo,
            AuditService auditService,
            BusinessService businessService,
            BiFunction<String, Object[], String> translator,
            Runnable refreshCallback,
            boolean editable) {
        this.position = position;
        this.resourceNeedService = resourceNeedService;
        this.purchaseOrderService = purchaseOrderService;
        this.catalogItemRepository = catalogItemRepository;
        this.opRepo = opRepo;
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

        loadPurchaseLookups();

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
            for (ResourceNeed resourceNeed : resources) {
                contentSlot.add(createResourceRow(resourceNeed));
            }
        }

        contentSlot.add(createFooterButtons());
    }

    private void loadPurchaseLookups() {
        List<PurchasePosition> purchases =
                position.getPurchasePositions() == null
                        ? List.of()
                        : List.copyOf(position.getPurchasePositions());
        List<UUID> purchaseIds = purchases.stream().map(PurchasePosition::getId).toList();
        purchaseBizMap =
                purchaseIds.isEmpty()
                        ? Map.of()
                        : businessService.findByLinkedPurchasePositions(purchaseIds);

        purchasesByNeed =
                purchases.stream()
                        .filter(purchase -> purchase.getResourceNeed() != null)
                        .collect(
                                Collectors.groupingBy(
                                        purchase -> purchase.getResourceNeed().getId()));
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

    private Div createResourceRow(ResourceNeed resourceNeed) {
        Div row = new Div();
        row.setWidthFull();
        row.getStyle()
                .set(
                        "border-left",
                        "2px solid " + ResourceBadges.resourceColor(resourceNeed.getResourceType()))
                .set("padding", "4px 0 4px 10px")
                .set("margin-bottom", "4px");

        // Resource info line
        HorizontalLayout info = new HorizontalLayout();
        info.setSpacing(true);
        info.setPadding(false);
        info.setAlignItems(FlexComponent.Alignment.CENTER);
        info.getStyle().set("flex-wrap", "wrap").set("gap", "4px");

        info.add(createTypeBadge(resourceNeed));
        Span description = createDescriptionLabel(resourceNeed);
        if (description != null) {
            info.add(description);
        }
        info.add(createCoverageBadge(resourceNeed));
        info.add(createOriginBadge(resourceNeed));

        if (resourceNeed.getQuantity() != null && resourceNeed.getQuantity() > 1) {
            info.add(
                    ResourceBadges.small(
                            "x" + resourceNeed.getQuantity(), "var(--rom-text-secondary)"));
        }

        // Per-demand Verkehrstage (day count) + von/nach route, when set.
        List<LocalDate> days = ValidityJsonCodec.fromJson(resourceNeed.getValidity());
        if (!days.isEmpty()) {
            info.add(
                    ResourceBadges.small(
                            days.size() + " " + tr("resource.days"), "var(--rom-text-secondary)"));
        }
        if (resourceNeed.getFromLocation() != null || resourceNeed.getToLocation() != null) {
            info.add(
                    ResourceBadges.small(
                            nz(resourceNeed.getFromLocation())
                                    + " → "
                                    + nz(resourceNeed.getToLocation()),
                            "var(--rom-status-info)"));
        }

        info.add(createHistoryButton(resourceNeed));

        row.add(info);

        // Nested purchase positions
        List<PurchasePosition> purchases = findPurchasesForNeed(resourceNeed);
        if (resourceNeed.getCoverageType() == CoverageType.INTERNAL && purchases.isEmpty()) {
            Span intern = new Span("(" + tr("resource.coverage.INTERNAL").toLowerCase() + ")");
            intern.getStyle()
                    .set("font-size", "10px")
                    .set("color", "var(--rom-text-muted)")
                    .set("padding-left", "12px");
            row.add(intern);
        } else {
            for (PurchasePosition purchase : purchases) {
                row.add(
                        ResourcePurchaseRow.build(
                                purchase,
                                resourceNeed,
                                editable,
                                position.getOperationalTrainNumber(),
                                buildRouteLabel(),
                                purchaseBizMap.getOrDefault(purchase.getId(), List.of()),
                                purchaseOrderService,
                                translator,
                                this::reloadAll));
            }
        }

        // Add Purchase button for EXTERNAL resources (mutators on an unlocked order only)
        if (editable && resourceNeed.getCoverageType() == CoverageType.EXTERNAL) {
            row.add(createAddPurchaseButton(resourceNeed));
        }

        return row;
    }

    private Button createHistoryButton(ResourceNeed resourceNeed) {
        Button historyButton = new Button(VaadinIcon.CLOCK.create());
        historyButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        historyButton
                .getStyle()
                .set("color", "var(--rom-text-muted)")
                .set("min-width", "24px")
                .set("padding", "0");
        historyButton.setTooltipText(tr("audit.button"));
        historyButton.addClickListener(event -> openResourceHistory(resourceNeed));
        return historyButton;
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
                                    opRepo,
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
        orderAll.addClickListener(event -> triggerAllCapacityOrders());
        footer.add(orderAll);

        return footer;
    }

    private void triggerAllCapacityOrders() {
        try {
            purchaseOrderService.triggerAllCapacityOrders(position.getId());
            reloadAll();
            Notification.show("TTT", 2000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (Exception ex) {
            Notification.show(ex.getMessage(), 4000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private Button createAddPurchaseButton(ResourceNeed resourceNeed) {
        Button button = new Button("+ " + tr("purchase.add"));
        button.addThemeVariants(ButtonVariant.LUMO_SMALL);
        button.getStyle()
                .set("font-size", "10px")
                .set("color", "var(--rom-text-muted)")
                .set("background", "transparent")
                .set("padding", "1px 6px 1px 12px");
        button.addClickListener(
                e -> {
                    PurchaseDialog dialog =
                            new PurchaseDialog(
                                    resourceNeed.getId(),
                                    tr("resource.type." + resourceNeed.getResourceType().name()),
                                    tr(
                                            "resource.coverage."
                                                    + resourceNeed.getCoverageType().name()),
                                    resourceNeed.getResourceType() == ResourceType.CAPACITY,
                                    position.getValidity(),
                                    position.getOperationalTrainNumber(),
                                    buildRouteLabel(),
                                    purchaseOrderService,
                                    translator);
                    // reloadAll() (not loadResources()) so the parent re-fetches the position from
                    // the DB — a newly created purchase is added in a separate tx and is NOT in
                    // this
                    // panel's detached purchasePositions snapshot. Mirrors the status-action
                    // buttons.
                    dialog.addSaveListener(ev -> reloadAll());
                    dialog.open();
                });
        return button;
    }

    private List<PurchasePosition> findPurchasesForNeed(ResourceNeed resourceNeed) {
        return purchasesByNeed.getOrDefault(resourceNeed.getId(), List.of());
    }

    private void openResourceHistory(ResourceNeed resourceNeed) {
        if (auditService == null) {
            return;
        }
        var entries = auditService.getResourceNeedHistory(resourceNeed.getId());
        String title =
                tr("audit.title")
                        + " — "
                        + tr("resource.type." + resourceNeed.getResourceType().name());
        var dialog = new AuditHistoryDialog(title, entries, translator);
        dialog.open();
    }

    // --- Badge helpers ---

    private static String nz(String s) {
        return s == null || s.isBlank() ? "…" : s;
    }

    private Span createTypeBadge(ResourceNeed resourceNeed) {
        String label = tr("resource.type." + resourceNeed.getResourceType().name());
        String color = ResourceBadges.resourceColor(resourceNeed.getResourceType());
        return ResourceBadges.small(label, color);
    }

    /**
     * Descriptive label next to the type badge. Returns {@code null} when there is nothing to add
     * beyond the badge, so the row no longer prints e.g. "Kapazität · Kapazität". For a CAPACITY
     * need — the Fahrplantrasse that has to be ordered — the route is shown instead, which is more
     * telling than repeating the type.
     */
    private Span createDescriptionLabel(ResourceNeed resourceNeed) {
        String text = resourceNeed.getDescription() != null ? resourceNeed.getDescription() : "";
        if (resourceNeed.getCatalogItem() != null) {
            text = resourceNeed.getCatalogItem().getName() + (text.isEmpty() ? "" : " " + text);
        }
        if (text.isEmpty() && resourceNeed.getResourceType() == ResourceType.CAPACITY) {
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

    private Span createCoverageBadge(ResourceNeed resourceNeed) {
        String label = tr("resource.coverage." + resourceNeed.getCoverageType().name());
        String color =
                resourceNeed.getCoverageType() == CoverageType.EXTERNAL
                        ? "var(--rom-status-warning)"
                        : "var(--rom-status-active)";
        return ResourceBadges.small(label, color);
    }

    private Span createOriginBadge(ResourceNeed resourceNeed) {
        String label = tr("resource.origin." + resourceNeed.getOrigin().name());
        return ResourceBadges.small(label, "var(--rom-text-muted)");
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
