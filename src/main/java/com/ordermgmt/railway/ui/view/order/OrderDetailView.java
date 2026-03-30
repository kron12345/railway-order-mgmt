package com.ordermgmt.railway.ui.view.order;

import java.time.format.DateTimeFormatter;
import java.util.UUID;

import jakarta.annotation.security.PermitAll;

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
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import com.ordermgmt.railway.domain.customer.repository.CustomerRepository;
import com.ordermgmt.railway.domain.order.model.Order;
import com.ordermgmt.railway.domain.order.model.ProcessStatus;
import com.ordermgmt.railway.domain.order.service.OrderService;
import com.ordermgmt.railway.ui.component.StatusBadge;
import com.ordermgmt.railway.ui.component.order.OrderFormPanel;
import com.ordermgmt.railway.ui.component.order.OrderPositionPanel;
import com.ordermgmt.railway.ui.layout.MainLayout;

/** Handles creation, editing, and deletion for a single order. */
@Route(value = "orders/:orderId", layout = MainLayout.class)
@PageTitle("Order Detail")
@PermitAll
public class OrderDetailView extends VerticalLayout implements BeforeEnterObserver {

    private final OrderService orderService;
    private final CustomerRepository customerRepository;
    private Order order;
    private boolean isNew;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public OrderDetailView(OrderService orderService, CustomerRepository customerRepository) {
        this.orderService = orderService;
        this.customerRepository = customerRepository;
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

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String param = event.getRouteParameters().get("orderId").orElse("new");

        if ("new".equals(param)) {
            isNew = true;
            order = new Order();
            order.setProcessStatus(ProcessStatus.AUFTRAG);
            buildNewOrderView();
        } else {
            isNew = false;
            try {
                UUID id = UUID.fromString(param);
                order = orderService.findById(id).orElse(null);
            } catch (IllegalArgumentException e) {
                order = null;
            }
            if (order == null) {
                event.forwardTo("orders");
                return;
            }
            buildDetailView();
        }
    }

    /** New order: show form directly (needs data first). */
    private void buildNewOrderView() {
        removeAll();
        add(createBackRow(getTranslation("order.new")));
        OrderFormPanel form = new OrderFormPanel(order, customerRepository, this::getTranslation);
        add(form);

        Button save = new Button(getTranslation("common.save"), VaadinIcon.CHECK.create());
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.getStyle()
                .set("background", "var(--rom-accent)")
                .set("color", "var(--rom-bg-primary)")
                .set("font-weight", "600")
                .set("align-self", "flex-end")
                .set("margin-top", "var(--lumo-space-s)");
        save.addClickListener(
                e -> {
                    if (!form.validate()) return;
                    form.writeTo(order);
                    order = orderService.save(order);
                    Notification.show("✓", 2000, Notification.Position.BOTTOM_END)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    UI.getCurrent().navigate("orders/" + order.getId());
                });
        add(save);
    }

    /** Existing order: compact header + positions. */
    private void buildDetailView() {
        removeAll();
        add(createCompactHeader());
        var positionPanel = new OrderPositionPanel(order, orderService, this::getTranslation);
        add(positionPanel);
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

    /** Compact summary header: order info in one line + action buttons. */
    private Div createCompactHeader() {
        Div header = createHeaderContainer();
        Span spacer = new Span();
        spacer.getStyle().set("flex", "1");
        header.add(
                createCompactBackButton(),
                createOrderNumberLabel(),
                createOrderNameLabel(),
                createCustomerLabel(),
                createProcessBadge(order.getProcessStatus()),
                createDatesLabel(),
                spacer,
                createEditButton(),
                createDeleteButton());
        return header;
    }

    private Div createHeaderContainer() {
        Div header = new Div();
        header.setWidthFull();
        header.getStyle()
                .set("background", "var(--rom-bg-card)")
                .set("border", "1px solid var(--rom-border)")
                .set("border-radius", "6px")
                .set("padding", "10px 16px")
                .set("margin-bottom", "var(--lumo-space-s)")
                .set("box-sizing", "border-box")
                .set("display", "flex")
                .set("align-items", "center")
                .set("gap", "16px")
                .set("flex-wrap", "wrap");
        return header;
    }

    private Button createCompactBackButton() {
        Button backButton = new Button(VaadinIcon.ARROW_LEFT.create());
        backButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        backButton.getStyle().set("color", "var(--rom-text-muted)");
        backButton.addClickListener(e -> UI.getCurrent().navigate("orders"));
        return backButton;
    }

    private Span createOrderNumberLabel() {
        Span orderNumber = new Span(order.getOrderNumber());
        orderNumber
                .getStyle()
                .set("font-family", "'JetBrains Mono', monospace")
                .set("font-size", "13px")
                .set("font-weight", "700")
                .set("color", "var(--rom-accent)");
        return orderNumber;
    }

    private Span createOrderNameLabel() {
        Span orderName = new Span(order.getName());
        orderName
                .getStyle()
                .set("font-weight", "600")
                .set("font-size", "14px")
                .set("color", "var(--rom-text-primary)");
        return orderName;
    }

    private Span createCustomerLabel() {
        String customerName = order.getCustomer() != null ? order.getCustomer().getName() : "—";
        Span customer = new Span(customerName);
        customer.getStyle().set("color", "var(--rom-text-muted)").set("font-size", "12px");
        return customer;
    }

    private Span createDatesLabel() {
        Span dates = new Span(formatDates());
        dates.getStyle()
                .set("font-family", "'JetBrains Mono', monospace")
                .set("font-size", "11px")
                .set("color", "var(--rom-text-muted)");
        return dates;
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

        OrderFormPanel form = new OrderFormPanel(order, customerRepository, this::getTranslation);
        dialog.add(form);

        Button cancel = new Button(getTranslation("common.cancel"));
        cancel.addClickListener(e -> dialog.close());

        Button save = new Button(getTranslation("common.save"));
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.getStyle()
                .set("background", "var(--rom-accent)")
                .set("color", "var(--rom-bg-primary)");
        save.addClickListener(
                e -> {
                    if (!form.validate()) return;
                    form.writeTo(order);
                    order = orderService.save(order);
                    Notification.show("✓", 2000, Notification.Position.BOTTOM_END)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    dialog.close();
                    buildDetailView();
                });

        dialog.getFooter().add(cancel, save);
        dialog.open();
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

    private StatusBadge createProcessBadge(ProcessStatus status) {
        if (status == null) return new StatusBadge("—", StatusBadge.StatusType.NEUTRAL);
        String label = getTranslation("process." + status.name());
        return switch (status) {
            case AUFTRAG -> new StatusBadge(label, StatusBadge.StatusType.INFO);
            case PLANUNG -> new StatusBadge(label, StatusBadge.StatusType.WARNING);
            case PRODUKT_LEISTUNG -> new StatusBadge(label, StatusBadge.StatusType.INFO);
            case PRODUKTION -> new StatusBadge(label, StatusBadge.StatusType.SUCCESS);
            case ABRECHNUNG_NACHBEREITUNG -> new StatusBadge(label, StatusBadge.StatusType.NEUTRAL);
        };
    }

    private String formatDates() {
        String from = order.getValidFrom() != null ? order.getValidFrom().format(DATE_FMT) : "—";
        String to = order.getValidTo() != null ? order.getValidTo().format(DATE_FMT) : "—";
        return from + " → " + to;
    }
}
