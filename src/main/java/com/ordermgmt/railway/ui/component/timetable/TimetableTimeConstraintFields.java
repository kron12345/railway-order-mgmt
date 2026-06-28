package com.ordermgmt.railway.ui.component.timetable;

import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.defaultMode;
import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.formatTime;
import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.parseTime;
import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.timeModeLabel;
import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.timeOrDash;
import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.timingQualifierCode;

import java.time.Duration;
import java.time.LocalTime;
import java.util.Locale;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.timepicker.TimePicker;

import com.ordermgmt.railway.domain.timetable.model.TimeConstraintMode;
import com.ordermgmt.railway.domain.timetable.model.TimetableRowData;

/**
 * One side (arrival or departure) of the row editor's time-constraint inputs: the mode select plus
 * the mode-specific pickers (exact / window earliest+latest / commercial), the day offset, and the
 * estimate readout. {@link TimetableRowEditorPanel} owns two instances (arrival + departure); the
 * component <em>is</em> the bordered section that the panel adds to its layout.
 *
 * <p>The {@code isArrival} flag drives the intentionally asymmetric bits — field labels, the WINDOW
 * source-preference when switching modes (arrival keeps the earliest bound, departure the latest),
 * and which row getters/setters are read/written. Pure UI plus per-side write/validate; cross-row
 * propagation stays in the editing service.
 */
class TimetableTimeConstraintFields extends Div {

    private static final int TIME_VALIDATION_NOTIFICATION_DURATION_MS = 3000;
    private static final Duration TIME_PICKER_STEP = Duration.ofMinutes(1);

    private final boolean isArrival;
    private final Component i18n;

    private final Select<TimeConstraintMode> modeField = new Select<>();
    private final TimePicker exactField = createTimePicker();
    private final TimePicker earliestField = createTimePicker();
    private final TimePicker latestField = createTimePicker();
    private final TimePicker commercialField = createTimePicker();
    private final IntegerField offsetField = new IntegerField();
    private final Div exactWrapper = new Div();
    private final Div windowWrapper = new Div();
    private final Div commercialWrapper = new Div();
    private final Span estimateLabel = new Span();

    TimetableTimeConstraintFields(boolean isArrival, Component i18n) {
        this.isArrival = isArrival;
        this.i18n = i18n;
        build();
        updateVisibility();
    }

    // ── Public surface used by the editor panel ───────────────────────────

    /** Populates this side's fields from the row. */
    void populate(TimetableRowData row) {
        modeField.setValue(defaultMode(isArrival ? row.getArrivalMode() : row.getDepartureMode()));
        exactField.setValue(parseTime(isArrival ? row.getArrivalExact() : row.getDepartureExact()));
        earliestField.setValue(
                parseTime(isArrival ? row.getArrivalEarliest() : row.getDepartureEarliest()));
        latestField.setValue(
                parseTime(isArrival ? row.getArrivalLatest() : row.getDepartureLatest()));
        commercialField.setValue(
                parseTime(isArrival ? row.getCommercialArrival() : row.getCommercialDeparture()));
        Integer offset = isArrival ? row.getArrivalOffset() : row.getDepartureOffset();
        offsetField.setValue(offset == null ? 0 : offset);
        populateEstimateLabel(row);
        clearInvalid(exactField, earliestField, latestField, commercialField);
        updateVisibility();
    }

    /**
     * Writes this side's field values back to the row (mode, day offset, and the active constraint
     * times). Returns false on a validation failure, having marked the offending field.
     */
    boolean writeToRow(TimetableRowData row, boolean showNotifications) {
        if (isArrival) {
            row.setArrivalOffset(offsetField.getValue() == null ? 0 : offsetField.getValue());
        } else {
            row.setDepartureOffset(offsetField.getValue() == null ? 0 : offsetField.getValue());
        }
        return writeTimeMode(row, showNotifications);
    }

    /**
     * Show/hide the whole section (endpoints and pass-through rows hide the side they cannot have).
     */
    void setSectionVisible(boolean visible) {
        setVisible(visible);
    }

    /** Clears mode + all pickers — used when a row stops being a halt. */
    void clearFields() {
        modeField.setValue(TimeConstraintMode.NONE);
        exactField.clear();
        earliestField.clear();
        latestField.clear();
        commercialField.clear();
    }

    // ── Layout ────────────────────────────────────────────────────────────

