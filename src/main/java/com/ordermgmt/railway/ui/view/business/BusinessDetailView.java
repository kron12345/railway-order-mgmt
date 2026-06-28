package com.ordermgmt.railway.ui.view.business;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextAreaVariant;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;

import com.ordermgmt.railway.domain.business.model.Business;
import com.ordermgmt.railway.domain.business.model.BusinessStatus;
import com.ordermgmt.railway.domain.business.service.BusinessService;
import com.ordermgmt.railway.domain.userprefs.service.UserViewPreferenceService;
import com.ordermgmt.railway.ui.component.business.BusinessDocsCard;
import com.ordermgmt.railway.ui.component.business.BusinessLinksTreeFactory;
import com.ordermgmt.railway.ui.util.StringUtils;

/**
 * Embeddable detail panel for a single {@link Business}. Owns its lifecycle: pass an id (or {@code
 * null} for new) to the constructor; no Vaadin route — embedded by {@link BusinessOverviewView}
 * into the right pane of the master-detail layout.
 */
public class BusinessDetailView extends VerticalLayout {

    private static final Logger log = LoggerFactory.getLogger(BusinessDetailView.class);

    private final BusinessService businessService;
    private final UserViewPreferenceService prefsService;
    private Business business;
    private boolean isNew;

    /** Draft mode (new view): collected IDs to link on save. */
    private final Set<UUID> draftOrderPositionIds = new LinkedHashSet<>();

    private final Set<UUID> draftPurchasePositionIds = new LinkedHashSet<>();

    /** Draft mode (new view): files buffered until the business is created. */
    private final List<DraftDoc> draftDocuments = new ArrayList<>();

    private TextField titleField;
    private TextArea descField;
    private TextField teamField;
    private DatePicker validFromField;
    private DatePicker validToField;
    private DatePicker dueDateField;
    private TextField tagsField;

    /**
     * @param businessService injected business service
     * @param prefsService injected user-view-preferences service
     * @param businessId the id to load; {@code null} means "new business"
     */
    public BusinessDetailView(
            BusinessService businessService,
            UserViewPreferenceService prefsService,
            UUID businessId) {
        this.businessService = businessService;
        this.prefsService = prefsService;
        addClassName("biz-detail");
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        getStyle().set("background", "var(--rom-bg-primary)");

        if (businessId == null) {
            isNew = true;
            business = new Business();
            business.setStatus(BusinessStatus.IN_BEARBEITUNG);
        } else {
            isNew = false;
            business = businessService.getById(businessId).orElse(null);
            if (business == null) {
                UI.getCurrent().navigate("businesses");
                return;
            }
        }
        buildLayout();
    }

    // ─── Layout ────────────────────────

    private void buildLayout() {
        add(buildHeaderBar());

        var horizontalSplit = new SplitLayout();
        horizontalSplit.setOrientation(SplitLayout.Orientation.HORIZONTAL);
        horizontalSplit.setSplitterPosition(38);
        horizontalSplit.setSizeFull();
        horizontalSplit.addClassName("biz-detail__split");

        horizontalSplit.addToPrimary(buildLeftColumn());
        horizontalSplit.addToSecondary(buildRightColumn());

        var verticalSplit = new SplitLayout();
        verticalSplit.setOrientation(SplitLayout.Orientation.VERTICAL);
        verticalSplit.setSplitterPosition(65);
        verticalSplit.setSizeFull();
        verticalSplit.addClassName("biz-detail__vsplit");
        verticalSplit.addToPrimary(horizontalSplit);
        verticalSplit.addToSecondary(buildDocsRow());

        add(verticalSplit);
        setFlexGrow(1, verticalSplit);
    }

    private HorizontalLayout buildHeaderBar() {
        var bar = new HorizontalLayout();
        bar.addClassName("biz-detail__header");
        bar.setWidthFull();
        bar.setPadding(false);
        bar.setSpacing(true);
        bar.setAlignItems(FlexComponent.Alignment.CENTER);

        // From edit mode: go back to read view; from new mode: back to overview.
        var backBtn = new Button(VaadinIcon.ARROW_LEFT.create(), e -> navigateBack());
        backBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);

        var label =
                new Span(
                        isNew
                                ? getTranslation("business.new").toUpperCase()
                                : getTranslation("business.title").toUpperCase());
        label.addClassName("biz-section-title");
        var titleSpan = new Span(isNew ? "—" : safe(business.getTitle()));
        titleSpan.addClassName("biz-detail__title");

