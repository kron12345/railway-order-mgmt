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

    private final TimePicker firstDep = createTimePicker();
    private final TimePicker lastDep = createTimePicker();
    private final Checkbox crossMidnight = new Checkbox();
    private final IntegerField interval = new IntegerField();
    private final TextField namePrefix = new TextField();
    private final TextField otnStart = new TextField();
    private final Span preview = new Span();
    private final Button generateBtn = new Button();

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
        firstDep.setLabel(t("timetable.interval.firstDeparture"));
        lastDep.setLabel(t("timetable.interval.lastDeparture"));
        crossMidnight.setLabel(t("timetable.interval.crossMidnight"));

        interval.setLabel(t("timetable.interval.interval"));
        interval.setMin(5);
        interval.setMax(240);
        interval.setValue(30);
        interval.setStepButtonsVisible(true);
        interval.setWidthFull();

        namePrefix.setLabel(t("timetable.interval.namePrefix"));
        namePrefix.setPlaceholder("IC 717");
        namePrefix.setWidthFull();

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

        generateBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        generateBtn.setIcon(VaadinIcon.CALENDAR_CLOCK.create());
        generateBtn
                .getStyle()
                .set("background", "var(--rom-accent)")
                .set("color", "var(--rom-bg-primary)");
        generateBtn.addClickListener(e -> fireGenerate());

        // Auto-detect midnight crossing
        lastDep.addValueChangeListener(
                e -> {
                    if (e.getValue() != null && firstDep.getValue() != null) {
                        crossMidnight.setValue(e.getValue().isBefore(firstDep.getValue()));
                    }
                    updatePreview();
                });
        firstDep.addValueChangeListener(e -> updatePreview());
        interval.addValueChangeListener(e -> updatePreview());

        // Layout
        FormLayout form = new FormLayout();
        form.setWidthFull();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("400px", 2));
        form.add(firstDep, lastDep, crossMidnight, interval, namePrefix, otnStart);

        add(title, form, preview, generateBtn);
        updatePreview();
    }

    public void setRouteAvailable(boolean available) {
        setVisible(available);
    }

    public void setDefaultDeparture(LocalTime departure) {
        if (departure != null && firstDep.getValue() == null) {
            firstDep.setValue(departure);
        }
    }

    public void setOnGenerate(Consumer<IntervalConfig> callback) {
        this.onGenerate = callback;
    }

    private void fireGenerate() {
        if (onGenerate == null
                || firstDep.getValue() == null
                || lastDep.getValue() == null
                || namePrefix.getValue().isBlank()) {
            return;
        }
        onGenerate.accept(
                new IntervalConfig(
                        firstDep.getValue(),
                        lastDep.getValue(),
                        Boolean.TRUE.equals(crossMidnight.getValue()),
                        interval.getValue() != null ? interval.getValue() : 30,
                        namePrefix.getValue().trim(),
                        otnStart.getValue()));
    }

    private void updatePreview() {
        if (firstDep.getValue() == null
                || lastDep.getValue() == null
                || interval.getValue() == null
                || interval.getValue() < 5) {
            preview.setText("");
            generateBtn.setText(t("timetable.interval.generate", 0));
            generateBtn.setEnabled(false);
            return;
        }

        int count = calculateCount();
        String first = firstDep.getValue().format(HH_MM);
        String last = lastDep.getValue().format(HH_MM);
        preview.setText(t("timetable.interval.preview", count, first, last));
        generateBtn.setText(t("timetable.interval.generate", count));
        generateBtn.setEnabled(count > 0 && !namePrefix.getValue().isBlank());
    }

    private int calculateCount() {
        int firstMin = firstDep.getValue().getHour() * 60 + firstDep.getValue().getMinute();
        int lastMin = lastDep.getValue().getHour() * 60 + lastDep.getValue().getMinute();
        boolean cross = Boolean.TRUE.equals(crossMidnight.getValue());
        if (cross && lastMin <= firstMin) {
            lastMin += 1440;
        }
        if (lastMin < firstMin) {
            return 0;
        }
        int iv = interval.getValue();
        return ((lastMin - firstMin) / iv) + 1;
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
