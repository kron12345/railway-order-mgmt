package com.ordermgmt.railway.ui.view.order;

import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.distanceLabel;
import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.timeOrDash;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.annotation.security.RolesAllowed;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import com.ordermgmt.railway.domain.business.service.BusinessService;
import com.ordermgmt.railway.domain.infrastructure.model.PredefinedTag;
import com.ordermgmt.railway.domain.infrastructure.repository.OperationalPointRepository;
import com.ordermgmt.railway.domain.infrastructure.repository.PredefinedTagRepository;
import com.ordermgmt.railway.domain.order.model.Order;
import com.ordermgmt.railway.domain.order.model.OrderPosition;
import com.ordermgmt.railway.domain.order.model.PositionType;
import com.ordermgmt.railway.domain.order.service.OrderService;
import com.ordermgmt.railway.domain.timetable.model.IntervalGenerationCommand;
import com.ordermgmt.railway.domain.timetable.model.TimetableActivityOption;
import com.ordermgmt.railway.domain.timetable.model.TimetableRouteResult;
import com.ordermgmt.railway.domain.timetable.model.TimetableRowData;
import com.ordermgmt.railway.domain.timetable.service.IntervalTimetableService;
import com.ordermgmt.railway.domain.timetable.service.TimetableArchiveService;
import com.ordermgmt.railway.domain.timetable.service.TimetableEditingService;
import com.ordermgmt.railway.domain.timetable.service.TimetableRoutingService;
import com.ordermgmt.railway.ui.component.ValidityCalendar;
import com.ordermgmt.railway.ui.component.timetable.IntervalTimetablePanel;
import com.ordermgmt.railway.ui.component.timetable.TimetableBuilderChrome;
import com.ordermgmt.railway.ui.component.timetable.TimetableBuilderChrome.StatusKind;
import com.ordermgmt.railway.ui.component.timetable.TimetableBuilderChrome.Step;
import com.ordermgmt.railway.ui.component.timetable.TimetableDataLoader;
import com.ordermgmt.railway.ui.component.timetable.TimetableMetadataCard;
import com.ordermgmt.railway.ui.component.timetable.TimetableRouteStep;
import com.ordermgmt.railway.ui.component.timetable.TimetableTableStep;
import com.ordermgmt.railway.ui.layout.MainLayout;

