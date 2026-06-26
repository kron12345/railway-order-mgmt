package com.ordermgmt.railway.ui.view.pathmanager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import jakarta.annotation.security.RolesAllowed;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.AbstractBackEndHierarchicalDataProvider;
import com.vaadin.flow.data.provider.hierarchy.HierarchicalQuery;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import com.ordermgmt.railway.domain.order.repository.OrderPositionRepository;
import com.ordermgmt.railway.domain.pathmanager.model.PathProcessState;
import com.ordermgmt.railway.domain.pathmanager.model.PathProcessType;
import com.ordermgmt.railway.domain.pathmanager.model.PmJourneyLocation;
import com.ordermgmt.railway.domain.pathmanager.model.PmReferenceTrain;
import com.ordermgmt.railway.domain.pathmanager.model.PmTimetableYear;
import com.ordermgmt.railway.domain.pathmanager.model.PmTrainVersion;
import com.ordermgmt.railway.domain.pathmanager.model.TtrPhase;
import com.ordermgmt.railway.domain.pathmanager.repository.PmJourneyLocationRepository;
import com.ordermgmt.railway.domain.pathmanager.repository.PmProcessStepRepository;
import com.ordermgmt.railway.domain.pathmanager.repository.PmReferenceTrainRepository;
import com.ordermgmt.railway.domain.pathmanager.repository.PmTimetableYearRepository;
import com.ordermgmt.railway.domain.pathmanager.repository.PmTrainVersionRepository;
import com.ordermgmt.railway.domain.pathmanager.service.PathManagerService;
import com.ordermgmt.railway.domain.pathmanager.service.PathProcessEngine;
import com.ordermgmt.railway.domain.pathmanager.service.TtrPhaseResolver;
import com.ordermgmt.railway.ui.component.StatusBadge;
import com.ordermgmt.railway.ui.component.pathmanager.JourneyLocationPanel;
import com.ordermgmt.railway.ui.component.pathmanager.ProcessSimulationPanel;
import com.ordermgmt.railway.ui.component.pathmanager.TrainHeaderPanel;
import com.ordermgmt.railway.ui.component.pathmanager.TreeNode;
import com.ordermgmt.railway.ui.layout.MainLayout;

/** Main view for the Path Manager module with tree hierarchy and detail panels. */
@Route(value = "pathmanager", layout = MainLayout.class)
@PageTitle("RailOpt")
@RolesAllowed({"ADMIN", "DISPATCHER"})
public class PathManagerView extends VerticalLayout implements BeforeEnterObserver {

    private final PmTimetableYearRepository timetableYearRepository;
    private final PmReferenceTrainRepository referenceTrainRepository;
    private final PmTrainVersionRepository trainVersionRepository;
    private final PmJourneyLocationRepository journeyLocationRepository;
    private final PathManagerService pathManagerService;
    private final PathProcessEngine processEngine;
    private final PmProcessStepRepository processStepRepository;
    private final TtrPhaseResolver ttrPhaseResolver;
    private final OrderPositionRepository orderPositionRepository;

    private TreeGrid<TreeNode> treeGrid;
    private Div detailContainer;
    private UUID pendingTrainId;

