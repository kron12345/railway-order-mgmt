package com.ordermgmt.railway.ui.component.business;

import java.util.Locale;
import java.util.function.Function;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import com.ordermgmt.railway.domain.business.model.BusinessStatus;

/**
 * Shared status pill (icon + label) for a business status, used by both the master-list {@link
 * BusinessCard} and the {@link com.ordermgmt.railway.ui.view.business.BusinessReadView} detail. The
 * icon mapping and {@code biz-status-pill-icon*} classes were duplicated line-for-line in both;
 * callers pass extra CSS classes (e.g. the card's {@code biz-card-tile__status-pill}).
 */
public final class BusinessStatusPill {

    private static final String STATUS_TRANSLATION_PREFIX = "business.status.";

    private BusinessStatusPill() {}

    /** Builds the icon+label pill for the status; {@code extraClasses} are added to the wrapper. */
    public static HorizontalLayout icon(
            BusinessStatus status, Function<String, String> tr, String... extraClasses) {
        HorizontalLayout pill = new HorizontalLayout();
        pill.addClassName("biz-status-pill-icon");
        pill.addClassName("biz-status-pill-icon--" + status.name().toLowerCase(Locale.ROOT));
        for (String extraClass : extraClasses) {
            pill.addClassName(extraClass);
        }
        pill.setPadding(false);
        pill.setSpacing(false);

        var icon = iconFor(status).create();
        icon.addClassName("biz-status-pill-icon__icon");
        icon.getElement().setAttribute("aria-hidden", "true");
        pill.add(icon);

        var label = new Span(tr.apply(STATUS_TRANSLATION_PREFIX + status.name()));
        label.addClassName("biz-status-pill-icon__label");
        pill.add(label);
        return pill;
    }

    private static VaadinIcon iconFor(BusinessStatus status) {
        return switch (status) {
            case IN_BEARBEITUNG -> VaadinIcon.HOURGLASS;
            case FREIGEGEBEN -> VaadinIcon.CHECK_CIRCLE_O;
            case UEBERARBEITEN -> VaadinIcon.WARNING;
            case ABGESCHLOSSEN -> VaadinIcon.LOCK;
            case ANNULLIERT -> VaadinIcon.BAN;
        };
    }
}
