package com.ordermgmt.railway.ui.component.business;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.ComboBoxVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import com.ordermgmt.railway.domain.business.model.AssignmentType;
import com.ordermgmt.railway.domain.business.model.Business;
import com.ordermgmt.railway.domain.business.model.BusinessStatus;
import com.ordermgmt.railway.domain.business.service.BusinessService;
import com.ordermgmt.railway.infrastructure.keycloak.KeycloakUserService;

/**
 * Bloomberg-style master-list card for a {@link Business}. Status gutter, status pill
 * (icon + text — never colour alone), title, link counts, due date.
 *
 * <p>Two inline quick-edits revealed on hover/focus: status (limited to allowed
 * transitions) and assignee (Keycloak user or group). Mutations call the service
 * directly and trigger {@code onChange} so the master list can refresh.
 */
public class BusinessCard extends Div {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yy");

    public BusinessCard(Business business,
                        Function<String, String> tr,
                        int linkedOrderPositions,
                        int linkedPurchasePositions,
                        BusinessService businessService,
                        KeycloakUserService keycloakUserService,
                        Runnable onChange) {
        addClassName("biz-card-tile");
        addClassName("biz-card-tile--" + business.getStatus().name().toLowerCase());

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

        body.add(buildStatusRow(business, tr, businessService, onChange));

        Span title = new Span(safe(business.getTitle()).isEmpty()
                ? "—" : safe(business.getTitle()));
        title.addClassName("biz-card-tile__title");
        body.add(title);

        body.add(buildMetaRow(business, linkedOrderPositions, linkedPurchasePositions, tr));
        body.add(buildAssigneeRow(business, tr, businessService, keycloakUserService, onChange));

        add(body);
    }

    // ─── Status pill + inline-edit overlay ─────────────────────

    private Div buildStatusRow(Business business, Function<String, String> tr,
                               BusinessService businessService, Runnable onChange) {
        Div row = new Div();
        row.addClassName("biz-card-tile__status-row");

        // Static pill (default state).
        row.add(buildStatusPill(business.getStatus(), tr));

        // Hidden quick-edit select (revealed on hover via CSS).
        var nextStatuses = business.getStatus().nextTargets();
        if (!nextStatuses.isEmpty()) {
            ComboBox<BusinessStatus> select = new ComboBox<>();
            select.addClassName("biz-card-tile__status-select");
            select.addThemeVariants(ComboBoxVariant.LUMO_SMALL);
            select.setItems(List.copyOf(nextStatuses));
            select.setItemLabelGenerator(s -> tr.apply("business.status." + s.name()));
            select.setPlaceholder(tr.apply("business.quickEdit.changeStatus"));
            // Stop click propagation so the card body click (open detail) does not fire.
            select.getElement().addEventListener("click", e -> {})
                    .addEventData("event.stopPropagation()");
            select.addValueChangeListener(e -> {
                if (e.getValue() == null || !e.isFromClient()) return;
                try {
                    businessService.setStatus(business.getId(), e.getValue());
                    onChange.run();
                } catch (RuntimeException ex) {
                    // swallow; could surface a notification but parent will refresh anyway
                }
            });
            row.add(select);
        }
        return row;
    }

    private HorizontalLayout buildStatusPill(BusinessStatus status, Function<String, String> tr) {
        var pill = new HorizontalLayout();
        pill.addClassName("biz-status-pill-icon");
        pill.addClassName("biz-card-tile__status-pill");
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

    // ─── Assignee row + inline-edit overlay ───────────────────

    private Div buildAssigneeRow(Business business, Function<String, String> tr,
                                 BusinessService businessService,
                                 KeycloakUserService keycloakUserService, Runnable onChange) {
        Div row = new Div();
        row.addClassName("biz-card-tile__assignee-row");

        // Static label (default).
        Div display = new Div();
        display.addClassName("biz-card-tile__assignee");
        AssignmentType type = AssignmentType.fromString(business.getAssignmentType());
        String name = business.getAssignmentName();
        String text;
        if (type != null && name != null && !name.isBlank()) {
            text = (type == AssignmentType.USER ? "👤 " : "👥 ") + name;
        } else if (name != null && !name.isBlank()) {
            text = name;
        } else {
            text = "— " + tr.apply("business.unassigned");
        }
        display.setText(text);
        row.add(display);

        // Hidden quick-edit (revealed on hover).
        var picker = new AssigneeComboBox(keycloakUserService, (t, v) -> {
            try {
                businessService.setAssignee(business.getId(), t, v);
                onChange.run();
            } catch (RuntimeException ex) {
                // ignore — parent refresh will reflect failure
            }
        });
        picker.addClassName("biz-card-tile__assignee-select");
        picker.preset(type, name);
        picker.setPlaceholder(tr.apply("business.quickEdit.changeAssignee"));
        picker.getElement().addEventListener("click", e -> {})
                .addEventData("event.stopPropagation()");
        row.add(picker);

        return row;
    }

    private Div buildMetaRow(Business business, int linkedOps, int linkedPps,
                             Function<String, String> tr) {
        Div meta = new Div();
        meta.addClassName("biz-card-tile__meta");

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
