package com.ordermgmt.railway.ui.component.a11y;

import java.util.List;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Nav;
import com.vaadin.flow.component.html.Span;

/**
 * Pinned breadcrumb trail for orientation in nested master-detail flows (e.g. <i>Aufträge ›
 * #A-2026-001 › Position 04</i>).
 *
 * <p>Renders as {@code <nav aria-label="breadcrumb">} with the last crumb marked {@code
 * aria-current="page"} — meets WAI-ARIA Authoring Practices breadcrumb pattern.
 */
public class BreadcrumbBar extends Nav {

    public BreadcrumbBar() {
        addClassName("biz-breadcrumb");
        getElement().setAttribute("aria-label", "Breadcrumb");
    }

    public BreadcrumbBar(List<Crumb> crumbs) {
        this();
        setCrumbs(crumbs);
    }

    public void setCrumbs(List<Crumb> crumbs) {
        removeAll();
        for (int i = 0; i < crumbs.size(); i++) {
            Crumb crumb = crumbs.get(i);
            boolean isLastCrumb = i == crumbs.size() - 1;
            if (isLastCrumb || crumb.route() == null) {
                Span currentCrumb = new Span(crumb.label());
                currentCrumb.addClassName("biz-breadcrumb__current");
                if (isLastCrumb) {
                    currentCrumb.getElement().setAttribute("aria-current", "page");
                }
                add(currentCrumb);
            } else {
                Anchor breadcrumbLink = new Anchor("", crumb.label());
                breadcrumbLink.addClassName("biz-breadcrumb__link");
                String route = crumb.route();
                breadcrumbLink
                        .getElement()
                        .addEventListener("click", e -> UI.getCurrent().navigate(route))
                        .addEventData("event.preventDefault()");
                add(breadcrumbLink);
            }
            if (!isLastCrumb) {
                Span separator = new Span("›");
                separator.addClassName("biz-breadcrumb__sep");
                separator.getElement().setAttribute("aria-hidden", "true");
                add(separator);
            }
        }
    }

    /** A single breadcrumb step. {@code route} may be null for the current step. */
    public record Crumb(String label, String route) {}
}
