package com.ordermgmt.railway.ui.view.order;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
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
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.timepicker.TimePicker;
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
import com.ordermgmt.railway.domain.timetable.model.TimeConstraintMode;
import com.ordermgmt.railway.domain.timetable.model.TimetableActivityOption;
import com.ordermgmt.railway.domain.timetable.model.TimetableArchive;
import com.ordermgmt.railway.domain.timetable.model.TimetableRouteResult;
import com.ordermgmt.railway.domain.timetable.model.TimetableRowData;
import com.ordermgmt.railway.domain.timetable.service.TimetableArchiveService;
import com.ordermgmt.railway.domain.timetable.service.TimetableRoutingService;
import com.ordermgmt.railway.ui.component.ValidityCalendar;
import com.ordermgmt.railway.ui.component.timetable.TimetableMap;
import com.ordermgmt.railway.ui.layout.MainLayout;

/** Full-screen two-step builder for timetable order positions. */
@Route(value = "orders/:orderId/timetable-builder", layout = MainLayout.class)
@PageTitle("Timetable Builder")
@PermitAll
public class TimetableBuilderView extends VerticalLayout implements BeforeEnterObserver {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final OrderService orderService;
    private final OperationalPointRepository operationalPointRepository;
    private final PredefinedTagRepository predefinedTagRepository;
    private final TimetableRoutingService timetableRoutingService;
    private final TimetableArchiveService timetableArchiveService;

    private final TextField positionName = new TextField();
    private final CheckboxGroup<PredefinedTag> tagSelector = new CheckboxGroup<>();
    private final TextArea commentField = new TextArea();

    private final ComboBox<OperationalPoint> fromField = new ComboBox<>();
    private final ComboBox<OperationalPoint> toField = new ComboBox<>();
    private final VerticalLayout viaList = new VerticalLayout();
    private final List<ViaPointEditor> viaEditors = new ArrayList<>();
    private final TimePicker departureAnchorField = createTimePicker();
    private final TimePicker arrivalAnchorField = createTimePicker();
    private final Span routeSummary = new Span();
    private final Span routeError = new Span();
    private final TimetableMap routeMap = new TimetableMap();

    private final Grid<TimetableRowData> rowGrid = new Grid<>(TimetableRowData.class, false);
    private final Div rowEditor = new Div();
    private final Span rowEditorTitle = new Span();
    private final Span rowEditorContext = new Span();
    private final Span arrivalEstimateLabel = new Span();
    private final Span departureEstimateLabel = new Span();
    private final Checkbox haltField = new Checkbox();
    private final ComboBox<TimetableActivityOption> activityField = new ComboBox<>();
    private final IntegerField dwellMinutesField = new IntegerField();
    private final Select<TimeConstraintMode> arrivalModeField = new Select<>();
    private final Select<TimeConstraintMode> departureModeField = new Select<>();
    private final TimePicker arrivalExactField = createTimePicker();
    private final TimePicker arrivalEarliestField = createTimePicker();
    private final TimePicker arrivalLatestField = createTimePicker();
    private final TimePicker departureExactField = createTimePicker();
    private final TimePicker departureEarliestField = createTimePicker();
    private final TimePicker departureLatestField = createTimePicker();
    private final Div arrivalExactWrapper = new Div();
    private final Div arrivalWindowWrapper = new Div();
    private final Div departureExactWrapper = new Div();
    private final Div departureWindowWrapper = new Div();
    private final Div arrivalSection = new Div();
    private final Div departureSection = new Div();

    private final Div contentSlot = new Div();
    private final Span stepOneBadge = new Span();
    private final Span stepTwoBadge = new Span();
    private final Button stepBackButton = new Button();
    private final Button stepNextButton = new Button();
    private final Button saveButton = new Button();

    private final LinkedHashSet<String> unmatchedTags = new LinkedHashSet<>();
    private final List<TimetableRowData> timetableRows = new ArrayList<>();

    private ValidityCalendar validityCalendar;
    private Order order;
    private OrderPosition existingPosition;
    private TimetableRouteResult currentRoute = new TimetableRouteResult(List.of(), 0D);
    private TimetableRowData selectedRow;
    private Step currentStep = Step.ROUTE;
    private List<OperationalPoint> availableOperationalPoints = List.of();
    private Map<String, OperationalPoint> operationalPointsByUopid = Map.of();
    private List<PredefinedTag> availableTags = List.of();
    private List<TimetableActivityOption> activityOptions = List.of();

