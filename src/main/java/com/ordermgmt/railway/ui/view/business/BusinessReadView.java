package com.ordermgmt.railway.ui.view.business;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.StreamResource;

import com.ordermgmt.railway.domain.business.model.AssignmentType;
import com.ordermgmt.railway.domain.business.model.Business;
import com.ordermgmt.railway.domain.business.model.BusinessDocument;
import com.ordermgmt.railway.domain.business.service.BusinessService;
import com.ordermgmt.railway.domain.order.model.OrderPosition;
import com.ordermgmt.railway.domain.order.model.PurchasePosition;

/**
 * Read-only detail view for a {@link Business}. Default mode in /businesses/{id}.
 * Edit / create still uses {@link BusinessDetailView} (form), reachable via the
 * "Bearbeiten" button which navigates to {@code /businesses/{id}/edit}.
 *
 * <p>Layout: header (title + status pill + edit/delete actions), then four cards —
 * Stammdaten, Verknüpfte Auftragspositionen (clickable → /orders/...), Verknüpfte
 * Bestellpositionen, Dokumente (download via StreamResource).
 */
public class BusinessReadView extends VerticalLayout {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("dd.MM.yy HH:mm");

    private final BusinessService businessService;
    private final java.util.function.Function<String, String> tr;
    private final Business business;

    public BusinessReadView(BusinessService businessService, UUID businessId,
                            java.util.function.Function<String, String> tr) {
        this.businessService = businessService;
        this.tr = tr;
        this.business = businessService.getById(businessId).orElse(null);

        addClassName("biz-read");
        setPadding(false);
        setSpacing(false);
        setSizeFull();
        getStyle().set("background", "var(--rom-bg-primary)");

        if (business == null) {
            add(new Span(tr.apply("business.notFound")));
            return;
        }

        add(buildHeader());
        add(buildStammdatenCard());
        add(buildLinkedOrderPositionsCard());
        add(buildLinkedPurchasePositionsCard());
        add(buildDocumentsCard());
    }

    // ─── Header ────────────────────────────────────────────────

    private HorizontalLayout buildHeader() {
        var bar = new HorizontalLayout();
        bar.addClassName("biz-detail__header");
        bar.addClassName("biz-read__header");
        bar.setWidthFull();
        bar.setPadding(false);
        bar.setSpacing(true);
        bar.setAlignItems(FlexComponent.Alignment.CENTER);

        var titleSpan = new Span(safe(business.getTitle()).isEmpty() ? "—" : business.getTitle());
        titleSpan.addClassName("biz-read__title");

        var statusBadge = buildStatusPillWithIcon(tr);

        var spacer = new Div();
        spacer.getStyle().set("flex", "1");

        var editBtn = new Button(tr.apply("common.edit"), VaadinIcon.EDIT.create());
        editBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        editBtn.addClassName("rom-btn-primary");
        editBtn.addClickListener(e -> UI.getCurrent().navigate("businesses/" + business.getId() + "/edit"));

        var deleteBtn = new Button(VaadinIcon.TRASH.create(), e -> confirmDelete());
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        deleteBtn.getStyle().setColor("var(--rom-status-danger)");

        bar.add(titleSpan, statusBadge, spacer, editBtn, deleteBtn);
        bar.setFlexGrow(1, spacer);
        return bar;
    }

