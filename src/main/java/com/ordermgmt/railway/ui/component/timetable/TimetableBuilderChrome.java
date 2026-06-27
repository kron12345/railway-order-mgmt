package com.ordermgmt.railway.ui.component.timetable;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.function.BiFunction;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import com.ordermgmt.railway.domain.order.model.Order;

/**
 * Header + status-bar chrome and the step badges/buttons of {@link TimetableBuilderView}. Split out
 * so the view stays under the size limit. Owns the visual elements; the view supplies the order,
 * the translator, and the click callbacks (the step-decision logic stays in the view). Behaviour is
 * the same code, just relocated.
 */
public class TimetableBuilderChrome {

    /** The three wizard steps of the builder. */
    public enum Step {
        ROUTE,
        TABLE,
        INTERVAL
    }

    /** Schedule readiness shown at the right of the status bar. */
    public enum StatusKind {
        READY,
        DIRTY,
        INCOMPLETE
    }

    private static final DateTimeFormatter VALIDITY_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final BiFunction<String, Object[], String> translator;

    private final Span stepOneBadge = new Span();
    private final Span stepTwoBadge = new Span();
    private final Span stepThreeBadge = new Span();
    private final Button stepBackButton = new Button();
    private final Button stepNextButton = new Button();
    private final Button saveButton = new Button();
    private final Span statusOtn = new Span();
    private final Span statusRoute = new Span();
    private final Span statusState = new Span();

    public TimetableBuilderChrome(BiFunction<String, Object[], String> translator) {
        this.translator = translator;
    }

    public Component buildHeader(Order order, Runnable onBack) {
        Button back = new Button(VaadinIcon.ARROW_LEFT.create());
        back.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        back.getStyle().set("color", "var(--rom-text-secondary)");
        back.addClickListener(e -> onBack.run());
        H2 title = new H2(t("timetable.builder.title"));
        title.getStyle()
                .set("margin", "0")
                .set("font-size", "var(--lumo-font-size-xl)")
                .set("color", "var(--rom-text-primary)");
        Span sub =
                new Span(
                        order.getOrderNumber()
                                + " · "
                                + order.getName()
                                + " · "
                                + formatValidityDate(order.getValidFrom())
                                + " → "
                                + formatValidityDate(order.getValidTo()));
        sub.getStyle()
                .set("display", "block")
                .set("font-size", "12px")
                .set("font-family", "'JetBrains Mono', monospace")
                .set("color", "var(--rom-text-muted)");
        HorizontalLayout badges = new HorizontalLayout(stepOneBadge, stepTwoBadge, stepThreeBadge);
        badges.setSpacing(true);
        badges.setAlignItems(FlexComponent.Alignment.CENTER);
        HorizontalLayout acts = new HorizontalLayout(stepBackButton, stepNextButton, saveButton);
        acts.setSpacing(true);
        acts.setAlignItems(FlexComponent.Alignment.CENTER);
        HorizontalLayout row = new HorizontalLayout(back, new Div(title, sub), badges, acts);
        row.setWidthFull();
        row.expand(row.getComponentAt(1));
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.getStyle()
                .set("background", "var(--rom-bg-card)")
                .set("border", "1px solid var(--rom-border)")
                .set("border-radius", "6px")
                .set("padding", "12px 16px")
                .set("box-sizing", "border-box")
                .set("margin-bottom", "var(--lumo-space-s)");
        return row;
    }

    public Component buildStatusBar() {
        Div bar = new Div(statusOtn, statusRoute, statusState);
        bar.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("gap", "12px")
                .set("background", "var(--rom-bg-secondary)")
                .set("border-bottom", "1px solid var(--rom-border)")
                .set("padding", "4px 16px")
                .set("font-size", "11px")
                .set("font-family", "'JetBrains Mono', monospace");
        statusOtn.getStyle().set("color", "var(--rom-accent)").set("font-weight", "600");
        statusRoute.getStyle().set("color", "var(--rom-text-secondary)");
        statusState.getStyle().set("margin-left", "auto");
        return bar;
    }

    /** Wires the three action buttons (the step-decision logic lives in the supplied callbacks). */
    public void configureActions(Runnable onBack, Runnable onNext, Runnable onSave) {
        stepBackButton.setText(t("common.back"));
        stepBackButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
        stepBackButton.addClickListener(e -> onBack.run());
        stepNextButton.setText(t("common.next"));
        stepNextButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
        stepNextButton
                .getStyle()
                .set("background", "var(--rom-accent)")
                .set("color", "var(--rom-bg-primary)");
        stepNextButton.addClickListener(e -> onNext.run());
        saveButton.setText(t("common.save"));
        saveButton.setTooltipText(t("timetable.save.railoptHint"));
        saveButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
        saveButton.getStyle().set("background", "var(--rom-status-info)").set("color", "white");
        saveButton.addClickListener(e -> onSave.run());
        saveButton.addClickShortcut(Key.KEY_S, KeyModifier.CONTROL);
    }

