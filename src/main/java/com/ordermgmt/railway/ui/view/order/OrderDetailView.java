package com.ordermgmt.railway.ui.view.order;

import java.util.UUID;

import jakarta.annotation.security.PermitAll;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.H2;
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
import com.ordermgmt.railway.ui.layout.MainLayout;

@Route(value = "orders/:orderId", layout = MainLayout.class)
@PageTitle("Order Detail")
@PermitAll
public class OrderDetailView extends VerticalLayout implements BeforeEnterObserver {

    private final OrderService orderService;
    private final CustomerRepository customerRepository;
    private Order order;
    private boolean isNew;

    private OrderFormPanel formPanel;
    private OrderPositionPanel positionPanel;

    public OrderDetailView(OrderService orderService, CustomerRepository customerRepository) {
        this.orderService = orderService;
        this.customerRepository = customerRepository;
        setPadding(true);
        setSpacing(false);
        setSizeFull();
        getStyle().set("background", "var(--rom-bg-primary)");
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String param = event.getRouteParameters().get("orderId").orElse("new");

        if ("new".equals(param)) {
            isNew = true;
            order = new Order();
            order.setProcessStatus(ProcessStatus.AUFTRAG);
        } else {
            isNew = false;
            try {
                UUID id = UUID.fromString(param);
                order = orderService.findById(id).orElse(null);
            } catch (IllegalArgumentException e) {
                order = null;
            }
        }

        if (order == null && !isNew) {
            event.forwardTo("orders");
            return;
        }

        buildView();
    }

    private void buildView() {
        removeAll();
        add(createHeader());
        formPanel = new OrderFormPanel(order, customerRepository, this::getTranslation);
        add(formPanel);

        if (!isNew) {
            positionPanel = new OrderPositionPanel(order, orderService, this::getTranslation);
            add(positionPanel);
        }
    }

    private HorizontalLayout createHeader() {
        Button back = new Button(VaadinIcon.ARROW_LEFT.create());
        back.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        back.getStyle().set("color", "var(--rom-text-secondary)");
        back.addClickListener(e -> UI.getCurrent().navigate("orders"));

        String titleText =
                isNew
                        ? getTranslation("order.new")
                        : getTranslation("order.edit") + " — " + order.getOrderNumber();
        H2 title = new H2(titleText);
        title.getStyle()
                .set("color", "var(--rom-text-primary)")
                .set("font-weight", "600")
                .set("margin", "0");

        Span spacer = new Span();

        Button save = new Button(getTranslation("common.save"), VaadinIcon.CHECK.create());
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.getStyle()
                .set("background", "var(--rom-accent)")
                .set("color", "var(--rom-bg-primary)")
                .set("font-weight", "600");
        save.addClickListener(e -> saveOrder());

        HorizontalLayout actions = new HorizontalLayout(save);

        if (!isNew) {
            Button delete = new Button(getTranslation("common.delete"), VaadinIcon.TRASH.create());
            delete.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
            delete.addClickListener(e -> confirmDelete());
            actions.add(delete);
        }

        HorizontalLayout header = new HorizontalLayout(back, title, spacer, actions);
        header.setWidthFull();
        header.expand(spacer);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.getStyle().set("margin-bottom", "var(--lumo-space-l)");
        return header;
    }

    private void saveOrder() {
        if (!formPanel.validate()) {
            return;
        }
        formPanel.writeTo(order);
        order = orderService.save(order);

        Notification.show(getTranslation("common.save") + " ✓", 3000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

        if (isNew) {
            UI.getCurrent().navigate("orders/" + order.getId());
        } else {
            buildView();
        }
    }

    private void confirmDelete() {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader(getTranslation("common.delete") + "?");
        dialog.setCancelable(true);
        dialog.setCancelText(getTranslation("common.cancel"));
        dialog.setConfirmText(getTranslation("common.delete"));
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(e -> {
            orderService.delete(order.getId());
            UI.getCurrent().navigate("orders");
        });
        dialog.open();
    }
}
