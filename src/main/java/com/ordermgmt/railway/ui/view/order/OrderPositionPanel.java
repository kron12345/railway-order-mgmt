package com.ordermgmt.railway.ui.view.order;

import java.time.format.DateTimeFormatter;
import java.util.function.BiFunction;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import com.ordermgmt.railway.domain.order.model.Order;
import com.ordermgmt.railway.domain.order.model.OrderPosition;
import com.ordermgmt.railway.domain.order.service.OrderService;
import com.ordermgmt.railway.ui.component.StatusBadge;

public class OrderPositionPanel extends Div {

    private final Order order;
    private final OrderService orderService;
    private final BiFunction<String, Object[], String> translator;
    private final Grid<OrderPosition> grid = new Grid<>(OrderPosition.class, false);

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public OrderPositionPanel(
            Order order,
            OrderService orderService,
            BiFunction<String, Object[], String> translator) {
        this.order = order;
        this.orderService = orderService;
        this.translator = translator;

        getStyle()
                .set("background", "var(--rom-bg-card)")
                .set("border", "1px solid var(--rom-border)")
                .set("border-radius", "6px")
                .set("padding", "var(--lumo-space-l)")
                .set("flex", "1");

        add(createHeader());
        add(createGrid());
        refreshGrid();
    }

    private HorizontalLayout createHeader() {
        H3 title = new H3(t("position.title"));
        title.getStyle()
                .set("color", "var(--rom-text-primary)")
                .set("margin", "0")
                .set("font-size", "var(--lumo-font-size-l)");

        Button addBtn = new Button(t("position.new"), VaadinIcon.PLUS.create());
        addBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
        addBtn.getStyle()
                .set("background", "var(--rom-accent)")
                .set("color", "var(--rom-bg-primary)");
        addBtn.addClickListener(e -> openPositionDialog(null));

        HorizontalLayout header = new HorizontalLayout(title, addBtn);
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.getStyle().set("margin-bottom", "var(--lumo-space-m)");
        return header;
    }

    private Div createGrid() {
        grid.addColumn(OrderPosition::getName)
                .setHeader(t("position.name"))
                .setSortable(true)
                .setFlexGrow(2);

        grid.addComponentColumn(pos -> {
                    String label = t("position.type." + pos.getType().name());
                    StatusBadge.StatusType badgeType =
                            switch (pos.getType()) {
                                case FAHRPLAN -> StatusBadge.StatusType.INFO;
                                case LEISTUNG -> StatusBadge.StatusType.WARNING;
                            };
                    return new StatusBadge(label, badgeType);
                })
                .setHeader(t("position.type"))
                .setWidth("130px")
                .setFlexGrow(0);

        grid.addColumn(pos -> formatLocation(pos.getFromLocation(), pos.getToLocation()))
                .setHeader(t("position.from") + " → " + t("position.to"))
                .setFlexGrow(2);

        grid.addColumn(pos -> pos.getStart() != null ? pos.getStart().format(DT_FMT) : "—")
                .setHeader(t("position.start"))
                .setWidth("140px")
                .setFlexGrow(0);

        grid.addComponentColumn(pos -> {
                    if (pos.getInternalStatus() == null) {
                        return new StatusBadge("—", StatusBadge.StatusType.NEUTRAL);
                    }
                    String label = t("position.status." + pos.getInternalStatus().name());
                    StatusBadge.StatusType badgeType =
                            switch (pos.getInternalStatus()) {
                                case IN_BEARBEITUNG -> StatusBadge.StatusType.INFO;
                                case FREIGEGEBEN -> StatusBadge.StatusType.SUCCESS;
                                case UEBERARBEITEN -> StatusBadge.StatusType.WARNING;
                                case UEBERMITTELT -> StatusBadge.StatusType.INFO;
                                case BEANTRAGT -> StatusBadge.StatusType.WARNING;
                                case ABGESCHLOSSEN -> StatusBadge.StatusType.SUCCESS;
                                case ANNULLIERT -> StatusBadge.StatusType.DANGER;
                            };
                    return new StatusBadge(label, badgeType);
                })
                .setHeader(t("position.status"))
                .setWidth("150px")
                .setFlexGrow(0);

        grid.addComponentColumn(pos -> createRowActions(pos))
                .setHeader("")
                .setWidth("100px")
                .setFlexGrow(0);

        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_COMPACT);

        Div wrapper = new Div(grid);
        wrapper.setWidthFull();
        return wrapper;
    }

    private HorizontalLayout createRowActions(OrderPosition pos) {
        Button edit = new Button(VaadinIcon.EDIT.create());
        edit.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        edit.getStyle().set("color", "var(--rom-text-secondary)");
        edit.addClickListener(e -> openPositionDialog(pos));

        Button del = new Button(VaadinIcon.TRASH.create());
        del.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        del.getStyle().set("color", "var(--rom-status-danger)");
        del.addClickListener(e -> confirmDeletePosition(pos));

        return new HorizontalLayout(edit, del);
    }

    private void openPositionDialog(OrderPosition existing) {
        OrderPositionDialog dialog =
                new OrderPositionDialog(order, existing, orderService, translator);
        dialog.addSaveListener(e -> refreshGrid());
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
            refreshGrid();
        });
        dialog.open();
    }

    private void refreshGrid() {
        grid.setItems(orderService.findPositionsByOrderId(order.getId()));
    }

    private String formatLocation(String from, String to) {
        if (from == null && to == null) return "—";
        return (from != null ? from : "?") + " → " + (to != null ? to : "?");
    }

    private String t(String key) {
        return translator.apply(key, new Object[0]);
    }
}
