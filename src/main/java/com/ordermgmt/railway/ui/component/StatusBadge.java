package com.ordermgmt.railway.ui.component;

import com.vaadin.flow.component.html.Span;

/** Color-coded status pill for Bloomberg-style grid display. */
public class StatusBadge extends Span {

    public StatusBadge(String text, StatusType type) {
        setText(text);
        String color =
                switch (type) {
                    case SUCCESS -> "var(--rom-status-active)";
                    case WARNING -> "var(--rom-status-warning)";
                    case DANGER -> "var(--rom-status-danger)";
                    case INFO -> "var(--rom-status-info)";
                    case NEUTRAL -> "var(--rom-status-neutral)";
                };

        addClassName("status-badge");
        getStyle()
                .set("display", "inline-flex")
                .set("align-items", "center")
                .set("padding", "3px 9px")
                .set("border-radius", "3px")
                .set("font-size", "0.78rem")
                .set("font-weight", "700")
                .set("font-family", "'JetBrains Mono', monospace")
                .set("text-transform", "uppercase")
                .set("letter-spacing", "0.06em")
                .set("color", color)
                .set("background", "color-mix(in srgb, " + color + " 12%, transparent)")
                .set("border", "1px solid " + color);
    }

    public enum StatusType {
        SUCCESS,
        WARNING,
        DANGER,
        INFO,
        NEUTRAL
    }
}
