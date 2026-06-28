package com.ordermgmt.railway.ui.view.business;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

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
import com.ordermgmt.railway.domain.business.service.BusinessService;
import com.ordermgmt.railway.domain.order.model.OrderPosition;
import com.ordermgmt.railway.domain.order.model.PurchasePosition;
import com.ordermgmt.railway.domain.order.service.AuditService;
import com.ordermgmt.railway.dto.business.BusinessDocumentMeta;
import com.ordermgmt.railway.infrastructure.keycloak.CurrentUserHelper;
import com.ordermgmt.railway.ui.component.AuditHistoryDialog;
import com.ordermgmt.railway.ui.component.business.BusinessStatusPill;
import com.ordermgmt.railway.ui.util.StringUtils;

/**
 * Read-only detail view for a {@link Business}. Default mode in /businesses/{id}. Edit / create
 * still uses {@link BusinessDetailView} (form), reachable via the "Bearbeiten" button which
 * navigates to {@code /businesses/{id}/edit}.
 *
 * <p>Layout: header (title + status pill + edit/delete actions), then four cards — Stammdaten,
 * Verknüpfte Auftragspositionen (clickable → /orders/...), Verknüpfte Bestellpositionen, Dokumente
 * (download via StreamResource).
 */
public class BusinessReadView extends VerticalLayout {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("dd.MM.yy HH:mm");

    private final BusinessService businessService;
    private final AuditService auditService;
    private final Function<String, String> tr;
    private final Business business;
    private final List<OrderPosition> linkedOrderPositions;
    private final List<PurchasePosition> linkedPurchasePositions;
    private final List<BusinessDocumentMeta> documents;

    public BusinessReadView(
            BusinessService businessService,
            AuditService auditService,
            UUID businessId,
            Function<String, String> tr) {
        this.businessService = businessService;
        this.auditService = auditService;
        this.tr = tr;
        // One transaction loads the business + its three UI collections (was 4-5 findById).
        var readModel = businessService.loadReadModel(businessId).orElse(null);
        this.business = readModel == null ? null : readModel.business();
        this.linkedOrderPositions =
                readModel == null ? java.util.List.of() : readModel.orderPositions();
        this.linkedPurchasePositions =
                readModel == null ? java.util.List.of() : readModel.purchasePositions();
        this.documents = readModel == null ? java.util.List.of() : readModel.documents();

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

        var statusBadge = BusinessStatusPill.icon(business.getStatus(), tr);

        var spacer = new Div();
        spacer.getStyle().set("flex", "1");

        var historyBtn = new Button(tr.apply("audit.button"), VaadinIcon.CLOCK.create());
        historyBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        historyBtn.addClickListener(e -> openAuditHistory());

        bar.add(titleSpan, statusBadge, spacer, historyBtn);

        // Edit/Delete only for users who may mutate (service layer enforces it too).
        if (CurrentUserHelper.hasAnyRole("ADMIN", "DISPATCHER")) {
            var editBtn = new Button(tr.apply("common.edit"), VaadinIcon.EDIT.create());
            editBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
            editBtn.addClassName("rom-btn-primary");
            editBtn.addClickListener(
                    e -> UI.getCurrent().navigate("businesses/" + business.getId() + "/edit"));

            var deleteBtn = new Button(VaadinIcon.TRASH.create(), e -> confirmDelete());
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            deleteBtn.getStyle().setColor("var(--rom-status-danger)");

            bar.add(editBtn, deleteBtn);
        }
        bar.setFlexGrow(1, spacer);
        return bar;
    }

    private void openAuditHistory() {
        var entries = auditService.getBusinessHistory(business.getId());
        new AuditHistoryDialog(
                        tr.apply("audit.title") + " — " + safe(business.getTitle()),
                        entries,
                        (k, args) -> tr.apply(k))
                .open();
    }

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
        addRow(
                grid,
                tr.apply("business.dueDate"),
                business.getDueDate() == null ? "—" : business.getDueDate().format(DATE_FMT));
        addRow(grid, tr.apply("business.tags"), safe(business.getTags()));
        addRow(grid, tr.apply("audit.created"), formatAudit(business.getCreatedAt()));
        addRow(grid, tr.apply("audit.updated"), formatAudit(business.getUpdatedAt()));

