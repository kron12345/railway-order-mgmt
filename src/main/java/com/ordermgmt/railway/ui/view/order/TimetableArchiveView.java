package com.ordermgmt.railway.ui.view.order;

import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.distanceLabel;
import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.timeOrDash;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.annotation.security.PermitAll;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import com.ordermgmt.railway.domain.order.model.Order;
import com.ordermgmt.railway.domain.order.model.OrderPosition;
import com.ordermgmt.railway.domain.order.model.PositionType;
import com.ordermgmt.railway.domain.order.service.OrderService;
import com.ordermgmt.railway.domain.timetable.model.TimetableArchive;
import com.ordermgmt.railway.domain.timetable.model.TimetableRouteResult;
import com.ordermgmt.railway.domain.timetable.model.TimetableRowData;
import com.ordermgmt.railway.domain.timetable.service.TimetableArchiveService;
import com.ordermgmt.railway.domain.timetable.service.TimetableRoutingService;
import com.ordermgmt.railway.ui.component.StatusBadge;
import com.ordermgmt.railway.ui.component.timetable.TimetableArchiveSidebar;
import com.ordermgmt.railway.ui.component.timetable.TimetableArchiveTable;
import com.ordermgmt.railway.ui.layout.MainLayout;

/** Read-only timetable detail view (Fahrplan-Detailansicht). */
@Route(value = "orders/:orderId/timetable/:positionId", layout = MainLayout.class)
@PageTitle("Timetable Detail")
@PermitAll
public class TimetableArchiveView extends VerticalLayout implements BeforeEnterObserver {

    private final OrderService orderService;
    private final TimetableArchiveService timetableArchiveService;
    private final TimetableRoutingService timetableRoutingService;

    private Order order;
    private OrderPosition position;
    private TimetableArchive archive;
    private List<TimetableRowData> rows = List.of();
    private TimetableRouteResult routeResult;

    public TimetableArchiveView(
            OrderService orderService,
            TimetableArchiveService timetableArchiveService,
            TimetableRoutingService timetableRoutingService) {
        this.orderService = orderService;
        this.timetableArchiveService = timetableArchiveService;
        this.timetableRoutingService = timetableRoutingService;
        setPadding(false);
        setSpacing(false);
        setSizeFull();
        getStyle()
                .set("background", "var(--rom-bg-primary)")
                .set("padding", "var(--lumo-space-s) var(--lumo-space-m)")
                .set("box-sizing", "border-box")
                .set("overflow", "hidden");
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String orderId = event.getRouteParameters().get("orderId").orElse(null);
        String positionId = event.getRouteParameters().get("positionId").orElse(null);
        if (orderId == null || positionId == null) {
            event.forwardTo("orders");
            return;
        }
        try {
            order = orderService.findById(UUID.fromString(orderId)).orElse(null);
            position = orderService.findPositionById(UUID.fromString(positionId)).orElse(null);
        } catch (IllegalArgumentException ex) {
            order = null;
            position = null;
        }
        if (order == null || position == null || position.getType() != PositionType.FAHRPLAN) {
            event.forwardTo(order != null ? "orders/" + order.getId() : "orders");
            return;
        }
        if (position.getOrder() == null || !order.getId().equals(position.getOrder().getId())) {
            event.forwardTo("orders/" + order.getId());
            return;
        }
        Optional<TimetableArchive> archiveOpt = timetableArchiveService.findArchive(position);
        if (archiveOpt.isEmpty()) {
            event.forwardTo("orders/" + order.getId());
            return;
        }
        archive = archiveOpt.get();
        rows = timetableArchiveService.readRows(archive);
        routeResult = timetableRoutingService.routeFromStoredRows(rows);
        buildView();
    }

    private void buildView() {
        removeAll();
        add(createHeader());

        SplitLayout split = new SplitLayout();
        split.setOrientation(SplitLayout.Orientation.HORIZONTAL);
        split.setSplitterPosition(65);
        split.setSizeFull();
        split.getStyle().set("min-height", "0");

        split.addToPrimary(new TimetableArchiveTable(rows, routeSummaryText()));
        split.addToSecondary(
                new TimetableArchiveSidebar(
                        archive, position, routeResult, timetableArchiveService));

        add(split);
        expand(split);
    }

