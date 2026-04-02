package com.ordermgmt.railway.ui.component.order;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;

import com.ordermgmt.railway.domain.customer.model.Customer;
import com.ordermgmt.railway.domain.customer.repository.CustomerRepository;
import com.ordermgmt.railway.domain.infrastructure.model.PredefinedTag;
import com.ordermgmt.railway.domain.infrastructure.repository.PredefinedTagRepository;
import com.ordermgmt.railway.domain.order.model.Order;

/**
 * Order form for creation and editing. Only shows fields relevant to the user. ProcessStatus
 * defaults to AUFTRAG on creation and is not editable here. InternalStatus and timetableYearLabel
 * are removed.
 */
public class OrderFormPanel extends Div {

    private final TextField orderNumber = new TextField();
    private final TextField name = new TextField();
    private final ComboBox<Customer> customerCombo = new ComboBox<>();
    private final DatePicker validFrom = new DatePicker();
    private final DatePicker validTo = new DatePicker();
    private final CheckboxGroup<PredefinedTag> tags = new CheckboxGroup<>();
    private final TextArea comment = new TextArea();
    private final BiFunction<String, Object[], String> translator;
    private final List<PredefinedTag> availableTags;
    private final LinkedHashSet<String> unmatchedTags = new LinkedHashSet<>();

