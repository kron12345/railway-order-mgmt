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
import java.util.List;
import java.util.Locale;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.timepicker.TimePicker;

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
    private final Checkbox haltField = new Checkbox();
    private final com.vaadin.flow.component.textfield.IntegerField dwellMinutesField =
            new com.vaadin.flow.component.textfield.IntegerField();
    private final ComboBox<TimetableActivityOption> activityField = new ComboBox<>();

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
        haltField.setValue(Boolean.TRUE.equals(row.getHalt()));
        dwellMinutesField.setValue(row.getDwellMinutes());
        activityField.setValue(
                findActivityOption(row.getActivityCode(), activityOptions).orElse(null));

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
        pinnedField.setValue(Boolean.TRUE.equals(row.getPinned()));

        arrivalSection.setVisible(!isOrigin);
        departureSection.setVisible(!isDestination);
        activityField.setVisible(Boolean.TRUE.equals(row.getHalt()));
        if (Boolean.TRUE.equals(row.getHalt()) && activityField.getValue() == null) {
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
        row.setHalt(Boolean.TRUE.equals(haltField.getValue()));
        row.setDwellMinutes(
                Boolean.TRUE.equals(haltField.getValue()) ? dwellMinutesField.getValue() : null);
        row.setPinned(Boolean.TRUE.equals(pinnedField.getValue()));

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
            if (activityField.getValue() == null) {
                if (showNotifications) {
                    Notification.show(
                                    t("timetable.stop.activityRequired"), 3000, Position.BOTTOM_END)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
                return false;
            }
            row.setActivityCode(activityField.getValue().code());
            ensureStopTimes(row, isOrigin, isDestination);
        } else {
            row.setActivityCode(null);
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
        configureActivityField();
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

        Div propagationSection = buildPropagationSection();

        HorizontalLayout rowFlags = new HorizontalLayout(haltField, dwellMinutesField);
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
                rowFlags,
                activityField,
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
                    if (halt) {
                        activityField.getStyle().set("background", "rgba(250,204,21,0.15)");
                        activityField.setRequiredIndicatorVisible(true);
                        // Auto-select first activity if none selected
                        if (activityField.getValue() == null && !activityOptions.isEmpty()) {
                            activityField.setValue(activityOptions.getFirst());
                        }
                    }
                });
    }

    private void configureActivityField() {
        activityField.setLabel(t("timetable.editor.activity"));
        activityField.setItems(activityOptions);
        activityField.setItemLabelGenerator(opt -> activityOptionLabel(opt));
        activityField.setWidthFull();
        activityField.addValueChangeListener(
                e -> {
                    if (e.getValue() != null) {
                        activityField.getStyle().remove("background");
                    } else if (Boolean.TRUE.equals(haltField.getValue())) {
                        activityField.getStyle().set("background", "rgba(250,204,21,0.15)");
                    }
                });
    }

    private void configureDwellField() {
        dwellMinutesField.setLabel(t("timetable.editor.dwell"));
        dwellMinutesField.setMin(0);
        dwellMinutesField.setStepButtonsVisible(true);
        dwellMinutesField.setWidthFull();
    }

    private void configureTimePickers() {
        configureNamedTimePicker(arrivalExactField, t("timetable.editor.exact"));
        configureNamedTimePicker(arrivalEarliestField, t("timetable.editor.earliest"));
        configureNamedTimePicker(arrivalLatestField, t("timetable.editor.latest"));
        configureNamedTimePicker(arrivalCommercialField, t("timetable.editor.commercial"));
        configureNamedTimePicker(departureExactField, t("timetable.editor.exact"));
        configureNamedTimePicker(departureEarliestField, t("timetable.editor.earliest"));
        configureNamedTimePicker(departureLatestField, t("timetable.editor.latest"));
        configureNamedTimePicker(departureCommercialField, t("timetable.editor.commercial"));

        String relativeHelp = t("timetable.time.relative.help");
        arrivalExactField.setHelperText(relativeHelp);
        departureExactField.setHelperText(relativeHelp);
    }

    private void configureConstraintMode(Select<TimeConstraintMode> field, String labelKey) {
        field.setLabel(t(labelKey));
        field.setItems(TimeConstraintMode.values());
        field.setItemLabelGenerator(mode -> timeModeLabel(mode, this));
        field.setValue(TimeConstraintMode.NONE);
        field.addValueChangeListener(e -> updateModeVisibility());
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
        propagationModeField.setValue(TimePropagationMode.SHIFT);
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

        TimeConstraintMode resolvedMode = defaultMode(mode);
        if (arrival) {
            row.setArrivalMode(resolvedMode);
            row.setArrivalExact(null);
            row.setArrivalEarliest(null);
            row.setArrivalLatest(null);
            row.setCommercialArrival(null);
        } else {
            row.setDepartureMode(resolvedMode);
            row.setDepartureExact(null);
            row.setDepartureEarliest(null);
            row.setDepartureLatest(null);
            row.setCommercialDeparture(null);
        }

        if (resolvedMode == TimeConstraintMode.NONE) {
            return true;
        }
        if (resolvedMode == TimeConstraintMode.EXACT) {
            if (exactField.getValue() == null) {
                return notifyIfNeeded(showNotifications, "timetable.editor.time.required");
            }
            String formatted = formatTime(exactField.getValue());
            if (arrival) {
                row.setArrivalExact(formatted);
            } else {
                row.setDepartureExact(formatted);
            }
            return true;
        }
        if (resolvedMode == TimeConstraintMode.COMMERCIAL) {
            if (commercialField.getValue() == null) {
                return notifyIfNeeded(showNotifications, "timetable.editor.time.required");
            }
            String formatted = formatTime(commercialField.getValue());
            if (arrival) {
                row.setCommercialArrival(formatted);
            } else {
                row.setCommercialDeparture(formatted);
            }
            return true;
        }

        /* WINDOW mode */
        if (earliestField.getValue() == null
                || latestField.getValue() == null
                || latestField.getValue().isBefore(earliestField.getValue())) {
            return notifyIfNeeded(showNotifications, "timetable.editor.window.invalid");
        }
        if (arrival) {
            row.setArrivalEarliest(formatTime(earliestField.getValue()));
            row.setArrivalLatest(formatTime(latestField.getValue()));
        } else {
            row.setDepartureEarliest(formatTime(earliestField.getValue()));
            row.setDepartureLatest(formatTime(latestField.getValue()));
        }
        return true;
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
        arrivalWindowWrapper.setVisible(arrMode == TimeConstraintMode.WINDOW);
        arrivalCommercialWrapper.setVisible(arrMode == TimeConstraintMode.COMMERCIAL);

        TimeConstraintMode depMode = departureModeField.getValue();
        departureExactWrapper.setVisible(depMode == TimeConstraintMode.EXACT);
        departureWindowWrapper.setVisible(depMode == TimeConstraintMode.WINDOW);
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

    // ── Micro-helpers ─────────────────────────────────────────────────

    private boolean notifyIfNeeded(boolean show, String key) {
        if (show) {
            Notification.show(t(key), 3000, Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
        return false;
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
