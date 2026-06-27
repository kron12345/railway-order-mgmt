package com.ordermgmt.railway.ui.component.a11y;

import java.util.List;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;

/**
 * "Skip to ..." links rendered visually-hidden until they receive focus, allowing keyboard users to
 * bypass repetitive landmarks (search bar, master list, detail).
 *
 * <p>Each link targets an element by id; clicking/activating it moves focus there. Place this as
 * the first child of the page so Tab from the URL bar lands here.
 */
public class SkipLinks extends Div {

    public SkipLinks(List<SkipTarget> targets) {
        addClassName("skip-links");
        getElement().setAttribute("role", "navigation");
        getElement().setAttribute("aria-label", "Skip links");
        for (SkipTarget target : targets) {
            Anchor link = new Anchor("#" + target.id(), target.label());
            link.addClassName("skip-links__link");
            // Move focus to target on activation (anchor scrolling alone may not focus).
            link.getElement()
                    .addEventListener("click", e -> {})
                    .addEventData(
                            "event.target.dispatchEvent(new CustomEvent('skip-target', "
                                    + "{detail: '"
                                    + target.id()
                                    + "'}))");
            add(link);
        }
    }

    public record SkipTarget(String id, String label) {}
}
