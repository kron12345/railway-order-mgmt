package com.ordermgmt.railway.ui.component.timetable;

import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.activityOptionLabel;

import java.time.LocalTime;
import java.util.List;

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
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.timepicker.TimePicker;

import com.ordermgmt.railway.domain.infrastructure.model.OperationalPoint;
import com.ordermgmt.railway.domain.infrastructure.repository.OperationalPointRepository;
import com.ordermgmt.railway.domain.timetable.model.TimeConstraintMode;
import com.ordermgmt.railway.domain.timetable.model.TimetableActivityOption;
import com.ordermgmt.railway.ui.component.OperationalPointComboBox;

/**
 * Inline form for inserting a new timetable stop. Asks for OP, activity, plus the arrival and
 * departure times the user wants to enforce — both are mandatory so the caller can build a fully
 * populated row and let the editing service propagate neighbours from real anchors instead of
 * guessing a default 2-minute dwell.
 *
 * <p>Mode-aware: WINDOW takes earliest+latest, COMMERCIAL takes a single commercial value, EXACT
 * and NONE take a single time. The form refuses submit until every required field for the selected
 * mode carries a value.
 */
class AddStopForm extends Div {

    private final ComboBox<OperationalPoint> pointCombo = new ComboBox<>();
    private final ComboBox<TimetableActivityOption> activityCombo = new ComboBox<>();

    private final Select<TimeConstraintMode> arrivalMode = new Select<>();
    private final TimePicker arrivalPrimary = new TimePicker();
    private final TimePicker arrivalSecondary = new TimePicker(); // WINDOW.latest

    private final Select<TimeConstraintMode> departureMode = new Select<>();
    private final TimePicker departurePrimary = new TimePicker();
    private final TimePicker departureSecondary = new TimePicker(); // WINDOW.earliest

    private final Button addButton = new Button();
    private final Button cancelButton = new Button();
    private final Span title = new Span();
    private final Span contextHint = new Span();

    /** Index after which the new stop will be inserted. */
    private int insertAfterIndex = -1;

    AddStopForm(
            OperationalPointRepository opRepo,
            List<TimetableActivityOption> activityOptions,
            AddStopCallback onAdd) {

        setWidthFull();
        setVisible(false);
        addClassName("add-stop-form");
        getStyle()
                .set("background", "var(--rom-bg-card)")
                .set("border", "1px solid var(--rom-accent)")
                .set("border-radius", "6px")
                .set("padding", "12px 16px")
                .set("box-sizing", "border-box")
                .set("margin-top", "6px");

        title.getStyle()
                .set("font-weight", "600")
                .set("font-size", "13px")
                .set("color", "var(--rom-text-primary)")
                .set("display", "block")
                .set("margin-bottom", "4px");
        contextHint
                .getStyle()
                .set("font-size", "11px")
                .set("color", "var(--rom-text-muted)")
                .set("display", "block")
                .set("margin-bottom", "10px");

        OperationalPointComboBox.bindLazySearch(pointCombo, opRepo);
        pointCombo.setItemLabelGenerator(
                op -> op.getCountry() + " · " + op.getName() + " (" + op.getUopid() + ")");
        pointCombo.setWidthFull();
        pointCombo.setClearButtonVisible(true);
        pointCombo.setAllowCustomValue(false);
        pointCombo.setRequired(true);

        activityCombo.setItems(activityOptions);
        activityCombo.setItemLabelGenerator(opt -> activityOptionLabel(opt));
        activityCombo.setWidthFull();
        activityCombo.setRequired(true);
        activityCombo.setClearButtonVisible(true);

        configureModeSelect(arrivalMode);
        configureModeSelect(departureMode);
        arrivalMode.addValueChangeListener(e -> updateFieldVisibility());
        departureMode.addValueChangeListener(e -> updateFieldVisibility());

        arrivalPrimary.setStep(java.time.Duration.ofMinutes(1));
        arrivalSecondary.setStep(java.time.Duration.ofMinutes(1));
        departurePrimary.setStep(java.time.Duration.ofMinutes(1));
        departureSecondary.setStep(java.time.Duration.ofMinutes(1));

        addButton.setIcon(VaadinIcon.CHECK.create());
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        addButton.addClickListener(e -> submit(onAdd));

        cancelButton.setIcon(VaadinIcon.CLOSE_SMALL.create());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        cancelButton.addClickListener(e -> hide());

        HorizontalLayout opRow = new HorizontalLayout(pointCombo, activityCombo);
        opRow.setWidthFull();
        opRow.expand(pointCombo);

        HorizontalLayout arrRow =
                new HorizontalLayout(arrivalMode, arrivalPrimary, arrivalSecondary);
        arrRow.setAlignItems(FlexComponent.Alignment.END);

        HorizontalLayout depRow =
                new HorizontalLayout(departureMode, departurePrimary, departureSecondary);
        depRow.setAlignItems(FlexComponent.Alignment.END);

        HorizontalLayout actions = new HorizontalLayout(addButton, cancelButton);
        actions.setAlignItems(FlexComponent.Alignment.CENTER);
        actions.getStyle().set("margin-top", "8px");

        add(title, contextHint, opRow, arrRow, depRow, actions);
    }

    private void configureModeSelect(Select<TimeConstraintMode> select) {
        select.setItems(TimeConstraintMode.values());
        select.setValue(TimeConstraintMode.NONE);
        select.setItemLabelGenerator(this::modeLabel);
        select.setWidth("160px");
    }

