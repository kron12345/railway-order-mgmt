package com.ordermgmt.railway.ui.component.timetable;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Consumer;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.timepicker.TimePicker;

/**
 * Collapsible panel for generating interval timetable positions (Taktfahrplan). Shown in the route
 * step after a route has been calculated.
 */
public class IntervalTimetablePanel extends Div {

    /** Configuration record passed to the generate callback. */
    public record IntervalConfig(
            LocalTime firstDeparture,
            LocalTime lastDeparture,
            boolean crossMidnight,
            int intervalMinutes,
            String namePrefix,
            String otnStart) {}

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");
    private static final int MIN_INTERVAL_MINUTES = 5;
    private static final int MAX_INTERVAL_MINUTES = 240;
    private static final int DEFAULT_INTERVAL_MINUTES = 30;
    private static final int MINUTES_PER_DAY = 1_440;

    private final TimePicker firstDepartureField = createTimePicker();
    private final TimePicker lastDepartureField = createTimePicker();
    private final Checkbox crossMidnight = new Checkbox();
    private final IntegerField intervalField = new IntegerField();
    private final TextField namePrefix = new TextField();
    private final TextField otnStart = new TextField();
    private final Span preview = new Span();
    private final Button generateButton = new Button();

    private Consumer<IntervalConfig> onGenerate;

    public IntervalTimetablePanel() {
        setWidthFull();
        setVisible(false);
        getStyle()
                .set("background", "var(--rom-bg-card)")
                .set("border", "1px solid var(--rom-border)")
                .set("border-radius", "6px")
                .set("padding", "12px 16px")
                .set("margin-top", "12px")
                .set("box-sizing", "border-box");

        // Title
        Span title = new Span(t("timetable.interval.title"));
        title.getStyle()
                .set("font-weight", "600")
                .set("font-size", "14px")
                .set("color", "var(--rom-accent)")
                .set("display", "block")
                .set("margin-bottom", "10px");

        // Configure fields
        firstDepartureField.setLabel(t("timetable.interval.firstDeparture"));
        lastDepartureField.setLabel(t("timetable.interval.lastDeparture"));
        crossMidnight.setLabel(t("timetable.interval.crossMidnight"));

        intervalField.setLabel(t("timetable.interval.interval"));
        intervalField.setMin(MIN_INTERVAL_MINUTES);
        intervalField.setMax(MAX_INTERVAL_MINUTES);
        intervalField.setValue(DEFAULT_INTERVAL_MINUTES);
        intervalField.setStepButtonsVisible(true);
        intervalField.setWidthFull();

        namePrefix.setLabel(t("timetable.interval.namePrefix"));
        namePrefix.setPlaceholder("IC 717");
        namePrefix.setRequired(true);
        namePrefix.setWidthFull();
        namePrefix.addValueChangeListener(
                e -> {
                    if (!e.getValue().isBlank()) {
                        namePrefix.setInvalid(false);
                    }
                    updatePreview();
                });

        otnStart.setLabel(t("timetable.interval.otnStart"));
        otnStart.setPlaceholder("95001");
        otnStart.setHelperText(t("timetable.interval.otnStart"));
        otnStart.setWidthFull();

        preview.getStyle()
                .set("display", "block")
                .set("font-family", "'JetBrains Mono', monospace")
                .set("font-size", "12px")
                .set("color", "var(--rom-text-secondary)")
                .set("padding", "8px 0");

        generateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        generateButton.setIcon(VaadinIcon.CALENDAR_CLOCK.create());
        generateButton
                .getStyle()
                .set("background", "var(--rom-accent)")
                .set("color", "var(--rom-bg-primary)");
        generateButton.addClickListener(e -> fireGenerate());

        // Auto-detect midnight crossing
        lastDepartureField.addValueChangeListener(
                e -> {
                    if (e.getValue() != null && firstDepartureField.getValue() != null) {
                        crossMidnight.setValue(
                                e.getValue().isBefore(firstDepartureField.getValue()));
                    }
                    updatePreview();
                });
        firstDepartureField.addValueChangeListener(e -> updatePreview());
        intervalField.addValueChangeListener(e -> updatePreview());

        // Layout
        FormLayout form = new FormLayout();
        form.setWidthFull();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("400px", 2));
        form.add(
                firstDepartureField,
                lastDepartureField,
                crossMidnight,
                intervalField,
                namePrefix,
                otnStart);

        add(title, form, preview, generateButton);
        updatePreview();
    }

    public void setRouteAvailable(boolean available) {
        setVisible(available);
    }

    public void setDefaultDeparture(LocalTime departure) {
        if (departure != null && firstDepartureField.getValue() == null) {
            firstDepartureField.setValue(departure);
        }
    }

    public void setOnGenerate(Consumer<IntervalConfig> callback) {
        this.onGenerate = callback;
    }

    private void fireGenerate() {
        if (onGenerate == null) {
            return;
        }
        boolean valid = true;
        if (firstDepartureField.getValue() == null) {
            firstDepartureField.setInvalid(true);
            valid = false;
        }
        if (lastDepartureField.getValue() == null) {
            lastDepartureField.setInvalid(true);
            valid = false;
        }
        if (namePrefix.getValue().isBlank()) {
            namePrefix.setInvalid(true);
            namePrefix.setErrorMessage(t("timetable.interval.namePrefix.required"));
            valid = false;
        }
        if (!valid) {
            return;
        }
        onGenerate.accept(
                new IntervalConfig(
                        firstDepartureField.getValue(),
                        lastDepartureField.getValue(),
                        Boolean.TRUE.equals(crossMidnight.getValue()),
                        intervalField.getValue() != null
                                ? intervalField.getValue()
                                : DEFAULT_INTERVAL_MINUTES,
                        namePrefix.getValue().trim(),
                        otnStart.getValue()));
    }

    private void updatePreview() {
        if (firstDepartureField.getValue() == null
                || lastDepartureField.getValue() == null
                || intervalField.getValue() == null
                || intervalField.getValue() < MIN_INTERVAL_MINUTES) {
            preview.setText("");
            generateButton.setText(t("timetable.interval.generate", 0));
            generateButton.setEnabled(false);
            return;
        }

        int count = calculateCount();
        String firstDeparture = firstDepartureField.getValue().format(HH_MM);
        String lastDeparture = lastDepartureField.getValue().format(HH_MM);
        preview.setText(t("timetable.interval.preview", count, firstDeparture, lastDeparture));
        generateButton.setText(t("timetable.interval.generate", count));
        generateButton.setEnabled(count > 0);
    }

    private int calculateCount() {
        int firstDepartureMinute =
                firstDepartureField.getValue().getHour() * 60
                        + firstDepartureField.getValue().getMinute();
        int lastDepartureMinute =
                lastDepartureField.getValue().getHour() * 60
                        + lastDepartureField.getValue().getMinute();
        boolean crossesMidnight = Boolean.TRUE.equals(crossMidnight.getValue());
        if (crossesMidnight && lastDepartureMinute <= firstDepartureMinute) {
            lastDepartureMinute += MINUTES_PER_DAY;
        }
        if (lastDepartureMinute < firstDepartureMinute) {
            return 0;
        }
        return ((lastDepartureMinute - firstDepartureMinute) / intervalField.getValue()) + 1;
    }

    private TimePicker createTimePicker() {
        TimePicker picker = new TimePicker();
        picker.setStep(Duration.ofMinutes(1));
        picker.setLocale(Locale.GERMANY);
        picker.setPlaceholder("HH:mm");
        picker.setWidthFull();
        return picker;
    }

    private String t(String key, Object... params) {
        return getTranslation(key, params);
    }
}