        card.add(grid);
        return card;
    }

    private Component buildLinkedOrderPositionsCard() {
        var card = new Div();
        card.addClassName("biz-card");
        card.add(sectionTitle(tr.apply("business.linkedPositions")));

        List<OrderPosition> ops = linkedOrderPositions;
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

        var orderNum =
                new Span(
                        op.getOrder() != null && op.getOrder().getOrderNumber() != null
                                ? op.getOrder().getOrderNumber()
                                : "—");
        orderNum.addClassName("biz-link-row__sub");

        var spacer = new Div();
        spacer.getStyle().set("flex", "1");

        String route =
                op.getOrder() != null
                        ? "orders/" + op.getOrder().getId() + "/positions/" + op.getId()
                        : null;
        var goBtn = createOpenPositionButton(route);

        row.add(tag, name, orderNum, spacer, goBtn);
        row.setFlexGrow(1, spacer);
        makeRowNavigable(row, route);
        return row;
    }

    private Component buildLinkedPurchasePositionsCard() {
        var card = new Div();
        card.addClassName("biz-card");
        card.add(sectionTitle(tr.apply("business.linkedPurchasePositions")));

        List<PurchasePosition> pps = linkedPurchasePositions;
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

        var orderNum =
                new Span(
                        op != null
                                        && op.getOrder() != null
                                        && op.getOrder().getOrderNumber() != null
                                ? op.getOrder().getOrderNumber()
                                : "—");
        orderNum.addClassName("biz-link-row__sub");

        var spacer = new Div();
        spacer.getStyle().set("flex", "1");

        // BP does not have its own detail route — jump to the parent order position.
        String route =
                op != null && op.getOrder() != null
                        ? "orders/" + op.getOrder().getId() + "/positions/" + op.getId()
                        : null;
        var goBtn = createOpenPositionButton(route);

        row.add(tag, num, name, orderNum, spacer, goBtn);
        row.setFlexGrow(1, spacer);
        makeRowNavigable(row, route);
        return row;
    }

    private Button createOpenPositionButton(String route) {
        var button = new Button(VaadinIcon.ARROW_RIGHT.create());
        button.addThemeVariants(
                ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        button.setEnabled(route != null);
        button.getElement().setAttribute("aria-label", tr.apply("business.openPosition"));
        if (route != null) {
            button.addClickListener(e -> UI.getCurrent().navigate(route));
        }
        return button;
    }

    private void makeRowNavigable(HorizontalLayout row, String route) {
        if (route == null) {
            return;
        }
        row.getElement().getStyle().set("cursor", "pointer");
        row.getElement().addEventListener("click", e -> UI.getCurrent().navigate(route));
    }

    private Component buildDocumentsCard() {
        var card = new Div();
        card.addClassName("biz-card");
        card.add(sectionTitle(tr.apply("business.documents")));

        List<BusinessDocumentMeta> docs = documents;
        if (docs.isEmpty()) {
            card.add(empty(tr.apply("business.noDocuments")));
            return card;
        }
        Div list = new Div();
        list.addClassName("biz-link-list");
        for (BusinessDocumentMeta doc : docs) {
            list.add(buildDocumentRow(doc));
        }
        card.add(list);
        return card;
    }

    private Component buildDocumentRow(BusinessDocumentMeta doc) {
        var row = new HorizontalLayout();
        row.addClassName("biz-link-row");
        row.setWidthFull();
        row.setSpacing(true);
        row.setPadding(false);
        row.setAlignItems(FlexComponent.Alignment.CENTER);

        var icon = VaadinIcon.FILE_O.create();
        icon.addClassName("biz-doc-icon");

        var name = new Span(safe(doc.filename()));
        name.addClassName("biz-link-row__name");

        var meta =
                new Span(
                        (doc.contentType() == null ? "" : doc.contentType())
                                + (doc.createdAt() != null
                                        ? "  ·  " + formatAudit(doc.createdAt())
                                        : ""));
        meta.addClassName("biz-link-row__sub");

        var spacer = new Div();
        spacer.getStyle().set("flex", "1");

        // Always force download. Even with a sanitised content-type, never let the
        // browser render uploaded business documents inline (defence in depth against
        // user-supplied HTML/SVG slipping past the MIME whitelist via filename guesses).
        // The blob is fetched on demand here, not when the document list is rendered.
        String safeName = doc.filename() != null ? doc.filename() : "document";
        StreamResource resource =
                new StreamResource(
                        safeName,
                        () -> new ByteArrayInputStream(businessService.getDocumentData(doc.id())));
        if (doc.contentType() != null) resource.setContentType(doc.contentType());
        resource.setHeader(
                "Content-Disposition",
                "attachment; filename=\"" + sanitiseHeaderValue(safeName) + "\"");

        var download = new Anchor(resource, "");
        download.add(VaadinIcon.DOWNLOAD.create());
        download.getElement().setAttribute("download", true);
        download.addClassName("biz-doc-download");
        download.getElement().setAttribute("aria-label", tr.apply("business.downloadDocument"));

        var historyBtn = new Button(VaadinIcon.CLOCK.create(), e -> openDocumentHistory(doc));
        historyBtn.addThemeVariants(
                ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        historyBtn.getElement().setAttribute("aria-label", tr.apply("audit.button"));

        row.add(icon, name, meta, spacer, historyBtn, download);
        row.setFlexGrow(1, spacer);
        return row;
    }

    private void openDocumentHistory(BusinessDocumentMeta doc) {
        var entries = auditService.getBusinessDocumentHistory(doc.id());
        new AuditHistoryDialog(
                        tr.apply("audit.title") + " — " + safe(doc.filename()),
                        entries,
                        (k, args) -> tr.apply(k))
                .open();
    }

    private void confirmDelete() {
        var dialog = new ConfirmDialog();
        dialog.setHeader(tr.apply("business.deleteTitle"));
        dialog.setText(tr.apply("business.deleteInfo"));
        dialog.setCancelable(true);
        dialog.setConfirmText(tr.apply("common.delete"));
        dialog.setCancelText(tr.apply("common.cancel"));
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(
                e -> {
                    businessService.delete(business.getId());
                    Notification.show(
                                    tr.apply("business.deleted"),
                                    1500,
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
        return StringUtils.nvl(s);
    }

    /** Strip CR/LF/quotes from a value before it lands in an HTTP header (RFC 7230). */
    private static String sanitiseHeaderValue(String s) {
        if (s == null) return "";
        return s.replaceAll("[\\r\\n\"]", "");
    }
}
