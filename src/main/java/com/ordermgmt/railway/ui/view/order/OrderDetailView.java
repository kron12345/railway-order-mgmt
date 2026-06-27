package com.ordermgmt.railway.ui.view.order;

import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.spring.annotation.SpringComponent;

import com.ordermgmt.railway.domain.business.service.BusinessService;
import com.ordermgmt.railway.domain.customer.repository.CustomerRepository;
import com.ordermgmt.railway.domain.infrastructure.repository.OperationalPointRepository;
import com.ordermgmt.railway.domain.infrastructure.repository.PredefinedTagRepository;
import com.ordermgmt.railway.domain.order.model.Order;
import com.ordermgmt.railway.domain.order.model.OrderType;
import com.ordermgmt.railway.domain.order.model.ProcessStatus;
import com.ordermgmt.railway.domain.order.repository.ResourceCatalogItemRepository;
import com.ordermgmt.railway.domain.order.service.AuditService;
import com.ordermgmt.railway.domain.order.service.OrderService;
import com.ordermgmt.railway.domain.order.service.PurchaseOrderService;
import com.ordermgmt.railway.domain.order.service.ResourceNeedService;
import com.ordermgmt.railway.domain.pathmanager.service.PathManagerService;
import com.ordermgmt.railway.domain.timetable.service.TimetableArchiveService;
import com.ordermgmt.railway.infrastructure.keycloak.CurrentUserHelper;
import com.ordermgmt.railway.ui.component.AuditHistoryDialog;
import com.ordermgmt.railway.ui.component.StatusBadge;
import com.ordermgmt.railway.ui.component.business.LinkedBusinessesPanel;
import com.ordermgmt.railway.ui.component.order.OrderFormPanel;
import com.ordermgmt.railway.ui.component.order.OrderInternalStatusBar;
import com.ordermgmt.railway.ui.component.order.OrderPositionPanel;
import com.ordermgmt.railway.ui.component.order.OrderStatusStepper;

/**
 * Embeddable detail panel for a single {@link Order}. No longer a Vaadin route — it is instantiated
 * by {@link OrderOverviewView} via {@link org.springframework.beans.factory.ObjectProvider} so each
 * navigation gets a fresh instance with all 11 services auto-wired.
 *
 * <p>Caller drives the lifecycle: {@link #setMode(UUID, boolean)} loads the order and builds the
 * appropriate sub-tree (new-form vs edit + positions panel).
 */
