package com.ordermgmt.railway.ui.component.pathmanager;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import com.ordermgmt.railway.domain.order.repository.OrderPositionRepository;
import com.ordermgmt.railway.domain.pathmanager.model.PathProcessType;
import com.ordermgmt.railway.domain.pathmanager.model.PmJourneyLocation;
import com.ordermgmt.railway.domain.pathmanager.model.PmReferenceTrain;
import com.ordermgmt.railway.domain.pathmanager.model.PmTimetableYear;
import com.ordermgmt.railway.domain.pathmanager.model.PmTrainVersion;
import com.ordermgmt.railway.domain.pathmanager.model.TtrPhase;
import com.ordermgmt.railway.domain.pathmanager.repository.PmJourneyLocationRepository;
import com.ordermgmt.railway.domain.pathmanager.repository.PmProcessStepRepository;
import com.ordermgmt.railway.domain.pathmanager.service.PathManagerService;
import com.ordermgmt.railway.domain.pathmanager.service.PathProcessEngine;
import com.ordermgmt.railway.domain.pathmanager.service.TtrPhaseResolver;

/**
 * Right-hand detail pane of the {@link com.ordermgmt.railway.ui.view.pathmanager.PathManagerView}.
 * Renders the selected tree node (timetable year / train / version / journey location) into itself.
 * Extracted from the view so each stays readable; the view owns the tree + toolbar and calls {@link
 * #show(TreeNode)}. Train edits delegate the tree refresh back via {@code onTreeRefresh}.
 */
public class PathManagerDetailPanel extends Div {

    private final PathManagerService pathManagerService;
    private final PathProcessEngine processEngine;
    private final PmProcessStepRepository processStepRepository;
    private final TtrPhaseResolver ttrPhaseResolver;
    private final PmJourneyLocationRepository journeyLocationRepository;
    private final OrderPositionRepository orderPositionRepository;
    private final Runnable onTreeRefresh;

    public PathManagerDetailPanel(
            PathManagerService pathManagerService,
            PathProcessEngine processEngine,
            PmProcessStepRepository processStepRepository,
            TtrPhaseResolver ttrPhaseResolver,
            PmJourneyLocationRepository journeyLocationRepository,
            OrderPositionRepository orderPositionRepository,
            Runnable onTreeRefresh) {
        this.pathManagerService = pathManagerService;
        this.processEngine = processEngine;
        this.processStepRepository = processStepRepository;
        this.ttrPhaseResolver = ttrPhaseResolver;
        this.journeyLocationRepository = journeyLocationRepository;
        this.orderPositionRepository = orderPositionRepository;
        this.onTreeRefresh = onTreeRefresh;
        setSizeFull();
        getStyle().set("overflow-y", "auto").set("padding", "var(--lumo-space-xs)");
        showEmpty();
    }

    /** Shows a placeholder hint when nothing is selected. */
    public void showEmpty() {
        removeAll();
        Span hint = new Span(getTranslation("pm.title"));
        hint.getStyle()
                .set("color", "var(--rom-text-muted)")
                .set("font-size", "13px")
                .set("padding", "var(--lumo-space-m)");
        add(hint);
    }

    /** Renders the given tree node's detail, or the empty hint when {@code node} is null. */
    public void show(TreeNode node) {
        if (node == null) {
            showEmpty();
            return;
        }
        removeAll();
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
            TtrPhase phase = ttrPhaseResolver.resolvePhase(year, LocalDate.now());
            PathProcessType processType =
                    ttrPhaseResolver.resolveProcessType(year, LocalDate.now());
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
            if (!ttrPhaseResolver.isDraftOfferAllowed(year, LocalDate.now())) {
                card.add(createInfoRow("", getTranslation("ttr.phase.noDraft")));
            }
        }

        add(card);
    }

    private HorizontalLayout createTtrPhaseBadge(PmTimetableYear year) {
        HorizontalLayout row = new HorizontalLayout();
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setSpacing(true);
        row.getStyle().set("margin-bottom", "var(--lumo-space-s)");

        if (year.getStartDate() == null) {
            return row;
        }

        TtrPhase phase = ttrPhaseResolver.resolvePhase(year, LocalDate.now());
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
            add(createBackToOrderLink(train.getSourcePositionId()));
        }

        TrainHeaderPanel headerPanel =
                new TrainHeaderPanel(
                        train,
                        pathManagerService,
                        (key, args) -> getTranslation(key),
                        savedTrain -> onTreeRefresh.run());

        ProcessSimulationPanel processPanel =
                new ProcessSimulationPanel(
                        train,
                        processEngine,
                        processStepRepository,
                        ttrPhaseResolver,
                        (key, args) -> getTranslation(key),
                        updatedTrain -> onTreeRefresh.run());

        add(headerPanel, processPanel);
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

        add(card);
    }

    private void showLocationDetail(TreeNode.LocationNode node) {
        PmJourneyLocation location = node.location();
        JourneyLocationPanel panel =
                new JourneyLocationPanel(
                        location, pathManagerService, (key, args) -> getTranslation(key));
        add(panel);
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
