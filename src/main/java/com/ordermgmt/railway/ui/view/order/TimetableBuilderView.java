package com.ordermgmt.railway.ui.view.order;

import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.distanceLabel;
import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.timeOrDash;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import jakarta.annotation.security.PermitAll;

import org.springframework.data.domain.Sort;

import com.vaadin.flow.component.Component;
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
import com.ordermgmt.railway.domain.timetable.model.RoutePointRole;
import com.ordermgmt.railway.domain.timetable.model.TimetableActivityOption;
import com.ordermgmt.railway.domain.timetable.model.TimetableArchive;
import com.ordermgmt.railway.domain.timetable.model.TimetableRouteResult;
import com.ordermgmt.railway.domain.timetable.model.TimetableRowData;
import com.ordermgmt.railway.domain.timetable.service.TimetableArchiveService;
import com.ordermgmt.railway.domain.timetable.service.TimetableEditingService;
import com.ordermgmt.railway.domain.timetable.service.TimetableRoutingService;
import com.ordermgmt.railway.ui.component.ValidityCalendar;
import com.ordermgmt.railway.ui.component.timetable.TimetableRouteStep;
import com.ordermgmt.railway.ui.component.timetable.TimetableTableStep;
import com.ordermgmt.railway.ui.layout.MainLayout;

/** Full-screen two-step builder for timetable order positions. */
@Route(value = "orders/:orderId/timetable-builder", layout = MainLayout.class)
@PageTitle("Timetable Builder")
@PermitAll
public class TimetableBuilderView extends VerticalLayout implements BeforeEnterObserver {

    private final OrderService orderService;
    private final OperationalPointRepository operationalPointRepository;
    private final PredefinedTagRepository predefinedTagRepository;
    private final TimetableRoutingService timetableRoutingService;
    private final TimetableArchiveService timetableArchiveService;
    private final TimetableEditingService timetableEditingService;
    private final TextField positionName = new TextField();
    private final TextField otnField = new TextField();
    private final CheckboxGroup<PredefinedTag> tagSelector = new CheckboxGroup<>();
    private final TextArea commentField = new TextArea();
    private final Div contentSlot = new Div();
    private final Span stepOneBadge = new Span();
    private final Span stepTwoBadge = new Span();
    private final Button stepBackButton = new Button();
    private final Button stepNextButton = new Button();
    private final Button saveButton = new Button();
    private final LinkedHashSet<String> unmatchedTags = new LinkedHashSet<>();
    private final List<TimetableRowData> timetableRows = new ArrayList<>();
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

