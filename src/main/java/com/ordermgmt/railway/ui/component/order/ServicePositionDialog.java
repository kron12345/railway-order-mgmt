package com.ordermgmt.railway.ui.component.order;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiFunction;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.timepicker.TimePicker;
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
import com.ordermgmt.railway.ui.component.ValidityCalendar;
import com.ordermgmt.railway.ui.util.StringUtils;

/**
 * Dialog for creating/editing a "Leistung" (service) order position. Fahrplan positions use a
 * separate editor.
 */
public class ServicePositionDialog extends Dialog {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final OrderService orderService;
    private final BiFunction<String, Object[], String> translator;
    private final Order order;
    private final OrderPosition position;
    private final boolean isNew;
    private final List<PredefinedTag> availableTags;
    private final List<OperationalPoint> availableOperationalPoints = new ArrayList<>();
    private final LinkedHashSet<String> unmatchedTags = new LinkedHashSet<>();

    private final TextField name = new TextField();
    private final TextField serviceType = new TextField();
    private final ComboBox<OperationalPoint> fromOp = new ComboBox<>();
    private final ComboBox<OperationalPoint> toOp = new ComboBox<>();
    private final TimePicker startTime = new TimePicker();
    private final TimePicker endTime = new TimePicker();
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

        setHeaderTitle(
                isNew
                        ? t("position.new") + " — " + t("position.type.LEISTUNG")
                        : t("position.edit") + " — " + position.getName());
        setWidth("800px");

