package com.ordermgmt.railway.ui.view.order;

import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.distanceLabel;
import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.timeOrDash;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.annotation.security.RolesAllowed;

import org.springframework.data.domain.Sort;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import com.ordermgmt.railway.domain.infrastructure.model.OperationalPoint;
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
import com.ordermgmt.railway.ui.component.timetable.TimetableDataLoader;
import com.ordermgmt.railway.ui.component.timetable.TimetableRouteStep;
import com.ordermgmt.railway.ui.component.timetable.TimetableTableStep;
import com.ordermgmt.railway.ui.component.timetable.TimetableTagHelper;
import com.ordermgmt.railway.ui.layout.MainLayout;

/** Full-screen two-step builder for timetable order positions. */
@Route(value = "orders/:orderId/timetable-builder", layout = MainLayout.class)
@PageTitle("Timetable Builder")
@RolesAllowed({"ADMIN", "DISPATCHER"})
public class TimetableBuilderView extends VerticalLayout implements BeforeEnterObserver {

    private final OrderService orderService;
    private final OperationalPointRepository operationalPointRepository;
    private final PredefinedTagRepository predefinedTagRepository;
    private final TimetableRoutingService timetableRoutingService;
    private final TimetableArchiveService timetableArchiveService;
    private final TimetableEditingService timetableEditingService;
    private final IntervalTimetableService intervalTimetableService;
    private final TextField positionName = new TextField();
    private final TextField otnField = new TextField();
    private final CheckboxGroup<PredefinedTag> tagSelector = new CheckboxGroup<>();
    private final TextArea commentField = new TextArea();
    private final Div contentSlot = new Div();
    private final Span stepOneBadge = new Span();
    private final Span stepTwoBadge = new Span();
    private final Span stepThreeBadge = new Span();
    private final Button stepBackButton = new Button();
    private final Button stepNextButton = new Button();
    private final Button saveButton = new Button();
    private final Span statusOtn = new Span();
    private final Span statusRoute = new Span();
    private final Span statusState = new Span();
    private final LinkedHashSet<String> unmatchedTags = new LinkedHashSet<>();
    private final List<TimetableRowData> timetableRows = new ArrayList<>();
    private boolean routeDirty = false;
    private Details metadataDetails;
    private ValidityCalendar validityCalendar;
    private Order order;
    private OrderPosition existingPosition;
    private TimetableRouteResult currentRoute = new TimetableRouteResult(List.of(), 0D);
    private Step currentStep = Step.ROUTE;
    private List<OperationalPoint> availableOperationalPoints = List.of();
    private Map<String, OperationalPoint> operationalPointsByUopid = Map.of();
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
            IntervalTimetableService intervalTimetableService) {
        this.orderService = orderService;
        this.operationalPointRepository = operationalPointRepository;
        this.predefinedTagRepository = predefinedTagRepository;
        this.timetableRoutingService = timetableRoutingService;
        this.timetableArchiveService = timetableArchiveService;
        this.timetableEditingService = timetableEditingService;
        this.intervalTimetableService = intervalTimetableService;
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
        availableOperationalPoints =
                operationalPointRepository.findAll(
                        Sort.by("country").ascending().and(Sort.by("name").ascending()));
        Map<String, OperationalPoint> map = new LinkedHashMap<>();
        availableOperationalPoints.forEach(p -> map.put(p.getUopid(), p));
        operationalPointsByUopid = map;
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
        configureMetaFields();
        configureStepActions();
        assembleLayout();
        switchStep(Step.ROUTE);
    }

    /** Creates route, table, and interval step components. */
    private void initializeSteps() {
        routeStep =
                new TimetableRouteStep(
                        availableOperationalPoints, activityOptions, timetableRoutingService);
        tableStep =
                new TimetableTableStep(
                        activityOptions, timetableEditingService, availableOperationalPoints);
        intervalPanel = new IntervalTimetablePanel();
    }

    /** Wires the callback for when a route calculation completes or becomes dirty. */
    private void wireRouteCallbacks() {
        routeStep.setOnRouteCalculated(
                result -> {
                    currentRoute = result.route();
                    timetableRows.clear();
                    timetableRows.addAll(result.rows());
                    tableStep.setRows(new ArrayList<>(result.rows()));
                    // Auto-fill position name from route endpoints
                    if (positionName.getValue().isBlank() && !result.rows().isEmpty()) {
                        positionName.setValue(
                                result.rows().getFirst().getName()
                                        + " \u2192 "
                                        + result.rows().getLast().getName());
                    }
                    routeDirty = false;
                    notify(
                            t(
                                    "timetable.route.calculated.summary",
                                    result.rows().size(),
                                    distanceLabel(result.route().totalLengthMeters())),
                            NotificationVariant.LUMO_SUCCESS);
                    updateStepControls();
                    refreshStatusBar();
                });
        routeStep.setOnRouteDirty(
                () -> {
                    routeDirty = true;
                    refreshStatusBar();
                    updateStepControls();
                });
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
                                                joinSelectedTags(),
                                                commentField.getValue()));
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
        add(createHeader(), createStatusBar(), createMetadataCard(), contentSlot);
        expand(contentSlot);
    }

    private Component createHeader() {
        Button back = new Button(VaadinIcon.ARROW_LEFT.create());
        back.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        back.getStyle().set("color", "var(--rom-text-secondary)");
        back.addClickListener(e -> navigateToOrder());
        H2 title = new H2(t("timetable.builder.title"));
        title.getStyle()
                .set("margin", "0")
                .set("font-size", "var(--lumo-font-size-xl)")
                .set("color", "var(--rom-text-primary)");
        Span sub =
                new Span(
                        order.getOrderNumber()
                                + " \u00b7 "
                                + order.getName()
                                + " \u00b7 "
                                + formatValidityDate(order.getValidFrom())
                                + " \u2192 "
                                + formatValidityDate(order.getValidTo()));
        sub.getStyle()
                .set("display", "block")
                .set("font-size", "12px")
                .set("font-family", "'JetBrains Mono', monospace")
                .set("color", "var(--rom-text-muted)");
        HorizontalLayout badges = new HorizontalLayout(stepOneBadge, stepTwoBadge, stepThreeBadge);
        badges.setSpacing(true);
        badges.setAlignItems(FlexComponent.Alignment.CENTER);
        HorizontalLayout acts = new HorizontalLayout(stepBackButton, stepNextButton, saveButton);
        acts.setSpacing(true);
        acts.setAlignItems(FlexComponent.Alignment.CENTER);
        HorizontalLayout row = new HorizontalLayout(back, new Div(title, sub), badges, acts);
        row.setWidthFull();
        row.expand(row.getComponentAt(1));
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.getStyle()
                .set("background", "var(--rom-bg-card)")
                .set("border", "1px solid var(--rom-border)")
                .set("border-radius", "6px")
                .set("padding", "12px 16px")
                .set("box-sizing", "border-box")
                .set("margin-bottom", "var(--lumo-space-s)");
        return row;
    }

    private Component createMetadataCard() {
        FormLayout form = new FormLayout();
        form.setWidthFull();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2),
                new FormLayout.ResponsiveStep("900px", 4));
        form.add(positionName, otnField, tagSelector, commentField);
        metadataDetails = new Details(t("timetable.meta.title"), form);
        metadataDetails.setOpened(existingPosition == null);
        metadataDetails.setWidthFull();
        metadataDetails
                .getStyle()
                .set("background", "var(--rom-bg-card)")
                .set("border", "1px solid var(--rom-border)")
                .set("border-radius", "6px")
                .set("margin-bottom", "var(--lumo-space-s)");
        return metadataDetails;
    }

    private void configureMetaFields() {
        positionName.setLabel(t("position.name"));
        positionName.setRequired(true);
        positionName.setMaxLength(255);
        positionName.setWidthFull();
        positionName.setHelperText(t("timetable.meta.name.help"));
        otnField.setLabel(t("timetable.otn"));
        otnField.setMaxLength(20);
        otnField.setWidthFull();
        otnField.setHelperText(t("timetable.otn.help"));
        otnField.setPlaceholder("z.B. 95345 oder 95xxx");
        tagSelector.setLabel(t("order.tags"));
        tagSelector.setItems(availableTags);
        tagSelector.setItemLabelGenerator(this::tagLabel);
        tagSelector.setWidthFull();
        updateTagHelper();
        commentField.setLabel(t("order.comment"));
        commentField.setMaxLength(2000);
        commentField.setWidthFull();
        commentField.setHeight("60px");
        commentField.setHelperText(t("timetable.meta.comment.help"));
    }

    private void configureStepActions() {
        stepBackButton.setText(t("common.back"));
        stepBackButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
        stepBackButton.addClickListener(
                e -> {
                    switch (currentStep) {
                        case ROUTE -> navigateToOrder();
                        case TABLE -> switchStep(Step.ROUTE);
                        case INTERVAL -> switchStep(Step.TABLE);
                    }
                });
        stepNextButton.setText(t("common.next"));
        stepNextButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
        stepNextButton
                .getStyle()
                .set("background", "var(--rom-accent)")
                .set("color", "var(--rom-bg-primary)");
        stepNextButton.addClickListener(
                e -> {
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
                });
        saveButton.setText(t("common.save"));
        saveButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
        saveButton.getStyle().set("background", "var(--rom-status-info)").set("color", "white");
        saveButton.addClickListener(e -> savePosition());
        saveButton.addClickShortcut(Key.KEY_S, KeyModifier.CONTROL);
    }

    private Component createStatusBar() {
        Div bar = new Div(statusOtn, statusRoute, statusState);
        bar.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("gap", "12px")
                .set("background", "var(--rom-bg-secondary)")
                .set("border-bottom", "1px solid var(--rom-border)")
                .set("padding", "4px 16px")
                .set("font-size", "11px")
                .set("font-family", "'JetBrains Mono', monospace");
        statusOtn.getStyle().set("color", "var(--rom-accent)").set("font-weight", "600");
        statusRoute.getStyle().set("color", "var(--rom-text-secondary)");
        statusState.getStyle().set("margin-left", "auto");
        return bar;
    }

    private void refreshStatusBar() {
        String otn = otnField.getValue();
        statusOtn.setText(otn != null && !otn.isBlank() ? "OTN " + otn : "");

        if (!timetableRows.isEmpty()) {
            statusRoute.setText(
                    timetableRows.getFirst().getName()
                            + " \u2192 "
                            + timetableRows.getLast().getName());
        } else {
            statusRoute.setText("");
        }

        boolean ready = !positionName.getValue().isBlank() && !timetableRows.isEmpty();
        if (routeDirty) {
            statusState.setText(t("timetable.status.routeDirty"));
            statusState.getStyle().set("color", "var(--rom-status-warning)");
        } else if (ready) {
            statusState.setText("\u2713 " + t("timetable.status.ready"));
            statusState.getStyle().set("color", "var(--rom-status-active)");
        } else {
            statusState.setText("\u26a0 " + t("timetable.status.incomplete"));
            statusState.getStyle().set("color", "var(--rom-status-warning)");
        }
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

        Div wrapper = new Div();
        wrapper.setWidthFull();
        wrapper.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("align-items", "center")
                .set("padding", "var(--lumo-space-l) 0")
                .set("height", "100%")
                .set("box-sizing", "border-box");

        Div card = new Div();
        card.getStyle()
                .set("width", "100%")
                .set("max-width", "720px")
                .set("background", "var(--rom-bg-card)")
                .set("border", "1px solid var(--rom-border)")
                .set("border-radius", "6px")
                .set("padding", "24px")
                .set("box-sizing", "border-box");

        Span header = new Span(t("timetable.interval.step.header"));
        header.getStyle()
                .set("display", "block")
                .set("font-size", "15px")
                .set("font-weight", "600")
                .set("color", "var(--rom-text-primary)")
                .set("margin-bottom", "6px");

        Span help = new Span(t("timetable.interval.step.help"));
        help.getStyle()
                .set("display", "block")
                .set("font-size", "12px")
                .set("color", "var(--rom-text-muted)")
                .set("margin-bottom", "16px");

        // Make the interval panel always visible inside step 3
        intervalPanel.setVisible(true);
        intervalPanel.getStyle().set("margin-top", "0");

        card.add(header, help, intervalPanel);
        wrapper.add(card);
        return wrapper;
    }

    private void loadExistingData() {
        TimetableDataLoader loader =
                new TimetableDataLoader(
                        timetableArchiveService,
                        timetableRoutingService,
                        operationalPointsByUopid,
                        this);
        TimetableDataLoader.LoadResult result =
                loader.load(
                        order,
                        existingPosition,
                        positionName,
                        otnField,
                        commentField,
                        () ->
                                readTags(
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
        if (positionName.getValue().isBlank()) {
            positionName.setInvalid(true);
            metadataDetails.setOpened(true);
            positionName.focus();
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
            timetableArchiveService.saveTimetablePosition(
                    order,
                    existingPosition,
                    positionName.getValue(),
                    joinSelectedTags(),
                    commentField.getValue(),
                    dates,
                    new ArrayList<>(rows),
                    otnField.getValue());
            Notification.show(t("timetable.save.success"), 2500, Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            navigateToOrder();
        } catch (RuntimeException ex) {
            notify(ex.getMessage(), NotificationVariant.LUMO_ERROR);
        }
    }

    private void updateStepControls() {
        boolean hasRows = !timetableRows.isEmpty();
        styleStepBadge(stepOneBadge, t("timetable.step.route"), currentStep == Step.ROUTE, true);
        styleStepBadge(stepTwoBadge, t("timetable.step.table"), currentStep == Step.TABLE, hasRows);
        styleStepBadge(
                stepThreeBadge,
                t("timetable.step.interval"),
                currentStep == Step.INTERVAL,
                hasRows);
        stepBackButton.setText(
                currentStep == Step.ROUTE ? t("timetable.backToOrder") : t("common.back"));

        switch (currentStep) {
            case ROUTE -> stepNextButton.setText(t("common.next"));
            case TABLE -> stepNextButton.setText(t("timetable.step.interval"));
            case INTERVAL -> stepNextButton.setText("");
        }

        saveButton.setVisible(currentStep == Step.TABLE || currentStep == Step.INTERVAL);
        stepNextButton.setVisible(currentStep != Step.INTERVAL);

        boolean nextEnabled =
                switch (currentStep) {
                    case ROUTE -> hasRows && !routeDirty;
                    case TABLE -> hasRows;
                    case INTERVAL -> false;
                };
        stepNextButton.setEnabled(nextEnabled);
    }

    private void styleStepBadge(Span badge, String label, boolean active, boolean enabled) {
        badge.setText(label);
        badge.getStyle()
                .set("padding", "4px 10px")
                .set("border-radius", "999px")
                .set("font-size", "11px")
                .set("font-family", "'JetBrains Mono', monospace")
                .set("font-weight", "600")
                .set("border", "1px solid " + (active ? "var(--rom-accent)" : "var(--rom-border)"))
                .set("background", active ? "rgba(45,212,191,0.12)" : "rgba(148,163,184,0.08)")
                .set(
                        "color",
                        active
                                ? "var(--rom-accent)"
                                : enabled ? "var(--rom-text-secondary)" : "var(--rom-text-muted)")
                .set("opacity", enabled ? "1" : "0.55");
    }

    // ── Tag handling (delegated to TimetableTagHelper) ─────────────────

    private void readTags(String stored) {
        tagHelper().readTags(stored);
    }

    private String joinSelectedTags() {
        return tagHelper().joinSelectedTags();
    }

    private void updateTagHelper() {
        tagHelper().updateTagHelper();
    }

    private String tagLabel(PredefinedTag tag) {
        return tagHelper().tagLabel(tag);
    }

    private TimetableTagHelper tagHelper() {
        return new TimetableTagHelper(tagSelector, availableTags, unmatchedTags, this);
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

    private String formatValidityDate(LocalDate date) {
        return date != null
                ? date.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                : "\u2014";
    }

    private String t(String key, Object... params) {
        return getTranslation(key, params);
    }

    private enum Step {
        ROUTE,
        TABLE,
        INTERVAL
    }
}