    public PathManagerView(
            PmTimetableYearRepository timetableYearRepository,
            PmReferenceTrainRepository referenceTrainRepository,
            PmTrainVersionRepository trainVersionRepository,
            PmJourneyLocationRepository journeyLocationRepository,
            PathManagerService pathManagerService,
            PathProcessEngine processEngine,
            PmProcessStepRepository processStepRepository,
            TtrPhaseResolver ttrPhaseResolver,
            OrderPositionRepository orderPositionRepository) {
        this.timetableYearRepository = timetableYearRepository;
        this.referenceTrainRepository = referenceTrainRepository;
        this.trainVersionRepository = trainVersionRepository;
        this.journeyLocationRepository = journeyLocationRepository;
        this.pathManagerService = pathManagerService;
        this.processEngine = processEngine;
        this.processStepRepository = processStepRepository;
        this.ttrPhaseResolver = ttrPhaseResolver;
        this.orderPositionRepository = orderPositionRepository;

        setPadding(false);
        setSpacing(false);
        setSizeFull();
        getStyle()
                .set("background", "var(--rom-bg-primary)")
                .set("padding", "var(--lumo-space-xs) var(--lumo-space-m)")
                .set("overflow", "hidden")
                .set("box-sizing", "border-box");

        buildLayout();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        var values = event.getLocation().getQueryParameters().getParameters().get("train");
        pendingTrainId = null;
        if (values != null && !values.isEmpty()) {
            try {
                pendingTrainId = UUID.fromString(values.get(0));
            } catch (IllegalArgumentException ignored) {
                pendingTrainId = null;
            }
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        if (pendingTrainId != null) {
            selectTrain(pendingTrainId);
            pendingTrainId = null;
        }
    }

    /** Deep-link target: expands and selects the given reference train and shows its detail. */
    private void selectTrain(UUID trainId) {
        referenceTrainRepository
                .findById(trainId)
                .ifPresent(
                        train -> {
                            PmTimetableYear year = train.getTimetableYear();
                            if (year != null) {
                                treeGrid.expand(new TreeNode.YearNode(year));
                            }
                            TreeNode.TrainNode node = new TreeNode.TrainNode(train);
                            treeGrid.select(node);
                            onNodeSelected(node);
                        });
    }

    private void buildLayout() {
        add(buildToolbar());

        SplitLayout split = new SplitLayout();
        split.setSizeFull();
        split.setSplitterPosition(40);

        split.addToPrimary(createTreePanel());
        split.addToSecondary(createDetailPanel());

        add(split);
        setFlexGrow(1, split);
    }

    /**
     * Toolbar with the mock-only "Reset" button. The path manager is a stand-in for an upstream
     * system; this lets a developer wipe the local state without going through the database
     * directly. The button is admin-only client-side <em>and</em> server-side (via
     * {@code @PreAuthorize}).
     */
    private com.vaadin.flow.component.Component buildToolbar() {
        var bar = new com.vaadin.flow.component.orderedlayout.HorizontalLayout();
        bar.setWidthFull();
        bar.setPadding(false);
        bar.setSpacing(true);
        bar.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
        bar.getStyle()
                .set("padding", "8px 12px")
                .set("border-bottom", "1px solid var(--rom-border-subtle)");

        var label = new com.vaadin.flow.component.html.Span(getTranslation("pm.view.badge"));
        label.addClassName("biz-section-title");
        label.getStyle().set("margin", "0");

        var spacer = new com.vaadin.flow.component.html.Div();
        spacer.getStyle().set("flex", "1");

        var resetBtn =
                new com.vaadin.flow.component.button.Button(
                        getTranslation("pm.reset"),
                        com.vaadin.flow.component.icon.VaadinIcon.TRASH.create(),
                        e -> confirmReset());
        resetBtn.addThemeVariants(
                com.vaadin.flow.component.button.ButtonVariant.LUMO_SMALL,
                com.vaadin.flow.component.button.ButtonVariant.LUMO_TERTIARY);
        resetBtn.getStyle().setColor("var(--rom-status-danger)");

        bar.add(label, spacer, resetBtn);
        bar.setFlexGrow(1, spacer);
        return bar;
    }

    private void confirmReset() {
        var dialog = new com.vaadin.flow.component.confirmdialog.ConfirmDialog();
        dialog.setHeader(getTranslation("pm.reset.title"));
        dialog.setText(getTranslation("pm.reset.text"));
        dialog.setCancelable(true);
        dialog.setCancelText(getTranslation("common.cancel"));
        dialog.setConfirmText(getTranslation("common.delete"));
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(
                e -> {
                    try {
                        pathManagerService.clearAllMockData();
                        com.vaadin.flow.component.notification.Notification.show(
                                        getTranslation("pm.reset.done"),
                                        1500,
                                        com.vaadin.flow.component.notification.Notification.Position
                                                .BOTTOM_END)
                                .addThemeVariants(
                                        com.vaadin.flow.component.notification.NotificationVariant
                                                .LUMO_SUCCESS);
                        com.vaadin.flow.component.UI.getCurrent().getPage().reload();
                    } catch (RuntimeException ex) {
                        com.vaadin.flow.component.notification.Notification.show(
                                        getTranslation("common.errorGeneric"),
                                        3000,
                                        com.vaadin.flow.component.notification.Notification.Position
                                                .BOTTOM_END)
                                .addThemeVariants(
                                        com.vaadin.flow.component.notification.NotificationVariant
                                                .LUMO_ERROR);
                    }
                });
        dialog.open();
    }

    private VerticalLayout createTreePanel() {
        VerticalLayout panel = new VerticalLayout();
        panel.setPadding(false);
        panel.setSpacing(false);
        panel.setSizeFull();

        Span title = new Span(getTranslation("pm.title"));
        title.getStyle()
                .set("font-size", "var(--lumo-font-size-l)")
                .set("font-weight", "600")
                .set("color", "var(--rom-text-primary)")
                .set("padding", "var(--lumo-space-xs) 0");
        panel.add(title);

        treeGrid = new TreeGrid<>();
        treeGrid.setSizeFull();

        treeGrid.addHierarchyColumn(TreeNode::label)
                .setHeader(getTranslation("pm.train"))
                .setFlexGrow(1);

        treeGrid.addComponentColumn(this::createTypeBadge)
                .setHeader("Type")
                .setWidth("80px")
                .setFlexGrow(0);

        treeGrid.addComponentColumn(this::createStateBadge)
                .setHeader("State")
                .setWidth("110px")
                .setFlexGrow(0);

        treeGrid.addColumn(this::getOtn)
                .setHeader(getTranslation("pm.otn"))
                .setWidth("100px")
                .setFlexGrow(0);

        treeGrid.setDataProvider(createDataProvider());
        treeGrid.asSingleSelect().addValueChangeListener(e -> onNodeSelected(e.getValue()));

        panel.add(treeGrid);
        return panel;
    }

    private Div createDetailPanel() {
        detailContainer = new Div();
        detailContainer.setSizeFull();
        detailContainer.getStyle().set("overflow-y", "auto").set("padding", "var(--lumo-space-xs)");

        showEmptyDetail();
        return detailContainer;
    }

    private void showEmptyDetail() {
        detailContainer.removeAll();
        Span hint = new Span(getTranslation("pm.title"));
        hint.getStyle()
                .set("color", "var(--rom-text-muted)")
                .set("font-size", "13px")
                .set("padding", "var(--lumo-space-m)");
        detailContainer.add(hint);
    }

    private void onNodeSelected(TreeNode node) {
        if (node == null) {
            showEmptyDetail();
            return;
        }
        detailContainer.removeAll();

        switch (node) {
            case TreeNode.YearNode yearNode -> showYearDetail(yearNode);
            case TreeNode.TrainNode trainNode -> showTrainDetail(trainNode);
            case TreeNode.VersionNode versionNode -> showVersionDetail(versionNode);
            case TreeNode.LocationNode locationNode -> showLocationDetail(locationNode);
        }
    }

    private void showYearDetail(TreeNode.YearNode node) {
        PmTimetableYear year = node.year();
        Div card = createDetailCard();

        Span title = new Span(getTranslation("pm.year") + ": " + year.getYear());
        title.getStyle()
                .set("font-size", "var(--lumo-font-size-l)")
                .set("font-weight", "600")
                .set("color", "var(--rom-text-primary)")
                .set("display", "block")
                .set("margin-bottom", "var(--lumo-space-s)");
        card.add(title);

        // TTR phase indicator
        card.add(createTtrPhaseBadge(year));

        card.add(createInfoRow("Label", year.getLabel()));
        card.add(
                createInfoRow(
                        "Start",
                        year.getStartDate() != null ? year.getStartDate().toString() : "--"));
        card.add(
                createInfoRow(
                        "End", year.getEndDate() != null ? year.getEndDate().toString() : "--"));

        // Phase-specific process info
        if (year.getStartDate() != null) {
            TtrPhase phase = ttrPhaseResolver.resolvePhase(year, java.time.LocalDate.now());
            PathProcessType processType =
                    ttrPhaseResolver.resolveProcessType(year, java.time.LocalDate.now());
            card.add(
                    createInfoRow(
                            getTranslation("ttr.phase.info", ""),
                            getTranslation("ttr.phase." + phase.name())));
            if (processType != null) {
                card.add(
                        createInfoRow(
                                getTranslation("ttr.phase.processType", ""),
                                processType.name() + " (" + processType.code() + ")"));
            }
            if (!ttrPhaseResolver.isDraftOfferAllowed(year, java.time.LocalDate.now())) {
                card.add(createInfoRow("", getTranslation("ttr.phase.noDraft")));
            }
        }

        detailContainer.add(card);
    }

    private HorizontalLayout createTtrPhaseBadge(PmTimetableYear year) {
        HorizontalLayout row = new HorizontalLayout();
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setSpacing(true);
        row.getStyle().set("margin-bottom", "var(--lumo-space-s)");

        if (year.getStartDate() == null) {
            return row;
        }

        TtrPhase phase = ttrPhaseResolver.resolvePhase(year, java.time.LocalDate.now());
        String badgeText =
                "FPJ " + year.getYear() + " — " + getTranslation("ttr.phase." + phase.name());

        Span badge = new Span(badgeText);
        badge.getStyle()
                .set("padding", "4px 12px")
                .set("border-radius", "12px")
                .set("font-size", "12px")
                .set("font-weight", "600");

        switch (phase) {
            case ANNUAL_ORDERING ->
                    badge.getStyle()
                            .set("background", "rgba(34, 197, 94, 0.15)")
                            .set("color", "rgb(34, 197, 94)")
                            .set("border", "1px solid rgba(34, 197, 94, 0.3)");
            case LATE_ORDERING ->
                    badge.getStyle()
                            .set("background", "rgba(234, 179, 8, 0.15)")
                            .set("color", "rgb(202, 138, 4)")
                            .set("border", "1px solid rgba(234, 179, 8, 0.3)");
            case AD_HOC_ORDERING ->
                    badge.getStyle()
                            .set("background", "rgba(249, 115, 22, 0.15)")
                            .set("color", "rgb(234, 88, 12)")
                            .set("border", "1px solid rgba(249, 115, 22, 0.3)");
            case PAST ->
                    badge.getStyle()
                            .set("background", "rgba(156, 163, 175, 0.15)")
                            .set("color", "rgb(107, 114, 128)")
                            .set("border", "1px solid rgba(156, 163, 175, 0.3)");
            default ->
                    badge.getStyle()
                            .set("background", "rgba(99, 102, 241, 0.15)")
                            .set("color", "rgb(79, 70, 229)")
                            .set("border", "1px solid rgba(99, 102, 241, 0.3)");
        }

        row.add(badge);
        return row;
    }

    private void showTrainDetail(TreeNode.TrainNode node) {
        PmReferenceTrain train = node.train();

        if (train.getSourcePositionId() != null) {
            detailContainer.add(createBackToOrderLink(train.getSourcePositionId()));
        }

        TrainHeaderPanel headerPanel =
                new TrainHeaderPanel(
                        train,
                        pathManagerService,
                        (key, args) -> getTranslation(key),
                        savedTrain -> refreshTree());

        ProcessSimulationPanel processPanel =
                new ProcessSimulationPanel(
                        train,
                        processEngine,
                        processStepRepository,
                        ttrPhaseResolver,
                        (key, args) -> getTranslation(key),
                        updatedTrain -> refreshTree());

        detailContainer.add(headerPanel, processPanel);
    }

    /**
     * Back-link from a RailOpt train to the originating order (uses the train's sourcePositionId).
     */
    private Button createBackToOrderLink(UUID sourcePositionId) {
        Button back = new Button(getTranslation("pm.backToOrder"), VaadinIcon.ARROW_LEFT.create());
        back.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        back.getStyle()
                .set("color", "var(--rom-accent)")
                .set("margin-bottom", "var(--lumo-space-s)");
        back.addClickListener(
                e ->
                        orderPositionRepository
                                .findOrderIdById(sourcePositionId)
                                .ifPresent(
                                        orderId -> UI.getCurrent().navigate("orders/" + orderId)));
        return back;
    }

    private void showVersionDetail(TreeNode.VersionNode node) {
        PmTrainVersion version = node.version();
        Div card = createDetailCard();

        Span title = new Span(getTranslation("pm.version") + ": " + version.getLabel());
        title.getStyle()
                .set("font-size", "var(--lumo-font-size-l)")
                .set("font-weight", "600")
                .set("color", "var(--rom-text-primary)")
                .set("display", "block")
                .set("margin-bottom", "var(--lumo-space-s)");
        card.add(title);

        card.add(createInfoRow("Number", String.valueOf(version.getVersionNumber())));
        card.add(
                createInfoRow(
                        "Type",
                        version.getVersionType() != null ? version.getVersionType().name() : "--"));
        card.add(
                createInfoRow(
                        getTranslation("pm.otn"), safeString(version.getOperationalTrainNumber())));
        card.add(createInfoRow(getTranslation("pm.trainType"), safeString(version.getTrainType())));
        card.add(createInfoRow(getTranslation("pm.weight"), safeInt(version.getTrainWeight())));
        card.add(createInfoRow(getTranslation("pm.length"), safeInt(version.getTrainLength())));
        card.add(createInfoRow(getTranslation("pm.maxSpeed"), safeInt(version.getTrainMaxSpeed())));

        // Location summary
        List<PmJourneyLocation> locations =
                journeyLocationRepository.findByTrainVersionIdOrderBySequenceAsc(version.getId());
        if (!locations.isEmpty()) {
            Span locTitle = new Span(getTranslation("pm.location") + " (" + locations.size() + ")");
            locTitle.getStyle()
                    .set("font-weight", "600")
                    .set("color", "var(--rom-text-secondary)")
                    .set("font-size", "12px")
                    .set("text-transform", "uppercase")
                    .set("letter-spacing", "0.05em")
                    .set("display", "block")
                    .set("margin-top", "var(--lumo-space-s)")
                    .set("margin-bottom", "var(--lumo-space-xs)");
            card.add(locTitle);

            Grid<PmJourneyLocation> locGrid = new Grid<>();
            locGrid.setWidthFull();
            locGrid.setAllRowsVisible(true);
            locGrid.addColumn(PmJourneyLocation::getSequence).setHeader("#").setWidth("50px");
            locGrid.addColumn(loc -> safeString(loc.getPrimaryLocationName()))
                    .setHeader(getTranslation("pm.location"))
                    .setFlexGrow(1);
            locGrid.addColumn(loc -> safeString(loc.getArrivalTime()))
                    .setHeader("Arr")
                    .setWidth("80px");
            locGrid.addColumn(loc -> safeString(loc.getDepartureTime()))
                    .setHeader("Dep")
                    .setWidth("80px");
            locGrid.setItems(locations);
            card.add(locGrid);
        }

        detailContainer.add(card);
    }

    private void showLocationDetail(TreeNode.LocationNode node) {
        PmJourneyLocation location = node.location();
        JourneyLocationPanel panel =
                new JourneyLocationPanel(
                        location, pathManagerService, (key, args) -> getTranslation(key));
        detailContainer.add(panel);
    }

    private AbstractBackEndHierarchicalDataProvider<TreeNode, Void> createDataProvider() {
        return new AbstractBackEndHierarchicalDataProvider<>() {
            @Override
            public int getChildCount(HierarchicalQuery<TreeNode, Void> query) {
                TreeNode parent = query.getParent();
                if (parent == null) {
                    return (int) timetableYearRepository.count();
                }
                return switch (parent) {
                    case TreeNode.YearNode yearNode ->
                            (int)
                                    referenceTrainRepository.countByTimetableYearYear(
                                            yearNode.year().getYear());
                    case TreeNode.TrainNode trainNode ->
                            (int)
                                    trainVersionRepository.countByReferenceTrainId(
                                            trainNode.train().getId());
                    case TreeNode.VersionNode versionNode ->
                            (int)
                                    journeyLocationRepository.countByTrainVersionId(
                                            versionNode.version().getId());
                    case TreeNode.LocationNode ignored -> 0;
                };
            }

            @Override
            public Object getId(TreeNode item) {
                return item.id();
            }

            @Override
            public boolean hasChildren(TreeNode item) {
                return !(item instanceof TreeNode.LocationNode);
            }

            @Override
            protected Stream<TreeNode> fetchChildrenFromBackEnd(
                    HierarchicalQuery<TreeNode, Void> query) {
                TreeNode parent = query.getParent();
                Pageable pageable = pageOf(query);
                List<TreeNode> children = new ArrayList<>();

                if (parent == null) {
                    timetableYearRepository
                            .findAll(pageable)
                            .forEach(y -> children.add(new TreeNode.YearNode(y)));
                } else {
                    switch (parent) {
                        case TreeNode.YearNode yearNode ->
                                referenceTrainRepository
                                        .findByTimetableYearYearOrderByOperationalTrainNumberAsc(
                                                yearNode.year().getYear(), pageable)
                                        .forEach(tr -> children.add(new TreeNode.TrainNode(tr)));
                        case TreeNode.TrainNode trainNode ->
                                trainVersionRepository
                                        .findByReferenceTrainIdOrderByVersionNumberDesc(
                                                trainNode.train().getId(), pageable)
                                        .forEach(v -> children.add(new TreeNode.VersionNode(v)));
                        case TreeNode.VersionNode versionNode ->
                                journeyLocationRepository
                                        .findByTrainVersionIdOrderBySequenceAsc(
                                                versionNode.version().getId(), pageable)
                                        .forEach(l -> children.add(new TreeNode.LocationNode(l)));
                        case TreeNode.LocationNode ignored -> {
                            /* leaf */
                        }
                    }
                }

                // Page already applied in the DB query — return the fetched page as-is.
                return children.stream();
            }
        };
    }

    /** The TreeGrid's offset/limit as a Spring page request (offset is page-aligned for grids). */
    private static Pageable pageOf(HierarchicalQuery<TreeNode, Void> query) {
        int limit = Math.max(1, query.getLimit());
        return PageRequest.of(query.getOffset() / limit, limit);
    }

    private void refreshTree() {
        treeGrid.getDataProvider().refreshAll();
    }

    private Span createTypeBadge(TreeNode node) {
        String typeLabel = node.type();
        StatusBadge.StatusType badgeType =
                switch (typeLabel) {
                    case "YEAR" -> StatusBadge.StatusType.NEUTRAL;
                    case "TRAIN" -> StatusBadge.StatusType.INFO;
                    case "VERSION" -> StatusBadge.StatusType.WARNING;
                    case "OP" -> StatusBadge.StatusType.SUCCESS;
                    default -> StatusBadge.StatusType.NEUTRAL;
                };
        return new StatusBadge(typeLabel, badgeType);
    }

    private Span createStateBadge(TreeNode node) {
        if (node instanceof TreeNode.TrainNode trainNode) {
            PathProcessState state = trainNode.train().getProcessState();
            if (state != null) {
                return stateToStatusBadge(state);
            }
        }
        return new Span();
    }

    private StatusBadge stateToStatusBadge(PathProcessState state) {
        String label = getTranslation("pm.state." + state.name());
        return switch (state) {
            case NEW -> new StatusBadge(label, StatusBadge.StatusType.NEUTRAL);
            case CREATED, MODIFIED, RECEIPT_CONFIRMED ->
                    new StatusBadge(label, StatusBadge.StatusType.INFO);
            case DRAFT_OFFERED,
                    FINAL_OFFERED,
                    REVISION_REQUESTED,
                    MODIFICATION_REQUESTED,
                    ALTERATION_ANNOUNCED,
                    ALTERATION_OFFERED ->
                    new StatusBadge(label, StatusBadge.StatusType.WARNING);
            case BOOKED -> new StatusBadge(label, StatusBadge.StatusType.SUCCESS);
            case WITHDRAWN, CANCELED, NO_ALTERNATIVE ->
                    new StatusBadge(label, StatusBadge.StatusType.DANGER);
            case SUPERSEDED -> new StatusBadge(label, StatusBadge.StatusType.NEUTRAL);
        };
    }

    private String getOtn(TreeNode node) {
        if (node instanceof TreeNode.TrainNode trainNode) {
            return safeString(trainNode.train().getOperationalTrainNumber());
        }
        return "";
    }

    private Div createDetailCard() {
        Div card = new Div();
        card.setWidthFull();
        card.getStyle()
                .set("background", "var(--rom-bg-card)")
                .set("border", "1px solid var(--rom-border)")
                .set("border-radius", "6px")
                .set("padding", "var(--lumo-space-m)")
                .set("box-sizing", "border-box")
                .set("margin-bottom", "var(--lumo-space-s)");
        return card;
    }

    private Div createInfoRow(String labelText, String value) {
        Div row = new Div();
        row.getStyle()
                .set("display", "flex")
                .set("gap", "var(--lumo-space-s)")
                .set("padding", "2px 0")
                .set("font-size", "12px");

        Span label = new Span(labelText + ":");
        label.getStyle()
                .set("color", "var(--rom-text-muted)")
                .set("min-width", "120px")
                .set("font-weight", "600");

        Span val = new Span(value);
        val.getStyle()
                .set("color", "var(--rom-text-primary)")
                .set("font-family", "'JetBrains Mono', monospace");

        row.add(label, val);
        return row;
    }

    private String safeString(String value) {
        return value != null ? value : "--";
    }

    private String safeInt(Integer value) {
        return value != null ? value.toString() : "--";
    }
}
