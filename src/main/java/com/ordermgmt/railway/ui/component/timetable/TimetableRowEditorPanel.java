package com.ordermgmt.railway.ui.component.timetable;

import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.activityOptionLabel;
import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.findActivityOption;
import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.firstNonBlank;
import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.formatTime;
import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.nvl;
import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.parseTime;
import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.roleLabel;

import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;

import com.ordermgmt.railway.domain.timetable.model.JourneyLocationType;
import com.ordermgmt.railway.domain.timetable.model.TimeConstraintMode;
import com.ordermgmt.railway.domain.timetable.model.TimePropagationMode;
import com.ordermgmt.railway.domain.timetable.model.TimetableActivityOption;
import com.ordermgmt.railway.domain.timetable.model.TimetableRowData;

/**
 * Right-side editor panel for a single timetable row.
 *
 * <p>Owns the row-level fields (halt/dwell, activity, journey-location type, propagation) and
 * delegates the arrival and departure time-constraint inputs to two symmetric {@link
 * TimetableTimeConstraintFields} instances. Changes are applied back to the {@link
 * TimetableRowData} via {@link #syncToRow}.
 *
 * <p>Extracted from {@link TimetableTableStep} to keep file sizes manageable.
 */
class TimetableRowEditorPanel extends Div {

    private static final int TIME_VALIDATION_NOTIFICATION_DURATION_MS = 3000;

    private static final Set<String> VEHICLE_LINK_ACTIVITIES =
            Set.of("0010", "0011", "0012", "0013", "0014", "0015", "0016", "0017", "0044", "0045");

    private final List<TimetableActivityOption> activityOptions;

    // ── Row-level fields ──────────────────────────────────────────────────

    private final Span titleLabel = new Span();
    private final Span contextLabel = new Span();
    private final Select<JourneyLocationType> journeyLocationTypeField = new Select<>();
    private final Checkbox tttRelevantField = new Checkbox();
    private final Checkbox haltField = new Checkbox();
    private final IntegerField dwellMinutesField = new IntegerField();
    private final MultiSelectComboBox<TimetableActivityOption> activityField =
            new MultiSelectComboBox<>();
    private final TextField associatedTrainField = new TextField();
    private final TextField locationSubsidiaryField = new TextField();
    private final TextArea networkSpecificParametersField = new TextArea();

    // ── Time constraints (one symmetric component per side) ───────────────

    private final TimetableTimeConstraintFields arrivalFields;
    private final TimetableTimeConstraintFields departureFields;

    // ── Propagation ───────────────────────────────────────────────────────

    private final Select<TimePropagationMode> propagationModeField = new Select<>();
    private final Checkbox pinnedField = new Checkbox();

    TimetableRowEditorPanel(List<TimetableActivityOption> activityOptions, Runnable onApply) {
        this.activityOptions = activityOptions;
        this.arrivalFields = new TimetableTimeConstraintFields(true, this);
        this.departureFields = new TimetableTimeConstraintFields(false, this);
        setWidthFull();
        buildLayout(onApply);
        setVisible(false);
    }

    // ── Public accessors for TimetableTableStep ───────────────────────

    /** Returns the selected time propagation mode (SHIFT or STRETCH). */
    TimePropagationMode getPropagationMode() {
        return propagationModeField.getValue();
    }

