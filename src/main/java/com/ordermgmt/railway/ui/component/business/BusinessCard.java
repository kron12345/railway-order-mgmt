package com.ordermgmt.railway.ui.component.business;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;

import com.ordermgmt.railway.domain.business.model.AssignmentType;
import com.ordermgmt.railway.domain.business.model.BusinessStatus;
import com.ordermgmt.railway.domain.business.service.BusinessService;
import com.ordermgmt.railway.dto.business.BusinessListItem;
import com.ordermgmt.railway.infrastructure.keycloak.CurrentUserHelper;
import com.ordermgmt.railway.infrastructure.keycloak.KeycloakUserService;

/**
 * Bloomberg-style master-list card for a business, built from the lazy {@link BusinessListItem}
 * projection (P3/P4) — no entity or n:m collection is touched. Status gutter, status (icon + text),
 * title, link counts, due date, assignee.
 *
 * <p>Status and assignee are always rendered as real form controls (not toggled on hover). Default
 * styling makes them look like static pills — the dropdown affordance shows only on hover/focus.
 * This keeps the card height stable (no layout jiggle when cycling cards) and makes both edits
 * reachable via Tab without any visibility games.
 */
public class BusinessCard extends Div {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yy");

    public BusinessCard(
            BusinessListItem business,
            Function<String, String> tr,
            BusinessService businessService,
            KeycloakUserService keycloakUserService,
            Runnable onChange) {
        addClassName("biz-card-tile");
        addClassName("biz-card-tile--" + business.status().name().toLowerCase());

        getElement()
                .setAttribute(
                        "aria-label",
                        tr.apply("business.title")
                                + ": "
                                + safe(business.title())
                                + ", "
                                + tr.apply("business.status.aria")
                                + " "
                                + tr.apply("business.status." + business.status().name()));

        Div gutter = new Div();
        gutter.addClassName("biz-card-tile__gutter");
        gutter.getElement().setAttribute("aria-hidden", "true");
        add(gutter);

        Div body = new Div();
        body.addClassName("biz-card-tile__body");

        body.add(buildStatusControl(business, tr, businessService, onChange));

        Span title = new Span(safe(business.title()).isEmpty() ? "—" : safe(business.title()));
        title.addClassName("biz-card-tile__title");
        body.add(title);

        body.add(buildMetaRow(business, tr));
        body.add(
                buildAssigneeControl(business, tr, businessService, keycloakUserService, onChange));

        add(body);
    }

    // ─── Status as Select styled like a pill ───────────────────

    private Component buildStatusControl(
            BusinessListItem business,
            Function<String, String> tr,
            BusinessService businessService,
            Runnable onChange) {
        // Non-mutating users (no ADMIN/DISPATCHER role) see a read-only pill; the service layer
        // enforces the same rule, so this just removes a control that would always fail.
        if (!CurrentUserHelper.hasAnyRole("ADMIN", "DISPATCHER")) {
            return buildStaticStatusPill(business.status(), tr);
        }
        // Terminal status (no transitions allowed) → static pill, not editable.
        Set<BusinessStatus> next = business.status().nextTargets();
        if (next.isEmpty()) {
            return buildStaticStatusPill(business.status(), tr);
        }

        // Otherwise: a Select that includes current + valid transitions, styled like the pill.
        Set<BusinessStatus> options = new LinkedHashSet<>();
        options.add(business.status());
        options.addAll(next);

        Select<BusinessStatus> select = new Select<>();
        select.addClassName("biz-card-tile__status-select");
        select.addClassName("biz-status-pill-icon--" + business.status().name().toLowerCase());
        select.setItems(options);
        select.setValue(business.status());
        select.setItemLabelGenerator(s -> tr.apply("business.status." + s.name()));
        select.getElement().setAttribute("aria-label", tr.apply("business.status"));
        // Stop card click → detail when interacting with the select.
        select.getElement()
                .addEventListener("click", e -> {})
                .addEventData("event.stopPropagation()");
        select.getElement()
                .addEventListener("mousedown", e -> {})
                .addEventData("event.stopPropagation()");
        select.addValueChangeListener(
                e -> {
                    if (e.getValue() == null || !e.isFromClient()) return;
                    if (e.getValue() == e.getOldValue()) return;
                    try {
                        businessService.setStatus(business.id(), e.getValue());
                        onChange.run();
                    } catch (RuntimeException ex) {
                        // parent refresh shows current state
                    }
                });
        return select;
    }

    private HorizontalLayout buildStaticStatusPill(
            BusinessStatus status, Function<String, String> tr) {
        var pill = new HorizontalLayout();
        pill.addClassName("biz-status-pill-icon");
        pill.addClassName("biz-card-tile__status-pill");
        pill.addClassName("biz-status-pill-icon--" + status.name().toLowerCase());
        pill.setPadding(false);
        pill.setSpacing(false);

        VaadinIcon iconSpec =
                switch (status) {
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

    // ─── Assignee always-on combo styled like text ─────────────

    private Component buildAssigneeControl(
            BusinessListItem business,
            Function<String, String> tr,
            BusinessService businessService,
            KeycloakUserService keycloakUserService,
            Runnable onChange) {
        var picker =
                new AssigneeComboBox(
                        keycloakUserService,
                        (t, v) -> {
                            try {
                                businessService.setAssignee(business.id(), t, v);
                                onChange.run();
                            } catch (RuntimeException ex) {
                                // parent refresh shows current state
                            }
                        });
        picker.addClassName("biz-card-tile__assignee-select");
        picker.preset(
                AssignmentType.fromString(business.assignmentType()), business.assignmentName());
        picker.setPlaceholder("— " + tr.apply("business.unassigned"));
        picker.getElement().setAttribute("aria-label", tr.apply("business.assignment"));
        picker.getElement()
                .addEventListener("click", e -> {})
                .addEventData("event.stopPropagation()");
        picker.getElement()
                .addEventListener("mousedown", e -> {})
                .addEventData("event.stopPropagation()");
        return picker;
    }

    private Div buildMetaRow(BusinessListItem business, Function<String, String> tr) {
        Div meta = new Div();
        meta.addClassName("biz-card-tile__meta");

        Span counts =
                new Span(
                        business.linkedOrderPositionCount()
                                + " "
                                + tr.apply("business.tree.tag.AP")
                                + " · "
                                + business.linkedPurchasePositionCount()
                                + " "
                                + tr.apply("business.tree.tag.BP"));
        counts.addClassName("biz-card-tile__counts");
        meta.add(counts);

        Span sep = new Span(" · ");
        sep.addClassName("biz-card-tile__sep");
        sep.getElement().setAttribute("aria-hidden", "true");
        meta.add(sep);

        Span due = new Span();
        due.addClassName("biz-card-tile__due");
        if (business.validTo() != null) {
            due.setText(tr.apply("business.validTo") + " " + business.validTo().format(DATE_FMT));
        } else if (business.dueDate() != null) {
            due.setText(tr.apply("business.dueDate") + " " + business.dueDate().format(DATE_FMT));
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
