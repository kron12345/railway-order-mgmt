package com.ordermgmt.railway.ui.component.masterdetail;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.html.Div;

/**
 * Focusable "load more" row appended to a lazy {@link MasterDetailLayout} while further pages
 * exist. Triggers {@code onLoad} on click, on Enter/Space, and (via {@link #observe}) when scrolled
 * into view. Self-contained so the layout stays under the file-size limit and owns no
 * IntersectionObserver JS itself.
 */
class LazyLoadSentinel extends Div {

    // onAutoLoad fires when scrolled into view (no focus move — the user is scrolling, not
    // tabbing);
    // onInteractiveLoad fires on click/Enter/Space (the layout then moves focus to the new
    // content).
    private final transient Runnable onAutoLoad;
    private final transient Runnable onInteractiveLoad;

    LazyLoadSentinel(String label, Runnable onAutoLoad, Runnable onInteractiveLoad) {
        this.onAutoLoad = onAutoLoad;
        this.onInteractiveLoad = onInteractiveLoad;
        addClassName("md-load-more");
        // A role=listbox may only contain role=option children — the cards are options, so this
        // "load more" row is an option too (activating it loads the next page) rather than a
        // role=button nested illegally inside the listbox.
        getElement().setAttribute("role", "option");
        getElement().setAttribute("tabindex", "0");
        setText(label);
        getElement().setAttribute("aria-label", label);
        getStyle()
                .set("padding", "10px")
                .set("text-align", "center")
                .set("cursor", "pointer")
                .set("font-size", "12px")
                .set("color", "var(--rom-accent)")
                .set("outline", "none");
        getElement().addEventListener("click", e -> onInteractiveLoad.run());
        // stopPropagation so Enter/Space doesn't also bubble to the listbox keydown handler (which
        // would select the current row in the same keystroke).
        getElement()
                .addEventListener("keydown", e -> onInteractiveLoad.run())
                .addEventData("event.preventDefault()")
                .addEventData("event.stopPropagation()")
                .setFilter(
                        "event.key === 'Enter' || event.key === ' ' || event.key === 'Spacebar'");
    }

    /** Client callback: the IntersectionObserver saw this sentinel enter the scroll viewport. */
    @ClientCallable
    public void onVisible() {
        onAutoLoad.run();
    }

    /**
     * Observe this sentinel against {@code root} so scrolling it into view auto-loads. The observer
     * is stored on the stable scroll root (not this per-render element) and the previous one is
     * disconnected first, so re-rendering never leaks observers.
     */
    void observe(com.vaadin.flow.dom.Element scrollRoot) {
        getElement()
                .executeJs(
                        "const s=this, self=this, root=$0;"
                                + "if(root.__romObs){root.__romObs.disconnect();}"
                                + "const obs=new IntersectionObserver(function(es){"
                                + " for(const e of es){ if(e.isIntersecting){"
                                + " self.$server.onVisible(); break; } }"
                                + "},{root:root, rootMargin:'160px'});"
                                + "obs.observe(s); root.__romObs=obs;",
                        scrollRoot);
    }
}