    private String modeLabel(TimeConstraintMode m) {
        try {
            return getTranslation("timetable.timeMode." + m.name());
        } catch (RuntimeException ex) {
            return m.name();
        }
    }

    private void updateFieldVisibility() {
        TimeConstraintMode am = arrivalMode.getValue();
        arrivalSecondary.setVisible(am == TimeConstraintMode.WINDOW);
        arrivalPrimary.setLabel(primaryLabel(am, true));
        arrivalSecondary.setLabel(getTranslation("timetable.editor.latestArrival"));

        TimeConstraintMode dm = departureMode.getValue();
        departureSecondary.setVisible(dm == TimeConstraintMode.WINDOW);
        departurePrimary.setLabel(primaryLabel(dm, false));
        departureSecondary.setLabel(getTranslation("timetable.editor.earliestDeparture"));
    }

    private String primaryLabel(TimeConstraintMode mode, boolean isArrival) {
        return switch (mode) {
            case EXACT ->
                    getTranslation(
                            isArrival ? "timetable.editor.arrival" : "timetable.editor.departure");
            case WINDOW ->
                    getTranslation(
                            isArrival
                                    ? "timetable.editor.earliestArrival"
                                    : "timetable.editor.latestDeparture");
            case AFTER ->
                    getTranslation(
                            isArrival
                                    ? "timetable.editor.earliestArrival"
                                    : "timetable.editor.earliestDeparture");
            case BEFORE ->
                    getTranslation(
                            isArrival
                                    ? "timetable.editor.latestArrival"
                                    : "timetable.editor.latestDeparture");
            case COMMERCIAL ->
                    getTranslation(
                            isArrival
                                    ? "timetable.editor.commercialArrival"
                                    : "timetable.editor.commercialDeparture");
            case NONE ->
                    getTranslation(
                            isArrival
                                    ? "timetable.editor.estimatedArrival"
                                    : "timetable.editor.estimatedDeparture");
        };
    }

    private void submit(AddStopCallback onAdd) {
        if (pointCombo.getValue() == null || activityCombo.getValue() == null) {
            warn("timetable.stop.missingOpOrActivity");
            return;
        }
        TimeConstraintMode am = arrivalMode.getValue();
        TimeConstraintMode dm = departureMode.getValue();
        if (arrivalPrimary.getValue() == null) {
            warn("timetable.stop.missingArrival");
            return;
        }
        if (am == TimeConstraintMode.WINDOW && arrivalSecondary.getValue() == null) {
            warn("timetable.stop.missingArrivalLatest");
            return;
        }
        if (departurePrimary.getValue() == null) {
            warn("timetable.stop.missingDeparture");
            return;
        }
        if (dm == TimeConstraintMode.WINDOW && departureSecondary.getValue() == null) {
            warn("timetable.stop.missingDepartureEarliest");
            return;
        }
        // WINDOW order: earliest ≤ latest
        if (am == TimeConstraintMode.WINDOW
                && arrivalPrimary.getValue().isAfter(arrivalSecondary.getValue())) {
            warn("timetable.stop.windowOrder");
            return;
        }
        if (dm == TimeConstraintMode.WINDOW
                && departureSecondary.getValue().isAfter(departurePrimary.getValue())) {
            warn("timetable.stop.windowOrder");
            return;
        }

        StopTimes times =
                new StopTimes(
                        am,
                        arrivalPrimary.getValue(),
                        arrivalSecondary.getValue(),
                        dm,
                        departurePrimary.getValue(),
                        departureSecondary.getValue());
        onAdd.accept(
                insertAfterIndex, pointCombo.getValue(), activityCombo.getValue().code(), times);
        hide();
    }

    private void warn(String key) {
        Notification.show(getTranslation(key), 2500, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    /** Show the form. {@code afterRowName} is the visible name of the row we insert AFTER. */
    void show(int afterIndex, String afterRowName) {
        this.insertAfterIndex = afterIndex;
        pointCombo.setValue(null);
        activityCombo.setValue(null);
        arrivalMode.setValue(TimeConstraintMode.NONE);
        departureMode.setValue(TimeConstraintMode.NONE);
        arrivalPrimary.setValue(null);
        arrivalSecondary.setValue(null);
        departurePrimary.setValue(null);
        departureSecondary.setValue(null);

        pointCombo.setLabel(getTranslation("timetable.stop.selectPoint"));
        activityCombo.setLabel(getTranslation("timetable.editor.activity"));
        addButton.setText(getTranslation("timetable.stop.add"));
        cancelButton.setText(getTranslation("common.cancel"));
        arrivalMode.setLabel(getTranslation("timetable.editor.arrivalMode"));
        departureMode.setLabel(getTranslation("timetable.editor.departureMode"));
        title.setText(getTranslation("timetable.stop.addAfter") + " — " + afterRowName);
        contextHint.setText(getTranslation("timetable.stop.addAfter.hint"));
        updateFieldVisibility();

        setVisible(true);
        pointCombo.focus();
    }

    void hide() {
        setVisible(false);
        insertAfterIndex = -1;
    }

    /** Time inputs the form collects per side. */
    record StopTimes(
            TimeConstraintMode arrivalMode,
            LocalTime arrivalPrimary,
            LocalTime arrivalSecondary,
            TimeConstraintMode departureMode,
            LocalTime departurePrimary,
            LocalTime departureSecondary) {}

    @FunctionalInterface
    interface AddStopCallback {
        void accept(
                int insertAfterIndex, OperationalPoint point, String activityCode, StopTimes times);
    }
}
