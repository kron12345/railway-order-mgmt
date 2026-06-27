package com.ordermgmt.railway.ui.component;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

public class DataReadout extends Div {

    private static final String BUSY_MARKER = "▌";

    private final Span marker = new Span(BUSY_MARKER);
    private final Span status = new Span();

    public DataReadout() {
        addClassName("data-readout");
        // Polite by default so standalone users (e.g. the topology grid) announce status changes.
        // Hosts that already own a dedicated live region (MasterDetailLayout's AriaLive) call
        // {@link #setAnnouncing(false)} to avoid two competing polite regions double-announcing.
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

    public void setStatus(String text) {
        status.setText(text);
    }

    public void setBusy(boolean busy) {
        getElement().setAttribute("aria-busy", busy);
        marker.getStyle().set("opacity", busy ? "0.4" : "1");
    }

    /**
     * Turns the polite live region on/off. Off when the host has its own announcer (e.g.
     * MasterDetailLayout's AriaLive) so the two don't double-announce.
     */
    public void setAnnouncing(boolean announcing) {
        if (announcing) {
            getElement().setAttribute("aria-live", "polite");
        } else {
            getElement().removeAttribute("aria-live");
        }
    }
}
