package com.ordermgmt.railway.ui.component;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

public class DataReadout extends Div {

    private static final String BUSY_MARKER = "▌";

    private final Span marker = new Span(BUSY_MARKER);
    private final Span status = new Span();

    public DataReadout() {
        addClassName("data-readout");
        // No aria-live here: the master-detail layout already owns a dedicated polite live region
        // (AriaLive) for announcements. Two competing polite regions double-announce; this is a
        // visual status line (aria-busy still reflects loading), not a second announcer.
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

    public void setStatus(String text) {
        status.setText(text);
    }

    public void setBusy(boolean busy) {
        getElement().setAttribute("aria-busy", busy);
        marker.getStyle().set("opacity", busy ? "0.4" : "1");
    }
}