        var leftGroup = new HorizontalLayout(backBtn, label, titleSpan);
        leftGroup.setAlignItems(FlexComponent.Alignment.CENTER);
        leftGroup.setSpacing(true);

        var spacer = new Div();
        spacer.getStyle().set("flex", "1");

        var rightGroup = new HorizontalLayout();
        rightGroup.setAlignItems(FlexComponent.Alignment.CENTER);
        rightGroup.setSpacing(true);

        if (!isNew) {
            rightGroup.add(buildStatusControl());
        }

        var saveBtn = new Button(getTranslation("common.save"), VaadinIcon.CHECK.create());
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        saveBtn.addClickListener(e -> saveCurrentBusiness());
        rightGroup.add(saveBtn);

        if (!isNew) {
            var deleteBtn = new Button(VaadinIcon.TRASH.create(), e -> confirmDelete());
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            deleteBtn.getStyle().setColor("var(--rom-status-danger)");
            rightGroup.add(deleteBtn);
        }

        bar.add(leftGroup, spacer, rightGroup);
        bar.setFlexGrow(1, spacer);
        return bar;
    }

    private void navigateBack() {
        if (!isNew && business.getId() != null) {
            UI.getCurrent().navigate("businesses/" + business.getId());
            return;
        }
        UI.getCurrent().navigate("businesses");
    }

    private void saveCurrentBusiness() {
        if (isNew) {
            saveNew();
        } else {
            saveEdit();
        }
    }

    private Component buildStatusControl() {
        var nextStatuses = business.getStatus().nextTargets();
        if (nextStatuses.isEmpty()) {
            var current =
                    new Span(getTranslation("business.status." + business.getStatus().name()));
            current.addClassName("biz-status-pill");
            return current;
        }
        var box = new ComboBox<BusinessStatus>();
        box.setPlaceholder(getTranslation("business.status." + business.getStatus().name()));
        box.setItems(List.copyOf(nextStatuses));
        box.setItemLabelGenerator(s -> getTranslation("business.status." + s.name()));
        box.addThemeName("small");
        box.setWidth("180px");
        box.addValueChangeListener(
                e -> {
                    if (e.getValue() != null) {
                        changeStatus(e.getValue());
                    }
                });
        return box;
    }

    private void changeStatus(BusinessStatus newStatus) {
        try {
            businessService.setStatus(business.getId(), newStatus);
            Notification.show(
                            getTranslation("business.statusChanged"),
                            1500,
                            Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            UI.getCurrent().navigate("businesses/" + business.getId());
        } catch (IllegalArgumentException ex) {
            Notification.show(ex.getMessage(), 3000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    // ─── Left column: form (+ documents in edit mode) ─────────

    private Component buildLeftColumn() {
        var col = new VerticalLayout();
        col.addClassName("biz-detail__left");
        col.setPadding(false);
        col.setSpacing(false);
        col.setSizeFull();

        var form = buildFormCard();
        col.add(form);
        col.setFlexGrow(1, form);
        return col;
    }

    private Component buildDocsRow() {
        if (isNew) {
            return new BusinessDocsCard(
                    this::getTranslation,
                    () ->
                            draftDocuments.stream()
                                    .map(
                                            d ->
                                                    new BusinessDocsCard.DocRow(
                                                            d.id,
                                                            d.filename,
                                                            d.contentType,
                                                            d.createdAt))
                                    .toList(),
                    (filename, contentType, data) ->
                            draftDocuments.add(
                                    new DraftDoc(
                                            UUID.randomUUID(),
                                            filename,
                                            contentType,
                                            data,
                                            Instant.now())),
                    id -> draftDocuments.removeIf(d -> d.id.equals(id)));
        }
        return new BusinessDocsCard(
                this::getTranslation,
                () ->
                        businessService.getDocuments(business.getId()).stream()
                                .map(
                                        d ->
                                                new BusinessDocsCard.DocRow(
                                                        d.id(),
                                                        d.filename(),
                                                        d.contentType(),
                                                        d.createdAt()))
                                .toList(),
                (filename, contentType, data) ->
                        businessService.addDocument(business.getId(), filename, contentType, data),
                id -> businessService.removeDocument(business.getId(), id));
    }

    /** In-memory document buffered during the new-business flow. */
    private record DraftDoc(
            UUID id, String filename, String contentType, byte[] data, Instant createdAt) {}

    private Component buildFormCard() {
        var card = new Div();
        card.addClassName("biz-card");

        var header = new Span(getTranslation("business.title").toUpperCase());
        header.addClassName("biz-section-title");
        card.add(header);

        titleField = compactText(getTranslation("business.title"));
        titleField.setRequired(true);
        titleField.setRequiredIndicatorVisible(true);

        descField = new TextArea(getTranslation("business.description"));
        descField.addThemeVariants(TextAreaVariant.LUMO_SMALL);
        descField.setMinHeight("64px");
        descField.setMaxHeight("96px");
        descField.setWidthFull();

        teamField = compactText(getTranslation("business.team"));
        validFromField = compactDate(getTranslation("business.validFrom"));
        validToField = compactDate(getTranslation("business.validTo"));
        dueDateField = compactDate(getTranslation("business.dueDate"));
        tagsField = compactText(getTranslation("business.tags"));

        if (!isNew) {
            titleField.setValue(safe(business.getTitle()));
            descField.setValue(safe(business.getDescription()));
            teamField.setValue(safe(business.getTeam()));
            validFromField.setValue(business.getValidFrom());
            validToField.setValue(business.getValidTo());
            dueDateField.setValue(business.getDueDate());
            tagsField.setValue(safe(business.getTags()));
        }

        var form = new FormLayout();
        form.addClassName("biz-form");
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("420px", 2));
        form.add(titleField);
        form.setColspan(titleField, 2);
        form.add(descField);
        form.setColspan(descField, 2);
        form.add(teamField, tagsField);
        form.add(validFromField, validToField);
        form.add(dueDateField);
        card.add(form);

        return card;
    }

    private TextField compactText(String label) {
        var f = new TextField(label);
        f.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        f.setWidthFull();
        return f;
    }

    private DatePicker compactDate(String label) {
        var f = new DatePicker(label);
        f.addThemeName("small");
        f.setClearButtonVisible(true);
        f.setWidthFull();
        return f;
    }

    // ─── Right column: unified links tree ─────────

    private Component buildRightColumn() {
        return BusinessLinksTreeFactory.build(
                isNew,
                business,
                businessService,
                prefsService,
                draftOrderPositionIds,
                draftPurchasePositionIds,
                this);
    }

    // ─── Save ──────────────────────────

    private void saveNew() {
        String t = titleField.getValue();
        if (t == null || t.isBlank()) {
            Notification.show(
                            getTranslation("business.title") + " ist Pflichtfeld",
                            2500,
                            Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        Business saved =
                businessService.create(
                        t,
                        descField.getValue(),
                        List.copyOf(draftOrderPositionIds),
                        List.copyOf(draftPurchasePositionIds));
        try {
            businessService.update(
                    saved.getId(),
                    saved.getTitle(),
                    saved.getDescription(),
                    null,
                    null,
                    teamField.getValue(),
                    validFromField.getValue(),
                    validToField.getValue(),
                    dueDateField.getValue(),
                    tagsField.getValue());
            for (DraftDoc d : draftDocuments) {
                businessService.addDocument(saved.getId(), d.filename, d.contentType, d.data);
            }
        } catch (Exception ex) {
            log.error("Failed to save new business {}", saved.getId(), ex);
            Notification.show(
                            getTranslation("common.errorGeneric"),
                            3000,
                            Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        Notification.show(
                        getTranslation("business.created"), 1500, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        UI.getCurrent().navigate("businesses/" + saved.getId());
    }

    private void saveEdit() {
        try {
            businessService.update(
                    business.getId(),
                    titleField.getValue(),
                    descField.getValue(),
                    null,
                    null,
                    teamField.getValue(),
                    validFromField.getValue(),
                    validToField.getValue(),
                    dueDateField.getValue(),
                    tagsField.getValue());
        } catch (Exception ex) {
            log.error("Failed to update business {}", business.getId(), ex);
            Notification.show(
                            getTranslation("common.errorGeneric"),
                            3000,
                            Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        Notification.show(getTranslation("business.saved"), 1500, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        UI.getCurrent().navigate("businesses/" + business.getId());
    }

    private void confirmDelete() {
        var dialog = new ConfirmDialog();
        dialog.setHeader(getTranslation("business.deleteTitle"));
        dialog.setCancelable(true);
        dialog.setText(getTranslation("business.deleteInfo"));
        dialog.setConfirmText(getTranslation("common.delete"));
        dialog.setCancelText(getTranslation("common.cancel"));
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(
                e -> {
                    businessService.delete(business.getId());
                    Notification.show(
                                    getTranslation("business.deleted"),
                                    1500,
                                    Notification.Position.BOTTOM_END)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    UI.getCurrent().navigate("businesses");
                });
        dialog.open();
    }

    private static String safe(String s) {
        return StringUtils.nvl(s);
    }
}
