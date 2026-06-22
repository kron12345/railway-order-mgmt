package com.ordermgmt.railway.ui.component.order;

import java.util.function.BiFunction;
import java.util.function.Consumer;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import com.ordermgmt.railway.domain.order.model.ProcessStatus;

/**
 * Interactive process-status stepper for an order. Renders the five {@link ProcessStatus} phases as
 * a horizontal track: the current phase is highlighted, earlier phases are marked complete, later
 * phases are muted. When {@code editable} is set, clicking (or pressing Enter/Space on) a phase
 * reports the chosen status through {@code onSelect}; the caller persists it. The stepper itself
 * holds no business logic so it stays trivially reusable and testable.
 */
public class OrderStatusStepper extends HorizontalLayout {

    private final ProcessStatus current;
    private final BiFunction<String, Object[], String> translator;
    private final Consumer<ProcessStatus> onSelect;

    public OrderStatusStepper(
            ProcessStatus current,
            boolean editable,
            BiFunction<String, Object[], String> translator,
            Consumer<ProcessStatus> onSelect) {
        this.current = current == null ? ProcessStatus.AUFTRAG : current;
        this.translator = translator;
        this.onSelect = onSelect;

        addClassName("order-status-stepper");
        setWidthFull();
        setPadding(false);
        setSpacing(false);
        setAlignItems(FlexComponent.Alignment.CENTER);
        getElement().setAttribute("role", "list");
        getElement().setAttribute("aria-label", t("order.phase.workflow"));
        getStyle()
                .set("gap", "0")
                .set("background", "var(--rom-bg-card)")
                .set("border", "1px solid var(--rom-border)")
                .set("border-radius", "6px")
                .set("padding", "var(--lumo-space-s) var(--lumo-space-m)")
                .set("margin-bottom", "var(--lumo-space-s)")
                .set("overflow-x", "auto");

        ProcessStatus[] phases = ProcessStatus.values();
        for (int i = 0; i < phases.length; i++) {
            if (i > 0) {
                add(buildConnector(i <= this.current.ordinal()));
            }
            add(buildStep(phases[i], i, editable));
        }
    }

    private Div buildStep(ProcessStatus phase, int index, boolean editable) {
        int currentIdx = current.ordinal();
        boolean isCurrent = index == currentIdx;
        boolean isDone = index < currentIdx;

        Span circle = new Span(String.valueOf(index + 1));
        circle.addClassName("order-status-stepper__circle");
        circle.getStyle()
                .set("display", "inline-flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("width", "26px")
                .set("height", "26px")
                .set("border-radius", "50%")
                .set("font-size", "var(--lumo-font-size-s)")
                .set("font-weight", "700")
                .set("flex", "0 0 auto");

        Span label = new Span(t("process." + phase.name()));
        label.getStyle().set("font-size", "var(--lumo-font-size-s)").set("white-space", "nowrap");

        if (isCurrent) {
            circle.getStyle()
                    .set("background", "var(--rom-accent)")
                    .set("color", "var(--rom-bg-primary)");
            label.getStyle().set("color", "var(--rom-text-primary)").set("font-weight", "600");
        } else if (isDone) {
            circle.getStyle()
                    .set("background", "rgba(45,212,191,0.18)")
                    .set("color", "var(--rom-accent)");
            label.getStyle().set("color", "var(--rom-text-secondary)");
        } else {
            circle.getStyle()
                    .set("background", "var(--rom-bg-secondary)")
                    .set("color", "var(--rom-text-muted)")
                    .set("border", "1px solid var(--rom-border)");
            label.getStyle().set("color", "var(--rom-text-muted)");
        }

        Div step = new Div(circle, label);
        step.addClassName("order-status-stepper__step");
        step.getElement().setAttribute("role", "listitem");
        step.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("gap", "var(--lumo-space-xs)")
                .set("padding", "0 var(--lumo-space-xs)");
        if (isCurrent) {
            step.getElement().setAttribute("aria-current", "step");
        }

        if (editable && !isCurrent) {
            step.getStyle().set("cursor", "pointer");
            step.getElement().setAttribute("role", "button");
            step.getElement().setAttribute("tabindex", "0");
            step.getElement()
                    .setAttribute(
                            "aria-label", t("order.phase.setTo", t("process." + phase.name())));
            step.addClickListener(e -> onSelect.accept(phase));
            step.getElement()
                    .addEventListener("keydown", e -> onSelect.accept(phase))
                    .setFilter(
                            "(event.key === 'Enter' || event.key === ' ')"
                                    + " && (event.preventDefault(), true)");
        }
        return step;
    }

    private Div buildConnector(boolean filled) {
        Div line = new Div();
        line.getStyle()
                .set("flex", "1")
                .set("min-width", "16px")
                .set("height", "2px")
                .set("margin", "0 4px")
                .set("background", filled ? "var(--rom-accent)" : "var(--rom-border)");
        return line;
    }

    private String t(String key, Object... params) {
        return translator.apply(key, params);
    }
}