    /** Status pill with icon, matching the master-card style. */
    private com.vaadin.flow.component.orderedlayout.HorizontalLayout buildStatusPillWithIcon(
            java.util.function.Function<String, String> tr) {
        var pill = new com.vaadin.flow.component.orderedlayout.HorizontalLayout();
        pill.addClassName("biz-status-pill-icon");
        pill.addClassName("biz-status-pill-icon--"
                + business.getStatus().name().toLowerCase());
        pill.setPadding(false);
        pill.setSpacing(false);
        var iconSpec = switch (business.getStatus()) {
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
        var label = new Span(tr.apply("business.status." + business.getStatus().name()));
        label.addClassName("biz-status-pill-icon__label");
        pill.add(label);
        return pill;
    }

    // ─── Stammdaten ────────────────────────────────────────────

    private Component buildStammdatenCard() {
        var card = new Div();
        card.addClassName("biz-card");
        card.add(sectionTitle(tr.apply("business.stammdaten")));

        Div grid = new Div();
        grid.addClassName("biz-info-grid");

        addRow(grid, tr.apply("business.description"), safe(business.getDescription()));
        addRow(grid, tr.apply("business.assignment"), formatAssignee());
        addRow(grid, tr.apply("business.team"), safe(business.getTeam()));
        addRow(grid, tr.apply("business.validity"), formatValidity());
        addRow(grid, tr.apply("business.dueDate"),
                business.getDueDate() == null ? "—" : business.getDueDate().format(DATE_FMT));
        addRow(grid, tr.apply("business.tags"), safe(business.getTags()));
        addRow(grid, tr.apply("audit.created"), formatAudit(business.getCreatedAt()));
        addRow(grid, tr.apply("audit.updated"), formatAudit(business.getUpdatedAt()));

        card.add(grid);
        return card;
    }

    // ─── Verknüpfte Auftragspositionen ─────────────────────────

    private Component buildLinkedOrderPositionsCard() {
        var card = new Div();
        card.addClassName("biz-card");
        card.add(sectionTitle(tr.apply("business.linkedPositions")));

        List<OrderPosition> ops = businessService.getLinkedOrderPositions(business.getId());
        if (ops.isEmpty()) {
            card.add(empty(tr.apply("business.noLinkedPositions")));
            return card;
        }
        Div list = new Div();
        list.addClassName("biz-link-list");
        for (OrderPosition op : ops) {
            list.add(buildOrderPositionRow(op));
        }
        card.add(list);
        return card;
    }

    private Component buildOrderPositionRow(OrderPosition op) {
        var row = new HorizontalLayout();
        row.addClassName("biz-link-row");
        row.setWidthFull();
        row.setSpacing(true);
        row.setPadding(false);
        row.setAlignItems(FlexComponent.Alignment.CENTER);

        var tag = new Span("AP");
        tag.addClassName("biz-tree-tag");
        tag.addClassName("biz-tree-tag--order_position");

        var name = new Span(safe(op.getName()).isEmpty() ? "—" : op.getName());
        name.addClassName("biz-link-row__name");

        var orderNum = new Span(op.getOrder() != null && op.getOrder().getOrderNumber() != null
                ? op.getOrder().getOrderNumber() : "—");
        orderNum.addClassName("biz-link-row__sub");

        var spacer = new Div();
        spacer.getStyle().set("flex", "1");

        var goBtn = new Button(VaadinIcon.ARROW_RIGHT.create());
        goBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY,
                ButtonVariant.LUMO_ICON);
        String route = op.getOrder() != null
                ? "orders/" + op.getOrder().getId() + "/positions/" + op.getId()
                : null;
        goBtn.setEnabled(route != null);
        goBtn.getElement().setAttribute("aria-label", tr.apply("business.openPosition"));
        if (route != null) goBtn.addClickListener(e -> UI.getCurrent().navigate(route));

        row.add(tag, name, orderNum, spacer, goBtn);
        row.setFlexGrow(1, spacer);
        if (route != null) {
            row.getElement().getStyle().set("cursor", "pointer");
            row.getElement().addEventListener("click", e -> UI.getCurrent().navigate(route));
        }
        return row;
    }

    // ─── Verknüpfte Bestellpositionen ──────────────────────────

    private Component buildLinkedPurchasePositionsCard() {
        var card = new Div();
        card.addClassName("biz-card");
        card.add(sectionTitle(tr.apply("business.linkedPurchasePositions")));

        List<PurchasePosition> pps = businessService.getLinkedPurchasePositions(business.getId());
        if (pps.isEmpty()) {
            card.add(empty(tr.apply("business.noLinkedPurchasePositions")));
            return card;
        }
        Div list = new Div();
        list.addClassName("biz-link-list");
        for (PurchasePosition pp : pps) {
            list.add(buildPurchasePositionRow(pp));
        }
        card.add(list);
        return card;
    }

    private Component buildPurchasePositionRow(PurchasePosition pp) {
        var row = new HorizontalLayout();
        row.addClassName("biz-link-row");
        row.setWidthFull();
        row.setSpacing(true);
        row.setPadding(false);
        row.setAlignItems(FlexComponent.Alignment.CENTER);

        var tag = new Span("BP");
        tag.addClassName("biz-tree-tag");
        tag.addClassName("biz-tree-tag--purchase_position");

        var num = new Span(safe(pp.getPositionNumber()).isEmpty() ? "—" : pp.getPositionNumber());
        num.addClassName("biz-link-row__sub");

        OrderPosition op = pp.getOrderPosition();
        var name = new Span(op != null && op.getName() != null ? op.getName() : "—");
        name.addClassName("biz-link-row__name");

        var orderNum = new Span(op != null && op.getOrder() != null && op.getOrder().getOrderNumber() != null
                ? op.getOrder().getOrderNumber() : "—");
        orderNum.addClassName("biz-link-row__sub");

        var spacer = new Div();
        spacer.getStyle().set("flex", "1");

        var goBtn = new Button(VaadinIcon.ARROW_RIGHT.create());
        goBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY,
                ButtonVariant.LUMO_ICON);
        // BP does not have its own detail route — jump to the parent order position.
        String route = op != null && op.getOrder() != null
                ? "orders/" + op.getOrder().getId() + "/positions/" + op.getId()
                : null;
        goBtn.setEnabled(route != null);
        goBtn.getElement().setAttribute("aria-label", tr.apply("business.openPosition"));
        if (route != null) goBtn.addClickListener(e -> UI.getCurrent().navigate(route));