    /** Populates the editor fields from the given row. */
    void populate(TimetableRowData row, boolean isOrigin, boolean isDestination) {
        if (row == null) {
            setVisible(false);
            return;
        }
        setVisible(true);
        titleLabel.setText(
                row.getSequence()
                        + ". "
                        + row.getName()
                        + " ("
                        + roleLabel(row.getRoutePointRole(), this)
                        + ")");
        contextLabel.setText(
                t("timetable.editor.context", nvl(row.getFromName()), nvl(row.getToName())));
        journeyLocationTypeField.setValue(
                JourneyLocationType.fromString(row.getJourneyLocationType()));
        journeyLocationTypeField.setEnabled(!isOrigin && !isDestination);
        tttRelevantField.setValue(Boolean.TRUE.equals(row.getTttRelevant()));
        haltField.setValue(Boolean.TRUE.equals(row.getHalt()));
        dwellMinutesField.setValue(row.getDwellMinutes());
        activityField.setValue(activityOptionsForRow(row));
        associatedTrainField.setValue(
                row.getAssociatedTrainOtn() != null ? row.getAssociatedTrainOtn() : "");
        locationSubsidiaryField.setValue(
                row.getLocationSubsidiaryCode() != null ? row.getLocationSubsidiaryCode() : "");
        networkSpecificParametersField.setValue(
                row.getNetworkSpecificParametersText() != null
                        ? row.getNetworkSpecificParametersText()
                        : "");
        updateAssociatedTrainVisibility();

        arrivalFields.populate(row);
        departureFields.populate(row);
        pinnedField.setValue(Boolean.TRUE.equals(row.getPinned()));

        // Origin and destination are implicit halts (the train stops there by definition) —
        // they have no halt-checkbox or dwell-input. Origin only allows departure-side
        // constraints, destination only arrival-side. Per TTT spec: §3.6 + §5.7.
        boolean isEndpoint = isOrigin || isDestination;
        haltField.setVisible(!isEndpoint);
        boolean halt = isEndpoint || Boolean.TRUE.equals(row.getHalt());
        if (isEndpoint) {
            haltField.setValue(true);
        }
        arrivalFields.setSectionVisible(!isOrigin && halt);
        departureFields.setSectionVisible(!isDestination && halt);
        // No dwell at origin (no arrival to compute from) or destination (no departure).
        dwellMinutesField.setVisible(halt && !isEndpoint);
        activityField.setVisible(halt);
        if (halt && activityField.getSelectedItems().isEmpty()) {
            activityField.getStyle().set("background", "rgba(250,204,21,0.15)");
            activityField.setRequiredIndicatorVisible(true);
        } else {
            activityField.getStyle().remove("background");
        }
    }

    /**
     * Writes the editor field values back to the given row.
     *
     * @return false on validation failure
     */
    boolean syncToRow(
            TimetableRowData row,
            boolean isOrigin,
            boolean isDestination,
            boolean showNotifications) {
        if (row == null) {
            return true;
        }
        // Origin and destination are implicit halts with no dwell — UI hides both controls,
        // so we lock the data here regardless of whatever the (hidden) checkbox holds.
        boolean endpoint = isOrigin || isDestination;
        boolean halt = endpoint || Boolean.TRUE.equals(haltField.getValue());
        row.setHalt(halt);
        Integer dwellValue = (halt && !endpoint) ? dwellMinutesField.getValue() : null;
        row.setDwellMinutes(dwellValue);
        // userEnteredDwell mirrors whether the field has a non-null value on a non-endpoint halt.
        row.setUserEnteredDwell(dwellValue != null);
        row.setPinned(Boolean.TRUE.equals(pinnedField.getValue()));
        row.setTttRelevant(Boolean.TRUE.equals(tttRelevantField.getValue()));
        JourneyLocationType locationType = journeyLocationTypeField.getValue();
        if (locationType != null) {
            row.setJourneyLocationType(locationType.name());
        }
        row.setLocationSubsidiaryCode(blankToNull(locationSubsidiaryField.getValue()));
        row.setNetworkSpecificParametersText(
                blankToNull(networkSpecificParametersField.getValue()));

        if (!arrivalFields.writeToRow(row, showNotifications)) {
            return false;
        }
        if (!departureFields.writeToRow(row, showNotifications)) {
            return false;
        }

        if (Boolean.TRUE.equals(row.getHalt())) {
            if (activityField.getSelectedItems().isEmpty()) {
                if (showNotifications) {
                    Notification.show(
                                    t("timetable.stop.activityRequired"),
                                    TIME_VALIDATION_NOTIFICATION_DURATION_MS,
                                    Position.BOTTOM_END)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
                return false;
            }
            List<String> activityCodes = selectedActivityCodes();
            row.setActivityCodes(activityCodes);
            row.setActivityCode(activityCodes.isEmpty() ? null : activityCodes.getFirst());
            String otnVal = associatedTrainField.getValue();
            row.setAssociatedTrainOtn(otnVal != null && !otnVal.isBlank() ? otnVal.trim() : null);
            ensureStopTimes(row, isOrigin, isDestination);
        } else {
            row.setActivityCode(null);
            row.setActivityCodes(List.of());
            row.setAssociatedTrainOtn(null);
        }
        return true;
    }

    // ── Private build ─────────────────────────────────────────────────

    private void buildLayout(Runnable onApply) {
        getStyle()
                .set("background", "var(--rom-bg-card)")
                .set("border", "1px solid var(--rom-border)")
                .set("border-radius", "6px")
                .set("padding", "16px")
                .set("box-sizing", "border-box")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "12px");

        titleLabel
                .getStyle()
                .set("font-size", "15px")
                .set("font-weight", "600")
                .set("color", "var(--rom-text-primary)");
        contextLabel
                .getStyle()
                .set("font-size", "12px")
                .set("font-family", "'JetBrains Mono', monospace")
                .set("color", "var(--rom-text-muted)");

        configureHaltField();
        configureTttLocationFields();
        configureActivityField();
        configureAssociatedTrainField();
        configureLocationSubsidiaryField();
        configureNetworkSpecificParametersField();
        configureDwellField();

        Div propagationSection = buildPropagationSection();

        HorizontalLayout rowFlags =
                new HorizontalLayout(tttRelevantField, haltField, dwellMinutesField);
        rowFlags.setWidthFull();
        rowFlags.expand(dwellMinutesField);
        rowFlags.setAlignItems(FlexComponent.Alignment.END);

        Button applyBtn = new Button(t("common.apply"), VaadinIcon.CHECK.create());
        applyBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        applyBtn.getStyle()
                .set("background", "var(--rom-accent)")
                .set("color", "var(--rom-bg-primary)");
        applyBtn.addClickListener(e -> onApply.run());

        add(
                titleLabel,
                contextLabel,
                journeyLocationTypeField,
                rowFlags,
                activityField,
                associatedTrainField,
                locationSubsidiaryField,
                networkSpecificParametersField,
                arrivalFields,
                departureFields,
                propagationSection,
                applyBtn);
    }