    private void build() {
        modeField.setLabel(
                t(isArrival ? "timetable.editor.arrivalMode" : "timetable.editor.departureMode"));
        modeField.setItems(TimeConstraintMode.values());
        modeField.setItemLabelGenerator(mode -> timeModeLabel(mode, i18n));
        modeField.setValue(TimeConstraintMode.NONE);
        modeField.addValueChangeListener(
                e -> {
                    if (e.isFromClient()) {
                        preserveValueAcrossMode(e.getOldValue(), e.getValue());
                    }
                    updateVisibility();
                });

        configureNamedTimePicker(exactField, t("timetable.editor.exact"));
        configureNamedTimePicker(
                earliestField,
                t(
                        isArrival
                                ? "timetable.editor.earliestArrival"
                                : "timetable.editor.earliestDeparture"));
        configureNamedTimePicker(
                latestField,
                t(
                        isArrival
                                ? "timetable.editor.latestArrival"
                                : "timetable.editor.latestDeparture"));
        configureNamedTimePicker(commercialField, t("timetable.editor.commercial"));
        exactField.setHelperText(t("timetable.time.relative.help"));

        offsetField.setLabel(t("timetable.editor.dayOffset"));
        offsetField.setHelperText(t("timetable.editor.dayOffset.help"));
        offsetField.setMin(-1);
        offsetField.setMax(2);
        offsetField.setStepButtonsVisible(true);
        offsetField.setValue(0);
        offsetField.setWidthFull();

        styleEstimateLabel(estimateLabel);

        exactWrapper.add(exactField);
        windowWrapper
                .getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "1fr 1fr")
                .set("gap", "12px");
        windowWrapper.add(earliestField, latestField);
        commercialWrapper.add(commercialField);

