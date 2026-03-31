package com.ordermgmt.railway.ui.component.order;

import java.util.function.BiFunction;

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
import com.ordermgmt.railway.domain.order.service.OrderService;

/** Displays and manages the positions that belong to an order. */
public class OrderPositionPanel extends Div {

    private final Order order;
    private final OrderService orderService;
    private final OperationalPointRepository opRepo;
    private final PredefinedTagRepository tagRepo;
    private final BiFunction<String, Object[], String> translator;
    private final VerticalLayout rowContainer = new VerticalLayout();

    public OrderPositionPanel(
            Order order,
            OrderService orderService,
            OperationalPointRepository opRepo,
            PredefinedTagRepository tagRepo,
            BiFunction<String, Object[], String> translator) {
        this.order = order;
        this.orderService = orderService;
        this.opRepo = opRepo;
        this.tagRepo = tagRepo;
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

        Button addService = new Button(
                "+ " + t("position.type.LEISTUNG"), VaadinIcon.TOOLS.create());
        addService.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
        addService.getStyle()
                .set("background", "var(--rom-accent)")
                .set("color", "var(--rom-bg-primary)");
        addService.addClickListener(e -> openServiceDialog(null));

        Button addTrain = new Button(
                "+ " + t("position.type.FAHRPLAN"), VaadinIcon.TRAIN.create());
        addTrain.addThemeVariants(ButtonVariant.LUMO_SMALL);
        addTrain.getStyle()
                .set("color", "var(--rom-status-info)")
                .set("border", "1px solid var(--rom-status-info)")
                .set("background", "rgba(68,138,255,0.08)");
        addTrain.addClickListener(e ->
                Notification.show(t("position.fahrplan.coming"), 3000,
                                Notification.Position.BOTTOM_END)
                        .addThemeVariants(NotificationVariant.LUMO_CONTRAST));

        HorizontalLayout buttons = new HorizontalLayout(addService, addTrain);
        buttons.setSpacing(true);

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
            rowContainer.add(
                    new OrderPositionRow(
                            pos, translator,
                            this::openPositionForEdit,
                            this::confirmDeletePosition));
        }
    }

    private void openPositionForEdit(OrderPosition pos) {
        if (pos.getType() == PositionType.LEISTUNG) {
            openServiceDialog(pos);
        } else {
            // Fahrplan editor — coming soon
            Notification.show(t("position.fahrplan.coming"), 3000,
                            Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_CONTRAST);
        }
    }

    private void openServiceDialog(OrderPosition existing) {
        ServicePositionDialog dialog = new ServicePositionDialog(
                order, existing, orderService, opRepo, tagRepo, translator);
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
        dialog.addConfirmListener(e -> {
            orderService.deletePosition(pos.getId());
            refreshPositions();
        });
        dialog.open();
    }

    private String t(String key) {
        return translator.apply(key, new Object[0]);
    }
}