    public void refreshStatus(String otn, String routeText, StatusKind kind) {
        statusOtn.setText(otn != null && !otn.isBlank() ? "OTN " + otn : "");
        statusRoute.setText(routeText == null ? "" : routeText);
        switch (kind) {
            case DIRTY -> {
                statusState.setText(t("timetable.status.routeDirty"));
                statusState.getStyle().set("color", "var(--rom-status-warning)");
            }
            case READY -> {
                statusState.setText("✓ " + t("timetable.status.ready"));
                statusState.getStyle().set("color", "var(--rom-status-active)");
            }
            case INCOMPLETE -> {
                statusState.setText("⚠ " + t("timetable.status.incomplete"));
                statusState.getStyle().set("color", "var(--rom-status-warning)");
            }
        }
    }

    public void updateControls(Step currentStep, boolean hasRows, boolean routeDirty) {
        styleStepBadge(stepOneBadge, t("timetable.step.route"), currentStep == Step.ROUTE, true);
        styleStepBadge(stepTwoBadge, t("timetable.step.table"), currentStep == Step.TABLE, hasRows);
        styleStepBadge(
                stepThreeBadge,
                t("timetable.step.interval"),
                currentStep == Step.INTERVAL,
                hasRows);
        stepBackButton.setText(
                currentStep == Step.ROUTE ? t("timetable.backToOrder") : t("common.back"));
        switch (currentStep) {
            case ROUTE -> stepNextButton.setText(t("common.next"));
            case TABLE -> stepNextButton.setText(t("timetable.step.interval"));
            case INTERVAL -> stepNextButton.setText("");
        }
        saveButton.setVisible(currentStep == Step.TABLE || currentStep == Step.INTERVAL);
        stepNextButton.setVisible(currentStep != Step.INTERVAL);
        boolean nextEnabled =
                switch (currentStep) {
                    case ROUTE -> hasRows && !routeDirty;
                    case TABLE -> hasRows;
                    case INTERVAL -> false;
                };
        stepNextButton.setEnabled(nextEnabled);
    }

    private void styleStepBadge(Span badge, String label, boolean active, boolean enabled) {
        badge.setText(label);
        badge.getStyle()
                .set("padding", "4px 10px")
                .set("border-radius", "999px")
                .set("font-size", "11px")
                .set("font-family", "'JetBrains Mono', monospace")
                .set("font-weight", "600")
                .set("border", "1px solid " + (active ? "var(--rom-accent)" : "var(--rom-border)"))
                .set("background", active ? "rgba(45,212,191,0.12)" : "rgba(148,163,184,0.08)")
                .set(
                        "color",
                        active
                                ? "var(--rom-accent)"
                                : enabled ? "var(--rom-text-secondary)" : "var(--rom-text-muted)")
                .set("opacity", enabled ? "1" : "0.55");
    }

    /** The centered interval-step card wrapping the supplied interval panel. */
    public Component intervalStepCard(Component intervalPanel) {
        Div wrapper = new Div();
        wrapper.setWidthFull();
        wrapper.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("align-items", "center")
                .set("padding", "var(--lumo-space-l) 0")
                .set("height", "100%")
                .set("box-sizing", "border-box");

        Div card = new Div();
        card.getStyle()
                .set("width", "100%")
                .set("max-width", "720px")
                .set("background", "var(--rom-bg-card)")
                .set("border", "1px solid var(--rom-border)")
                .set("border-radius", "6px")
                .set("padding", "24px")
                .set("box-sizing", "border-box");

        Span header = new Span(t("timetable.interval.step.header"));
        header.getStyle()
                .set("display", "block")
                .set("font-size", "15px")
                .set("font-weight", "600")
                .set("color", "var(--rom-text-primary)")
                .set("margin-bottom", "6px");

        Span help = new Span(t("timetable.interval.step.help"));
        help.getStyle()
                .set("display", "block")
                .set("font-size", "12px")
                .set("color", "var(--rom-text-muted)")
                .set("margin-bottom", "16px");

        card.add(header, help, intervalPanel);
        wrapper.add(card);
        return wrapper;
    }

    static String formatValidityDate(LocalDate date) {
        return date != null ? date.format(VALIDITY_DATE_FORMAT) : "—";
    }

    private String t(String key) {
        return translator.apply(key, new Object[0]);
    }
}
