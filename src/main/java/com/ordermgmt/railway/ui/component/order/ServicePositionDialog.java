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
import com.ordermgmt.railway.ui.component.ValidityCalendar;
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
    private ValidityCalendar validityCalendar;
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

        // Validity calendar — multi-date selection within order range
        LocalDate orderFrom = order.getValidFrom() != null ? order.getValidFrom() : LocalDate.now();
        LocalDate orderTo = order.getValidTo() != null ? order.getValidTo() : orderFrom.plusMonths(3);
        validityCalendar = new ValidityCalendar(orderFrom, orderTo);

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

        // Validity calendar spans full width
        Div calSection = new Div();
        Span calLabel = new Span(t("position.validity"));
        calLabel.getStyle()
                .set("font-weight", "600")
                .set("font-size", "12px")
                .set("color", "var(--rom-text-primary)")
                .set("display", "block")
                .set("margin-bottom", "6px");
        Span calHelper = new Span(t("position.validity.help",
                orderFrom.toString(), orderTo.toString()));
        calHelper.getStyle()
                .set("font-size", "10px")
                .set("font-family", "'JetBrains Mono', monospace")
                .set("color", "var(--rom-text-muted)")
                .set("display", "block")
                .set("margin-bottom", "8px");
        calSection.add(calLabel, calHelper, validityCalendar);
        form.setColspan(calSection, 2);
        form.add(calSection);

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

        // Parse validity JSON → calendar dates
        List<LocalDate> dates = parseValidityDates(position.getValidity());
        if (dates.isEmpty() && position.getStart() != null) {
            // Fallback: use start/end as range
            LocalDate d = position.getStart().toLocalDate();
            LocalDate end = position.getEnd() != null ? position.getEnd().toLocalDate() : d;
            while (!d.isAfter(end)) { dates.add(d); d = d.plusDays(1); }
        }
        validityCalendar.setSelectedDates(dates);
        readTags(position.getTags());
    }

    private void savePosition() {
        if (name.getValue().isBlank()) { name.setInvalid(true); return; }

        List<LocalDate> selectedDates = validityCalendar.getSelectedDates();
        if (selectedDates.isEmpty()) {
            Notification.show(t("position.validity.required"), 3000,
                            Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        position.setName(name.getValue().trim());
        position.setType(PositionType.LEISTUNG);
        position.setServiceType(blankToNull(serviceType.getValue()));

        position.setFromLocation(fromOp.getValue() != null ? fromOp.getValue().getName() : null);
        position.setToLocation(toOp.getValue() != null ? toOp.getValue().getName() : null);

        // Store first/last as start/end for backwards compat
        position.setStart(selectedDates.getFirst().atStartOfDay());
        position.setEnd(selectedDates.getLast().atTime(23, 59));
        // Store all dates as validity JSON
        position.setValidity(toValidityJson(selectedDates));

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

    // --- Validity JSON ---

    private String toValidityJson(List<LocalDate> dates) {
        // Group consecutive dates into segments
        StringBuilder sb = new StringBuilder("[");
        LocalDate rangeStart = null;
        LocalDate prev = null;
        boolean first = true;
        for (LocalDate d : dates) {
            if (rangeStart == null) {
                rangeStart = d;
            } else if (!d.equals(prev.plusDays(1))) {
                if (!first) sb.append(",");
                sb.append("{\"startDate\":\"").append(rangeStart).append("\",\"endDate\":\"").append(prev).append("\"}");
                first = false;
                rangeStart = d;
            }
            prev = d;
        }
        if (rangeStart != null) {
            if (!first) sb.append(",");
            sb.append("{\"startDate\":\"").append(rangeStart).append("\",\"endDate\":\"").append(prev).append("\"}");
        }
        sb.append("]");
        return sb.toString();
    }

    private List<LocalDate> parseValidityDates(String json) {
        List<LocalDate> dates = new ArrayList<>();
        if (json == null || json.isBlank()) return dates;
        try {
            var array = new com.fasterxml.jackson.databind.ObjectMapper().readTree(json);
            if (array.isArray()) {
                for (var seg : array) {
                    var sn = seg.get("startDate");
                    var en = seg.get("endDate");
                    if (sn == null || en == null) continue;
                    LocalDate s = LocalDate.parse(sn.asText());
                    LocalDate e = LocalDate.parse(en.asText());
                    for (LocalDate d = s; !d.isAfter(e); d = d.plusDays(1)) dates.add(d);
                }
            }
        } catch (Exception ignored) {}
        return dates;
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