    private void configureHaltField() {
        haltField.setLabel(t("timetable.editor.halt"));
        haltField.addValueChangeListener(
                e -> {
                    boolean halt = Boolean.TRUE.equals(e.getValue());
                    activityField.setVisible(halt);
                    applyHaltVisibility(halt);
                    if (halt) {
                        activityField.getStyle().set("background", "rgba(250,204,21,0.15)");
                        activityField.setRequiredIndicatorVisible(true);
                        // Auto-select first activity if none selected
                        if (activityField.getSelectedItems().isEmpty()
                                && !activityOptions.isEmpty()) {
                            activityField.setValue(Set.of(activityOptions.getFirst()));
                        }
                    } else {
                        // Pass-through: clear any half-typed times so they aren't accidentally
                        // exported
                        clearAllTimeFields();
                    }
                });
    }

    /**
     * Halt=false (Durchfahrt) → arrival/departure constraint sections + dwell field hidden, per
     * Regel 3 (no time inputs allowed without halt). Pin and propagation stay visible because
     * pinning a pass-through is allowed (Edge-Case #5).
     */
    private void applyHaltVisibility(boolean halt) {
        arrivalFields.setSectionVisible(halt);
        departureFields.setSectionVisible(halt);
        dwellMinutesField.setVisible(halt);
    }

    /** Clear all editor time inputs and their displays — used when halt is unchecked. */
    private void clearAllTimeFields() {
        arrivalFields.clearFields();
        departureFields.clearFields();
        dwellMinutesField.clear();
    }

    private void configureActivityField() {
        activityField.setLabel(t("timetable.editor.activity"));
        activityField.setItems(activityOptions);
        activityField.setItemLabelGenerator(opt -> activityOptionLabel(opt));
        activityField.setWidthFull();
        activityField.setClearButtonVisible(true);
        activityField.addValueChangeListener(
                e -> {
                    if (!e.getValue().isEmpty()) {
                        activityField.getStyle().remove("background");
                    } else if (Boolean.TRUE.equals(haltField.getValue())) {
                        activityField.getStyle().set("background", "rgba(250,204,21,0.15)");
                    }
                    updateAssociatedTrainVisibility();
                });
    }

    private void configureAssociatedTrainField() {
        associatedTrainField.setLabel(t("timetable.editor.associatedTrain"));
        associatedTrainField.setHelperText(t("timetable.editor.associatedTrain.help"));
        associatedTrainField.setWidthFull();
        associatedTrainField.setMaxLength(20);
        associatedTrainField.setVisible(false);
    }

    private void updateAssociatedTrainVisibility() {
        boolean show =
                activityField.getSelectedItems().stream()
                        .anyMatch(selected -> VEHICLE_LINK_ACTIVITIES.contains(selected.code()));
        associatedTrainField.setVisible(show);
    }

