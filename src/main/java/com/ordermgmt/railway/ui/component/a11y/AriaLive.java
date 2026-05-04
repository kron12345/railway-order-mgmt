package com.ordermgmt.railway.ui.component.a11y;

import com.vaadin.flow.component.html.Span;

/**
 * Visually hidden live region for screen-reader announcements. Add once to a view, then
 * call {@link #announce(String)} when something the user should hear changes (selection,
 * filter result count, save success, etc.).
 *
 * <p>{@code aria-live=polite} ensures the announcement is queued without interrupting
 * the current task — appropriate for status changes that are not errors.
 */
public class AriaLive extends Span {

    public AriaLive() {
        addClassName("sr-only");
        getElement().setAttribute("aria-live", "polite");
        getElement().setAttribute("aria-atomic", "true");
        getElement().setAttribute("role", "status");
    }

    /** Push a message into the live region. Empty/null clears it. */
    public void announce(String message) {
        // Clearing first ensures repeated identical messages still trigger SR readout.
        setText("");
        if (message != null && !message.isBlank()) {
            getUI().ifPresent(ui -> ui.access(() -> setText(message)));
        }
    }
}
