package com.ordermgmt.railway.ui.component.pathmanager;

import java.util.function.BiFunction;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;

import com.ordermgmt.railway.domain.pathmanager.model.PmJourneyLocation;
import com.ordermgmt.railway.domain.pathmanager.service.PathManagerService;
import com.ordermgmt.railway.domain.timetable.model.JourneyLocationType;
import com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils;

/** Editable form for a selected journey location (operational point). */
public class JourneyLocationPanel extends VerticalLayout {

    private final PmJourneyLocation location;
    private final PathManagerService pathManagerService;
    private final BiFunction<String, Object[], String> translator;

    private TextField locationNameField;
    private ComboBox<JourneyLocationType> locationTypeField;
    private TextField arrivalTimeField;
    private TextField departureTimeField;
    private ComboBox<String> arrivalQualifierField;
    private ComboBox<String> departureQualifierField;
    private IntegerField dwellField;
    private TextField activitiesField;
    private TextField associatedTrainOtnField;
    private TextField trackField;

    public JourneyLocationPanel(
            PmJourneyLocation location,
            PathManagerService pathManagerService,
            BiFunction<String, Object[], String> translator) {
        this.location = location;
        this.pathManagerService = pathManagerService;
        this.translator = translator;
        setPadding(false);
        setSpacing(false);
        buildPanel();
    }

    private String t(String key) {
        return translator.apply(key, null);
    }

    private void buildPanel() {
        Div card = TimetableFormatUtils.createCard();

        Span title = new Span(t("pm.location"));
        title.getStyle()
                .set("font-size", "var(--lumo-font-size-l)")
                .set("font-weight", "600")
                .set("color", "var(--rom-text-primary)")
                .set("margin-bottom", "var(--lumo-space-s)");
        card.add(title);

        Span locationLabel = new Span(safeString(location.getPrimaryLocationName()));
        locationLabel
                .getStyle()
                .set("font-family", "'JetBrains Mono', monospace")
                .set("font-size", "14px")
                .set("font-weight", "600")
                .set("color", "var(--rom-accent)")
                .set("margin-bottom", "var(--lumo-space-s)")
                .set("display", "block");
        card.add(locationLabel);

        card.add(createFormFields());
        card.add(createSaveButton());
        add(card);
    }

    private Div createFormFields() {
        Div form = new Div();
        form.setWidthFull();
        form.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "1fr 1fr")
                .set("gap", "var(--lumo-space-s)")
                .set("margin-bottom", "var(--lumo-space-s)");

        locationNameField = new TextField(t("pm.location"));
        locationNameField.setValue(safeString(location.getPrimaryLocationName()));
        locationNameField.setWidthFull();
        locationNameField.setReadOnly(true);

        locationTypeField = new ComboBox<>(t("pm.location") + " Type");
        locationTypeField.setItems(JourneyLocationType.values());
        locationTypeField.setItemLabelGenerator(type -> type.code() + " \u2014 " + type.label());
        locationTypeField.setValue(
                JourneyLocationType.fromString(location.getJourneyLocationType()));
        locationTypeField.setWidthFull();

        arrivalTimeField = new TextField(t("timetable.table.arrival"));
        arrivalTimeField.setValue(safeString(location.getArrivalTime()));
        arrivalTimeField.setWidthFull();
        applyMonoStyle(arrivalTimeField);

        departureTimeField = new TextField(t("timetable.table.departure"));
        departureTimeField.setValue(safeString(location.getDepartureTime()));
        departureTimeField.setWidthFull();
        applyMonoStyle(departureTimeField);

        arrivalQualifierField = new ComboBox<>(t("timetable.editor.arrivalMode"));
        arrivalQualifierField.setItems("ALA", "ELA", "LLA", "PLA");
        arrivalQualifierField.setValue(location.getArrivalQualifier());
        arrivalQualifierField.setWidthFull();
        arrivalQualifierField.addValueChangeListener(
                e -> updateTimeFieldLabel(arrivalTimeField, e.getValue(), true));
        updateTimeFieldLabel(arrivalTimeField, location.getArrivalQualifier(), true);

        departureQualifierField = new ComboBox<>(t("timetable.editor.departureMode"));
        departureQualifierField.setItems("ALD", "ELD", "LLD", "PLD");
        departureQualifierField.setValue(location.getDepartureQualifier());
        departureQualifierField.setWidthFull();
        departureQualifierField.addValueChangeListener(
                e -> updateTimeFieldLabel(departureTimeField, e.getValue(), false));
        updateTimeFieldLabel(departureTimeField, location.getDepartureQualifier(), false);

        dwellField = new IntegerField(t("timetable.editor.dwell"));
        dwellField.setValue(location.getDwellTime());
        dwellField.setWidthFull();

        activitiesField = new TextField(t("timetable.table.activity"));
        activitiesField.setValue(safeString(location.getActivities()));
        activitiesField.setWidthFull();

        associatedTrainOtnField = new TextField(t("pm.associatedTrain"));
        associatedTrainOtnField.setValue(safeString(location.getAssociatedTrainOtn()));
        associatedTrainOtnField.setWidthFull();
        associatedTrainOtnField.setMaxLength(20);

        trackField = new TextField("Track");
        trackField.setValue(safeString(location.getSubsidiaryCode()));
        trackField.setWidthFull();
        applyMonoStyle(trackField);

        form.add(
                locationNameField,
                locationTypeField,
                arrivalTimeField,
                arrivalQualifierField,
                departureTimeField,
                departureQualifierField,
                dwellField,
                activitiesField,
                associatedTrainOtnField,
                trackField);
        return form;
    }

    private HorizontalLayout createSaveButton() {
        Button save = new Button(t("pm.save"), VaadinIcon.CHECK.create());
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        save.getStyle()
                .set("background", "var(--rom-accent)")
                .set("color", "var(--rom-bg-primary)")
                .set("font-weight", "600");
        save.addClickListener(e -> saveLocation());

        HorizontalLayout row = new HorizontalLayout(save);
        row.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        row.setWidthFull();
        return row;
    }

    private void saveLocation() {
        pathManagerService.updateJourneyLocation(
                location.getId(),
                arrivalTimeField.getValue(),
                departureTimeField.getValue(),
                dwellField.getValue(),
                arrivalQualifierField.getValue(),
                departureQualifierField.getValue(),
                activitiesField.getValue(),
                locationTypeField.getValue() != null ? locationTypeField.getValue().code() : null,
                trackField.getValue(),
                associatedTrainOtnField.getValue());
        Notification.show(t("pm.save"), 2000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void applyMonoStyle(TextField field) {
        field.getStyle()
                .set("--vaadin-input-field-value-font-family", "'JetBrains Mono', monospace")
                .set("--vaadin-input-field-value-font-size", "12px");
    }

    private void updateTimeFieldLabel(TextField timeField, String qualifier, boolean isArrival) {
        if (qualifier == null || qualifier.isBlank()) {
            timeField.setLabel(
                    isArrival ? t("timetable.table.arrival") : t("timetable.table.departure"));
            return;
        }
        String label =
                switch (qualifier) {
                    case "ELA" -> t("timetable.editor.earliestArrival");
                    case "LLA" -> t("timetable.editor.latestArrival");
                    case "ELD" -> t("timetable.editor.earliestDeparture");
                    case "LLD" -> t("timetable.editor.latestDeparture");
                    default ->
                            isArrival
                                    ? t("timetable.table.arrival") + " [" + qualifier + "]"
                                    : t("timetable.table.departure") + " [" + qualifier + "]";
                };
        timeField.setLabel(label);
    }

    private String safeString(String value) {
        return value != null ? value : "";
    }
}
