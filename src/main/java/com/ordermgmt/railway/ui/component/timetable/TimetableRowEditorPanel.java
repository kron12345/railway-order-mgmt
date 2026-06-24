package com.ordermgmt.railway.ui.component.timetable;

import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.activityOptionLabel;
import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.defaultMode;
import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.findActivityOption;
import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.firstNonBlank;
import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.formatTime;
import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.nvl;
import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.parseTime;
import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.roleLabel;
import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.timeModeLabel;
import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.timeOrDash;
import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.timingQualifierCode;

import java.time.Duration;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
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
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.timepicker.TimePicker;

import com.ordermgmt.railway.domain.timetable.model.JourneyLocationType;
import com.ordermgmt.railway.domain.timetable.model.TimeConstraintMode;
import com.ordermgmt.railway.domain.timetable.model.TimePropagationMode;
import com.ordermgmt.railway.domain.timetable.model.TimetableActivityOption;
import com.ordermgmt.railway.domain.timetable.model.TimetableRowData;

/**
 * Right-side editor panel for a single timetable row.
 *
 * <p>Provides fields for halt/dwell, arrival/departure time constraints (exact, window,
 * commercial), time propagation mode, and activity selection. Changes are applied back to the
 * {@link TimetableRowData} via {@link #syncToRow}.
 *
 * <p>Extracted from {@link TimetableTableStep} to keep file sizes manageable.
 */
class TimetableRowEditorPanel extends Div {

    private final List<TimetableActivityOption> activityOptions;

    // ── Header fields ────────────────────────────────────────────────────

    private final Span titleLabel = new Span();
    private final Span contextLabel = new Span();
    private final Span arrivalEstimateLabel = new Span();
    private final Span departureEstimateLabel = new Span();
    private final Select<JourneyLocationType> journeyLocationTypeField = new Select<>();
    private final Checkbox tttRelevantField = new Checkbox();
    private final Checkbox haltField = new Checkbox();
    private final com.vaadin.flow.component.textfield.IntegerField dwellMinutesField =
            new com.vaadin.flow.component.textfield.IntegerField();
    private final MultiSelectComboBox<TimetableActivityOption> activityField =
            new MultiSelectComboBox<>();
    private final TextField associatedTrainField = new TextField();
    private final TextField locationSubsidiaryField = new TextField();
    private final TextArea networkSpecificParametersField = new TextArea();

    // ── Arrival Section ──────────────────────────────────────────────────

    private final Select<TimeConstraintMode> arrivalModeField = new Select<>();
    private final TimePicker arrivalExactField = createTimePicker();
    private final TimePicker arrivalEarliestField = createTimePicker();
    private final TimePicker arrivalLatestField = createTimePicker();
    private final TimePicker arrivalCommercialField = createTimePicker();
    private final Div arrivalExactWrapper = new Div();
    private final Div arrivalWindowWrapper = new Div();
    private final Div arrivalCommercialWrapper = new Div();
    private final Div arrivalSection = new Div();
    private final com.vaadin.flow.component.textfield.IntegerField arrivalOffsetField =
            new com.vaadin.flow.component.textfield.IntegerField();

    // ── Departure Section ────────────────────────────────────────────────

    private final Select<TimeConstraintMode> departureModeField = new Select<>();
    private final TimePicker departureExactField = createTimePicker();
    private final TimePicker departureEarliestField = createTimePicker();
    private final TimePicker departureLatestField = createTimePicker();
    private final TimePicker departureCommercialField = createTimePicker();
    private final Div departureExactWrapper = new Div();
    private final Div departureWindowWrapper = new Div();
    private final Div departureCommercialWrapper = new Div();
    private final Div departureSection = new Div();
    private final com.vaadin.flow.component.textfield.IntegerField departureOffsetField =
            new com.vaadin.flow.component.textfield.IntegerField();

    // ── Propagation ──────────────────────────────────────────────────────

    private final Select<TimePropagationMode> propagationModeField = new Select<>();
    private final Checkbox pinnedField = new Checkbox();

