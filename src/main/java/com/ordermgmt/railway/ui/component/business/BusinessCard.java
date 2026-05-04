package com.ordermgmt.railway.ui.component.business;

import java.time.format.DateTimeFormatter;
import java.util.function.Function;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import com.ordermgmt.railway.domain.business.model.Business;
import com.ordermgmt.railway.domain.business.model.BusinessStatus;

/**
 * Bloomberg-style master-list card for a {@link Business}: a colour-coded status
 * gutter on the left, status pill (icon + text — never colour alone), title,
 * link counts, and due date. Status colour coding is also exposed as a CSS class
 * so the gutter ribbon matches.
 */
public class BusinessCard extends Div {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yy");

    public BusinessCard(Business business,
                        Function<String, String> tr,
                        int linkedOrderPositions,
                        int linkedPurchasePositions) {
        addClassName("biz-card-tile");
        addClassName("biz-card-tile--" + business.getStatus().name().toLowerCase());

        // Accessible name: pronounced when the card receives focus.
        getElement().setAttribute("aria-label",
                tr.apply("business.title") + ": " + safe(business.getTitle())
                        + ", " + tr.apply("business.status.aria") + " "
                        + tr.apply("business.status." + business.getStatus().name()));

        Div gutter = new Div();
        gutter.addClassName("biz-card-tile__gutter");
        gutter.getElement().setAttribute("aria-hidden", "true");
        add(gutter);

        Div body = new Div();
        body.addClassName("biz-card-tile__body");

        body.add(buildStatusPill(business.getStatus(), tr));

        Span title = new Span(safe(business.getTitle()).isEmpty()
                ? "—" : safe(business.getTitle()));
        title.addClassName("biz-card-tile__title");
        body.add(title);

        body.add(buildMetaRow(business, linkedOrderPositions, linkedPurchasePositions, tr));

        add(body);
    }

    private HorizontalLayout buildStatusPill(BusinessStatus status, Function<String, String> tr) {
        var pill = new HorizontalLayout();
        pill.addClassName("biz-status-pill-icon");
        pill.addClassName("biz-status-pill-icon--" + status.name().toLowerCase());
        pill.setPadding(false);
        pill.setSpacing(false);

        VaadinIcon iconSpec = switch (status) {
            case IN_BEARBEITUNG -> VaadinIcon.HOURGLASS;
            case FREIGEGEBEN -> VaadinIcon.CHECK_CIRCLE_O;
            case UEBERARBEITEN -> VaadinIcon.WARNING;
            case ABGESCHLOSSEN -> VaadinIcon.LOCK;
            case ANNULLIERT -> VaadinIcon.BAN;
        };
        var icon = iconSpec.create();
        icon.addClassName("biz-status-pill-icon__icon");
        icon.getElement().setAttribute("aria-hidden", "true");
        pill.add(icon);

        Span label = new Span(tr.apply("business.status." + status.name()));
        label.addClassName("biz-status-pill-icon__label");
        pill.add(label);

        return pill;
    }

    private Div buildMetaRow(Business business, int linkedOps, int linkedPps,
                             Function<String, String> tr) {
        Div meta = new Div();
        meta.addClassName("biz-card-tile__meta");

        // Counts: explicit text + count, no colour-only signalling.
        Span counts = new Span(linkedOps + " " + tr.apply("business.tree.tag.AP")
                + " · " + linkedPps + " " + tr.apply("business.tree.tag.BP"));
        counts.addClassName("biz-card-tile__counts");
        meta.add(counts);

        Span sep = new Span(" · ");
        sep.addClassName("biz-card-tile__sep");
        sep.getElement().setAttribute("aria-hidden", "true");
        meta.add(sep);

        Span due = new Span();
        due.addClassName("biz-card-tile__due");
        if (business.getValidTo() != null) {
            due.setText(tr.apply("business.validTo") + " " + business.getValidTo().format(DATE_FMT));
        } else if (business.getDueDate() != null) {
            due.setText(tr.apply("business.dueDate") + " " + business.getDueDate().format(DATE_FMT));
        } else {
            due.setText("—");
        }
        meta.add(due);

        return meta;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
