package com.ordermgmt.railway.ui.component;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

public class KpiCard extends Div {

    private final Span valueSpan;
    private final Span labelSpan;
    private final Span trendSpan;

    public KpiCard(String label, String value) {
        this(label, value, null);
    }

    public KpiCard(String label, String value, String accentColor) {
        addClassName("kpi-card");
        applyCardStyle();

        if (accentColor != null) {
            getStyle().set("border-left", "3px solid " + accentColor);
        }

        labelSpan = new Span(label);
        applyLabelStyle(labelSpan);

        valueSpan = new Span(value);
        applyValueStyle(valueSpan);

        trendSpan = new Span();
        applyTrendStyle(trendSpan);

        add(labelSpan, valueSpan, trendSpan);
    }

    public void setValue(String value) {
        valueSpan.setText(value);
    }

    public void setTrend(String text, boolean positive) {
        trendSpan.setText(text);
        trendSpan
                .getStyle()
                .set("color", positive ? "var(--rom-status-active)" : "var(--rom-status-danger)");
    }

    private void applyCardStyle() {
        getStyle()
                .set("background", "var(--rom-bg-card)")
                .set("border", "1px solid var(--rom-border)")
                .set("border-radius", "6px")
                .set("padding", "var(--lumo-space-m)")
                .set("min-width", "160px")
                .set("flex", "1");
    }

    private void applyLabelStyle(Span label) {
        label.getStyle()
                .set("font-size", "0.7rem")
                .set("font-weight", "600")
                .set("text-transform", "uppercase")
                .set("letter-spacing", "0.05em")
                .set("color", "var(--rom-text-muted)")
                .set("display", "block")
                .set("margin-bottom", "var(--lumo-space-xs)");
    }

    private void applyValueStyle(Span value) {
        value.getStyle()
                .set("font-family", "'JetBrains Mono', monospace")
                .set("font-size", "1.5rem")
                .set("font-weight", "700")
                .set("color", "var(--rom-text-primary)")
                .set("display", "block")
                .set("line-height", "1.2");
    }

    private void applyTrendStyle(Span trend) {
        trend.getStyle()
                .set("font-size", "0.75rem")
                .set("margin-top", "var(--lumo-space-xs)")
                .set("display", "block");
    }
}
