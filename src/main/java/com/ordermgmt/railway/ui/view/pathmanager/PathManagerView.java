package com.ordermgmt.railway.ui.view.pathmanager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import jakarta.annotation.security.RolesAllowed;

import org.springframework.data.domain.Pageable;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
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
import com.ordermgmt.railway.domain.pathmanager.model.PmTimetableYear;
import com.ordermgmt.railway.domain.pathmanager.repository.PmJourneyLocationRepository;
import com.ordermgmt.railway.domain.pathmanager.repository.PmProcessStepRepository;
import com.ordermgmt.railway.domain.pathmanager.repository.PmReferenceTrainRepository;
import com.ordermgmt.railway.domain.pathmanager.repository.PmTimetableYearRepository;
import com.ordermgmt.railway.domain.pathmanager.repository.PmTrainVersionRepository;
import com.ordermgmt.railway.domain.pathmanager.service.PathManagerService;
import com.ordermgmt.railway.domain.pathmanager.service.PathProcessEngine;
import com.ordermgmt.railway.domain.pathmanager.service.TtrPhaseResolver;
import com.ordermgmt.railway.ui.component.StatusBadge;
import com.ordermgmt.railway.ui.component.pathmanager.PathManagerDetailPanel;
import com.ordermgmt.railway.ui.component.pathmanager.TreeNode;
import com.ordermgmt.railway.ui.layout.MainLayout;
import com.ordermgmt.railway.ui.support.OffsetPageable;

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
    private PathManagerDetailPanel detailPanel;
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
                            detailPanel.show(node);
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
    private Component buildToolbar() {
        var bar = new HorizontalLayout();
        bar.setWidthFull();
        bar.setPadding(false);
        bar.setSpacing(true);
        bar.setAlignItems(FlexComponent.Alignment.CENTER);
        bar.getStyle()
                .set("padding", "8px 12px")
                .set("border-bottom", "1px solid var(--rom-border-subtle)");

        var label = new Span(getTranslation("pm.view.badge"));
        label.addClassName("biz-section-title");
        label.getStyle().set("margin", "0");

        var spacer = new Div();
        spacer.getStyle().set("flex", "1");

        var resetBtn =
                new Button(
                        getTranslation("pm.reset"), VaadinIcon.TRASH.create(), e -> confirmReset());
        resetBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        resetBtn.getStyle().setColor("var(--rom-status-danger)");

        bar.add(label, spacer, resetBtn);
        bar.setFlexGrow(1, spacer);
        return bar;
    }

    private void confirmReset() {
        var dialog = new ConfirmDialog();
        dialog.setHeader(getTranslation("pm.reset.title"));
        dialog.setText(getTranslation("pm.reset.text"));
        dialog.setCancelable(true);
        dialog.setCancelText(getTranslation("common.cancel"));
        dialog.setConfirmText(getTranslation("common.delete"));
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(e -> resetMockData());
        dialog.open();
    }

    private void resetMockData() {
        try {
            pathManagerService.clearAllMockData();
            Notification.show(
                            getTranslation("pm.reset.done"), 1500, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            UI.getCurrent().getPage().reload();
        } catch (RuntimeException ex) {
            Notification.show(
                            getTranslation("common.errorGeneric"),
                            3000,
                            Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
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
        treeGrid.asSingleSelect().addValueChangeListener(e -> detailPanel.show(e.getValue()));

        panel.add(treeGrid);
        return panel;
    }

    private Component createDetailPanel() {
        detailPanel =
                new PathManagerDetailPanel(
                        pathManagerService,
                        processEngine,
                        processStepRepository,
                        ttrPhaseResolver,
                        journeyLocationRepository,
                        orderPositionRepository,
                        this::refreshTree);
        return detailPanel;
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

    /** The TreeGrid's exact offset/limit as a Pageable (honors non-page-aligned offsets). */
    private static Pageable pageOf(HierarchicalQuery<TreeNode, Void> query) {
        return new OffsetPageable(query.getOffset(), Math.max(1, query.getLimit()));
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

    private String safeString(String value) {
        return value != null ? value : "--";
    }
}
