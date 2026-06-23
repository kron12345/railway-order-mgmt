package com.ordermgmt.railway.ui.component.order;

import java.util.function.BiFunction;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import com.ordermgmt.railway.domain.infrastructure.repository.OperationalPointRepository;
import com.ordermgmt.railway.domain.infrastructure.repository.PredefinedTagRepository;
import com.ordermgmt.railway.domain.order.model.Order;
import com.ordermgmt.railway.domain.order.model.OrderPosition;
import com.ordermgmt.railway.domain.order.model.PositionType;
import com.ordermgmt.railway.domain.order.repository.PurchasePositionRepository;
import com.ordermgmt.railway.domain.order.repository.ResourceCatalogItemRepository;
import com.ordermgmt.railway.domain.order.service.AuditService;
import com.ordermgmt.railway.domain.order.service.OrderService;
import com.ordermgmt.railway.domain.order.service.PurchaseOrderService;
import com.ordermgmt.railway.domain.order.service.ResourceNeedService;
import com.ordermgmt.railway.domain.pathmanager.model.PmReferenceTrain;
import com.ordermgmt.railway.domain.pathmanager.service.PathManagerService;
import com.ordermgmt.railway.infrastructure.keycloak.CurrentUserHelper;

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

        add(createHeader());

        rowContainer.setPadding(false);
        rowContainer.setSpacing(false);
        rowContainer.setWidthFull();
        add(rowContainer);

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
            buttons.add(addService, addTrain);
        }

        HorizontalLayout header = new HorizontalLayout(title, buttons);
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.getStyle().set("margin-bottom", "var(--lumo-space-m)");
        return header;
    }

    private void refreshPositions() {
        rowContainer.removeAll();
        var positions = orderService.findPositionsByOrderId(order.getId());

        if (positions.isEmpty()) {
            Span empty = new Span(t("order.positions.empty"));
            empty.getStyle()
                    .set("color", "var(--rom-text-muted)")
                    .set("font-size", "12px")
                    .set("padding", "var(--lumo-space-m) 0");
            rowContainer.add(empty);
            return;
        }

        for (OrderPosition pos : positions) {
            PmReferenceTrain pmTrain = resolveTrain(pos);
            rowContainer.add(
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
                            editable));

            // Linked businesses for this position (clickable chips → business detail).
            var linkedBusinesses = businessService.findByLinkedOrderPosition(pos.getId());
            if (!linkedBusinesses.isEmpty()) {
                var chips =
                        new com.ordermgmt.railway.ui.component.business.BusinessChips(
                                linkedBusinesses, this::t);
                chips.getStyle().set("margin", "0 12px 6px 12px");
                rowContainer.add(chips);
            }

            // Resource panel (collapsible, shown below each position row)
            long resCount = pos.getResourceNeeds() != null ? pos.getResourceNeeds().size() : 0;
            if (resCount > 0) {
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
                rowContainer.add(resourcePanel);
            }
        }
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
            return pathManagerService.findById(pos.getPmReferenceTrainId());
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

    private String t(String key) {
        return translator.apply(key, new Object[0]);
    }
}
