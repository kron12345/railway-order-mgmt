package com.ordermgmt.railway.ui.view.pathmanager;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import jakarta.annotation.security.PermitAll;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.AbstractBackEndHierarchicalDataProvider;
import com.vaadin.flow.data.provider.hierarchy.HierarchicalQuery;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import com.ordermgmt.railway.domain.pathmanager.model.PathProcessState;
import com.ordermgmt.railway.domain.pathmanager.model.PmJourneyLocation;
import com.ordermgmt.railway.domain.pathmanager.model.PmReferenceTrain;
import com.ordermgmt.railway.domain.pathmanager.model.PmTimetableYear;
import com.ordermgmt.railway.domain.pathmanager.model.PmTrainVersion;
import com.ordermgmt.railway.domain.pathmanager.repository.PmJourneyLocationRepository;
import com.ordermgmt.railway.domain.pathmanager.repository.PmProcessStepRepository;
import com.ordermgmt.railway.domain.pathmanager.repository.PmReferenceTrainRepository;
import com.ordermgmt.railway.domain.pathmanager.repository.PmTimetableYearRepository;
import com.ordermgmt.railway.domain.pathmanager.repository.PmTrainVersionRepository;
import com.ordermgmt.railway.domain.pathmanager.service.PathManagerService;
import com.ordermgmt.railway.domain.pathmanager.service.PathProcessEngine;
import com.ordermgmt.railway.ui.component.StatusBadge;
import com.ordermgmt.railway.ui.component.pathmanager.JourneyLocationPanel;
import com.ordermgmt.railway.ui.component.pathmanager.ProcessSimulationPanel;
import com.ordermgmt.railway.ui.component.pathmanager.TrainHeaderPanel;
import com.ordermgmt.railway.ui.component.pathmanager.TreeNode;
import com.ordermgmt.railway.ui.layout.MainLayout;

/** Main view for the Path Manager module with tree hierarchy and detail panels. */
@Route(value = "pathmanager", layout = MainLayout.class)
@PageTitle("Path Manager")
@PermitAll
public class PathManagerView extends VerticalLayout {

    private final PmTimetableYearRepository timetableYearRepository;
    private final PmReferenceTrainRepository referenceTrainRepository;
    private final PmTrainVersionRepository trainVersionRepository;
    private final PmJourneyLocationRepository journeyLocationRepository;
    private final PathManagerService pathManagerService;
    private final PathProcessEngine processEngine;
    private final PmProcessStepRepository processStepRepository;

    private TreeGrid<TreeNode> treeGrid;
    private Div detailContainer;

    public PathManagerView(
            PmTimetableYearRepository timetableYearRepository,
            PmReferenceTrainRepository referenceTrainRepository,
            PmTrainVersionRepository trainVersionRepository,
            PmJourneyLocationRepository journeyLocationRepository,
            PathManagerService pathManagerService,
            PathProcessEngine processEngine,
            PmProcessStepRepository processStepRepository) {
        this.timetableYearRepository = timetableYearRepository;
        this.referenceTrainRepository = referenceTrainRepository;
        this.trainVersionRepository = trainVersionRepository;
        this.journeyLocationRepository = journeyLocationRepository;
        this.pathManagerService = pathManagerService;
        this.processEngine = processEngine;
        this.processStepRepository = processStepRepository;

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

    private void buildLayout() {
        SplitLayout split = new SplitLayout();
        split.setSizeFull();
        split.setSplitterPosition(40);

        split.addToPrimary(createTreePanel());
        split.addToSecondary(createDetailPanel());

        add(split);
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

        card.add(createInfoRow("Label", year.getLabel()));
        card.add(
                createInfoRow(
                        "Start",
                        year.getStartDate() != null ? year.getStartDate().toString() : "--"));
        card.add(
                createInfoRow(
                        "End", year.getEndDate() != null ? year.getEndDate().toString() : "--"));

        detailContainer.add(card);
    }

    private void showTrainDetail(TreeNode.TrainNode node) {
        PmReferenceTrain train = node.train();

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
                        (key, args) -> getTranslation(key),
                        updatedTrain -> refreshTree());

        detailContainer.add(headerPanel, processPanel);
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
                            referenceTrainRepository
                                    .findByTimetableYearYearOrderByOperationalTrainNumberAsc(
                                            yearNode.year().getYear())
                                    .size();
                    case TreeNode.TrainNode trainNode ->
                            trainVersionRepository
                                    .findByReferenceTrainIdOrderByVersionNumberDesc(
                                            trainNode.train().getId())
                                    .size();
                    case TreeNode.VersionNode versionNode ->
                            journeyLocationRepository
                                    .findByTrainVersionIdOrderBySequenceAsc(
                                            versionNode.version().getId())
                                    .size();
                    case TreeNode.LocationNode ignored -> 0;
                };
            }

            @Override
            public boolean hasChildren(TreeNode item) {
                return !(item instanceof TreeNode.LocationNode);
            }

            @Override
            protected Stream<TreeNode> fetchChildrenFromBackEnd(
                    HierarchicalQuery<TreeNode, Void> query) {
                TreeNode parent = query.getParent();
                List<TreeNode> children = new ArrayList<>();

                if (parent == null) {
                    List<PmTimetableYear> years = timetableYearRepository.findAll();
                    years.forEach(y -> children.add(new TreeNode.YearNode(y)));
                } else {
                    switch (parent) {
                        case TreeNode.YearNode yearNode -> {
                            List<PmReferenceTrain> trains =
                                    referenceTrainRepository
                                            .findByTimetableYearYearOrderByOperationalTrainNumberAsc(
                                                    yearNode.year().getYear());
                            trains.forEach(tr -> children.add(new TreeNode.TrainNode(tr)));
                        }
                        case TreeNode.TrainNode trainNode -> {
                            List<PmTrainVersion> versions =
                                    trainVersionRepository
                                            .findByReferenceTrainIdOrderByVersionNumberDesc(
                                                    trainNode.train().getId());
                            versions.forEach(v -> children.add(new TreeNode.VersionNode(v)));
                        }
                        case TreeNode.VersionNode versionNode -> {
                            List<PmJourneyLocation> locations =
                                    journeyLocationRepository
                                            .findByTrainVersionIdOrderBySequenceAsc(
                                                    versionNode.version().getId());
                            locations.forEach(l -> children.add(new TreeNode.LocationNode(l)));
                        }
                        case TreeNode.LocationNode ignored -> {
                            /* leaf */
                        }
                    }
                }

                return children.stream().skip(query.getOffset()).limit(query.getLimit());
            }
        };
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
        String label = state.name();
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