    TimetableRowEditorPanel(List<TimetableActivityOption> activityOptions, Runnable onApply) {
        this.activityOptions = activityOptions;
        setWidthFull();
        buildLayout(onApply);
        setVisible(false);
        updateModeVisibility();
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

        populateEstimateLabel(arrivalEstimateLabel, row, true);
        populateEstimateLabel(departureEstimateLabel, row, false);

        arrivalModeField.setValue(defaultMode(row.getArrivalMode()));
        departureModeField.setValue(defaultMode(row.getDepartureMode()));
        arrivalExactField.setValue(parseTime(row.getArrivalExact()));
        arrivalEarliestField.setValue(parseTime(row.getArrivalEarliest()));
        arrivalLatestField.setValue(parseTime(row.getArrivalLatest()));
        arrivalCommercialField.setValue(parseTime(row.getCommercialArrival()));
        departureExactField.setValue(parseTime(row.getDepartureExact()));
        departureEarliestField.setValue(parseTime(row.getDepartureEarliest()));
        departureLatestField.setValue(parseTime(row.getDepartureLatest()));
        departureCommercialField.setValue(parseTime(row.getCommercialDeparture()));
        arrivalOffsetField.setValue(row.getArrivalOffset() == null ? 0 : row.getArrivalOffset());
        departureOffsetField.setValue(
                row.getDepartureOffset() == null ? 0 : row.getDepartureOffset());
        pinnedField.setValue(Boolean.TRUE.equals(row.getPinned()));

        // Clear any leftover invalid states from a previously selected row.
        clearInvalid(
                arrivalExactField,
                arrivalEarliestField,
                arrivalLatestField,
                arrivalCommercialField,
                departureExactField,
                departureEarliestField,
                departureLatestField,
                departureCommercialField);

        // Origin and destination are implicit halts (the train stops there by definition) —
        // they have no halt-checkbox or dwell-input. Origin only allows departure-side
        // constraints, destination only arrival-side. Per TTT spec: §3.6 + §5.7.
        boolean isEndpoint = isOrigin || isDestination;
        haltField.setVisible(!isEndpoint);
        boolean halt = isEndpoint || Boolean.TRUE.equals(row.getHalt());
        if (isEndpoint) {
            haltField.setValue(true);
        }
        arrivalSection.setVisible(!isOrigin && halt);
        departureSection.setVisible(!isDestination && halt);
        // No dwell at origin (no arrival to compute from) or destination (no departure).
        dwellMinutesField.setVisible(halt && !isEndpoint);
        activityField.setVisible(halt);
        if (halt && activityField.getSelectedItems().isEmpty()) {
            activityField.getStyle().set("background", "rgba(250,204,21,0.15)");
            activityField.setRequiredIndicatorVisible(true);
        } else {
            activityField.getStyle().remove("background");
        }
        updateModeVisibility();
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
        row.setArrivalOffset(
                arrivalOffsetField.getValue() == null ? 0 : arrivalOffsetField.getValue());
        row.setDepartureOffset(
                departureOffsetField.getValue() == null ? 0 : departureOffsetField.getValue());

        if (!writeTimeMode(
                row,
                true,
                arrivalModeField.getValue(),
                arrivalExactField,
                arrivalEarliestField,
                arrivalLatestField,
                arrivalCommercialField,
                showNotifications)) {
            return false;
        }
        if (!writeTimeMode(
                row,
                false,
                departureModeField.getValue(),
                departureExactField,
                departureEarliestField,
                departureLatestField,
                departureCommercialField,
                showNotifications)) {
            return false;
        }

        if (Boolean.TRUE.equals(row.getHalt())) {
            if (activityField.getSelectedItems().isEmpty()) {
                if (showNotifications) {
                    Notification.show(
                                    t("timetable.stop.activityRequired"), 3000, Position.BOTTOM_END)
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
        configureConstraintMode(arrivalModeField, "timetable.editor.arrivalMode");
        configureConstraintMode(departureModeField, "timetable.editor.departureMode");
        configureTimePickers();

        styleEstimateLabel(arrivalEstimateLabel);
        styleEstimateLabel(departureEstimateLabel);

        buildTimeSection(
                arrivalSection,
                arrivalExactWrapper,
                arrivalExactField,
                arrivalWindowWrapper,
                arrivalEarliestField,
                arrivalLatestField,
                arrivalCommercialWrapper,
                arrivalCommercialField,
                arrivalEstimateLabel,
                arrivalModeField,
                "timetable.editor.arrival");
        arrivalSection.add(arrivalOffsetField);
        buildTimeSection(
                departureSection,
                departureExactWrapper,
                departureExactField,
                departureWindowWrapper,
                departureEarliestField,
                departureLatestField,
                departureCommercialWrapper,
                departureCommercialField,
                departureEstimateLabel,
                departureModeField,
                "timetable.editor.departure");
        departureSection.add(departureOffsetField);

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
                arrivalSection,
                departureSection,
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
        arrivalSection.setVisible(halt);
        departureSection.setVisible(halt);
        dwellMinutesField.setVisible(halt);
    }

    /** Clear all editor time inputs and their displays — used when halt is unchecked. */
    private void clearAllTimeFields() {
        arrivalModeField.setValue(TimeConstraintMode.NONE);
        departureModeField.setValue(TimeConstraintMode.NONE);
        arrivalExactField.clear();
        arrivalEarliestField.clear();
        arrivalLatestField.clear();
        arrivalCommercialField.clear();
        departureExactField.clear();
        departureEarliestField.clear();
        departureLatestField.clear();
        departureCommercialField.clear();
        dwellMinutesField.clear();
    }

    private static final java.util.Set<String> VEHICLE_LINK_ACTIVITIES =
            java.util.Set.of(
                    "0010", "0011", "0012", "0013", "0014", "0015", "0016", "0017", "0044", "0045");

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

    private void configureTimePickers() {
        configureNamedTimePicker(arrivalExactField, t("timetable.editor.exact"));
        configureNamedTimePicker(arrivalEarliestField, t("timetable.editor.earliestArrival"));
        configureNamedTimePicker(arrivalLatestField, t("timetable.editor.latestArrival"));
        configureNamedTimePicker(arrivalCommercialField, t("timetable.editor.commercial"));
        configureNamedTimePicker(departureExactField, t("timetable.editor.exact"));
        configureNamedTimePicker(departureEarliestField, t("timetable.editor.earliestDeparture"));
        configureNamedTimePicker(departureLatestField, t("timetable.editor.latestDeparture"));
        configureNamedTimePicker(departureCommercialField, t("timetable.editor.commercial"));

        configureOffsetField(arrivalOffsetField);
        configureOffsetField(departureOffsetField);

        String relativeHelp = t("timetable.time.relative.help");
        arrivalExactField.setHelperText(relativeHelp);
        departureExactField.setHelperText(relativeHelp);
    }

    private void configureOffsetField(com.vaadin.flow.component.textfield.IntegerField field) {
        field.setLabel(t("timetable.editor.dayOffset"));
        field.setHelperText(t("timetable.editor.dayOffset.help"));
        field.setMin(-1);
        field.setMax(2);
        field.setStepButtonsVisible(true);
        field.setValue(0);
        field.setWidthFull();
    }

    private void configureConstraintMode(Select<TimeConstraintMode> field, String labelKey) {
        field.setLabel(t(labelKey));
        field.setItems(TimeConstraintMode.values());
        field.setItemLabelGenerator(mode -> timeModeLabel(mode, this));
        field.setValue(TimeConstraintMode.NONE);
        boolean isArrival = (field == arrivalModeField);
        field.addValueChangeListener(
                e -> {
                    if (e.isFromClient()) {
                        // Mode-switch preservation (Edge-Case #3 / option a): copy any current
                        // value from the old mode's picker(s) into the new mode's picker(s) so
                        // the user doesn't silently lose what they typed.
                        preserveValueAcrossMode(isArrival, e.getOldValue(), e.getValue());
                    }
                    updateModeVisibility();
                });
    }

    /** Move whatever single value the user had under the old mode into the new-mode pickers. */
    private void preserveValueAcrossMode(
            boolean isArrival, TimeConstraintMode oldMode, TimeConstraintMode newMode) {
        if (oldMode == null || newMode == null || oldMode == newMode) return;
        java.time.LocalTime source = sourceForMode(isArrival, oldMode);
        if (source == null) return;
        switch (newMode) {
            case EXACT -> {
                if (isArrival) arrivalExactField.setValue(source);
                else departureExactField.setValue(source);
            }
            case WINDOW -> {
                if (isArrival) {
                    arrivalEarliestField.setValue(source);
                    arrivalLatestField.setValue(source);
                } else {
                    departureEarliestField.setValue(source);
                    departureLatestField.setValue(source);
                }
            }
            case AFTER -> {
                // Half-window "≥ X": only earliest is meaningful
                if (isArrival) arrivalEarliestField.setValue(source);
                else departureEarliestField.setValue(source);
            }
            case BEFORE -> {
                // Half-window "≤ X": only latest is meaningful
                if (isArrival) arrivalLatestField.setValue(source);
                else departureLatestField.setValue(source);
            }
            case COMMERCIAL -> {
                if (isArrival) arrivalCommercialField.setValue(source);
                else departureCommercialField.setValue(source);
            }
            case NONE -> {
                /* user explicitly cleared */
            }
        }
    }

    private java.time.LocalTime sourceForMode(boolean isArrival, TimeConstraintMode mode) {
        if (mode == null) return null;
        return switch (mode) {
            case EXACT -> isArrival ? arrivalExactField.getValue() : departureExactField.getValue();
            case WINDOW -> {
                if (isArrival) {
                    yield firstNonNullTime(
                            arrivalEarliestField.getValue(), arrivalLatestField.getValue());
                }
                yield firstNonNullTime(
                        departureLatestField.getValue(), departureEarliestField.getValue());
            }
            case AFTER ->
                    isArrival ? arrivalEarliestField.getValue() : departureEarliestField.getValue();
            case BEFORE ->
                    isArrival ? arrivalLatestField.getValue() : departureLatestField.getValue();
            case COMMERCIAL ->
                    isArrival
                            ? arrivalCommercialField.getValue()
                            : departureCommercialField.getValue();
            case NONE -> null;
        };
    }

    private java.time.LocalTime firstNonNullTime(java.time.LocalTime a, java.time.LocalTime b) {
        return a != null ? a : b;
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

    private void buildTimeSection(
            Div section,
            Div exactWrapper,
            TimePicker exactField,
            Div windowWrapper,
            TimePicker earliestField,
            TimePicker latestField,
            Div commercialWrapper,
            TimePicker commercialField,
            Span estimateLabel,
            Select<TimeConstraintMode> modeField,
            String headerKey) {
        exactWrapper.add(exactField);
        windowWrapper
                .getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "1fr 1fr")
                .set("gap", "12px");
        windowWrapper.add(earliestField, latestField);
        commercialWrapper.add(commercialField);
        section.add(
                sectionHeader(t(headerKey)),
                estimateLabel,
                modeField,
                exactWrapper,
                windowWrapper,
                commercialWrapper);
        section.getStyle()
                .set("padding", "12px")
                .set("border", "1px solid var(--rom-border)")
                .set("border-radius", "6px")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "10px");
    }

    // ── Sync helpers ──────────────────────────────────────────────────

    private boolean writeTimeMode(
            TimetableRowData row,
            boolean arrival,
            TimeConstraintMode mode,
            TimePicker exactField,
            TimePicker earliestField,
            TimePicker latestField,
            TimePicker commercialField,
            boolean showNotifications) {

        // Reset any prior invalid markers on this side; we only re-mark on the actual offender.
        clearInvalid(exactField, earliestField, latestField, commercialField);

        TimeConstraintMode resolvedMode = defaultMode(mode);
        if (arrival) {
            row.setArrivalMode(resolvedMode);
            row.setArrivalExact(null);
            row.setArrivalEarliest(null);
            row.setArrivalLatest(null);
            row.setCommercialArrival(null);
            // Reset user-entered flags; we set them back to true only for fields the user
            // actually filled in this sync pass.
            row.setUserEnteredArrivalExact(false);
            row.setUserEnteredArrivalEarliest(false);
            row.setUserEnteredArrivalLatest(false);
            row.setUserEnteredCommercialArrival(false);
        } else {
            row.setDepartureMode(resolvedMode);
            row.setDepartureExact(null);
            row.setDepartureEarliest(null);
            row.setDepartureLatest(null);
            row.setCommercialDeparture(null);
            row.setUserEnteredDepartureExact(false);
            row.setUserEnteredDepartureEarliest(false);
            row.setUserEnteredDepartureLatest(false);
            row.setUserEnteredCommercialDeparture(false);
        }

        if (resolvedMode == TimeConstraintMode.NONE) {
            return true;
        }
        if (resolvedMode == TimeConstraintMode.EXACT) {
            if (exactField.getValue() == null) {
                return invalidate(
                        exactField, arrival, showNotifications, "timetable.editor.time.required");
            }
            String formatted = formatTime(exactField.getValue());
            if (arrival) {
                row.setArrivalExact(formatted);
                row.setUserEnteredArrivalExact(true);
            } else {
                row.setDepartureExact(formatted);
                row.setUserEnteredDepartureExact(true);
            }
            return true;
        }
        if (resolvedMode == TimeConstraintMode.COMMERCIAL) {
            if (commercialField.getValue() == null) {
                return invalidate(
                        commercialField,
                        arrival,
                        showNotifications,
                        "timetable.editor.time.required");
            }
            String formatted = formatTime(commercialField.getValue());
            if (arrival) {
                row.setCommercialArrival(formatted);
                row.setUserEnteredCommercialArrival(true);
            } else {
                row.setCommercialDeparture(formatted);
                row.setUserEnteredCommercialDeparture(true);
            }
            return true;
        }

        /* AFTER (≥) — only earliest needed */
        if (resolvedMode == TimeConstraintMode.AFTER) {
            if (earliestField.getValue() == null) {
                return invalidate(
                        earliestField,
                        arrival,
                        showNotifications,
                        "timetable.editor.time.required");
            }
            String formatted = formatTime(earliestField.getValue());
            if (arrival) {
                row.setArrivalEarliest(formatted);
                row.setUserEnteredArrivalEarliest(true);
            } else {
                row.setDepartureEarliest(formatted);
                row.setUserEnteredDepartureEarliest(true);
            }
            return true;
        }

        /* BEFORE (≤) — only latest needed */
        if (resolvedMode == TimeConstraintMode.BEFORE) {
            if (latestField.getValue() == null) {
                return invalidate(
                        latestField, arrival, showNotifications, "timetable.editor.time.required");
            }
            String formatted = formatTime(latestField.getValue());
            if (arrival) {
                row.setArrivalLatest(formatted);
                row.setUserEnteredArrivalLatest(true);
            } else {
                row.setDepartureLatest(formatted);
                row.setUserEnteredDepartureLatest(true);
            }
            return true;
        }

        /* WINDOW mode — both bounds required */
        if (earliestField.getValue() == null) {
            return invalidate(
                    earliestField, arrival, showNotifications, "timetable.editor.time.required");
        }
        if (latestField.getValue() == null) {
            return invalidate(
                    latestField, arrival, showNotifications, "timetable.editor.time.required");
        }
        if (latestField.getValue().isBefore(earliestField.getValue())) {
            return invalidate(
                    latestField, arrival, showNotifications, "timetable.editor.window.invalid");
        }
        if (arrival) {
            row.setArrivalEarliest(formatTime(earliestField.getValue()));
            row.setArrivalLatest(formatTime(latestField.getValue()));
            row.setUserEnteredArrivalEarliest(true);
            row.setUserEnteredArrivalLatest(true);
        } else {
            row.setDepartureEarliest(formatTime(earliestField.getValue()));
            row.setDepartureLatest(formatTime(latestField.getValue()));
            row.setUserEnteredDepartureEarliest(true);
            row.setUserEnteredDepartureLatest(true);
        }
        return true;
    }

    private void clearInvalid(TimePicker... pickers) {
        for (TimePicker p : pickers) {
            p.setInvalid(false);
            p.setErrorMessage(null);
        }
    }

    private boolean invalidate(
            TimePicker field, boolean arrival, boolean showNotifications, String key) {
        field.setInvalid(true);
        field.setErrorMessage(t(key));
        if (showNotifications) {
            String sideLabel =
                    t(arrival ? "timetable.editor.arrival" : "timetable.editor.departure");
            Notification.show(sideLabel + ": " + t(key), 3000, Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
        // Bring the offending field into view and focus it so the user sees what's missing.
        field.focus();
        return false;
    }

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

    private void updateModeVisibility() {
        TimeConstraintMode arrMode = arrivalModeField.getValue();
        arrivalExactWrapper.setVisible(arrMode == TimeConstraintMode.EXACT);
        // Window wrapper holds both earliest+latest; AFTER and BEFORE reuse it but only show one.
        arrivalWindowWrapper.setVisible(
                arrMode == TimeConstraintMode.WINDOW
                        || arrMode == TimeConstraintMode.AFTER
                        || arrMode == TimeConstraintMode.BEFORE);
        arrivalEarliestField.setVisible(
                arrMode == TimeConstraintMode.WINDOW || arrMode == TimeConstraintMode.AFTER);
        arrivalLatestField.setVisible(
                arrMode == TimeConstraintMode.WINDOW || arrMode == TimeConstraintMode.BEFORE);
        arrivalCommercialWrapper.setVisible(arrMode == TimeConstraintMode.COMMERCIAL);

        TimeConstraintMode depMode = departureModeField.getValue();
        departureExactWrapper.setVisible(depMode == TimeConstraintMode.EXACT);
        departureWindowWrapper.setVisible(
                depMode == TimeConstraintMode.WINDOW
                        || depMode == TimeConstraintMode.AFTER
                        || depMode == TimeConstraintMode.BEFORE);
        departureEarliestField.setVisible(
                depMode == TimeConstraintMode.WINDOW || depMode == TimeConstraintMode.AFTER);
        departureLatestField.setVisible(
                depMode == TimeConstraintMode.WINDOW || depMode == TimeConstraintMode.BEFORE);
        departureCommercialWrapper.setVisible(depMode == TimeConstraintMode.COMMERCIAL);
    }

    private void populateEstimateLabel(Span label, TimetableRowData row, boolean isArrival) {
        String estKey =
                isArrival
                        ? "timetable.editor.estimatedArrival"
                        : "timetable.editor.estimatedDeparture";
        String estVal = isArrival ? row.getEstimatedArrival() : row.getEstimatedDeparture();
        String txt = t(estKey) + ": " + timeOrDash(estVal);
        TimeConstraintMode mode = isArrival ? row.getArrivalMode() : row.getDepartureMode();
        String qualifier = timingQualifierCode(mode, isArrival);
        if (qualifier != null) {
            txt += "  [" + qualifier + "]";
        }
        label.setText(txt);
    }

    private Span sectionHeader(String text) {
        Span s = new Span(text);
        s.getStyle().set("font-weight", "600").set("color", "var(--rom-text-primary)");
        return s;
    }

    private void styleEstimateLabel(Span label) {
        label.getStyle()
                .set("display", "block")
                .set("font-size", "11px")
                .set("font-family", "'JetBrains Mono', monospace")
                .set("color", "var(--rom-text-secondary)");
    }

    private void configureNamedTimePicker(TimePicker picker, String label) {
        picker.setLabel(label);
        picker.setClearButtonVisible(true);
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

    private TimePicker createTimePicker() {
        TimePicker picker = new TimePicker();
        picker.setStep(Duration.ofMinutes(1));
        picker.setAllowedCharPattern("[0-9:]");
        picker.setPlaceholder("HH:mm");
        picker.setLocale(getLocale() != null ? getLocale() : Locale.GERMANY);
        picker.setWidthFull();
        return picker;
    }

    private String t(String key, Object... params) {
        return getTranslation(key, params);
    }
}