/** Full-screen two-step builder for timetable order positions. */
@Route(value = "orders/:orderId/timetable-builder", layout = MainLayout.class)
@PageTitle("Timetable Builder")
@RolesAllowed({"ADMIN", "DISPATCHER"})
public class TimetableBuilderView extends VerticalLayout
        implements BeforeEnterObserver, BeforeLeaveObserver {

    private final OrderService orderService;
    private final OperationalPointRepository operationalPointRepository;
    private final PredefinedTagRepository predefinedTagRepository;
    private final TimetableRoutingService timetableRoutingService;
    private final TimetableArchiveService timetableArchiveService;
    private final TimetableEditingService timetableEditingService;
    private final IntervalTimetableService intervalTimetableService;
    private final BusinessService businessService;
    private final Div contentSlot = new Div();
    private final TimetableBuilderChrome chrome = new TimetableBuilderChrome(this::t);
    private TimetableMetadataCard metadataCard;
    private final List<TimetableRowData> timetableRows = new ArrayList<>();
    private boolean routeDirty = false;
    private ValidityCalendar validityCalendar;
    private Order order;
    private OrderPosition existingPosition;
    private TimetableRouteResult currentRoute = new TimetableRouteResult(List.of(), 0D);
    private Step currentStep = Step.ROUTE;
    private List<PredefinedTag> availableTags = List.of();
    private List<TimetableActivityOption> activityOptions = List.of();
    private TimetableRouteStep routeStep;
    private TimetableTableStep tableStep;
    private IntervalTimetablePanel intervalPanel;

    public TimetableBuilderView(
            OrderService orderService,
            OperationalPointRepository operationalPointRepository,
            PredefinedTagRepository predefinedTagRepository,
            TimetableRoutingService timetableRoutingService,
            TimetableArchiveService timetableArchiveService,
            TimetableEditingService timetableEditingService,
            IntervalTimetableService intervalTimetableService,
            BusinessService businessService) {
        this.orderService = orderService;
        this.operationalPointRepository = operationalPointRepository;
        this.predefinedTagRepository = predefinedTagRepository;
        this.timetableRoutingService = timetableRoutingService;
        this.timetableArchiveService = timetableArchiveService;
        this.timetableEditingService = timetableEditingService;
        this.intervalTimetableService = intervalTimetableService;
        this.businessService = businessService;
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
        if (orderId == null) {
            event.forwardTo("orders");
            return;
        }
        try {
            order = orderService.findById(UUID.fromString(orderId)).orElse(null);
        } catch (IllegalArgumentException ex) {
            order = null;
        }
        if (order == null) {
            event.forwardTo("orders");
            return;
        }
        existingPosition = resolvePosition(event);
        loadReferenceData();
        buildView();
        loadExistingData();
    }

    @Override
    public void beforeLeave(BeforeLeaveEvent event) {
        if (hasUnsavedChanges()) {
            BeforeLeaveEvent.ContinueNavigationAction action = event.postpone();
            ConfirmDialog dialog = new ConfirmDialog();
            dialog.setHeader(t("common.unsavedChanges"));
            dialog.setText(t("common.unsavedChanges.text"));
            dialog.setCancelable(true);
            dialog.addConfirmListener(e -> action.proceed());
            dialog.open();
        }
    }

    private boolean hasUnsavedChanges() {
        return !timetableRows.isEmpty() && currentStep != Step.ROUTE;
    }

    private OrderPosition resolvePosition(BeforeEnterEvent event) {
        String pid =
                event
                        .getLocation()
                        .getQueryParameters()
                        .getParameters()
                        .getOrDefault("positionId", List.of())
                        .stream()
                        .findFirst()
                        .orElse(null);
        if (pid == null || pid.isBlank()) {
            return null;
        }
        try {
            OrderPosition pos = orderService.findPositionById(UUID.fromString(pid)).orElse(null);
            if (pos == null
                    || pos.getOrder() == null
                    || !order.getId().equals(pos.getOrder().getId())
                    || pos.getType() != PositionType.FAHRPLAN) {
                return null;
            }
            return pos;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private void loadReferenceData() {
        // P1b: operational points are no longer pre-loaded. The route/table combos search the repo
        // lazily and the map fetches only its current viewport, so opening the builder never pulls
        // all ~19k points; existing-position pre-fill resolves just its referenced OPs on demand.
        availableTags =
                predefinedTagRepository.findAllByOrderByCategoryAscSortOrderAsc().stream()
                        .filter(PredefinedTag::isActive)
                        .filter(
                                t ->
                                        "POSITION".equals(t.getCategory())
                                                || "GENERAL".equals(t.getCategory()))
                        .toList();
        activityOptions = timetableArchiveService.activityOptions();
    }

    private void buildView() {
        removeAll();
        initializeSteps();
        wireRouteCallbacks();
        wireIntervalGeneration();
        metadataCard = new TimetableMetadataCard(businessService, availableTags, this);
        configureStepActions();
        assembleLayout();
        switchStep(Step.ROUTE);
    }

    /** Creates route, table, and interval step components. */
    private void initializeSteps() {
        routeStep =
                new TimetableRouteStep(
                        operationalPointRepository, activityOptions, timetableRoutingService);
        tableStep =
                new TimetableTableStep(
                        activityOptions, timetableEditingService, operationalPointRepository);
        intervalPanel = new IntervalTimetablePanel();
    }

    /** Wires the callback for when a route calculation completes or becomes dirty. */
    private void wireRouteCallbacks() {
        routeStep.setOnRouteCalculated(this::applyCalculatedRoute);
        routeStep.setOnRouteDirty(
                () -> {
                    routeDirty = true;
                    refreshStatusBar();
                    updateStepControls();
                });
    }

    private void applyCalculatedRoute(TimetableRouteStep.RouteCalculationResult result) {
        currentRoute = result.route();
        timetableRows.clear();
        timetableRows.addAll(result.rows());
        tableStep.setRows(new ArrayList<>(result.rows()));
        fillPositionNameFromRoute(result.rows());
        routeDirty = false;
        notify(
                t(
                        "timetable.route.calculated.summary",
                        result.rows().size(),
                        distanceLabel(result.route().totalLengthMeters())),
                NotificationVariant.LUMO_SUCCESS);
        updateStepControls();
        refreshStatusBar();
    }

    private void fillPositionNameFromRoute(List<TimetableRowData> rows) {
        if (!metadataCard.isNameBlank() || rows.isEmpty()) {
            return;
        }
        metadataCard.setName(rows.getFirst().getName() + " \u2192 " + rows.getLast().getName());
    }

    /** Wires the interval panel's generate callback for bulk timetable creation. */
    private void wireIntervalGeneration() {
        intervalPanel.setOnGenerate(
                config -> {
                    if (timetableRows.isEmpty()) {
                        notify(t("timetable.interval.noRoute"), NotificationVariant.LUMO_ERROR);
                        return;
                    }
                    try {
                        List<LocalDate> dates =
                                validityCalendar != null
                                        ? validityCalendar.getSelectedDates()
                                        : List.of();
                        var positions =
                                intervalTimetableService.generateIntervalPositions(
                                        new IntervalGenerationCommand(
                                                order,
                                                config.namePrefix(),
                                                config.otnStart(),
                                                new ArrayList<>(timetableRows),
                                                config.firstDeparture(),
                                                config.lastDeparture(),
                                                config.crossMidnight(),
                                                config.intervalMinutes(),
                                                dates,
                                                metadataCard.joinedTags(),
                                                metadataCard.comment()));
                        Notification.show(
                                        t("timetable.interval.generated", positions.size()),
                                        3000,
                                        Notification.Position.BOTTOM_END)
                                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                        navigateToOrder();
                    } catch (Exception ex) {
                        notify(ex.getMessage(), NotificationVariant.LUMO_ERROR);
                    }
                });
    }

    /** Assembles the main layout: header, status bar, metadata card, and content slot. */
    private void assembleLayout() {
        contentSlot.setWidthFull();
        contentSlot.getStyle().set("flex", "1").set("min-height", "0");
        add(
                chrome.buildHeader(order, this::navigateToOrder),
                chrome.buildStatusBar(),
                metadataCard.card(
                        existingPosition == null,
                        existingPosition == null ? null : existingPosition.getId()),
                contentSlot);
        expand(contentSlot);
    }

    private void configureStepActions() {
        chrome.configureActions(this::onStepBack, this::onStepNext, this::savePosition);
    }

    private void onStepBack() {
        switch (currentStep) {
            case ROUTE -> navigateToOrder();
            case TABLE -> switchStep(Step.ROUTE);
            case INTERVAL -> switchStep(Step.TABLE);
        }
    }

    private void onStepNext() {
        switch (currentStep) {
            case ROUTE -> {
                if (!timetableRows.isEmpty()) {
                    switchStep(Step.TABLE);
                } else {
                    routeStep.calculateRoute();
                }
            }
            case TABLE -> switchStep(Step.INTERVAL);
            case INTERVAL -> {} // No further step
        }
    }

    private void refreshStatusBar() {
        String routeText =
                timetableRows.isEmpty()
                        ? ""
                        : timetableRows.getFirst().getName()
                                + " \u2192 "
                                + timetableRows.getLast().getName();
        boolean ready = !metadataCard.isNameBlank() && !timetableRows.isEmpty();
        StatusKind kind =
                routeDirty ? StatusKind.DIRTY : ready ? StatusKind.READY : StatusKind.INCOMPLETE;
        chrome.refreshStatus(metadataCard.otn(), routeText, kind);
    }

    private void switchStep(Step step) {
        currentStep = step;
        contentSlot.removeAll();
        switch (step) {
            case ROUTE -> contentSlot.add(routeStep.createContent());
            case TABLE -> {
                LocalDate from =
                        order.getValidFrom() != null ? order.getValidFrom() : LocalDate.now();
                LocalDate to = order.getValidTo() != null ? order.getValidTo() : from.plusMonths(3);
                contentSlot.add(
                        tableStep.createContent(
                                from,
                                to,
                                validityCalendar,
                                routeSummaryText(timetableRows, currentRoute)));
            }
            case INTERVAL -> contentSlot.add(createIntervalStep());
        }
        updateStepControls();
        refreshStatusBar();
    }

    /** Creates the interval step layout: centered card with the IntervalTimetablePanel. */
    private Component createIntervalStep() {
        intervalPanel.setRouteAvailable(!timetableRows.isEmpty());
        if (routeStep.getDepartureAnchor() != null) {
            intervalPanel.setDefaultDeparture(routeStep.getDepartureAnchor());
        }
        // Make the interval panel always visible inside step 3.
        intervalPanel.setVisible(true);
        intervalPanel.getStyle().set("margin-top", "0");
        return chrome.intervalStepCard(intervalPanel);
    }

    private void loadExistingData() {
        TimetableDataLoader loader =
                new TimetableDataLoader(
                        timetableArchiveService,
                        timetableRoutingService,
                        operationalPointRepository,
                        this);
        TimetableDataLoader.LoadResult result =
                loader.load(
                        order,
                        existingPosition,
                        metadataCard.nameField(),
                        metadataCard.otnInput(),
                        metadataCard.commentInput(),
                        () ->
                                metadataCard.readTags(
                                        existingPosition != null
                                                ? existingPosition.getTags()
                                                : null),
                        routeStep,
                        tableStep,
                        routeSummaryText(timetableRows, currentRoute));
        this.validityCalendar = result.calendar();
        if (!result.rows().isEmpty()) {
            timetableRows.clear();
            timetableRows.addAll(result.rows());
            currentRoute = result.route();
        }
        if (result.switchToTable()) {
            switchStep(Step.TABLE);
        }
    }

    private void savePosition() {
        if ((currentStep == Step.TABLE || currentStep == Step.INTERVAL)
                && !tableStep.syncCurrentEditor()) {
            return;
        }
        if (metadataCard.isNameBlank()) {
            metadataCard.markNameInvalidAndFocus();
            notify(t("timetable.meta.name.required"), NotificationVariant.LUMO_ERROR);
            return;
        }
        List<TimetableRowData> rows = tableStep.getRows();
        if (rows.isEmpty()) {
            notify(t("timetable.route.calculateFirst"), NotificationVariant.LUMO_ERROR);
            switchStep(Step.ROUTE);
            return;
        }
        List<LocalDate> dates = validityCalendar.getSelectedDates();
        if (dates.isEmpty()) {
            notify(t("position.validity.required"), NotificationVariant.LUMO_ERROR);
            return;
        }
        try {
            OrderPosition saved =
                    timetableArchiveService.saveTimetablePosition(
                            order,
                            existingPosition,
                            metadataCard.name(),
                            metadataCard.joinedTags(),
                            metadataCard.comment(),
                            dates,
                            new ArrayList<>(rows),
                            metadataCard.otn());
            metadataCard.applyBusinessLinks(saved.getId());
            Notification.show(t("timetable.save.success"), 2500, Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            navigateToOrder();
        } catch (RuntimeException ex) {
            notify(ex.getMessage(), NotificationVariant.LUMO_ERROR);
        }
    }

    private void updateStepControls() {
        chrome.updateControls(currentStep, !timetableRows.isEmpty(), routeDirty);
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private String routeSummaryText(List<TimetableRowData> rows, TimetableRouteResult route) {
        if (rows == null || rows.isEmpty()) {
            return t("timetable.route.empty");
        }
        return t(
                "timetable.route.summary",
                rows.size(),
                distanceLabel(route.totalLengthMeters()),
                timeOrDash(rows.getFirst().getEstimatedDeparture()),
                timeOrDash(rows.getLast().getEstimatedArrival()));
    }

    private void notify(String msg, NotificationVariant variant) {
        Notification.show(msg, 3000, Position.BOTTOM_END).addThemeVariants(variant);
    }

    private void navigateToOrder() {
        UI.getCurrent().navigate("orders/" + order.getId());
    }

    private String t(String key, Object... params) {
        return getTranslation(key, params);
    }
}
