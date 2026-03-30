package com.ordermgmt.railway.ui.component.order;

import java.util.function.BiFunction;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.shared.Registration;

import com.ordermgmt.railway.domain.order.model.Order;
import com.ordermgmt.railway.domain.order.model.OrderPosition;
import com.ordermgmt.railway.domain.order.model.PositionStatus;
import com.ordermgmt.railway.domain.order.model.PositionType;
import com.ordermgmt.railway.domain.order.service.OrderService;

/** Dialog for creating and editing a single order position. */
public class OrderPositionDialog extends Dialog {

    private final OrderService orderService;
    private final BiFunction<String, Object[], String> translator;
    private final Order order;
    private final OrderPosition position;
    private final boolean isNew;

    private final TextField name = new TextField();
    private final ComboBox<PositionType> type = new ComboBox<>();
    private final TextField serviceType = new TextField();
    private final TextField fromLocation = new TextField();
    private final TextField toLocation = new TextField();
    private final DateTimePicker start = new DateTimePicker();
    private final DateTimePicker end = new DateTimePicker();
    private final ComboBox<PositionStatus> status = new ComboBox<>();
    private final TextField tags = new TextField();

    public OrderPositionDialog(
            Order order,
            OrderPosition existing,
            OrderService orderService,
            BiFunction<String, Object[], String> translator) {
        this.order = order;
        this.orderService = orderService;
        this.translator = translator;
        this.isNew = existing == null;
        this.position = isNew ? new OrderPosition() : existing;

        setHeaderTitle(isNew ? t("position.new") : t("position.edit"));
        setWidth("700px");

        buildForm();
        buildFooter();
    }

    private void buildForm() {
        configureFields();

        FormLayout form = createFormLayout();
        readFrom();
        add(form);
    }

    private void configureFields() {
        name.setLabel(t("position.name"));
        name.setRequired(true);
        name.setWidthFull();

        type.setLabel(t("position.type"));
        type.setItems(PositionType.values());
        type.setItemLabelGenerator(pt -> t("position.type." + pt.name()));
        type.setRequired(true);
        type.setWidthFull();

        serviceType.setLabel(t("position.serviceType"));
        serviceType.setWidthFull();

        fromLocation.setLabel(t("position.from"));
        fromLocation.setWidthFull();

        toLocation.setLabel(t("position.to"));
        toLocation.setWidthFull();

        start.setLabel(t("position.start"));
        start.setWidthFull();

        end.setLabel(t("position.end"));
        end.setWidthFull();

        status.setLabel(t("position.status"));
        status.setItems(PositionStatus.values());
        status.setItemLabelGenerator(ps -> t("position.status." + ps.name()));
        status.setWidthFull();

        tags.setLabel(t("order.tags"));
        tags.setWidthFull();
    }

    private FormLayout createFormLayout() {
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("500px", 2));

        form.add(name, type);
        form.add(fromLocation, toLocation);
        form.add(start, end);
        form.add(serviceType, status);
        form.setColspan(tags, 2);
        form.add(tags);
        return form;
    }

    private void buildFooter() {
        Button cancel = new Button(t("common.cancel"));
        cancel.addClickListener(e -> close());

        Button save = new Button(t("common.save"));
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.getStyle()
                .set("background", "var(--rom-accent)")
                .set("color", "var(--rom-bg-primary)");
        save.addClickListener(e -> savePosition());

        getFooter().add(cancel, save);
    }

    private void readFrom() {
        name.setValue(nvl(position.getName()));
        type.setValue(position.getType());
        serviceType.setValue(nvl(position.getServiceType()));
        fromLocation.setValue(nvl(position.getFromLocation()));
        toLocation.setValue(nvl(position.getToLocation()));
        start.setValue(position.getStart());
        end.setValue(position.getEnd());
        status.setValue(position.getInternalStatus());
        tags.setValue(nvl(position.getTags()));
    }

    private void savePosition() {
        if (name.getValue().isBlank()) {
            name.setInvalid(true);
            return;
        }
        if (type.getValue() == null) {
            type.setInvalid(true);
            return;
        }

        position.setName(name.getValue().trim());
        position.setType(type.getValue());
        position.setServiceType(blankToNull(serviceType.getValue()));
        position.setFromLocation(blankToNull(fromLocation.getValue()));
        position.setToLocation(blankToNull(toLocation.getValue()));
        position.setStart(start.getValue());
        position.setEnd(end.getValue());
        position.setInternalStatus(
                status.getValue() != null ? status.getValue() : PositionStatus.IN_BEARBEITUNG);
        position.setTags(blankToNull(tags.getValue()));

        if (isNew) {
            position.setOrder(order);
        }

        orderService.savePosition(position);
        Notification.show(t("common.save") + " ✓", 2000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

        fireEvent(new SaveEvent(this));
        close();
    }

    public Registration addSaveListener(ComponentEventListener<SaveEvent> listener) {
        return addListener(SaveEvent.class, listener);
    }

    /** Fired after a position has been saved successfully. */
    public static class SaveEvent extends ComponentEvent<OrderPositionDialog> {
        public SaveEvent(OrderPositionDialog source) {
            super(source, false);
        }
    }

    private String t(String key) {
        return translator.apply(key, new Object[0]);
    }

    private static String nvl(String s) {
        return s != null ? s : "";
    }

    private static String blankToNull(String s) {
        return s != null && !s.isBlank() ? s.trim() : null;
    }
}