@SpringComponent
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class OrderDetailView extends VerticalLayout {

    private final OrderService orderService;
    private final CustomerRepository customerRepository;
    private final PredefinedTagRepository predefinedTagRepository;
    private final OperationalPointRepository opRepo;
    private final PathManagerService pathManagerService;
    private final ResourceNeedService resourceNeedService;
    private final PurchaseOrderService purchaseOrderService;
    private final ResourceCatalogItemRepository catalogItemRepository;
    private final AuditService auditService;
    private final BusinessService businessService;
    private final TimetableArchiveService timetableArchiveService;
    private Order order;
    private boolean isNew;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public OrderDetailView(
            OrderService orderService,
            CustomerRepository customerRepository,
            PredefinedTagRepository predefinedTagRepository,
            OperationalPointRepository opRepo,
            PathManagerService pathManagerService,
            ResourceNeedService resourceNeedService,
            PurchaseOrderService purchaseOrderService,
            ResourceCatalogItemRepository catalogItemRepository,
            AuditService auditService,
            BusinessService businessService,
            TimetableArchiveService timetableArchiveService) {
        this.orderService = orderService;
        this.timetableArchiveService = timetableArchiveService;
        this.customerRepository = customerRepository;
        this.predefinedTagRepository = predefinedTagRepository;
        this.opRepo = opRepo;
        this.pathManagerService = pathManagerService;
        this.resourceNeedService = resourceNeedService;
        this.purchaseOrderService = purchaseOrderService;
        this.catalogItemRepository = catalogItemRepository;
        this.businessService = businessService;
        this.auditService = auditService;
        setPadding(false);
        setSpacing(false);
        setWidthFull();
        setSizeFull();
        getStyle()
                .set("background", "var(--rom-bg-primary)")
                .set("padding", "var(--lumo-space-xs) var(--lumo-space-m)")
                .set("overflow-x", "hidden")
                .set("box-sizing", "border-box");
    }

    /**
     * Configure this detail panel for the given order id. Pass {@code null} to render the new-order
     * form. Returns {@code true} on success, {@code false} when the id was given but no order
     * exists (caller should redirect away).
     */
    public boolean setMode(UUID orderId, boolean newMode) {
        if (newMode || orderId == null) {
            isNew = true;
            order = new Order();
            order.setProcessStatus(ProcessStatus.AUFTRAG);
            buildNewOrderView();
            return true;
        }
        isNew = false;
        order = orderService.findById(orderId).orElse(null);
        if (order == null) return false;
        buildDetailView();
        return true;
    }

    /** New order: show form directly (needs data first). */
    private void buildNewOrderView() {
        removeAll();
        // Defence-in-depth: a user without a mutation role could reach /orders/new by URL even
        // though the toolbar button is hidden; bounce them back to the list (service also guards).
        if (!canMutate()) {
            UI.getCurrent().navigate("orders");
            return;
        }
        add(createBackRow(getTranslation("order.new")));
        OrderFormPanel form =
                new OrderFormPanel(
                        order, customerRepository, predefinedTagRepository, this::getTranslation);
        add(form);

        Button save = new Button(getTranslation("common.save"), VaadinIcon.CHECK.create());
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.getStyle()
                .set("background", "var(--rom-accent)")
                .set("color", "var(--rom-bg-primary)")
                .set("font-weight", "600")
                .set("align-self", "flex-end")
                .set("margin-top", "var(--lumo-space-s)");
        save.addClickListener(e -> saveNewOrder(form));
        add(save);
    }

    private void saveNewOrder(OrderFormPanel form) {
        if (!form.validate()) {
            return;
        }
        form.writeTo(order);
        order = orderService.save(order);
        Notification.show("✓", 2000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        UI.getCurrent().navigate("orders/" + order.getId());
    }

    /** Existing order: compact header + positions. */
    private void buildDetailView() {
        removeAll();
        add(createCompactHeader());
        add(
                new OrderStatusStepper(
                        order.getProcessStatus(),
                        canMutate(),
                        this::getTranslation,
                        this::changeStatus));
        add(
                new OrderInternalStatusBar(
                        order,
                        orderService,
                        canMutate(),
                        this::getTranslation,
                        () -> setMode(order.getId(), false)));

        var positionPanel =
                new OrderPositionPanel(
                        order,
                        orderService,
                        timetableArchiveService,
                        opRepo,
                        predefinedTagRepository,
                        pathManagerService,
                        resourceNeedService,
                        purchaseOrderService,
                        catalogItemRepository,
                        auditService,
                        businessService,
                        this::getTranslation);

        var tabPositions = new Tab(getTranslation("order.tab.positions"));
        var tabBusinesses = new Tab(getTranslation("order.tab.linkedBusinesses"));
        var tabs = new Tabs(tabPositions, tabBusinesses);
        tabs.setWidthFull();

        var tabContent = new Div();
        tabContent.setSizeFull();
        tabContent.add(positionPanel);

        tabs.addSelectedChangeListener(
                e -> {
                    tabContent.removeAll();
                    if (e.getSelectedTab() == tabPositions) {
                        tabContent.add(positionPanel);
                    } else if (businessService != null) {
                        tabContent.add(
                                new LinkedBusinessesPanel(
                                        businessService.findByLinkedOrder(order.getId()),
                                        this::getTranslation));
                    }
                });

        add(tabs);
        add(tabContent);
    }

    /**
     * Advances (or rewinds) the order to the chosen process phase. Persistence is role-guarded by
     * {@link OrderService#save}; on a denied call we surface a friendly notification instead of an
     * error page. The change is audited automatically via Hibernate Envers.
     */
    private void changeStatus(ProcessStatus newStatus) {
        if (newStatus == null || newStatus == order.getProcessStatus()) {
            return;
        }
        ProcessStatus previous = order.getProcessStatus();
        try {
            order.setProcessStatus(newStatus);
            order = orderService.save(order);
            Notification.show(
                            getTranslation(
                                    "order.phase.changed",
                                    getTranslation("process." + newStatus.name())),
                            2000,
                            Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            buildDetailView();
        } catch (RuntimeException ex) {
            // Keep the in-memory entity consistent with what is actually persisted so a later
            // edit-save cannot silently carry over the rejected status change.
            order.setProcessStatus(previous);
            Notification.show(
                            getTranslation("order.phase.denied"),
                            3000,
                            Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private boolean canMutate() {
        return CurrentUserHelper.hasAnyRole("ADMIN", "DISPATCHER");
    }

    private HorizontalLayout createBackRow(String titleText) {
        Button back = new Button(VaadinIcon.ARROW_LEFT.create());
        back.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        back.getStyle().set("color", "var(--rom-text-secondary)");
        back.addClickListener(e -> UI.getCurrent().navigate("orders"));

        Span title = new Span(titleText);
        title.getStyle()
                .set("font-size", "var(--lumo-font-size-xl)")
                .set("font-weight", "600")
                .set("color", "var(--rom-text-primary)");

        HorizontalLayout row = new HorizontalLayout(back, title);
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setWidthFull();
        row.getStyle().set("margin-bottom", "var(--lumo-space-s)");
        return row;
    }

    /**
     * Compact Bloomberg-style header consistent with {@code BusinessReadView}: large title (number
     * + name), themed status pill with icon, customer + dates as a thin meta row, action buttons on
     * the right.
     */
    private Div createCompactHeader() {
        Div header = new Div();
        header.addClassName("biz-read__header");
        header.addClassName("order-detail__header");
        header.setWidthFull();

        // Title block: order number + name on the same line
        Div titleBlock = new Div();
        titleBlock.addClassName("order-detail__title-block");

        Span numberSpan = new Span(order.getOrderNumber() == null ? "—" : order.getOrderNumber());
        numberSpan.addClassName("order-detail__number");
        Span nameSpan = new Span(order.getName() == null ? "" : order.getName());
        nameSpan.addClassName("order-detail__name");
        titleBlock.add(numberSpan, nameSpan);

        // Meta row: customer and validity
        Div metaRow = new Div();
        metaRow.addClassName("order-detail__meta");
        if (order.getCustomer() != null && order.getCustomer().getName() != null) {
            Span customer = new Span(order.getCustomer().getName());
            metaRow.add(customer);
            metaRow.add(new Span(" · "));
        }
        Span dates = new Span(formatDates());
        metaRow.add(dates);

        OrderType orderType = OrderType.of(order);
        if (orderType != null) {
            metaRow.add(new Span(" · "));
            metaRow.add(
                    new StatusBadge(
                            getTranslation("order.type." + orderType.name()),
                            orderType == OrderType.JAHRESBESTELLUNG
                                    ? StatusBadge.StatusType.INFO
                                    : StatusBadge.StatusType.WARNING));
        }

        Div left = new Div(titleBlock, metaRow);
        left.addClassName("order-detail__left");

        Component statusPill = buildProcessStatusPill(order.getProcessStatus());

        Span spacer = new Span();
        spacer.getStyle().set("flex", "1");

        header.add(createCompactBackButton(), left, statusPill, spacer, createHistoryButton());
        // SOB §5.7: the content lock is against the Auftraggeber, who is the non-mutator here and
        // is
        // already fully role-gated. Die Planung (ADMIN/DISPATCHER) works on the order during "in
        // Bearbeitung", so mutators keep edit/delete regardless of status.
        if (canMutate()) {
            header.add(createEditButton());
            header.add(createDeleteButton());
        }
        header.getStyle().set("display", "flex").set("align-items", "center").set("gap", "12px");
        return header;
    }

    /** Status pill with icon, matching the master-card and BusinessReadView style. */
    private Component buildProcessStatusPill(ProcessStatus status) {
        var pill = new HorizontalLayout();
        pill.addClassName("order-status-pill");
        if (status != null) pill.addClassName("order-status-pill--" + status.name().toLowerCase());
        pill.setPadding(false);
        pill.setSpacing(false);
        VaadinIcon iconSpec =
                status == null
                        ? VaadinIcon.QUESTION_CIRCLE_O
                        : switch (status) {
                            case AUFTRAG -> VaadinIcon.FILE_TEXT_O;
                            case PLANUNG -> VaadinIcon.CALENDAR;
                            case PRODUKT_LEISTUNG -> VaadinIcon.PACKAGE;
                            case PRODUKTION -> VaadinIcon.COG;
                            case ABRECHNUNG_NACHBEREITUNG -> VaadinIcon.CHECK_CIRCLE_O;
                        };
        var icon = iconSpec.create();
        icon.addClassName("order-status-pill__icon");
        icon.getElement().setAttribute("aria-hidden", "true");
        pill.add(icon);
        Span label = new Span(status == null ? "—" : getTranslation("process." + status.name()));
        label.addClassName("order-status-pill__label");
        pill.add(label);
        return pill;
    }

    private Button createCompactBackButton() {
        Button backButton = new Button(VaadinIcon.ARROW_LEFT.create());
        backButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        backButton.getStyle().set("color", "var(--rom-text-muted)");
        backButton.addClickListener(e -> UI.getCurrent().navigate("orders"));
        return backButton;
    }

    private Button createHistoryButton() {
        Button historyBtn = new Button(getTranslation("audit.button"), VaadinIcon.CLOCK.create());
        historyBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
        historyBtn
                .getStyle()
                .set("color", "var(--rom-text-secondary)")
                .set("border", "1px solid var(--rom-border)")
                .set("background", "rgba(148,163,184,0.06)");
        historyBtn.addClickListener(e -> openAuditHistory());
        return historyBtn;
    }

    private void openAuditHistory() {
        var entries = auditService.getOrderHistory(order.getId());
        var dialog =
                new AuditHistoryDialog(
                        getTranslation("audit.title") + " — " + order.getOrderNumber(),
                        entries,
                        this::getTranslation);
        dialog.open();
    }

    private Button createEditButton() {
        Button editButton = new Button(getTranslation("common.edit"), VaadinIcon.EDIT.create());
        editButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
        editButton
                .getStyle()
                .set("color", "var(--rom-accent)")
                .set("border", "1px solid rgba(45,212,191,0.3)")
                .set("background", "rgba(45,212,191,0.08)");
        editButton.addClickListener(e -> openEditDialog());
        return editButton;
    }

    private Button createDeleteButton() {
        Button deleteButton = new Button(VaadinIcon.TRASH.create());
        deleteButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        deleteButton.getStyle().set("color", "var(--rom-status-danger)");
        deleteButton.addClickListener(e -> confirmDelete());
        return deleteButton;
    }

    private void openEditDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(getTranslation("order.edit") + " — " + order.getOrderNumber());
        dialog.setWidth("800px");

        OrderFormPanel form =
                new OrderFormPanel(
                        order, customerRepository, predefinedTagRepository, this::getTranslation);
        dialog.add(form);

        Button cancel = new Button(getTranslation("common.cancel"));
        cancel.addClickListener(e -> dialog.close());

        Button save = new Button(getTranslation("common.save"));
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.getStyle()
                .set("background", "var(--rom-accent)")
                .set("color", "var(--rom-bg-primary)");
        save.addClickListener(e -> saveEditedOrder(form, dialog));

        dialog.getFooter().add(cancel, save);
        dialog.open();
    }

    private void saveEditedOrder(OrderFormPanel form, Dialog dialog) {
        if (!form.validate()) {
            return;
        }
        form.writeTo(order);
        order = orderService.save(order);
        Notification.show("✓", 2000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        dialog.close();
        buildDetailView();
    }

    private void confirmDelete() {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader(getTranslation("common.delete") + "?");
        dialog.setCancelable(true);
        dialog.setCancelText(getTranslation("common.cancel"));
        dialog.setConfirmText(getTranslation("common.delete"));
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(
                e -> {
                    orderService.delete(order.getId());
                    UI.getCurrent().navigate("orders");
                });
        dialog.open();
    }

    private String formatDates() {
        String from = order.getValidFrom() != null ? order.getValidFrom().format(DATE_FMT) : "—";
        String to = order.getValidTo() != null ? order.getValidTo().format(DATE_FMT) : "—";
        return from + " → " + to;
    }
}
