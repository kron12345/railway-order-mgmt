package com.ordermgmt.railway.ui.component.order;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.BiFunction;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.shared.Registration;

import com.ordermgmt.railway.domain.infrastructure.model.OperationalPoint;
import com.ordermgmt.railway.domain.infrastructure.model.PredefinedTag;
import com.ordermgmt.railway.domain.infrastructure.repository.OperationalPointRepository;
import com.ordermgmt.railway.domain.infrastructure.repository.PredefinedTagRepository;
import com.ordermgmt.railway.domain.order.model.Order;
import com.ordermgmt.railway.domain.order.model.OrderPosition;
import com.ordermgmt.railway.domain.order.model.PositionStatus;
import com.ordermgmt.railway.domain.order.model.PositionType;
import com.ordermgmt.railway.domain.order.service.OrderService;

/**
 * Dialog for creating/editing a "Leistung" (service) order position.
 * Fahrplan positions use a separate editor.
 */
public class ServicePositionDialog extends Dialog {

    private final OrderService orderService;
    private final BiFunction<String, Object[], String> translator;
    private final Order order;
    private final OrderPosition position;
    private final boolean isNew;
    private final List<PredefinedTag> availableTags;

    private final TextField name = new TextField();
    private final TextField serviceType = new TextField();
    private final ComboBox<OperationalPoint> fromOp = new ComboBox<>();
    private final ComboBox<OperationalPoint> toOp = new ComboBox<>();
    private final DatePicker validFrom = new DatePicker();
    private final DatePicker validTo = new DatePicker();
    private final CheckboxGroup<PredefinedTag> tags = new CheckboxGroup<>();
    private final TextArea comment = new TextArea();

    public ServicePositionDialog(
            Order order,
            OrderPosition existing,
            OrderService orderService,
            OperationalPointRepository opRepo,
            PredefinedTagRepository tagRepo,
            BiFunction<String, Object[], String> translator) {
        this.order = order;
        this.orderService = orderService;
        this.translator = translator;
        this.isNew = existing == null;
        this.position = isNew ? new OrderPosition() : existing;
        this.availableTags = loadTags(tagRepo);

        setHeaderTitle(isNew ? t("position.new") + " — " + t("position.type.LEISTUNG")
                : t("position.edit") + " — " + position.getName());
        setWidth("800px");

        buildForm(opRepo);
        buildFooter();
    }

