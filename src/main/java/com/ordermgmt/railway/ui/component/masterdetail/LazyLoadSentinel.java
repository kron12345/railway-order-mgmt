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

    private final transient Runnable onLoad;

    LazyLoadSentinel(String label, Runnable onLoad) {
        this.onLoad = onLoad;
        addClassName("md-load-more");
        getElement().setAttribute("role", "button");
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
        getElement().addEventListener("click", e -> onLoad.run());
        // stopPropagation so Enter/Space doesn't also bubble to the listbox keydown handler (which
        // would select the current row in the same keystroke).
        getElement()
                .addEventListener("keydown", e -> onLoad.run())
                .addEventData("event.preventDefault()")
                .addEventData("event.stopPropagation()")
                .setFilter(
                        "event.key === 'Enter' || event.key === ' ' || event.key === 'Spacebar'");
    }

    /** Client callback: the IntersectionObserver saw this sentinel enter the scroll viewport. */
    @ClientCallable
    public void onVisible() {
        onLoad.run();
    }

    /**
     * Observe this sentinel against {@code root} so scrolling it into view auto-loads. The observer
     * is stored on the stable scroll root (not this per-render element) and the previous one is
     * disconnected first, so re-rendering never leaks observers.
     */
    void observe(com.vaadin.flow.dom.Element root) {
        getElement()
                .executeJs(
                        "const s=this, self=this, root=$0;"
                                + "if(root.__romObs){root.__romObs.disconnect();}"
                                + "const obs=new IntersectionObserver(function(es){"
                                + " for(const e of es){ if(e.isIntersecting){"
                                + " self.$server.onVisible(); break; } }"
                                + "},{root:root, rootMargin:'160px'});"
                                + "obs.observe(s); root.__romObs=obs;",
                        root);
    }
}