    public TimetableBuilderView(
            OrderService orderService,
            OperationalPointRepository operationalPointRepository,
            PredefinedTagRepository predefinedTagRepository,
            TimetableRoutingService timetableRoutingService,
            TimetableArchiveService timetableArchiveService) {
        this.orderService = orderService;
        this.operationalPointRepository = operationalPointRepository;
        this.predefinedTagRepository = predefinedTagRepository;
        this.timetableRoutingService = timetableRoutingService;
        this.timetableArchiveService = timetableArchiveService;

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
        } catch (IllegalArgumentException exception) {
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
        String positionId =
                event.getLocation().getQueryParameters().getParameters().getOrDefault("positionId", List.of())
                        .stream()
                        .findFirst()
                        .orElse(null);
        if (positionId == null || positionId.isBlank()) {
            return null;
        }
        try {
            OrderPosition position = orderService.findPositionById(UUID.fromString(positionId)).orElse(null);
            if (position == null
                    || position.getOrder() == null
                    || !order.getId().equals(position.getOrder().getId())
                    || position.getType() != PositionType.FAHRPLAN) {
                return null;
            }
            return position;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private void loadReferenceData() {
        availableOperationalPoints =
                operationalPointRepository.findAll(
                        Sort.by("country").ascending().and(Sort.by("name").ascending()));
        Map<String, OperationalPoint> byUopid = new LinkedHashMap<>();
        for (OperationalPoint point : availableOperationalPoints) {
            byUopid.put(point.getUopid(), point);
        }
        operationalPointsByUopid = byUopid;
        availableTags = predefinedTagRepository.findAllByOrderByCategoryAscSortOrderAsc().stream()
                .filter(PredefinedTag::isActive)
                .filter(tag -> "POSITION".equals(tag.getCategory()) || "GENERAL".equals(tag.getCategory()))
                .toList();
        activityOptions = timetableArchiveService.activityOptions();
    }

    private void buildView() {
        removeAll();

        configureMetaFields();
        configureRouteInputs();
        configureGrid();
        configureRowEditor();
        configureStepActions();

        contentSlot.setWidthFull();
        contentSlot.getStyle().set("flex", "1").set("min-height", "0");

        add(createHeader(), createMetadataCard(), contentSlot);
        expand(contentSlot);
        switchStep(Step.ROUTE);
    }

    private Component createHeader() {
        Button backToOrder = new Button(VaadinIcon.ARROW_LEFT.create());
        backToOrder.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        backToOrder.getStyle().set("color", "var(--rom-text-secondary)");
        backToOrder.addClickListener(e -> navigateToOrder());

        H2 title = new H2(t("timetable.builder.title"));
        title.getStyle()
                .set("margin", "0")
                .set("font-size", "var(--lumo-font-size-xl)")
                .set("color", "var(--rom-text-primary)");

        Span subtitle = new Span(order.getOrderNumber() + " · " + order.getName());
        subtitle.getStyle()
                .set("display", "block")
                .set("font-size", "12px")
                .set("font-family", "'JetBrains Mono', monospace")
                .set("color", "var(--rom-text-muted)");

        Div titleBlock = new Div(title, subtitle);

        HorizontalLayout stepBadges = new HorizontalLayout(stepOneBadge, stepTwoBadge);
        stepBadges.setSpacing(true);
        stepBadges.setAlignItems(FlexComponent.Alignment.CENTER);

        HorizontalLayout actions = new HorizontalLayout(stepBackButton, stepNextButton, saveButton);
        actions.setSpacing(true);
        actions.setAlignItems(FlexComponent.Alignment.CENTER);

        HorizontalLayout row = new HorizontalLayout(backToOrder, titleBlock, stepBadges, actions);
        row.setWidthFull();
        row.expand(titleBlock);
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
                new FormLayout.ResponsiveStep("760px", 2));

        form.add(positionName);
        form.add(tagSelector);
        form.setColspan(commentField, 2);
        form.add(commentField);

        return createCard(t("timetable.meta.title"), form);
    }

    private void configureMetaFields() {
        positionName.setLabel(t("position.name"));
        positionName.setRequired(true);
        positionName.setMaxLength(255);
        positionName.setHelperText(t("timetable.meta.name.help"));
        positionName.setWidthFull();

        tagSelector.setLabel(t("order.tags"));
        tagSelector.setItems(availableTags);
        tagSelector.setItemLabelGenerator(this::tagLabel);
        tagSelector.setWidthFull();
        updateTagHelper();

        commentField.setLabel(t("order.comment"));
        commentField.setMaxLength(2000);
        commentField.setWidthFull();
        commentField.setHeight("92px");
        commentField.setHelperText(t("timetable.meta.comment.help"));
    }

    private void configureRouteInputs() {
        configureOperationalPointCombo(fromField, t("position.from"), t("position.from.help"));
        configureOperationalPointCombo(toField, t("position.to"), t("position.to.help"));

        departureAnchorField.setLabel(t("timetable.route.departureAnchor"));
        departureAnchorField.setHelperText(t("timetable.route.departureAnchor.help"));
        arrivalAnchorField.setLabel(t("timetable.route.arrivalAnchor"));
        arrivalAnchorField.setHelperText(t("timetable.route.arrivalAnchor.help"));

        viaList.setPadding(false);
        viaList.setSpacing(true);
        viaList.setWidthFull();

        routeSummary.getStyle()
                .set("display", "block")
                .set("font-size", "12px")
                .set("font-family", "'JetBrains Mono', monospace")
                .set("color", "var(--rom-text-secondary)");

        routeError.getStyle()
                .set("display", "block")
                .set("font-size", "12px")
                .set("font-weight", "600")
                .set("color", "var(--rom-status-danger)");

        routeMap.getElement().getStyle().set("height", "100%").set("min-height", "580px");
    }

    private void configureGrid() {
        rowGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COLUMN_BORDERS);
        rowGrid.setWidthFull();
        rowGrid.setHeight("460px");
        rowGrid.setSelectionMode(Grid.SelectionMode.SINGLE);

        rowGrid.addColumn(TimetableRowData::getSequence).setHeader("#").setAutoWidth(true).setFlexGrow(0);
        rowGrid.addColumn(row -> roleLabel(row.getRoutePointRole()))
                .setHeader(t("timetable.table.role"))
                .setAutoWidth(true);
        rowGrid.addColumn(TimetableRowData::getName)
                .setHeader(t("timetable.table.point"))
                .setAutoWidth(true)
                .setFlexGrow(1);
        rowGrid.addColumn(row -> nvl(row.getFromName()))
                .setHeader(t("timetable.table.from"))
                .setAutoWidth(true);
        rowGrid.addColumn(row -> nvl(row.getToName()))
                .setHeader(t("timetable.table.to"))
                .setAutoWidth(true);
        rowGrid.addColumn(row -> distanceLabel(row.getSegmentLengthMeters()))
                .setHeader(t("timetable.table.segment"))
                .setAutoWidth(true);
        rowGrid.addColumn(row -> distanceLabel(row.getDistanceFromStartMeters()))
                .setHeader(t("timetable.table.distance"))
                .setAutoWidth(true);
        rowGrid.addColumn(row -> timeOrDash(row.getEstimatedArrival()))
                .setHeader(t("timetable.table.estimatedArrival"))
                .setAutoWidth(true);
        rowGrid.addColumn(this::arrivalConstraintLabel)
                .setHeader(t("timetable.table.arrival"))
                .setAutoWidth(true);
        rowGrid.addColumn(row -> timeOrDash(row.getEstimatedDeparture()))
                .setHeader(t("timetable.table.estimatedDeparture"))
                .setAutoWidth(true);
        rowGrid.addColumn(this::departureConstraintLabel)
                .setHeader(t("timetable.table.departure"))
                .setAutoWidth(true);
        rowGrid.addColumn(row -> Boolean.TRUE.equals(row.getHalt()) ? t("common.yes") : t("common.no"))
                .setHeader(t("timetable.table.halt"))
                .setAutoWidth(true);
        rowGrid.addColumn(this::activityLabel)
                .setHeader(t("timetable.table.activity"))
                .setAutoWidth(true);

        rowGrid.asSingleSelect().addValueChangeListener(event -> {
            selectedRow = event.getValue();
            populateRowEditor(selectedRow);
        });
    }

