package com.ordermgmt.railway.ui.component;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

/**
 * Persistent "data readout" status line for large lazy lists — a terminal-style footer (JetBrains
 * Mono, muted) showing the loaded range / total / filter state, with a thin aria-busy state during
 * a fetch. Presentation only: callers compose and set the status text. Reused by lazy grids (P2)
 * and the master/detail lists (P4).
 */
public class DataReadout extends Div {

    private final Span marker = new Span("▌"); // ▌
    private final Span status = new Span();

    public DataReadout() {
        addClassName("data-readout");
        getElement().setAttribute("aria-live", "polite");
        getStyle()
                .set("font-family", "'JetBrains Mono', monospace")
                .set("font-size", "11px")
                .set("color", "var(--rom-text-muted)")
                .set("padding", "4px 10px")
                .set("border-top", "1px solid var(--rom-border)")
                .set("background", "var(--rom-bg-card)")
                .set("display", "flex")
                .set("align-items", "center")
                .set("gap", "6px")
                .set("box-sizing", "border-box");
        marker.getStyle().set("color", "var(--rom-accent)");
        add(marker, status);
    }

    /** Sets the status text (e.g. {@code "1–100 / 19.341 · gefiltert"}). */
    public void setStatus(String text) {
        status.setText(text);
    }

    /** Toggles the busy state (dims the accent marker, sets {@code aria-busy}) during a fetch. */
    public void setBusy(boolean busy) {
        getElement().setAttribute("aria-busy", busy);
        marker.getStyle().set("opacity", busy ? "0.4" : "1");
    }
}