        add(
                sectionHeader(
                        t(isArrival ? "timetable.editor.arrival" : "timetable.editor.departure")),
                estimateLabel,
                modeField,
                exactWrapper,
                windowWrapper,
                commercialWrapper,
                offsetField);
        getStyle()
                .set("padding", "12px")
                .set("border", "1px solid var(--rom-border)")
                .set("border-radius", "6px")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "10px");
    }

    private void updateVisibility() {
        TimeConstraintMode mode = modeField.getValue();
        exactWrapper.setVisible(mode == TimeConstraintMode.EXACT);
        // The window wrapper holds both bounds; AFTER and BEFORE reuse it but show only one picker.
        windowWrapper.setVisible(
                mode == TimeConstraintMode.WINDOW
                        || mode == TimeConstraintMode.AFTER
                        || mode == TimeConstraintMode.BEFORE);
        earliestField.setVisible(
                mode == TimeConstraintMode.WINDOW || mode == TimeConstraintMode.AFTER);
        latestField.setVisible(
                mode == TimeConstraintMode.WINDOW || mode == TimeConstraintMode.BEFORE);
        commercialWrapper.setVisible(mode == TimeConstraintMode.COMMERCIAL);
    }

    // ── Cross-mode value preservation ─────────────────────────────────────

    /** Move whatever single value the user had under the old mode into the new-mode pickers. */
    private void preserveValueAcrossMode(TimeConstraintMode oldMode, TimeConstraintMode newMode) {
        if (oldMode == null || newMode == null || oldMode == newMode) {
            return;
        }
        LocalTime source = sourceForMode(oldMode);
        if (source == null) {
            return;
        }
        switch (newMode) {
            case EXACT -> exactField.setValue(source);
            case WINDOW -> {
                earliestField.setValue(source);
                latestField.setValue(source);
            }
            case AFTER -> earliestField.setValue(source); // half-window "≥ X"
            case BEFORE -> latestField.setValue(source); // half-window "≤ X"
            case COMMERCIAL -> commercialField.setValue(source);
            case NONE -> {
                /* user explicitly cleared */
            }
        }
    }

    private LocalTime sourceForMode(TimeConstraintMode mode) {
        if (mode == null) {
            return null;
        }
        return switch (mode) {
            case EXACT -> exactField.getValue();
            // Arrival keeps the earliest bound, departure the latest — preserved from the original.
            case WINDOW ->
                    isArrival
                            ? firstNonNullTime(earliestField.getValue(), latestField.getValue())
                            : firstNonNullTime(latestField.getValue(), earliestField.getValue());
            case AFTER -> earliestField.getValue();
            case BEFORE -> latestField.getValue();
            case COMMERCIAL -> commercialField.getValue();
            case NONE -> null;
        };
    }

    private LocalTime firstNonNullTime(LocalTime first, LocalTime second) {
        return first != null ? first : second;
    }

    // ── Write + validate ──────────────────────────────────────────────────

    private boolean writeTimeMode(TimetableRowData row, boolean showNotifications) {
        // Reset any prior invalid markers on this side; we only re-mark on the actual offender.
        clearInvalid(exactField, earliestField, latestField, commercialField);

        TimeConstraintMode resolvedMode = defaultMode(modeField.getValue());
        resetRowTimes(row, resolvedMode);

        if (resolvedMode == TimeConstraintMode.NONE) {
            return true;
        }
        if (resolvedMode == TimeConstraintMode.EXACT) {
            if (exactField.getValue() == null) {
                return invalidate(exactField, showNotifications, "timetable.editor.time.required");
            }
            setExact(row, formatTime(exactField.getValue()));
            return true;
        }
        if (resolvedMode == TimeConstraintMode.COMMERCIAL) {
            if (commercialField.getValue() == null) {
                return invalidate(
                        commercialField, showNotifications, "timetable.editor.time.required");
            }
            setCommercial(row, formatTime(commercialField.getValue()));
            return true;
        }
        if (resolvedMode == TimeConstraintMode.AFTER) { // ≥ — only earliest needed
            if (earliestField.getValue() == null) {
                return invalidate(
                        earliestField, showNotifications, "timetable.editor.time.required");
            }
            setEarliest(row, formatTime(earliestField.getValue()));
            return true;
        }
        if (resolvedMode == TimeConstraintMode.BEFORE) { // ≤ — only latest needed
            if (latestField.getValue() == null) {
                return invalidate(latestField, showNotifications, "timetable.editor.time.required");
            }
            setLatest(row, formatTime(latestField.getValue()));
            return true;
        }

        // WINDOW — both bounds required, latest must not precede earliest.
        if (earliestField.getValue() == null) {
            return invalidate(earliestField, showNotifications, "timetable.editor.time.required");
        }
        if (latestField.getValue() == null) {
            return invalidate(latestField, showNotifications, "timetable.editor.time.required");
        }
        if (latestField.getValue().isBefore(earliestField.getValue())) {
            return invalidate(latestField, showNotifications, "timetable.editor.window.invalid");
        }
        setEarliest(row, formatTime(earliestField.getValue()));
        setLatest(row, formatTime(latestField.getValue()));
        return true;
    }

    /** Clear this side's mode + all time fields and their user-entered flags before re-writing. */
    private void resetRowTimes(TimetableRowData row, TimeConstraintMode resolvedMode) {
        if (isArrival) {
            row.setArrivalMode(resolvedMode);
            row.setArrivalExact(null);
            row.setArrivalEarliest(null);
            row.setArrivalLatest(null);
            row.setCommercialArrival(null);
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
    }

    private void setExact(TimetableRowData row, String formatted) {
        if (isArrival) {
            row.setArrivalExact(formatted);
            row.setUserEnteredArrivalExact(true);
        } else {
            row.setDepartureExact(formatted);
            row.setUserEnteredDepartureExact(true);
        }
    }

    private void setCommercial(TimetableRowData row, String formatted) {
        if (isArrival) {
            row.setCommercialArrival(formatted);
            row.setUserEnteredCommercialArrival(true);
        } else {
            row.setCommercialDeparture(formatted);
            row.setUserEnteredCommercialDeparture(true);
        }
    }

    private void setEarliest(TimetableRowData row, String formatted) {
        if (isArrival) {
            row.setArrivalEarliest(formatted);
            row.setUserEnteredArrivalEarliest(true);
        } else {
            row.setDepartureEarliest(formatted);
            row.setUserEnteredDepartureEarliest(true);
        }
    }

    private void setLatest(TimetableRowData row, String formatted) {
        if (isArrival) {
            row.setArrivalLatest(formatted);
            row.setUserEnteredArrivalLatest(true);
        } else {
            row.setDepartureLatest(formatted);
            row.setUserEnteredDepartureLatest(true);
        }
    }

    private void clearInvalid(TimePicker... pickers) {
        for (TimePicker p : pickers) {
            p.setInvalid(false);
            p.setErrorMessage(null);
        }
    }

    private boolean invalidate(TimePicker field, boolean showNotifications, String key) {
        field.setInvalid(true);
        field.setErrorMessage(t(key));
        if (showNotifications) {
            String sideLabel =
                    t(isArrival ? "timetable.editor.arrival" : "timetable.editor.departure");
            Notification.show(
                            sideLabel + ": " + t(key),
                            TIME_VALIDATION_NOTIFICATION_DURATION_MS,
                            Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
        // Bring the offending field into view and focus it so the user sees what's missing.
        field.focus();
        return false;
    }

    // ── Small builders ────────────────────────────────────────────────────

    private void populateEstimateLabel(TimetableRowData row) {
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
        estimateLabel.setText(txt);
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
        picker.setStep(TIME_PICKER_STEP);
        picker.setAllowedCharPattern("[0-9:]");
        picker.setPlaceholder("HH:mm");
        picker.setLocale(getLocale() != null ? getLocale() : Locale.GERMANY);
        picker.setWidthFull();
        return picker;
    }

    private String t(String key, Object... params) {
        return i18n.getTranslation(key, params);
    }
}