    // ── Header ────────────────────────────────────────────────────────

    private Component createHeader() {
        Button back = new Button(VaadinIcon.ARROW_LEFT.create());
        back.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        back.getStyle().set("color", "var(--rom-text-secondary)");
        back.addClickListener(e -> navigateToOrder());

        String otn =
                hasText(archive.getOperationalTrainNumber())
                        ? archive.getOperationalTrainNumber()
                        : t("timetable.archive.noOtn");
        Span otnLabel = new Span(otn);
        otnLabel.getStyle()
                .set("font-family", "'JetBrains Mono', monospace")
                .set("font-size", "var(--lumo-font-size-xxl)")
                .set("font-weight", "700")
                .set(
                        "color",
                        hasText(archive.getOperationalTrainNumber())
                                ? "var(--rom-accent)"
                                : "var(--rom-text-muted)");

        Div titleBlock = new Div();
        Span posName = new Span(position.getName());
        posName.getStyle()
                .set("font-size", "13px")
                .set("font-weight", "600")
                .set("color", "var(--rom-text-primary)");
        Span routeInfo = new Span(routeSummaryText());
        routeInfo
                .getStyle()
                .set("display", "block")
                .set("font-size", "11px")
                .set("font-family", "'JetBrains Mono', monospace")
                .set("color", "var(--rom-text-muted)");
        titleBlock.add(posName, routeInfo);

        StatusBadge statusBadge = createStatusBadge();

        Button editBtn = new Button(t("timetable.archive.edit"), VaadinIcon.EDIT.create());
        editBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
        editBtn.getStyle()
                .set("background", "var(--rom-accent)")
                .set("color", "var(--rom-bg-primary)");
        editBtn.addClickListener(e -> navigateToBuilder());

        HorizontalLayout row =
                new HorizontalLayout(back, otnLabel, titleBlock, statusBadge, editBtn);
        row.setWidthFull();
        row.expand(titleBlock);
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.getStyle()
                .set("background", "var(--rom-bg-card)")
                .set("border", "1px solid var(--rom-border)")
                .set("border-radius", "6px")
                .set("padding", "12px 16px")
                .set("box-sizing", "border-box")
                .set("margin-bottom", "var(--lumo-space-s)")
                .set("gap", "12px");
        return row;
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private StatusBadge createStatusBadge() {
        if (position.getInternalStatus() == null) {
            return new StatusBadge("\u2014", StatusBadge.StatusType.NEUTRAL);
        }
        String label = t("position.status." + position.getInternalStatus().name());
        return switch (position.getInternalStatus()) {
            case IN_BEARBEITUNG, UEBERMITTELT ->
                    new StatusBadge(label, StatusBadge.StatusType.INFO);
            case FREIGEGEBEN, ABGESCHLOSSEN ->
                    new StatusBadge(label, StatusBadge.StatusType.SUCCESS);
            case UEBERARBEITEN, BEANTRAGT -> new StatusBadge(label, StatusBadge.StatusType.WARNING);
            case ANNULLIERT -> new StatusBadge(label, StatusBadge.StatusType.DANGER);
        };
    }

    private String routeSummaryText() {
        if (rows.isEmpty()) {
            return "\u2014";
        }
        return t(
                "timetable.route.summary",
                rows.size(),
                distanceLabel(routeResult.totalLengthMeters()),
                timeOrDash(rows.getFirst().getEstimatedDeparture()),
                timeOrDash(rows.getLast().getEstimatedArrival()));
    }

    private void navigateToOrder() {
        UI.getCurrent().navigate("orders/" + order.getId());
    }

    private void navigateToBuilder() {
        UI.getCurrent()
                .navigate(
                        "orders/"
                                + order.getId()
                                + "/timetable-builder?positionId="
                                + position.getId());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String t(String key, Object... params) {
        return getTranslation(key, params);
    }
}