    public TimetableBuilderView(
            OrderService orderService,
            OperationalPointRepository operationalPointRepository,
            PredefinedTagRepository predefinedTagRepository,
            TimetableRoutingService timetableRoutingService,
            TimetableArchiveService timetableArchiveService,
            TimetableEditingService timetableEditingService) {
        this.orderService = orderService;
        this.operationalPointRepository = operationalPointRepository;
        this.predefinedTagRepository = predefinedTagRepository;
        this.timetableRoutingService = timetableRoutingService;
        this.timetableArchiveService = timetableArchiveService;
        this.timetableEditingService = timetableEditingService;
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
        routeStep =
                new TimetableRouteStep(
                        availableOperationalPoints, activityOptions, timetableRoutingService);
        routeStep.setOnRouteCalculated(
                result -> {
                    currentRoute = result.route();
                    timetableRows.clear();
                    timetableRows.addAll(result.rows());
                    tableStep.setRows(new ArrayList<>(result.rows()));
                    notify(
                            t(
                                    "timetable.route.calculated.summary",
                                    result.rows().size(),
                                    distanceLabel(result.route().totalLengthMeters())),
                            NotificationVariant.LUMO_SUCCESS);
                    updateStepControls();
                });
        tableStep =
                new TimetableTableStep(
                        activityOptions, timetableEditingService, availableOperationalPoints);
        configureMetaFields();
        configureStepActions();
        contentSlot.setWidthFull();
        contentSlot.getStyle().set("flex", "1").set("min-height", "0");
        add(createHeader(), createMetadataCard(), contentSlot);
        expand(contentSlot);
        switchStep(Step.ROUTE);
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
        Span sub = new Span(order.getOrderNumber() + " \u00b7 " + order.getName());
        sub.getStyle()
                .set("display", "block")
                .set("font-size", "12px")
                .set("font-family", "'JetBrains Mono', monospace")
                .set("color", "var(--rom-text-muted)");
        HorizontalLayout badges = new HorizontalLayout(stepOneBadge, stepTwoBadge);
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
                    if (currentStep == Step.ROUTE) {
                        navigateToOrder();
                    } else {
                        switchStep(Step.ROUTE);
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
                    if (currentStep == Step.ROUTE) {
                        if (!timetableRows.isEmpty()) {
                            switchStep(Step.TABLE);
                        } else {
                            routeStep.calculateRoute();
                        }
                    } else {
                        switchStep(Step.ROUTE);
                    }
                });
        saveButton.setText(t("common.save"));
        saveButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
        saveButton.getStyle().set("background", "var(--rom-status-info)").set("color", "white");
        saveButton.addClickListener(e -> savePosition());
    }

    private void switchStep(Step step) {
        currentStep = step;
        contentSlot.removeAll();
        if (step == Step.ROUTE) {
            contentSlot.add(routeStep.createContent());
        } else {
            LocalDate from = order.getValidFrom() != null ? order.getValidFrom() : LocalDate.now();
            LocalDate to = order.getValidTo() != null ? order.getValidTo() : from.plusMonths(3);
            contentSlot.add(
                    tableStep.createContent(
                            from,
                            to,
                            validityCalendar,
                            routeSummaryText(timetableRows, currentRoute)));
        }
        updateStepControls();
    }

    private void loadExistingData() {
        positionName.setValue(
                existingPosition != null ? textOrBlank(existingPosition.getName()) : "");
        commentField.setValue(
                existingPosition != null ? textOrBlank(existingPosition.getComment()) : "");
        readTags(existingPosition != null ? existingPosition.getTags() : null);
        // Load OTN from archive if editing
        if (existingPosition != null) {
            timetableArchiveService
                    .findArchive(existingPosition)
                    .ifPresent(a -> otnField.setValue(textOrBlank(a.getOperationalTrainNumber())));
        }
        LocalDate min = order.getValidFrom() != null ? order.getValidFrom() : LocalDate.now();
        LocalDate max = order.getValidTo() != null ? order.getValidTo() : min.plusMonths(3);
        ValidityCalendar cal = new ValidityCalendar(min, max);
        if (existingPosition != null) {
            cal.setSelectedDates(
                    timetableArchiveService.parseValidityDates(existingPosition.getValidity()));
        }
        this.validityCalendar = cal;
        if (existingPosition == null) {
            routeStep.getRouteSummary().setText(t("timetable.route.empty"));
            routeStep.getRouteError().setText("");
            return;
        }
        Optional<TimetableArchive> archive = timetableArchiveService.findArchive(existingPosition);
        if (archive.isPresent()) {
            List<TimetableRowData> rows = timetableArchiveService.readRows(archive.get());
            timetableRows.clear();
            timetableRows.addAll(rows);
            tableStep.setRows(new ArrayList<>(rows));
            currentRoute = timetableRoutingService.routeFromStoredRows(rows);
            routeStep.setRoute(currentRoute);
            prefillRouteInputsFromRows(rows);
            routeStep.getRouteSummary().setText(routeSummaryText(rows, currentRoute));
            routeStep.getRouteError().setText("");
            switchStep(Step.TABLE);
            return;
        }
        prefillLegacyRoute();
    }

    private void prefillRouteInputsFromRows(List<TimetableRowData> rows) {
        if (rows.isEmpty()) {
            return;
        }
        List<TimetableRouteStep.ViaData> vias = new ArrayList<>();
        for (TimetableRowData row : rows) {
            if (row.getRoutePointRole() == RoutePointRole.VIA) {
                vias.add(
                        new TimetableRouteStep.ViaData(
                                operationalPointsByUopid.get(row.getUopid()),
                                Boolean.TRUE.equals(row.getHalt()),
                                row.getActivityCode()));
            }
        }
        routeStep.prefillFrom(
                operationalPointsByUopid.get(rows.getFirst().getUopid()),
                operationalPointsByUopid.get(rows.getLast().getUopid()),
                vias);
    }

    private void prefillLegacyRoute() {
        if (existingPosition == null) {
            return;
        }
        Optional<OperationalPoint> fromPt =
                timetableRoutingService.resolveLegacyPoint(existingPosition.getFromLocation());
        Optional<OperationalPoint> toPt =
                timetableRoutingService.resolveLegacyPoint(existingPosition.getToLocation());
        routeStep.prefillFrom(fromPt.orElse(null), toPt.orElse(null), null);
        if (existingPosition.getStart() != null) {
            routeStep.setDepartureAnchor(existingPosition.getStart().toLocalTime());
        }
        if (existingPosition.getEnd() != null && routeStep.getDepartureAnchor() == null) {
            routeStep.setArrivalAnchor(existingPosition.getEnd().toLocalTime());
        }
        if (fromPt.isPresent() && toPt.isPresent()) {
            List<TimetableRowData> rows =
                    routeStep.calculateRoute(
                            routeStep.getDepartureAnchor(), routeStep.getArrivalAnchor());
            if (rows != null) {
                timetableRows.clear();
                timetableRows.addAll(rows);
                tableStep.setRows(new ArrayList<>(rows));
                currentRoute = routeStep.getCurrentRoute();
            }
            return;
        }
        routeStep.getRouteSummary().setText(t("timetable.route.empty"));
        routeStep.getRouteError().setText(t("timetable.route.legacyUnresolved"));
    }

    private void savePosition() {
        if (currentStep == Step.TABLE && !tableStep.syncCurrentEditor()) {
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
        styleStepBadge(stepOneBadge, t("timetable.step.route"), currentStep == Step.ROUTE, true);
        styleStepBadge(
                stepTwoBadge,
                t("timetable.step.table"),
                currentStep == Step.TABLE,
                !timetableRows.isEmpty());
        stepBackButton.setText(
                currentStep == Step.ROUTE ? t("timetable.backToOrder") : t("common.back"));
        stepNextButton.setText(
                currentStep == Step.ROUTE ? t("common.next") : t("timetable.step.route"));
        saveButton.setVisible(currentStep == Step.TABLE);
        stepNextButton.setEnabled(currentStep == Step.ROUTE || !timetableRows.isEmpty());
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

    // ── Tag handling ───────────────────────────────────────────────────

    private void readTags(String stored) {
        Map<String, PredefinedTag> byName = new LinkedHashMap<>();
        availableTags.forEach(t -> byName.put(normalizeTagName(t.getName()), t));
        unmatchedTags.clear();
        LinkedHashSet<PredefinedTag> selected = new LinkedHashSet<>();
        for (String token : splitTags(stored)) {
            PredefinedTag match = byName.get(normalizeTagName(token));
            if (match != null) {
                selected.add(match);
            } else {
                unmatchedTags.add(token);
            }
        }
        tagSelector.setValue(selected);
        updateTagHelper();
    }

    private String joinSelectedTags() {
        LinkedHashSet<String> vals = new LinkedHashSet<>();
        for (PredefinedTag tag : availableTags) {
            if (tagSelector.getValue().contains(tag)) {
                vals.add(tag.getName());
            }
        }
        vals.addAll(unmatchedTags);
        return vals.isEmpty() ? null : String.join(", ", vals);
    }

    private void updateTagHelper() {
        String helper = t("position.tags.help");
        if (!unmatchedTags.isEmpty()) {
            helper += " " + t("position.tags.legacy", String.join(", ", unmatchedTags));
        }
        tagSelector.setHelperText(helper);
    }

    private String tagLabel(PredefinedTag tag) {
        return "[" + tagCategoryLabel(tag.getCategory()) + "] " + tag.getName();
    }

    private String tagCategoryLabel(String category) {
        String n = category == null ? "general" : category.trim().toLowerCase(Locale.ROOT);
        return switch (n) {
            case "order" -> t("settings.tags.cat.order");
            case "position" -> t("settings.tags.cat.position");
            default -> t("settings.tags.cat.general");
        };
    }

    private String normalizeTagName(String v) {
        return v == null ? "" : v.trim().toLowerCase(Locale.ROOT);
    }

    private List<String> splitTags(String raw) {
        List<String> result = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return result;
        }
        for (String tok : raw.split(",")) {
            String trimmed = tok.trim();
            if (!trimmed.isBlank()) {
                result.add(trimmed);
            }
        }
        return result;
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

    private String textOrBlank(String v) {
        return v != null ? v : "";
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

    private enum Step {
        ROUTE,
        TABLE
    }
}