    private void configureRowEditor() {
        rowEditor.setWidthFull();
        rowEditor.getStyle()
                .set("background", "var(--rom-bg-card)")
                .set("border", "1px solid var(--rom-border)")
                .set("border-radius", "6px")
                .set("padding", "16px")
                .set("box-sizing", "border-box")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "12px");

        rowEditorTitle.getStyle()
                .set("font-size", "15px")
                .set("font-weight", "600")
                .set("color", "var(--rom-text-primary)");
        rowEditorContext.getStyle()
                .set("font-size", "12px")
                .set("font-family", "'JetBrains Mono', monospace")
                .set("color", "var(--rom-text-muted)");

        haltField.setLabel(t("timetable.editor.halt"));
        haltField.addValueChangeListener(e -> activityField.setVisible(Boolean.TRUE.equals(e.getValue())));

        activityField.setLabel(t("timetable.editor.activity"));
        activityField.setItems(activityOptions);
        activityField.setItemLabelGenerator(this::activityOptionLabel);
        activityField.setWidthFull();

        dwellMinutesField.setLabel(t("timetable.editor.dwell"));
        dwellMinutesField.setMin(0);
        dwellMinutesField.setStepButtonsVisible(true);
        dwellMinutesField.setWidthFull();

        arrivalModeField.setLabel(t("timetable.editor.arrivalMode"));
        arrivalModeField.setItems(TimeConstraintMode.values());
        arrivalModeField.setItemLabelGenerator(this::timeModeLabel);
        arrivalModeField.setValue(TimeConstraintMode.NONE);
        arrivalModeField.addValueChangeListener(e -> updateModeVisibility());

        departureModeField.setLabel(t("timetable.editor.departureMode"));
        departureModeField.setItems(TimeConstraintMode.values());
        departureModeField.setItemLabelGenerator(this::timeModeLabel);
        departureModeField.setValue(TimeConstraintMode.NONE);
        departureModeField.addValueChangeListener(e -> updateModeVisibility());

        configureNamedTimePicker(arrivalExactField, t("timetable.editor.exact"));
        configureNamedTimePicker(arrivalEarliestField, t("timetable.editor.earliest"));
        configureNamedTimePicker(arrivalLatestField, t("timetable.editor.latest"));
        configureNamedTimePicker(departureExactField, t("timetable.editor.exact"));
        configureNamedTimePicker(departureEarliestField, t("timetable.editor.earliest"));
        configureNamedTimePicker(departureLatestField, t("timetable.editor.latest"));

        arrivalEstimateLabel.getStyle()
                .set("display", "block")
                .set("font-size", "11px")
                .set("font-family", "'JetBrains Mono', monospace")
                .set("color", "var(--rom-text-secondary)");
        departureEstimateLabel.getStyle()
                .set("display", "block")
                .set("font-size", "11px")
                .set("font-family", "'JetBrains Mono', monospace")
                .set("color", "var(--rom-text-secondary)");