    public OrderFormPanel(
            Order order,
            CustomerRepository customerRepository,
            PredefinedTagRepository predefinedTagRepository,
            BiFunction<String, Object[], String> translator) {
        this.translator = translator;
        this.availableTags = loadAvailableTags(predefinedTagRepository);

        setWidthFull();
        getStyle()
                .set("background", "var(--rom-bg-card)")
                .set("border", "1px solid var(--rom-border)")
                .set("border-radius", "6px")
                .set("padding", "var(--lumo-space-m) var(--lumo-space-l)")
                .set("margin-bottom", "var(--lumo-space-s)")
                .set("box-sizing", "border-box");

        H3 sectionTitle = new H3(t("order.edit"));
        sectionTitle
                .getStyle()
                .set("color", "var(--rom-text-primary)")
                .set("margin", "0 0 var(--lumo-space-s) 0")
                .set("font-size", "var(--lumo-font-size-m)");

        orderNumber.setLabel(t("order.number"));
        orderNumber.setRequired(true);
        orderNumber.setMaxLength(50);
        orderNumber.setAllowedCharPattern("[A-Za-z0-9\\-_]");
        orderNumber.setHelperText(t("order.number.help"));
        orderNumber.setWidthFull();

        name.setLabel(t("order.name"));
        name.setRequired(true);
        name.setMaxLength(255);
        name.setHelperText(t("order.name.help"));
        name.setWidthFull();

        customerCombo.setLabel(t("order.customer"));
        customerCombo.setItems(customerRepository.findAll());
        customerCombo.setItemLabelGenerator(Customer::getName);
        customerCombo.setClearButtonVisible(true);
        customerCombo.setHelperText(t("order.customer.help"));
        customerCombo.setWidthFull();

        validFrom.setLabel(t("order.validFrom"));
        validFrom.setRequired(true);
        validFrom.setHelperText(t("order.validFrom.help"));
        validFrom.setWidthFull();

        validTo.setLabel(t("order.validTo"));
        validTo.setRequired(true);
        validTo.setHelperText(t("order.validTo.help"));
        validTo.setWidthFull();

        tags.setLabel(t("order.tags"));
        tags.setItems(availableTags);
        tags.setItemLabelGenerator(this::tagLabel);
        tags.setWidthFull();
        updateTagsHelperText();

        comment.setLabel(t("order.comment"));
        comment.setMaxLength(2000);
        comment.setHelperText(t("order.comment.help"));
        comment.setWidthFull();
        comment.setHeight("80px");

        FormLayout form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2),
                new FormLayout.ResponsiveStep("800px", 3));

        form.add(orderNumber, name, customerCombo);
        form.add(validFrom, validTo);
        form.setColspan(tags, 3);
        form.add(tags);
        form.setColspan(comment, 3);
        form.add(comment);

        readFrom(order);
        add(sectionTitle, form);
    }

    private void readFrom(Order order) {
        orderNumber.setValue(nvl(order.getOrderNumber()));
        name.setValue(nvl(order.getName()));
        customerCombo.setValue(order.getCustomer());
        validFrom.setValue(order.getValidFrom());
        validTo.setValue(order.getValidTo());
        readTags(order.getTags());
        comment.setValue(nvl(order.getComment()));
    }

    public void writeTo(Order order) {
        order.setOrderNumber(orderNumber.getValue().trim());
        order.setName(name.getValue().trim());
        order.setCustomer(customerCombo.getValue());
        order.setValidFrom(validFrom.getValue());
        order.setValidTo(validTo.getValue());
        order.setTags(joinSelectedTags());
        order.setComment(blankToNull(comment.getValue()));
    }

    public boolean validate() {
        boolean valid = true;
        if (orderNumber.getValue().isBlank()) {
            orderNumber.setInvalid(true);
            valid = false;
        }
        if (name.getValue().isBlank()) {
            name.setInvalid(true);
            valid = false;
        }
        if (validFrom.getValue() == null) {
            validFrom.setInvalid(true);
            valid = false;
        }
        if (validTo.getValue() == null) {
            validTo.setInvalid(true);
            valid = false;
        }
        if (validFrom.getValue() != null
                && validTo.getValue() != null
                && validTo.getValue().isBefore(validFrom.getValue())) {
            validTo.setInvalid(true);
            validTo.setErrorMessage(t("order.validTo") + " < " + t("order.validFrom"));
            valid = false;
        }
        return valid;
    }

    private String t(String key) {
        return translator.apply(key, new Object[0]);
    }

    private String t(String key, Object... params) {
        return translator.apply(key, params);
    }

    private List<PredefinedTag> loadAvailableTags(PredefinedTagRepository predefinedTagRepository) {
        return predefinedTagRepository.findAllByOrderByCategoryAscSortOrderAsc().stream()
                .filter(PredefinedTag::isActive)
                .filter(
                        tag ->
                                "ORDER".equals(tag.getCategory())
                                        || "GENERAL".equals(tag.getCategory()))
                .sorted(
                        Comparator.comparingInt(this::categoryRank)
                                .thenComparingInt(PredefinedTag::getSortOrder)
                                .thenComparing(
                                        PredefinedTag::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private void readTags(String storedTags) {
        Map<String, PredefinedTag> tagsByName = new LinkedHashMap<>();
        for (PredefinedTag tag : availableTags) {
            tagsByName.put(normalizeTagName(tag.getName()), tag);
        }

        unmatchedTags.clear();
        LinkedHashSet<PredefinedTag> selected = new LinkedHashSet<>();
        for (String token : splitTags(storedTags)) {
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
        LinkedHashSet<String> values = new LinkedHashSet<>();
        LinkedHashSet<PredefinedTag> selectedTags = new LinkedHashSet<>(tags.getValue());
        for (PredefinedTag tag : availableTags) {
            if (selectedTags.contains(tag)) {
                values.add(tag.getName());
            }
        }
        values.addAll(unmatchedTags);
        return blankToNull(String.join(", ", values));
    }

    private List<String> splitTags(String storedTags) {
        List<String> values = new ArrayList<>();
        if (storedTags == null || storedTags.isBlank()) {
            return values;
        }

        for (String token : storedTags.split(",")) {
            String normalized = token.trim();
            if (!normalized.isBlank()) {
                values.add(normalized);
            }
        }
        return values;
    }

    private void updateTagsHelperText() {
        String helper = t("order.tags.help");
        if (!unmatchedTags.isEmpty()) {
            helper = helper + " " + t("order.tags.legacy", String.join(", ", unmatchedTags));
        }
        tags.setHelperText(helper);
    }

    private String tagLabel(PredefinedTag tag) {
        return "[" + categoryLabel(tag.getCategory()) + "] " + tag.getName();
    }

    private String categoryLabel(String category) {
        return switch (category) {
            case "ORDER" -> t("settings.tags.cat.order");
            case "GENERAL" -> t("settings.tags.cat.general");
            default -> category;
        };
    }

    private int categoryRank(PredefinedTag tag) {
        return switch (tag.getCategory()) {
            case "ORDER" -> 0;
            case "GENERAL" -> 1;
            default -> 99;
        };
    }

    private String normalizeTagName(String value) {
        return value.trim().toLowerCase();
    }

    private static String nvl(String s) {
        return s != null ? s : "";
    }

    private static String blankToNull(String s) {
        return s != null && !s.isBlank() ? s.trim() : null;
    }
}