    private void configureTttLocationFields() {
        journeyLocationTypeField.setLabel(t("timetable.editor.journeyLocationType"));
        journeyLocationTypeField.setItems(JourneyLocationType.values());
        journeyLocationTypeField.setItemLabelGenerator(type -> type.code() + " · " + type.label());
        journeyLocationTypeField.setValue(JourneyLocationType.INTERMEDIATE);
        journeyLocationTypeField.setWidthFull();

        tttRelevantField.setLabel(t("timetable.editor.tttRelevant"));
        tttRelevantField.setHelperText(t("timetable.editor.tttRelevant.help"));
    }

    private void configureLocationSubsidiaryField() {
        locationSubsidiaryField.setLabel(t("timetable.editor.locationSubsidiary"));
        locationSubsidiaryField.setHelperText(t("timetable.editor.locationSubsidiary.help"));
        locationSubsidiaryField.setMaxLength(50);
        locationSubsidiaryField.setWidthFull();
    }

    private void configureNetworkSpecificParametersField() {
        networkSpecificParametersField.setLabel(t("timetable.editor.networkSpecificParameters"));
        networkSpecificParametersField.setHelperText(
                t("timetable.editor.networkSpecificParameters.help"));
        networkSpecificParametersField.setWidthFull();
        networkSpecificParametersField.setMinHeight("88px");
    }

    private void configureDwellField() {
        dwellMinutesField.setLabel(t("timetable.editor.dwell"));
        dwellMinutesField.setMin(0);
        dwellMinutesField.setStepButtonsVisible(true);
        dwellMinutesField.setWidthFull();
    }

    private Div buildPropagationSection() {
        Div section = new Div();
        section.getStyle()
                .set("padding", "12px")
                .set("border", "1px solid var(--rom-border)")
                .set("border-radius", "6px")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "10px");
        propagationModeField.setLabel(t("timetable.time.propagation"));
        propagationModeField.setItems(TimePropagationMode.values());
        propagationModeField.setItemLabelGenerator(
                mode ->
                        mode == TimePropagationMode.SHIFT
                                ? t("timetable.time.shift")
                                : t("timetable.time.stretch"));
        propagationModeField.setValue(TimePropagationMode.STRETCH);
        propagationModeField.setWidthFull();

        pinnedField.setLabel(t("timetable.time.pinned"));

        section.add(
                sectionHeader(t("timetable.time.propagation")), propagationModeField, pinnedField);
        return section;
    }

    // ── Sync helpers ──────────────────────────────────────────────────

    private void ensureStopTimes(TimetableRowData row, boolean isOrigin, boolean isDestination) {
        if (!isOrigin && row.getArrivalMode() == TimeConstraintMode.NONE) {
            row.setArrivalMode(TimeConstraintMode.EXACT);
            row.setArrivalExact(firstNonBlank(row.getArrivalExact(), row.getEstimatedArrival()));
        }
        if (!isDestination && row.getDepartureMode() == TimeConstraintMode.NONE) {
            row.setDepartureMode(TimeConstraintMode.EXACT);
            String fallback = row.getEstimatedDeparture();
            if (fallback == null
                    && row.getEstimatedArrival() != null
                    && row.getDwellMinutes() != null) {
                LocalTime estimated = parseTime(row.getEstimatedArrival());
                if (estimated != null) {
                    fallback = formatTime(estimated.plusMinutes(row.getDwellMinutes()));
                }
            }
            row.setDepartureExact(firstNonBlank(row.getDepartureExact(), fallback));
        }
    }

    private Span sectionHeader(String text) {
        Span s = new Span(text);
        s.getStyle().set("font-weight", "600").set("color", "var(--rom-text-primary)");
        return s;
    }

    private Set<TimetableActivityOption> activityOptionsForRow(TimetableRowData row) {
        Set<TimetableActivityOption> selected = new LinkedHashSet<>();
        if (row.getActivityCodes() != null) {
            for (String code : row.getActivityCodes()) {
                findActivityOption(code, activityOptions).ifPresent(selected::add);
            }
        }
        if (selected.isEmpty()) {
            findActivityOption(row.getActivityCode(), activityOptions).ifPresent(selected::add);
        }
        return selected;
    }

    private List<String> selectedActivityCodes() {
        return activityField.getSelectedItems().stream()
                .map(TimetableActivityOption::code)
                .distinct()
                .collect(Collectors.toList());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String t(String key, Object... params) {
        return getTranslation(key, params);
    }
}