        row.add(tag, num, name, orderNum, spacer, goBtn);
        row.setFlexGrow(1, spacer);
        if (route != null) {
            row.getElement().getStyle().set("cursor", "pointer");
            row.getElement().addEventListener("click", e -> UI.getCurrent().navigate(route));
        }
        return row;
    }

    // ─── Dokumente ─────────────────────────────────────────────

    private Component buildDocumentsCard() {
        var card = new Div();
        card.addClassName("biz-card");
        card.add(sectionTitle(tr.apply("business.documents")));

        List<BusinessDocument> docs = businessService.getDocuments(business.getId());
        if (docs.isEmpty()) {
            card.add(empty(tr.apply("business.noDocuments")));
            return card;
        }
        Div list = new Div();
        list.addClassName("biz-link-list");
        for (BusinessDocument doc : docs) {
            list.add(buildDocumentRow(doc));
        }
        card.add(list);
        return card;
    }

    private Component buildDocumentRow(BusinessDocument doc) {
        var row = new HorizontalLayout();
        row.addClassName("biz-link-row");
        row.setWidthFull();
        row.setSpacing(true);
        row.setPadding(false);
        row.setAlignItems(FlexComponent.Alignment.CENTER);

        var icon = VaadinIcon.FILE_O.create();
        icon.addClassName("biz-doc-icon");

        var name = new Span(safe(doc.getFilename()));
        name.addClassName("biz-link-row__name");

        var meta = new Span((doc.getContentType() == null ? "" : doc.getContentType())
                + (doc.getCreatedAt() != null ? "  ·  " + formatAudit(doc.getCreatedAt()) : ""));
        meta.addClassName("biz-link-row__sub");

        var spacer = new Div();
        spacer.getStyle().set("flex", "1");

        StreamResource resource = new StreamResource(
                doc.getFilename() != null ? doc.getFilename() : "document",
                () -> new ByteArrayInputStream(doc.getData() == null ? new byte[0] : doc.getData()));
        if (doc.getContentType() != null) resource.setContentType(doc.getContentType());

        var download = new Anchor(resource, "");
        download.add(VaadinIcon.DOWNLOAD.create());
        download.getElement().setAttribute("download", true);
        download.addClassName("biz-doc-download");
        download.getElement().setAttribute("aria-label", tr.apply("business.downloadDocument"));

        row.add(icon, name, meta, spacer, download);
        row.setFlexGrow(1, spacer);
        return row;
    }

    // ─── Helpers ───────────────────────────────────────────────

    private void confirmDelete() {
        var dialog = new ConfirmDialog();
        dialog.setHeader(tr.apply("business.deleteTitle"));
        dialog.setText(tr.apply("business.deleteInfo"));
        dialog.setCancelable(true);
        dialog.setConfirmText(tr.apply("common.delete"));
        dialog.setCancelText(tr.apply("common.cancel"));
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(e -> {
            businessService.delete(business.getId());
            Notification.show(tr.apply("business.deleted"), 1500,
                    Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            UI.getCurrent().navigate("businesses");
        });
        dialog.open();
    }

    private Span sectionTitle(String text) {
        var s = new Span(text.toUpperCase());
        s.addClassName("biz-section-title");
        return s;
    }

    private Span empty(String text) {
        var s = new Span(text);
        s.addClassName("biz-empty");
        return s;
    }

    private void addRow(Div grid, String label, String value) {
        var l = new Span(label);
        l.addClassName("biz-info-grid__label");
        var v = new Span(value == null || value.isBlank() ? "—" : value);
        v.addClassName("biz-info-grid__value");
        grid.add(l, v);
    }

    private String formatValidity() {
        LocalDate from = business.getValidFrom();
        LocalDate to = business.getValidTo();
        if (from == null && to == null) return "—";
        if (from != null && to != null) return from.format(DATE_FMT) + " → " + to.format(DATE_FMT);
        if (to != null) return tr.apply("business.validTo") + " " + to.format(DATE_FMT);
        return tr.apply("business.validFrom") + " " + from.format(DATE_FMT);
    }

    private String formatAssignee() {
        AssignmentType type = AssignmentType.fromString(business.getAssignmentType());
        String name = business.getAssignmentName();
        if (type == null || name == null || name.isBlank()) {
            return name == null || name.isBlank() ? "—" : name;
        }
        return (type == AssignmentType.USER ? "👤 " : "👥 ") + name;
    }

    private String formatAudit(java.time.Instant ts) {
        if (ts == null) return "—";
        LocalDateTime ldt = ts.atZone(ZoneId.systemDefault()).toLocalDateTime();
        return ldt.format(TS_FMT);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