        buildForm(opRepo);
        buildFooter();
    }

    // ── Form construction ──────────────────────────────────────────────

    private void buildForm(OperationalPointRepository opRepo) {
        Locale locale = getLocale() != null ? getLocale() : Locale.GERMANY;

        name.setLabel(t("position.name"));
        name.setRequired(true);
        name.setMaxLength(255);
        name.setHelperText(t("position.name.help"));
        name.setWidthFull();

        serviceType.setLabel(t("position.serviceType"));
        serviceType.setHelperText(t("position.serviceType.help"));
        serviceType.setWidthFull();

        // OP-based location selection
        availableOperationalPoints.clear();
        availableOperationalPoints.addAll(opRepo.findAll());
        availableOperationalPoints.sort(
                Comparator.comparing(OperationalPoint::getName, String.CASE_INSENSITIVE_ORDER));

        fromOp.setLabel(t("position.from"));
        fromOp.setItems(availableOperationalPoints);
        fromOp.setItemLabelGenerator(this::opLabel);
        fromOp.setClearButtonVisible(true);
        fromOp.setHelperText(t("position.from.help"));
        fromOp.setWidthFull();

        toOp.setLabel(t("position.to"));
        toOp.setItems(availableOperationalPoints);
        toOp.setItemLabelGenerator(this::opLabel);
        toOp.setClearButtonVisible(true);
        toOp.setHelperText(t("position.to.help"));
        toOp.setWidthFull();

        startTime.setLabel(t("position.startTime"));
        startTime.setHelperText(t("position.startTime.help"));
        startTime.setWidthFull();
        startTime.setRequired(true);
        startTime.setStep(Duration.ofMinutes(1));
        startTime.setClearButtonVisible(false);
        startTime.setAllowedCharPattern("[0-9:]");
        startTime.setPlaceholder("HH:mm");
        startTime.setLocale(locale);
        startTime.setI18n(
                new TimePicker.TimePickerI18n()
                        .setRequiredErrorMessage(t("position.startTime.required"))
                        .setBadInputErrorMessage(t("position.time.format")));
        startTime.addValueChangeListener(
                event -> {
                    if (event.getValue() != null) {
                        startTime.setInvalid(false);
                    }
                });

        endTime.setLabel(t("position.endTime"));
        endTime.setHelperText(t("position.endTime.help"));
        endTime.setWidthFull();
        endTime.setRequired(true);
        endTime.setStep(Duration.ofMinutes(1));
        endTime.setClearButtonVisible(false);
        endTime.setAllowedCharPattern("[0-9:]");
        endTime.setPlaceholder("HH:mm");
        endTime.setLocale(locale);
        endTime.setI18n(
                new TimePicker.TimePickerI18n()
                        .setRequiredErrorMessage(t("position.endTime.required"))
                        .setBadInputErrorMessage(t("position.time.format")));
        endTime.addValueChangeListener(
                event -> {
                    if (event.getValue() != null) {
                        endTime.setInvalid(false);
                    }
                });

        // Validity calendar — multi-date selection within order range
        LocalDate orderFrom = order.getValidFrom() != null ? order.getValidFrom() : LocalDate.now();
        LocalDate orderTo =
                order.getValidTo() != null ? order.getValidTo() : orderFrom.plusMonths(3);
        validityCalendar = new ValidityCalendar(orderFrom, orderTo);

        tags.setLabel(t("order.tags"));
        tags.setItems(availableTags);
        tags.setItemLabelGenerator(PredefinedTag::getName);
        tags.setWidthFull();
        updateTagsHelperText();

        comment.setLabel(t("order.comment"));
        comment.setMaxLength(2000);
        comment.setHelperText(t("order.comment.help"));
        comment.setWidthFull();
        comment.setHeight("80px");

        FormLayout form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("500px", 2));

        form.add(name, serviceType);
        form.add(fromOp, toOp);
        form.add(startTime, endTime);

        // Validity calendar spans full width
        Div calSection = new Div();
        Span calLabel = new Span(t("position.validity"));
        calLabel.getStyle()
                .set("font-weight", "600")
                .set("font-size", "12px")
                .set("color", "var(--rom-text-primary)")
                .set("display", "block")
                .set("margin-bottom", "6px");
        Span calHelper =
                new Span(t("position.validity.help", orderFrom.toString(), orderTo.toString()));
        calHelper
                .getStyle()
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

    // ── Footer / actions ───────────────────────────────────────────────

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
        name.setValue(StringUtils.nvl(position.getName()));
        serviceType.setValue(StringUtils.nvl(position.getServiceType()));
        comment.setValue(StringUtils.nvl(position.getComment()));
        fromOp.setValue(findOperationalPoint(position.getFromLocation()));
        toOp.setValue(findOperationalPoint(position.getToLocation()));
        startTime.setValue(position.getStart() != null ? position.getStart().toLocalTime() : null);
        endTime.setValue(position.getEnd() != null ? position.getEnd().toLocalTime() : null);
        startTime.setInvalid(false);
        endTime.setInvalid(false);

        // Parse validity JSON → calendar dates
        List<LocalDate> dates = parseValidityDates(position.getValidity());
        if (dates.isEmpty() && position.getStart() != null) {
            // Fallback: use start/end as range
            LocalDate d = position.getStart().toLocalDate();
            LocalDate end = position.getEnd() != null ? position.getEnd().toLocalDate() : d;
            while (!d.isAfter(end)) {
                dates.add(d);
                d = d.plusDays(1);
            }
        }
        validityCalendar.setSelectedDates(dates);
        readTags(position.getTags());
    }

    // ── Validation and save ─────────────────────────────────────────────

    private void savePosition() {
        if (name.getValue().isBlank()) {
            name.setInvalid(true);
            return;
        }

        if (startTime.getValue() == null) {
            startTime.setInvalid(true);
            startTime.setErrorMessage(t("position.startTime.required"));
            return;
        }
        startTime.setInvalid(false);

        if (endTime.getValue() == null) {
            endTime.setInvalid(true);
            endTime.setErrorMessage(t("position.endTime.required"));
            return;
        }
        endTime.setInvalid(false);

        List<LocalDate> selectedDates = validityCalendar.getSelectedDates();
        if (selectedDates.isEmpty()) {
            Notification.show(
                            t("position.validity.required"), 3000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        LocalTime resolvedStartTime = startTime.getValue();
        LocalTime resolvedEndTime = endTime.getValue();
        if (selectedDates.size() == 1 && resolvedEndTime.isBefore(resolvedStartTime)) {
            endTime.setInvalid(true);
            endTime.setErrorMessage(t("position.time.invalid"));
            Notification.show(t("position.time.invalid"), 3000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        endTime.setInvalid(false);

        position.setName(name.getValue().trim());
        position.setType(PositionType.LEISTUNG);
        position.setServiceType(StringUtils.blankToNull(serviceType.getValue()));

        position.setFromLocation(fromOp.getValue() != null ? fromOp.getValue().getName() : null);
        position.setToLocation(toOp.getValue() != null ? toOp.getValue().getName() : null);

        // Store first/last as start/end for backwards compat
        position.setStart(selectedDates.getFirst().atTime(resolvedStartTime));
        position.setEnd(selectedDates.getLast().atTime(resolvedEndTime));
        // Store all dates as validity JSON
        position.setValidity(toValidityJson(selectedDates));

        position.setTags(joinSelectedTags());
        position.setComment(StringUtils.blankToNull(comment.getValue()));

        if (isNew) {
            position.setOrder(order);
            position.setInternalStatus(PositionStatus.IN_BEARBEITUNG);
        }

        orderService.savePosition(position);
        Notification.show(t("common.save") + " \u2713", 2000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        fireEvent(new SaveEvent(this));
        close();
    }

    // ── Tag handling ────────────────────────────────────────────────────

    private List<PredefinedTag> loadTags(PredefinedTagRepository tagRepo) {
        return tagRepo.findAllByOrderByCategoryAscSortOrderAsc().stream()
                .filter(PredefinedTag::isActive)
                .filter(
                        t ->
                                "POSITION".equals(t.getCategory())
                                        || "GENERAL".equals(t.getCategory()))
                .toList();
    }

    private void readTags(String stored) {
        Map<String, PredefinedTag> tagsByName = new LinkedHashMap<>();
        for (PredefinedTag tag : availableTags) {
            tagsByName.put(normalizeTagName(tag.getName()), tag);
        }

        unmatchedTags.clear();
        LinkedHashSet<PredefinedTag> selected = new LinkedHashSet<>();
        for (String token : StringUtils.splitTags(stored)) {
            PredefinedTag match = tagsByName.get(normalizeTagName(token));
            if (match != null) {
                selected.add(match);
            } else {
                unmatchedTags.add(token);
            }
        }
        tags.setValue(selected);
        updateTagsHelperText();
    }

    private String joinSelectedTags() {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (PredefinedTag tag : availableTags) {
            if (tags.getValue().contains(tag)) names.add(tag.getName());
        }
        names.addAll(unmatchedTags);
        return names.isEmpty() ? null : String.join(", ", names);
    }

    private String opLabel(OperationalPoint op) {
        return op.getName() + " (" + op.getUopid() + ")";
    }

    private OperationalPoint findOperationalPoint(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return availableOperationalPoints.stream()
                .filter(op -> name.equalsIgnoreCase(op.getName()))
                .findFirst()
                .orElse(null);
    }

    private void updateTagsHelperText() {
        String helper = t("position.tags.help");
        if (!unmatchedTags.isEmpty()) {
            helper = helper + " " + t("position.tags.legacy", String.join(", ", unmatchedTags));
        }
        tags.setHelperText(helper);
    }

    private String normalizeTagName(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    // ── Validity JSON ────────────────────────────────────────────────────

    private String toValidityJson(List<LocalDate> dates) {
        // Group consecutive dates into segments
        List<Map<String, String>> segments = new ArrayList<>();
        LocalDate rangeStart = null;
        LocalDate prev = null;
        for (LocalDate d : dates) {
            if (rangeStart == null) {
                rangeStart = d;
            } else if (!d.equals(prev.plusDays(1))) {
                Map<String, String> segment = new LinkedHashMap<>();
                segment.put("startDate", rangeStart.toString());
                segment.put("endDate", prev.toString());
                segments.add(segment);
                rangeStart = d;
            }
            prev = d;
        }
        if (rangeStart != null) {
            Map<String, String> segment = new LinkedHashMap<>();
            segment.put("startDate", rangeStart.toString());
            segment.put("endDate", prev.toString());
            segments.add(segment);
        }

        try {
            return OBJECT_MAPPER.writeValueAsString(segments);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private List<LocalDate> parseValidityDates(String json) {
        List<LocalDate> dates = new ArrayList<>();
        if (json == null || json.isBlank()) return dates;
        try {
            var array = OBJECT_MAPPER.readTree(json);
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
        } catch (Exception ignored) {
        }
        return dates;
    }

    // ── Events ────────────────────────────────────────────────────────

    public Registration addSaveListener(ComponentEventListener<SaveEvent> listener) {
        return addListener(SaveEvent.class, listener);
    }

    public static class SaveEvent extends ComponentEvent<ServicePositionDialog> {
        public SaveEvent(ServicePositionDialog source) {
            super(source, false);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private String t(String key) {
        return translator.apply(key, new Object[0]);
    }

    private String t(String key, Object... params) {
        return translator.apply(key, params);
    }
}