        arrivalExactWrapper.add(arrivalExactField);
        arrivalWindowWrapper.getStyle().set("display", "grid").set("grid-template-columns", "1fr 1fr").set("gap", "12px");
        arrivalWindowWrapper.add(arrivalEarliestField, arrivalLatestField);
        arrivalSection.add(createEditorSectionHeader(t("timetable.editor.arrival")), arrivalEstimateLabel, arrivalModeField,
                arrivalExactWrapper, arrivalWindowWrapper);
        arrivalSection.getStyle()
                .set("padding", "12px")
                .set("border", "1px solid var(--rom-border)")
                .set("border-radius", "6px")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "10px");

        departureExactWrapper.add(departureExactField);
        departureWindowWrapper.getStyle().set("display", "grid").set("grid-template-columns", "1fr 1fr").set("gap", "12px");
        departureWindowWrapper.add(departureEarliestField, departureLatestField);
        departureSection.add(createEditorSectionHeader(t("timetable.editor.departure")), departureEstimateLabel, departureModeField,
                departureExactWrapper, departureWindowWrapper);
        departureSection.getStyle()
                .set("padding", "12px")
                .set("border", "1px solid var(--rom-border)")
                .set("border-radius", "6px")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "10px");

        HorizontalLayout rowFlags = new HorizontalLayout(haltField, dwellMinutesField);
        rowFlags.setWidthFull();
        rowFlags.expand(dwellMinutesField);
        rowFlags.setAlignItems(FlexComponent.Alignment.END);

        Button applyRowChanges = new Button(t("common.apply"), VaadinIcon.CHECK.create());
        applyRowChanges.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        applyRowChanges.getStyle()
                .set("background", "var(--rom-accent)")
                .set("color", "var(--rom-bg-primary)");
        applyRowChanges.addClickListener(e -> {
            if (syncSelectedRowFromEditor(true)) {
                Notification.show(t("timetable.editor.applied"), 1800, Position.BOTTOM_END)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            }
        });

        rowEditor.add(
                rowEditorTitle,
                rowEditorContext,
                rowFlags,
                activityField,
                arrivalSection,
                departureSection,
                applyRowChanges);
        rowEditor.setVisible(false);
        updateModeVisibility();
    }

    private void configureStepActions() {
        stepBackButton.setText(t("common.back"));
        stepBackButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
        stepBackButton.addClickListener(e -> {
            if (currentStep == Step.ROUTE) {
                navigateToOrder();
            } else {
                switchStep(Step.ROUTE);
            }
        });

        stepNextButton.setText(t("common.next"));
        stepNextButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
        stepNextButton.getStyle()
                .set("background", "var(--rom-accent)")
                .set("color", "var(--rom-bg-primary)");
        stepNextButton.addClickListener(e -> {
            if (currentStep == Step.ROUTE) {
                calculateRoute();
            } else {
                switchStep(Step.ROUTE);
            }
        });

        saveButton.setText(t("common.save"));
        saveButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
        saveButton.getStyle()
                .set("background", "var(--rom-status-info)")
                .set("color", "white");
        saveButton.addClickListener(e -> savePosition());
    }

    private void loadExistingData() {
        positionName.setValue(existingPosition != null ? textOrBlank(existingPosition.getName()) : "");
        commentField.setValue(existingPosition != null ? textOrBlank(existingPosition.getComment()) : "");
        readTags(existingPosition != null ? existingPosition.getTags() : null);

        LocalDate minDate = order.getValidFrom() != null ? order.getValidFrom() : LocalDate.now();
        LocalDate maxDate = order.getValidTo() != null ? order.getValidTo() : minDate.plusMonths(3);
        ValidityCalendar calendar = new ValidityCalendar(minDate, maxDate);
        if (existingPosition != null) {
            calendar.setSelectedDates(timetableArchiveService.parseValidityDates(existingPosition.getValidity()));
        }
        replaceValidityCalendar(calendar);

        if (existingPosition == null) {
            routeSummary.setText(t("timetable.route.empty"));
            routeError.setText("");
            return;
        }

        Optional<TimetableArchive> archive = timetableArchiveService.findArchive(existingPosition);
        if (archive.isPresent()) {
            List<TimetableRowData> rows = timetableArchiveService.readRows(archive.get());
            setTimetableRows(rows);
            currentRoute = timetableRoutingService.routeFromStoredRows(rows);
            routeMap.setRoute(currentRoute.points());
            prefillRouteInputsFromRows(rows);
            routeSummary.setText(routeSummaryText(rows, currentRoute));
            routeError.setText("");
            switchStep(Step.TABLE);
            return;
        }

        prefillLegacyRoute();
    }

    private void prefillRouteInputsFromRows(List<TimetableRowData> rows) {
        if (rows.isEmpty()) {
            return;
        }

        fromField.setValue(operationalPointsByUopid.get(rows.getFirst().getUopid()));
        toField.setValue(operationalPointsByUopid.get(rows.getLast().getUopid()));
        clearViaEditors();

        for (TimetableRowData row : rows) {
            if (row.getRoutePointRole() == RoutePointRole.VIA) {
                addViaEditor(operationalPointsByUopid.get(row.getUopid()), Boolean.TRUE.equals(row.getHalt()), row.getActivityCode());
            }
        }
    }

    private void prefillLegacyRoute() {
        if (existingPosition == null) {
            return;
        }

        Optional<OperationalPoint> fromPoint =
                timetableRoutingService.resolveLegacyPoint(existingPosition.getFromLocation());
        Optional<OperationalPoint> toPoint =
                timetableRoutingService.resolveLegacyPoint(existingPosition.getToLocation());

        fromField.setValue(fromPoint.orElse(null));
        toField.setValue(toPoint.orElse(null));

        if (existingPosition.getStart() != null) {
            departureAnchorField.setValue(existingPosition.getStart().toLocalTime());
        }
        if (existingPosition.getEnd() != null && departureAnchorField.getValue() == null) {
            arrivalAnchorField.setValue(existingPosition.getEnd().toLocalTime());
        }

        if (fromPoint.isPresent() && toPoint.isPresent()) {
            calculateRoute(false);
            return;
        }

        routeSummary.setText(t("timetable.route.empty"));
        routeError.setText(t("timetable.route.legacyUnresolved"));
    }

    private void replaceValidityCalendar(ValidityCalendar calendar) {
        this.validityCalendar = calendar;
    }

    private void switchStep(Step step) {
        currentStep = step;
        contentSlot.removeAll();
        contentSlot.add(step == Step.ROUTE ? createRouteStep() : createTableStep());
        updateStepControls();
    }

    private Component createRouteStep() {
        Button addViaButton = new Button(t("timetable.route.addVia"), VaadinIcon.PLUS.create());
        addViaButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
        addViaButton.addClickListener(e -> addViaEditor(null, false, null));

        FormLayout routeForm = new FormLayout();
        routeForm.setWidthFull();
        routeForm.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("640px", 2));
        routeForm.add(fromField, toField);
        routeForm.add(departureAnchorField, arrivalAnchorField);

        Div viaHeader = new Div();
        viaHeader.getStyle()
                .set("display", "flex")
                .set("justify-content", "space-between")
                .set("align-items", "center")
                .set("margin-top", "8px");
        Span viaTitle = new Span(t("timetable.route.via"));
        viaTitle.getStyle().set("font-weight", "600").set("color", "var(--rom-text-primary)");
        viaHeader.add(viaTitle, addViaButton);

        Button calculateButton = new Button(t("timetable.route.calculate"), VaadinIcon.MAP_MARKER.create());
        calculateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        calculateButton.getStyle()
                .set("background", "var(--rom-accent)")
                .set("color", "var(--rom-bg-primary)");
        calculateButton.addClickListener(e -> calculateRoute());

        VerticalLayout left = new VerticalLayout();
        left.setPadding(false);
        left.setSpacing(true);
        left.setWidthFull();
        left.add(
                createCard(t("timetable.route.title"),
                        routeForm,
                        helperText(t("timetable.route.anchor.help")),
                        viaHeader,
                        viaList,
                        routeSummary,
                        routeError,
                        calculateButton));

        Div mapCard = new Div();
        mapCard.setSizeFull();
        mapCard.getStyle()
                .set("background", "var(--rom-bg-card)")
                .set("border", "1px solid var(--rom-border)")
                .set("border-radius", "6px")
                .set("padding", "12px")
                .set("box-sizing", "border-box")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("min-height", "0");
        Span mapLabel = new Span(t("timetable.route.map"));
        mapLabel.getStyle().set("font-weight", "600").set("margin-bottom", "10px");
        routeMap.getElement().getStyle().set("flex", "1").set("min-height", "0");
        mapCard.add(mapLabel, routeMap);

        SplitLayout split = new SplitLayout(left, mapCard);
        split.setWidthFull();
        split.setSizeFull();
        split.setSplitterPosition(35);
        return split;
    }

    private Component createTableStep() {
        Div wrapper = new Div();
        wrapper.setWidthFull();
        wrapper.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "12px")
                .set("height", "100%")
                .set("min-height", "0");

        LocalDate orderFrom = order.getValidFrom() != null ? order.getValidFrom() : LocalDate.now();
        LocalDate orderTo = order.getValidTo() != null ? order.getValidTo() : orderFrom.plusMonths(3);

        Div validityCard = createCard(
                t("position.validity"),
                helperText(t("position.validity.help", orderFrom, orderTo)),
                validityCalendar);
        wrapper.add(validityCard);

        Div gridCard = createCard(
                t("timetable.table.title"),
                helperText(routeSummaryText(timetableRows, currentRoute)),
                rowGrid);
        Div editorCard = createCard(t("timetable.editor.title"), rowEditor);

        Scroller scroller = new Scroller(gridCard);
        scroller.setWidthFull();
        scroller.setHeight("500px");

        wrapper.add(scroller, editorCard);
        return wrapper;
    }

    private void calculateRoute() {
        calculateRoute(true);
    }

    private void calculateRoute(boolean switchToTable) {
        positionName.setInvalid(false);
        routeError.setText("");

        if (fromField.getValue() == null || toField.getValue() == null) {
            routeError.setText(t("timetable.route.pointsRequired"));
            return;
        }
        if (fromField.getValue().getUopid().equals(toField.getValue().getUopid())) {
            routeError.setText(t("timetable.route.samePoint"));
            return;
        }
        if (departureAnchorField.getValue() == null && arrivalAnchorField.getValue() == null) {
            routeError.setText(t("timetable.route.anchorRequired"));
            return;
        }
        if (departureAnchorField.getValue() != null && arrivalAnchorField.getValue() != null) {
            routeError.setText(t("timetable.route.anchorExclusive"));
            return;
        }

        List<OperationalPoint> waypoints = new ArrayList<>();
        waypoints.add(fromField.getValue());
        for (ViaPointEditor viaEditor : viaEditors) {
            if (viaEditor.pointField.getValue() == null) {
                routeError.setText(t("timetable.route.viaRequired"));
                return;
            }
            if (Boolean.TRUE.equals(viaEditor.haltField.getValue()) && viaEditor.activityField.getValue() == null) {
                routeError.setText(t("timetable.route.viaActivityRequired"));
                return;
            }
            waypoints.add(viaEditor.pointField.getValue());
        }
        waypoints.add(toField.getValue());

        try {
            currentRoute = timetableRoutingService.calculateRoute(waypoints);
            List<TimetableRowData> estimatedRows =
                    timetableRoutingService.estimateRows(
                            currentRoute, departureAnchorField.getValue(), arrivalAnchorField.getValue());
            applyViaPreferences(estimatedRows);
            setTimetableRows(estimatedRows);
            routeMap.setRoute(currentRoute.points());
            routeSummary.setText(routeSummaryText(estimatedRows, currentRoute));
            routeError.setText("");
            if (switchToTable) {
                switchStep(Step.TABLE);
            }
        } catch (RuntimeException exception) {
            timetableRows.clear();
            rowGrid.setItems(timetableRows);
            routeMap.clearRoute();
            routeSummary.setText(t("timetable.route.empty"));
            routeError.setText(t("timetable.route.unresolvedSegment", exception.getMessage()));
        }
    }

    private void applyViaPreferences(List<TimetableRowData> rows) {
        List<TimetableRowData> explicitRows =
                rows.stream().filter(row -> row.getRoutePointRole() == RoutePointRole.VIA).toList();
        for (int index = 0; index < Math.min(viaEditors.size(), explicitRows.size()); index++) {
            ViaPointEditor editor = viaEditors.get(index);
            TimetableRowData row = explicitRows.get(index);
            if (!Boolean.TRUE.equals(editor.haltField.getValue())) {
                continue;
            }
            row.setHalt(true);
            row.setActivityCode(editor.activityField.getValue() != null
                    ? editor.activityField.getValue().code()
                    : null);
            if (!isOrigin(row)) {
                row.setArrivalMode(TimeConstraintMode.EXACT);
                row.setArrivalExact(firstNonBlank(row.getArrivalExact(), row.getEstimatedArrival()));
            }
            if (!isDestination(row)) {
                row.setDepartureMode(TimeConstraintMode.EXACT);
                row.setDepartureExact(firstNonBlank(row.getDepartureExact(), row.getEstimatedDeparture()));
            }
        }
    }

    private void setTimetableRows(List<TimetableRowData> rows) {
        timetableRows.clear();
        timetableRows.addAll(rows);
        rowGrid.setItems(timetableRows);
        if (!timetableRows.isEmpty()) {
            rowGrid.asSingleSelect().setValue(timetableRows.getFirst());
            selectedRow = timetableRows.getFirst();
            populateRowEditor(selectedRow);
        } else {
            selectedRow = null;
            rowEditor.setVisible(false);
        }
    }

    private void populateRowEditor(TimetableRowData row) {
        if (row == null) {
            rowEditor.setVisible(false);
            return;
        }
        rowEditor.setVisible(true);
        rowEditorTitle.setText(row.getSequence() + ". " + row.getName() + " (" + roleLabel(row.getRoutePointRole()) + ")");
        rowEditorContext.setText(t("timetable.editor.context", nvl(row.getFromName()), nvl(row.getToName())));
        haltField.setValue(Boolean.TRUE.equals(row.getHalt()));
        dwellMinutesField.setValue(row.getDwellMinutes());
        activityField.setValue(findActivityOption(row.getActivityCode()).orElse(null));

        arrivalEstimateLabel.setText(t("timetable.editor.estimatedArrival") + ": " + timeOrDash(row.getEstimatedArrival()));
        departureEstimateLabel.setText(t("timetable.editor.estimatedDeparture") + ": " + timeOrDash(row.getEstimatedDeparture()));

        arrivalModeField.setValue(defaultMode(row.getArrivalMode()));
        departureModeField.setValue(defaultMode(row.getDepartureMode()));
        arrivalExactField.setValue(parseTime(row.getArrivalExact()));
        arrivalEarliestField.setValue(parseTime(row.getArrivalEarliest()));
        arrivalLatestField.setValue(parseTime(row.getArrivalLatest()));
        departureExactField.setValue(parseTime(row.getDepartureExact()));
        departureEarliestField.setValue(parseTime(row.getDepartureEarliest()));
        departureLatestField.setValue(parseTime(row.getDepartureLatest()));

        arrivalSection.setVisible(!isOrigin(row));
        departureSection.setVisible(!isDestination(row));
        activityField.setVisible(Boolean.TRUE.equals(row.getHalt()));
        updateModeVisibility();
    }

    private boolean syncSelectedRowFromEditor(boolean showNotifications) {
        if (selectedRow == null) {
            return true;
        }

        selectedRow.setHalt(Boolean.TRUE.equals(haltField.getValue()));
        selectedRow.setDwellMinutes(Boolean.TRUE.equals(haltField.getValue()) ? dwellMinutesField.getValue() : null);

        if (!writeTimeMode(selectedRow, true, arrivalModeField.getValue(), arrivalExactField, arrivalEarliestField,
                arrivalLatestField, showNotifications)) {
            return false;
        }
        if (!writeTimeMode(selectedRow, false, departureModeField.getValue(), departureExactField, departureEarliestField,
                departureLatestField, showNotifications)) {
            return false;
        }

        if (Boolean.TRUE.equals(selectedRow.getHalt())) {
            if (activityField.getValue() == null) {
                if (showNotifications) {
                    Notification.show(t("timetable.editor.activity.required"), 3000, Position.BOTTOM_END)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
                return false;
            }
            selectedRow.setActivityCode(activityField.getValue().code());
            ensureStopTimes(selectedRow);
        } else {
            selectedRow.setActivityCode(null);
        }

        rowGrid.getDataProvider().refreshItem(selectedRow);
        activityField.setVisible(Boolean.TRUE.equals(selectedRow.getHalt()));
        populateRowEditor(selectedRow);
        return true;
    }

    private boolean writeTimeMode(
            TimetableRowData row,
            boolean arrival,
            TimeConstraintMode mode,
            TimePicker exactField,
            TimePicker earliestField,
            TimePicker latestField,
            boolean showNotifications) {
        TimeConstraintMode resolvedMode = defaultMode(mode);
        if (arrival) {
            row.setArrivalMode(resolvedMode);
            row.setArrivalExact(null);
            row.setArrivalEarliest(null);
            row.setArrivalLatest(null);
        } else {
            row.setDepartureMode(resolvedMode);
            row.setDepartureExact(null);
            row.setDepartureEarliest(null);
            row.setDepartureLatest(null);
        }

        if (resolvedMode == TimeConstraintMode.NONE) {
            return true;
        }

        if (resolvedMode == TimeConstraintMode.EXACT) {
            if (exactField.getValue() == null) {
                if (showNotifications) {
                    Notification.show(t("timetable.editor.time.required"), 3000, Position.BOTTOM_END)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
                return false;
            }
            if (arrival) {
                row.setArrivalExact(formatTime(exactField.getValue()));
            } else {
                row.setDepartureExact(formatTime(exactField.getValue()));
            }
            return true;
        }

        if (earliestField.getValue() == null || latestField.getValue() == null
                || latestField.getValue().isBefore(earliestField.getValue())) {
            if (showNotifications) {
                Notification.show(t("timetable.editor.window.invalid"), 3000, Position.BOTTOM_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
            return false;
        }

        if (arrival) {
            row.setArrivalEarliest(formatTime(earliestField.getValue()));
            row.setArrivalLatest(formatTime(latestField.getValue()));
        } else {
            row.setDepartureEarliest(formatTime(earliestField.getValue()));
            row.setDepartureLatest(formatTime(latestField.getValue()));
        }
        return true;
    }

    private void ensureStopTimes(TimetableRowData row) {
        if (!isOrigin(row) && row.getArrivalMode() == TimeConstraintMode.NONE) {
            row.setArrivalMode(TimeConstraintMode.EXACT);
            row.setArrivalExact(firstNonBlank(row.getArrivalExact(), row.getEstimatedArrival()));
        }
        if (!isDestination(row) && row.getDepartureMode() == TimeConstraintMode.NONE) {
            row.setDepartureMode(TimeConstraintMode.EXACT);
            String fallback = row.getEstimatedDeparture();
            if (fallback == null && row.getEstimatedArrival() != null && row.getDwellMinutes() != null) {
                LocalTime estimatedArrival = parseTime(row.getEstimatedArrival());
                if (estimatedArrival != null) {
                    fallback = formatTime(estimatedArrival.plusMinutes(row.getDwellMinutes()));
                }
            }
            row.setDepartureExact(firstNonBlank(row.getDepartureExact(), fallback));
        }
    }

    private void savePosition() {
        if (currentStep == Step.TABLE && !syncSelectedRowFromEditor(false)) {
            return;
        }
        if (positionName.getValue().isBlank()) {
            positionName.setInvalid(true);
            Notification.show(t("timetable.meta.name.required"), 3000, Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        if (timetableRows.isEmpty()) {
            Notification.show(t("timetable.route.calculateFirst"), 3000, Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            switchStep(Step.ROUTE);
            return;
        }
        List<LocalDate> validityDates = validityCalendar.getSelectedDates();
        if (validityDates.isEmpty()) {
            Notification.show(t("position.validity.required"), 3000, Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        try {
            timetableArchiveService.saveTimetablePosition(
                    order,
                    existingPosition,
                    positionName.getValue(),
                    joinSelectedTags(),
                    commentField.getValue(),
                    validityDates,
                    new ArrayList<>(timetableRows));
            Notification.show(t("timetable.save.success"), 2500, Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            navigateToOrder();
        } catch (RuntimeException exception) {
            Notification.show(exception.getMessage(), 4000, Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void updateStepControls() {
        styleStepBadge(stepOneBadge, t("timetable.step.route"), currentStep == Step.ROUTE, true);
        styleStepBadge(stepTwoBadge, t("timetable.step.table"), currentStep == Step.TABLE, !timetableRows.isEmpty());

        stepBackButton.setText(currentStep == Step.ROUTE ? t("timetable.backToOrder") : t("common.back"));
        stepNextButton.setText(currentStep == Step.ROUTE ? t("common.next") : t("timetable.step.route"));
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
                .set("border", "1px solid "
                        + (active ? "var(--rom-accent)" : "var(--rom-border)"))
                .set("background", active ? "rgba(45,212,191,0.12)" : "rgba(148,163,184,0.08)")
                .set("color", active
                        ? "var(--rom-accent)"
                        : enabled ? "var(--rom-text-secondary)" : "var(--rom-text-muted)")
                .set("opacity", enabled ? "1" : "0.55");
    }

    private void configureOperationalPointCombo(
            ComboBox<OperationalPoint> comboBox, String label, String helper) {
        comboBox.setLabel(label);
        comboBox.setItems(availableOperationalPoints);
        comboBox.setItemLabelGenerator(this::opLabel);
        comboBox.setWidthFull();
        comboBox.setClearButtonVisible(true);
        comboBox.setHelperText(helper);
    }

    private void addViaEditor(OperationalPoint point, boolean halt, String activityCode) {
        ViaPointEditor editor = new ViaPointEditor();
        editor.pointField.setItems(availableOperationalPoints);
        editor.pointField.setItemLabelGenerator(this::opLabel);
        editor.pointField.setWidthFull();
        editor.pointField.setClearButtonVisible(true);
        editor.pointField.setValue(point);

        editor.haltField.setLabel(t("timetable.route.viaStop"));
        editor.haltField.setValue(halt);
        editor.haltField.addValueChangeListener(e -> editor.updateActivityVisibility());

        editor.activityField.setItems(activityOptions);
        editor.activityField.setItemLabelGenerator(this::activityOptionLabel);
        editor.activityField.setWidthFull();
        editor.activityField.setLabel(t("timetable.editor.activity"));
        editor.activityField.setValue(findActivityOption(activityCode).orElse(null));

        editor.removeButton.setText(t("common.delete"));
        editor.removeButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        editor.removeButton.getStyle().set("color", "var(--rom-status-danger)");
        editor.removeButton.addClickListener(e -> {
            viaEditors.remove(editor);
            viaList.remove(editor.container);
            renumberViaEditors();
        });

        HorizontalLayout actions = new HorizontalLayout(editor.haltField, editor.removeButton);
        actions.setWidthFull();
        actions.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        actions.setAlignItems(FlexComponent.Alignment.CENTER);

        editor.container.getStyle()
                .set("border", "1px solid var(--rom-border)")
                .set("border-radius", "6px")
                .set("padding", "10px")
                .set("background", "rgba(148,163,184,0.04)")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "10px");
        editor.container.add(editor.label, editor.pointField, actions, editor.activityField);
        editor.updateActivityVisibility();

        viaEditors.add(editor);
        viaList.add(editor.container);
        renumberViaEditors();
    }

    private void clearViaEditors() {
        viaEditors.clear();
        viaList.removeAll();
    }

    private void renumberViaEditors() {
        for (int index = 0; index < viaEditors.size(); index++) {
            viaEditors.get(index).label.setText(t("timetable.route.viaPoint", index + 1));
        }
    }

    private void updateModeVisibility() {
        arrivalExactWrapper.setVisible(arrivalModeField.getValue() == TimeConstraintMode.EXACT);
        arrivalWindowWrapper.setVisible(arrivalModeField.getValue() == TimeConstraintMode.WINDOW);
        departureExactWrapper.setVisible(departureModeField.getValue() == TimeConstraintMode.EXACT);
        departureWindowWrapper.setVisible(departureModeField.getValue() == TimeConstraintMode.WINDOW);
    }

    private Div createCard(String title, Component... content) {
        Div card = new Div();
        card.setWidthFull();
        card.getStyle()
                .set("background", "var(--rom-bg-card)")
                .set("border", "1px solid var(--rom-border)")
                .set("border-radius", "6px")
                .set("padding", "14px 16px")
                .set("box-sizing", "border-box");
        if (title != null && !title.isBlank()) {
            H3 heading = new H3(title);
            heading.getStyle()
                    .set("margin", "0 0 12px 0")
                    .set("font-size", "var(--lumo-font-size-l)")
                    .set("color", "var(--rom-text-primary)");
            card.add(heading);
        }
        card.add(content);
        return card;
    }

    private Span helperText(String text) {
        Span helper = new Span(text);
        helper.getStyle()
                .set("display", "block")
                .set("font-size", "11px")
                .set("color", "var(--rom-text-muted)")
                .set("margin-bottom", "10px");
        return helper;
    }

    private Span createEditorSectionHeader(String text) {
        Span header = new Span(text);
        header.getStyle().set("font-weight", "600").set("color", "var(--rom-text-primary)");
        return header;
    }

    private void configureNamedTimePicker(TimePicker picker, String label) {
        picker.setLabel(label);
        picker.setStep(Duration.ofMinutes(1));
        picker.setLocale(resolveLocale());
        picker.setPlaceholder("HH:mm");
        picker.setWidthFull();
        picker.setClearButtonVisible(true);
    }

    private TimePicker createTimePicker() {
        TimePicker picker = new TimePicker();
        picker.setStep(Duration.ofMinutes(1));
        picker.setAllowedCharPattern("[0-9:]");
        picker.setPlaceholder("HH:mm");
        picker.setLocale(resolveLocale());
        picker.setWidthFull();
        return picker;
    }

    private Locale resolveLocale() {
        return getLocale() != null ? getLocale() : Locale.GERMANY;
    }

    private void readTags(String stored) {
        Map<String, PredefinedTag> tagsByName = new LinkedHashMap<>();
        for (PredefinedTag tag : availableTags) {
            tagsByName.put(normalizeTagName(tag.getName()), tag);
        }

        unmatchedTags.clear();
        LinkedHashSet<PredefinedTag> selected = new LinkedHashSet<>();
        for (String token : splitTags(stored)) {
            PredefinedTag match = tagsByName.get(normalizeTagName(token));
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
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (PredefinedTag tag : availableTags) {
            if (tagSelector.getValue().contains(tag)) {
                values.add(tag.getName());
            }
        }
        values.addAll(unmatchedTags);
        return values.isEmpty() ? null : String.join(", ", values);
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
        String normalized = category == null ? "general" : category.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "order" -> t("settings.tags.cat.order");
            case "position" -> t("settings.tags.cat.position");
            default -> t("settings.tags.cat.general");
        };
    }

    private String normalizeTagName(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private List<String> splitTags(String rawTags) {
        List<String> values = new ArrayList<>();
        if (rawTags == null || rawTags.isBlank()) {
            return values;
        }
        for (String token : rawTags.split(",")) {
            String normalized = token.trim();
            if (!normalized.isBlank()) {
                values.add(normalized);
            }
        }
        return values;
    }

    private Optional<TimetableActivityOption> findActivityOption(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return activityOptions.stream().filter(option -> code.equals(option.code())).findFirst();
    }

    private String opLabel(OperationalPoint point) {
        return point.getName() + " (" + point.getUopid() + ")";
    }

    private String activityOptionLabel(TimetableActivityOption option) {
        return option.code() + " · " + option.label();
    }

    private String activityLabel(TimetableRowData row) {
        return findActivityOption(row.getActivityCode()).map(this::activityOptionLabel).orElse("—");
    }

    private String roleLabel(RoutePointRole role) {
        if (role == null) {
            return "—";
        }
        return switch (role) {
            case ORIGIN -> t("timetable.role.origin");
            case VIA -> t("timetable.role.via");
            case DESTINATION -> t("timetable.role.destination");
            case AUTO -> t("timetable.role.auto");
        };
    }

    private String timeModeLabel(TimeConstraintMode mode) {
        if (mode == null) {
            return "—";
        }
        return switch (mode) {
            case NONE -> t("timetable.mode.none");
            case EXACT -> t("timetable.mode.exact");
            case WINDOW -> t("timetable.mode.window");
        };
    }

    private String arrivalConstraintLabel(TimetableRowData row) {
        return timeConstraintLabel(row.getArrivalMode(), row.getArrivalExact(), row.getArrivalEarliest(), row.getArrivalLatest());
    }

    private String departureConstraintLabel(TimetableRowData row) {
        return timeConstraintLabel(row.getDepartureMode(), row.getDepartureExact(), row.getDepartureEarliest(), row.getDepartureLatest());
    }

    private String timeConstraintLabel(TimeConstraintMode mode, String exact, String earliest, String latest) {
        TimeConstraintMode resolved = defaultMode(mode);
        if (resolved == TimeConstraintMode.NONE) {
            return "—";
        }
        if (resolved == TimeConstraintMode.EXACT) {
            return timeOrDash(exact);
        }
        return timeOrDash(earliest) + "–" + timeOrDash(latest);
    }

    private String routeSummaryText(List<TimetableRowData> rows, TimetableRouteResult route) {
        if (rows == null || rows.isEmpty()) {
            return t("timetable.route.empty");
        }
        String distance = distanceLabel(route.totalLengthMeters());
        String departure = timeOrDash(rows.getFirst().getEstimatedDeparture());
        String arrival = timeOrDash(rows.getLast().getEstimatedArrival());
        return t("timetable.route.summary", rows.size(), distance, departure, arrival);
    }

    private String distanceLabel(Double meters) {
        if (meters == null) {
            return "0.0 km";
        }
        return String.format(Locale.GERMANY, "%.1f km", meters / 1000D);
    }

    private String timeOrDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private TimeConstraintMode defaultMode(TimeConstraintMode mode) {
        return mode != null ? mode : TimeConstraintMode.NONE;
    }

    private LocalTime parseTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalTime.parse(value, TIME_FORMAT);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private String formatTime(LocalTime value) {
        return value == null ? null : value.format(TIME_FORMAT);
    }

    private boolean isOrigin(TimetableRowData row) {
        return !timetableRows.isEmpty() && row == timetableRows.getFirst();
    }

    private boolean isDestination(TimetableRowData row) {
        return !timetableRows.isEmpty() && row == timetableRows.getLast();
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private String nvl(String value) {
        return value != null ? value : "—";
    }

    private String textOrBlank(String value) {
        return value != null ? value : "";
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

    private static final class ViaPointEditor {
        private final Div container = new Div();
        private final Span label = new Span();
        private final ComboBox<OperationalPoint> pointField = new ComboBox<>();
        private final Checkbox haltField = new Checkbox();
        private final ComboBox<TimetableActivityOption> activityField = new ComboBox<>();
        private final Button removeButton = new Button();

        private void updateActivityVisibility() {
            activityField.setVisible(Boolean.TRUE.equals(haltField.getValue()));
        }
    }
}
