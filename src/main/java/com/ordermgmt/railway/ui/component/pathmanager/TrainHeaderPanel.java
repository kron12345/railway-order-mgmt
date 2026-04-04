package com.ordermgmt.railway.ui.component.pathmanager;

import java.util.function.BiFunction;
import java.util.function.Consumer;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
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

import com.ordermgmt.railway.domain.pathmanager.model.PmReferenceTrain;
import com.ordermgmt.railway.domain.pathmanager.model.TrainHeaderUpdate;
import com.ordermgmt.railway.domain.pathmanager.service.PathManagerService;
import com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils;

/** Editable header form for a selected reference train. */
public class TrainHeaderPanel extends VerticalLayout {

    private final PmReferenceTrain train;
    private final PathManagerService pathManagerService;
    private final BiFunction<String, Object[], String> translator;
    private final Consumer<PmReferenceTrain> onSaved;

    private TextField otnField;
    private TextField trainTypeField;
    private TextField trafficTypeField;
    private IntegerField weightField;
    private IntegerField lengthField;
    private IntegerField maxSpeedField;
    private DatePicker calendarStartField;
    private DatePicker calendarEndField;

    public TrainHeaderPanel(
            PmReferenceTrain train,
            PathManagerService pathManagerService,
            BiFunction<String, Object[], String> translator,
            Consumer<PmReferenceTrain> onSaved) {
        this.train = train;
        this.pathManagerService = pathManagerService;
        this.translator = translator;
        this.onSaved = onSaved;
        setPadding(false);
        setSpacing(false);
        buildPanel();
    }

    private String t(String key) {
        return translator.apply(key, null);
    }

    private void buildPanel() {
        Div card = TimetableFormatUtils.createCard();

        Span title = new Span(t("pm.train") + " " + t("common.edit"));
        title.getStyle()
                .set("font-size", "var(--lumo-font-size-l)")
                .set("font-weight", "600")
                .set("color", "var(--rom-text-primary)")
                .set("margin-bottom", "var(--lumo-space-s)");
        card.add(title);

        card.add(createFormFields());
        card.add(createSaveButton());
        add(card);
    }

    private Div createFormFields() {
        Div formContainer = new Div();
        formContainer.setWidthFull();
        formContainer
                .getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "1fr 1fr")
                .set("gap", "var(--lumo-space-s)")
                .set("margin-bottom", "var(--lumo-space-s)");

        otnField = new TextField(t("pm.otn"));
        otnField.setValue(safeString(train.getOperationalTrainNumber()));
        otnField.setWidthFull();
        applyMonoStyle(otnField);

        trainTypeField = new TextField(t("pm.trainType"));
        trainTypeField.setValue(safeString(train.getTrainType()));
        trainTypeField.setWidthFull();

        trafficTypeField = new TextField(t("pm.trafficType"));
        trafficTypeField.setValue(safeString(train.getTrafficTypeCode()));
        trafficTypeField.setWidthFull();

        weightField = new IntegerField(t("pm.weight"));
        weightField.setValue(train.getTrainWeight());
        weightField.setWidthFull();

        lengthField = new IntegerField(t("pm.length"));
        lengthField.setValue(train.getTrainLength());
        lengthField.setWidthFull();

        maxSpeedField = new IntegerField(t("pm.maxSpeed"));
        maxSpeedField.setValue(train.getTrainMaxSpeed());
        maxSpeedField.setWidthFull();

        calendarStartField = new DatePicker(t("pm.calendarStart"));
        calendarStartField.setValue(train.getCalendarStart());
        calendarStartField.setWidthFull();

        calendarEndField = new DatePicker(t("pm.calendarEnd"));
        calendarEndField.setValue(train.getCalendarEnd());
        calendarEndField.setWidthFull();

        formContainer.add(
                otnField,
                trainTypeField,
                trafficTypeField,
                weightField,
                lengthField,
                maxSpeedField,
                calendarStartField,
                calendarEndField);
        return formContainer;
    }

    private HorizontalLayout createSaveButton() {
        Button save = new Button(t("pm.save"), VaadinIcon.CHECK.create());
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        save.getStyle()
                .set("background", "var(--rom-accent)")
                .set("color", "var(--rom-bg-primary)")
                .set("font-weight", "600");
        save.addClickListener(e -> saveTrain());

        HorizontalLayout row = new HorizontalLayout(save);
        row.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        row.setWidthFull();
        return row;
    }

    private void saveTrain() {
        pathManagerService.updateTrainHeader(
                new TrainHeaderUpdate(
                        train.getId(),
                        otnField.getValue(),
                        trainTypeField.getValue(),
                        trafficTypeField.getValue(),
                        weightField.getValue(),
                        lengthField.getValue(),
                        maxSpeedField.getValue(),
                        null));
        Notification.show(t("pm.save"), 2000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        if (onSaved != null) {
            onSaved.accept(train);
        }
    }

    private void applyMonoStyle(TextField field) {
        field.getStyle()
                .set("--vaadin-input-field-value-font-family", "'JetBrains Mono', monospace")
                .set("--vaadin-input-field-value-font-size", "12px");
    }

    private String safeString(String value) {
        return value != null ? value : "";
    }
}