    private void buildForm(OperationalPointRepository opRepo) {
        name.setLabel(t("position.name"));
        name.setRequired(true);
        name.setMaxLength(255);
        name.setHelperText(t("position.name.help"));
        name.setWidthFull();

        serviceType.setLabel(t("position.serviceType"));
        serviceType.setHelperText(t("position.serviceType.help"));
        serviceType.setWidthFull();

        // OP-based location selection
        List<OperationalPoint> ops = opRepo.findAll();
        ops.sort(Comparator.comparing(OperationalPoint::getName, String.CASE_INSENSITIVE_ORDER));

        fromOp.setLabel(t("position.from"));
        fromOp.setItems(ops);
        fromOp.setItemLabelGenerator(this::opLabel);
        fromOp.setClearButtonVisible(true);
        fromOp.setHelperText(t("position.from.help"));
        fromOp.setWidthFull();

        toOp.setLabel(t("position.to"));
        toOp.setItems(ops);
        toOp.setItemLabelGenerator(this::opLabel);
        toOp.setClearButtonVisible(true);
        toOp.setHelperText(t("position.to.help"));
        toOp.setWidthFull();

        // Validity constrained to parent order dates
        LocalDate orderFrom = order.getValidFrom();
        LocalDate orderTo = order.getValidTo();

        validFrom.setLabel(t("position.validFrom"));
        validFrom.setRequired(true);
        if (orderFrom != null) validFrom.setMin(orderFrom);
        if (orderTo != null) validFrom.setMax(orderTo);
        validFrom.setHelperText(t("position.validFrom.help",
                orderFrom != null ? orderFrom.toString() : "—",
                orderTo != null ? orderTo.toString() : "—"));
        validFrom.setWidthFull();

        validTo.setLabel(t("position.validTo"));
        validTo.setRequired(true);
        if (orderFrom != null) validTo.setMin(orderFrom);
        if (orderTo != null) validTo.setMax(orderTo);
        validTo.setHelperText(t("position.validTo.help"));
        validTo.setWidthFull();

        // Link pickers: from constrains to
        validFrom.addValueChangeListener(e -> {
            if (e.getValue() != null) validTo.setMin(e.getValue());
        });

        tags.setLabel(t("order.tags"));
        tags.setItems(availableTags);
        tags.setItemLabelGenerator(PredefinedTag::getName);
        tags.setWidthFull();
        tags.setHelperText(t("position.tags.help"));

        comment.setLabel(t("order.comment"));
        comment.setMaxLength(2000);
        comment.setHelperText(t("order.comment.help"));
        comment.setWidthFull();
        comment.setHeight("80px");

        FormLayout form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2));

        form.add(name, serviceType);
        form.add(fromOp, toOp);
        form.add(validFrom, validTo);
        form.setColspan(tags, 2);
        form.add(tags);
        form.setColspan(comment, 2);
        form.add(comment);

        readFrom();
        add(form);
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
        serviceType.setValue(nvl(position.getServiceType()));
        comment.setValue(nvl(position.getComment()));

        // Parse validity JSON → dates
        if (position.getStart() != null) {
            validFrom.setValue(position.getStart().toLocalDate());
        }
        if (position.getEnd() != null) {
            validTo.setValue(position.getEnd().toLocalDate());
        }

        // Match fromLocation/toLocation to OPs
        // For now store as string, match by name
        readTags(position.getTags());
    }

    private void savePosition() {
        if (name.getValue().isBlank()) { name.setInvalid(true); return; }
        if (validFrom.getValue() == null) { validFrom.setInvalid(true); return; }
        if (validTo.getValue() == null) { validTo.setInvalid(true); return; }
        if (validTo.getValue().isBefore(validFrom.getValue())) {
            validTo.setInvalid(true);
            return;
        }

        position.setName(name.getValue().trim());
        position.setType(PositionType.LEISTUNG);
        position.setServiceType(blankToNull(serviceType.getValue()));

        position.setFromLocation(fromOp.getValue() != null ? fromOp.getValue().getName() : null);
        position.setToLocation(toOp.getValue() != null ? toOp.getValue().getName() : null);

        position.setStart(validFrom.getValue().atStartOfDay());
        position.setEnd(validTo.getValue().atTime(23, 59));

        position.setTags(joinSelectedTags());
        position.setComment(blankToNull(comment.getValue()));

        if (isNew) {
            position.setOrder(order);
            position.setInternalStatus(PositionStatus.IN_BEARBEITUNG);
        }

        orderService.savePosition(position);
        Notification.show(t("common.save") + " ✓", 2000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        fireEvent(new SaveEvent(this));
        close();
    }

    // --- Tag handling ---

    private List<PredefinedTag> loadTags(PredefinedTagRepository tagRepo) {
        return tagRepo.findAllByOrderByCategoryAscSortOrderAsc().stream()
                .filter(PredefinedTag::isActive)
                .filter(t -> "POSITION".equals(t.getCategory()) || "GENERAL".equals(t.getCategory()))
                .toList();
    }

    private void readTags(String stored) {
        if (stored == null || stored.isBlank()) return;
        LinkedHashSet<PredefinedTag> selected = new LinkedHashSet<>();
        for (String token : stored.split(",")) {
            String name = token.trim();
            availableTags.stream()
                    .filter(t -> t.getName().equalsIgnoreCase(name))
                    .findFirst()
                    .ifPresent(selected::add);
        }
        tags.setValue(selected);
    }

    private String joinSelectedTags() {
        List<String> names = new ArrayList<>();
        for (PredefinedTag tag : availableTags) {
            if (tags.getValue().contains(tag)) names.add(tag.getName());
        }
        return names.isEmpty() ? null : String.join(", ", names);
    }

    private String opLabel(OperationalPoint op) {
        return op.getName() + " (" + op.getUopid() + ")";
    }

    // --- Events ---

    public Registration addSaveListener(ComponentEventListener<SaveEvent> listener) {
        return addListener(SaveEvent.class, listener);
    }

    public static class SaveEvent extends ComponentEvent<ServicePositionDialog> {
        public SaveEvent(ServicePositionDialog source) {
            super(source, false);
        }
    }

    // --- Helpers ---

    private String t(String key) { return translator.apply(key, new Object[0]); }
    private String t(String key, Object... params) { return translator.apply(key, params); }
    private static String nvl(String s) { return s != null ? s : ""; }
    private static String blankToNull(String s) {
        return s != null && !s.isBlank() ? s.trim() : null;
    }
}
