package com.ordermgmt.railway.ui.component.timetable;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;

import com.ordermgmt.railway.domain.business.service.BusinessService;
import com.ordermgmt.railway.domain.infrastructure.model.PredefinedTag;
import com.ordermgmt.railway.ui.component.business.BusinessLinkField;

/**
 * The metadata card of {@link TimetableBuilderView} (name, OTN, tags, comment, linked businesses).
 * Owns those fields + their wiring so the view stays under the size limit; the view reads values
 * via the small getter surface. Same behaviour as before, relocated.
 */
public class TimetableMetadataCard {

    private final BusinessService businessService;
    private final List<PredefinedTag> availableTags;
    private final Component i18n;

    private final TextField positionName = new TextField();
    private final TextField otnField = new TextField();
    private final CheckboxGroup<PredefinedTag> tagSelector = new CheckboxGroup<>();
    private final TextArea commentField = new TextArea();
    private final LinkedHashSet<String> unmatchedTags = new LinkedHashSet<>();
    private BusinessLinkField businessLinkField;
    private Details details;

    public TimetableMetadataCard(
            BusinessService businessService, List<PredefinedTag> availableTags, Component i18n) {
        this.businessService = businessService;
        this.availableTags = availableTags;
        this.i18n = i18n;
        configureFields();
    }

    private void configureFields() {
        positionName.setLabel(t("position.name"));
        positionName.setRequired(true);
        positionName.setMaxLength(255);
        positionName.setWidthFull();
        positionName.setHelperText(t("timetable.meta.name.help"));
        otnField.setLabel(t("timetable.otn"));
        otnField.setMaxLength(20);
        otnField.setWidthFull();
        otnField.setHelperText(t("timetable.otn.help"));
        otnField.setPlaceholder("z.B. 95345 oder 95xxx");
        tagSelector.setLabel(t("order.tags"));
        tagSelector.setItems(availableTags);
        tagSelector.setItemLabelGenerator(this::tagLabel);
        tagSelector.setWidthFull();
        updateTagHelper();
        commentField.setLabel(t("order.comment"));
        commentField.setMaxLength(2000);
        commentField.setWidthFull();
        commentField.setHeight("60px");
        commentField.setHelperText(t("timetable.meta.comment.help"));
    }

    /** Builds the collapsible metadata card; {@code openByDefault} for a brand-new position. */
    public Component card(boolean openByDefault, UUID existingPositionId) {
        FormLayout form = new FormLayout();
        form.setWidthFull();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2),
                new FormLayout.ResponsiveStep("900px", 4));
        businessLinkField = new BusinessLinkField(businessService, this::t);
        form.add(positionName, otnField, tagSelector, commentField);
        form.setColspan(businessLinkField, 4);
        form.add(businessLinkField);
        if (existingPositionId != null) {
            businessLinkField.preset(businessService.findByLinkedOrderPosition(existingPositionId));
        }
        details = new Details(t("timetable.meta.title"), form);
        details.setOpened(openByDefault);
        details.setWidthFull();
        details.getStyle()
                .set("background", "var(--rom-bg-card)")
                .set("border", "1px solid var(--rom-border)")
                .set("border-radius", "6px")
                .set("margin-bottom", "var(--lumo-space-s)");
        return details;
    }

    // ── Accessors used by the view's save/load/refresh paths ───────────────

    public TextField nameField() {
        return positionName;
    }

    public TextField otnInput() {
        return otnField;
    }

    public TextArea commentInput() {
        return commentField;
    }

    public String name() {
        return positionName.getValue();
    }

    public String otn() {
        return otnField.getValue();
    }

    public String comment() {
        return commentField.getValue();
    }

    public void setName(String value) {
        positionName.setValue(value);
    }

    public boolean isNameBlank() {
        return positionName.getValue().isBlank();
    }

    public void markNameInvalidAndFocus() {
        positionName.setInvalid(true);
        if (details != null) {
            details.setOpened(true);
        }
        positionName.focus();
    }

    public void readTags(String stored) {
        tagHelper().readTags(stored);
    }

    public String joinedTags() {
        return tagHelper().joinSelectedTags();
    }

    public void applyBusinessLinks(UUID positionId) {
        businessLinkField.applyTo(positionId);
    }

    private String tagLabel(PredefinedTag tag) {
        return tagHelper().tagLabel(tag);
    }

    private void updateTagHelper() {
        tagHelper().updateTagHelper();
    }

    private TimetableTagHelper tagHelper() {
        return new TimetableTagHelper(tagSelector, availableTags, unmatchedTags, i18n);
    }

    private String t(String key) {
        return i18n.getTranslation(key);
    }
}
