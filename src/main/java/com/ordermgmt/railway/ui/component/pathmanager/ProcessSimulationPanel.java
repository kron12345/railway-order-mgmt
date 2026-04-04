package com.ordermgmt.railway.ui.component.pathmanager;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;

import com.ordermgmt.railway.domain.pathmanager.model.PathAction;
import com.ordermgmt.railway.domain.pathmanager.model.PathProcessState;
import com.ordermgmt.railway.domain.pathmanager.model.PathProcessType;
import com.ordermgmt.railway.domain.pathmanager.model.PmProcessStep;
import com.ordermgmt.railway.domain.pathmanager.model.PmReferenceTrain;
import com.ordermgmt.railway.domain.pathmanager.model.PmTimetableYear;
import com.ordermgmt.railway.domain.pathmanager.model.ProcessStepResult;
import com.ordermgmt.railway.domain.pathmanager.model.TtrPhase;
import com.ordermgmt.railway.domain.pathmanager.repository.PmProcessStepRepository;
import com.ordermgmt.railway.domain.pathmanager.service.PathProcessEngine;
import com.ordermgmt.railway.domain.pathmanager.service.TtrPhaseResolver;
import com.ordermgmt.railway.ui.component.StatusBadge;
import com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils;

/** Interactive TTT process simulation: state badge, action buttons, history grid. */
public class ProcessSimulationPanel extends VerticalLayout {

    private static final DateTimeFormatter TS_FMT =
            DateTimeFormatter.ofPattern("dd.MM.yy HH:mm:ss").withZone(ZoneId.systemDefault());

    private static final Set<String> IM_ACTION_PREFIXES =
            Set.of(
                    "IM_RECEIPT",
                    "IM_DRAFT",
                    "IM_FINAL",
                    "IM_NO",
                    "IM_ERROR",
                    "IM_BOOK",
                    "IM_ANNOUNCE",
                    "IM_ALTERATION");

    private final PmReferenceTrain train;
    private final PathProcessEngine processEngine;
    private final PmProcessStepRepository processStepRepository;
    private final TtrPhaseResolver ttrPhaseResolver;
    private final BiFunction<String, Object[], String> translator;
    private final Consumer<PmReferenceTrain> onTransitionExecuted;

    private Div stateContainer;
    private Div phaseInfoContainer;
    private Div actionsContainer;
    private Grid<PmProcessStep> historyGrid;
    private TextField commentField;

    public ProcessSimulationPanel(
            PmReferenceTrain train,
            PathProcessEngine processEngine,
            PmProcessStepRepository processStepRepository,
            TtrPhaseResolver ttrPhaseResolver,
            BiFunction<String, Object[], String> translator,
            Consumer<PmReferenceTrain> onTransitionExecuted) {
        this.train = train;
        this.processEngine = processEngine;
        this.processStepRepository = processStepRepository;
        this.ttrPhaseResolver = ttrPhaseResolver;
        this.translator = translator;
        this.onTransitionExecuted = onTransitionExecuted;
        setPadding(false);
        setSpacing(false);
        buildPanel();
    }

    private String t(String key) {
        return translator.apply(key, null);
    }

    private void buildPanel() {
        Div card = TimetableFormatUtils.createCard();

        Span title = new Span(t("pm.process.title"));
        title.getStyle()
                .set("font-size", "var(--lumo-font-size-l)")
                .set("font-weight", "600")
                .set("color", "var(--rom-text-primary)")
                .set("margin-bottom", "var(--lumo-space-s)");
        card.add(title);

        stateContainer = new Div();
        stateContainer.getStyle().set("margin-bottom", "var(--lumo-space-s)");
        card.add(stateContainer);

        phaseInfoContainer = new Div();
        phaseInfoContainer.getStyle().set("margin-bottom", "var(--lumo-space-s)");
        card.add(phaseInfoContainer);
        refreshPhaseInfo();

        commentField = new TextField(t("pm.process.comment"));
        commentField.setWidthFull();
        commentField.setClearButtonVisible(true);
        commentField.getStyle().set("margin-bottom", "var(--lumo-space-s)");
        card.add(commentField);

        actionsContainer = new Div();
        actionsContainer
                .getStyle()
                .set("display", "flex")
                .set("flex-wrap", "wrap")
                .set("gap", "var(--lumo-space-xs)")
                .set("margin-bottom", "var(--lumo-space-m)");
        card.add(actionsContainer);

        card.add(createHistorySection());
        add(card);

        refreshState();
        refreshActions();
        refreshHistory();
    }

    private void refreshState() {
        stateContainer.removeAll();
        PathProcessState state = train.getProcessState();
        HorizontalLayout stateRow = new HorizontalLayout();
        stateRow.setAlignItems(FlexComponent.Alignment.CENTER);
        stateRow.setSpacing(true);

        Span label = new Span(t("pm.process.current") + ":");
        label.getStyle()
                .set("font-weight", "600")
                .set("color", "var(--rom-text-secondary)")
                .set("font-size", "13px");

        StatusBadge badge = createStateBadge(state);
        stateRow.add(label, badge);
        stateContainer.add(stateRow);
    }

    private void refreshPhaseInfo() {
        phaseInfoContainer.removeAll();
        PmTimetableYear year = train.getTimetableYear();
        if (year == null || year.getStartDate() == null) {
            return;
        }
        java.time.LocalDate today = java.time.LocalDate.now();
        TtrPhase phase = ttrPhaseResolver.resolvePhase(year, today);
        PathProcessType processType = ttrPhaseResolver.resolveProcessType(year, today);

        // Show phase info when state is NEW (before SEND_REQUEST)
        if (train.getProcessState() == PathProcessState.NEW && processType != null) {
            Div info = new Div();
            info.getStyle()
                    .set("padding", "8px 12px")
                    .set("border-radius", "6px")
                    .set("font-size", "12px")
                    .set("border", "1px solid rgba(99, 102, 241, 0.3)")
                    .set("background", "rgba(99, 102, 241, 0.08)")
                    .set("color", "var(--rom-text-primary)");

            String phaseLabel = t("ttr.phase." + phase.name());
            String text =
                    phaseLabel
                            + " — ProcessType "
                            + processType.code()
                            + " ("
                            + processType.name()
                            + ")";
            info.setText(text);

            if (!ttrPhaseResolver.isDraftOfferAllowed(year, today)) {
                Div noDraft = new Div();
                noDraft.getStyle()
                        .set("font-size", "11px")
                        .set("color", "rgb(234, 88, 12)")
                        .set("margin-top", "4px");
                noDraft.setText(t("ttr.phase.noDraft"));
                info.add(noDraft);
            }

            phaseInfoContainer.add(info);
        }

        // Always show compact phase badge
        HorizontalLayout badgeRow = new HorizontalLayout();
        badgeRow.setAlignItems(FlexComponent.Alignment.CENTER);
        badgeRow.setSpacing(true);

        Span phaseLabel = new Span(t("ttr.phase.info").replace("{0}", ""));
        phaseLabel.getStyle().set("font-size", "11px").set("color", "var(--rom-text-muted)");

        Span badge = new Span(t("ttr.phase." + phase.name()));
        badge.getStyle()
                .set("padding", "2px 8px")
                .set("border-radius", "8px")
                .set("font-size", "11px")
                .set("font-weight", "600");

        switch (phase) {
            case ANNUAL_ORDERING ->
                    badge.getStyle()
                            .set("background", "rgba(34, 197, 94, 0.15)")
                            .set("color", "rgb(34, 197, 94)");
            case LATE_ORDERING ->
                    badge.getStyle()
                            .set("background", "rgba(234, 179, 8, 0.15)")
                            .set("color", "rgb(202, 138, 4)");
            case AD_HOC_ORDERING ->
                    badge.getStyle()
                            .set("background", "rgba(249, 115, 22, 0.15)")
                            .set("color", "rgb(234, 88, 12)");
            case PAST ->
                    badge.getStyle()
                            .set("background", "rgba(156, 163, 175, 0.15)")
                            .set("color", "rgb(107, 114, 128)");
            default ->
                    badge.getStyle()
                            .set("background", "rgba(99, 102, 241, 0.15)")
                            .set("color", "rgb(79, 70, 229)");
        }

        badgeRow.add(phaseLabel, badge);
        phaseInfoContainer.add(badgeRow);
    }

    private void refreshActions() {
        actionsContainer.removeAll();
        Set<PathAction> available = processEngine.getAvailableActions(train.getId());

        for (PathAction action : PathAction.values()) {
            if (!available.contains(action)) {
                continue;
            }
            Button btn = new Button(t("pm.actions." + action.name()));
            btn.addThemeVariants(ButtonVariant.LUMO_SMALL);

            if (isImAction(action)) {
                applyImStyle(btn);
            } else {
                applyRaStyle(btn);
            }

            btn.addClickListener(e -> executeAction(action));
            actionsContainer.add(btn);
        }
    }

    private void refreshHistory() {
        List<PmProcessStep> steps =
                processStepRepository.findByReferenceTrainIdOrderByCreatedAtDesc(train.getId());
        historyGrid.setItems(steps);
    }

    private Div createHistorySection() {
        Div section = new Div();
        section.setWidthFull();

        Span historyTitle = new Span(t("pm.process.history"));
        historyTitle
                .getStyle()
                .set("font-weight", "600")
                .set("color", "var(--rom-text-secondary)")
                .set("font-size", "12px")
                .set("text-transform", "uppercase")
                .set("letter-spacing", "0.05em")
                .set("display", "block")
                .set("margin-bottom", "var(--lumo-space-xs)");
        section.add(historyTitle);

        historyGrid = new Grid<>();
        historyGrid.setWidthFull();
        historyGrid.setHeight("250px");
        historyGrid.setAllRowsVisible(false);

        historyGrid
                .addColumn(
                        step ->
                                step.getCreatedAt() != null
                                        ? TS_FMT.format(step.getCreatedAt())
                                        : "")
                .setHeader("Time")
                .setWidth("130px")
                .setFlexGrow(0);

        historyGrid
                .addColumn(PmProcessStep::getStepType)
                .setHeader("Action")
                .setWidth("160px")
                .setFlexGrow(0);

        historyGrid
                .addColumn(PmProcessStep::getFromState)
                .setHeader("From")
                .setWidth("120px")
                .setFlexGrow(0);

        historyGrid
                .addColumn(PmProcessStep::getToState)
                .setHeader("To")
                .setWidth("120px")
                .setFlexGrow(0);

        historyGrid
                .addColumn(step -> step.getComment() != null ? step.getComment() : "")
                .setHeader(t("pm.process.comment"))
                .setFlexGrow(1);

        section.add(historyGrid);
        return section;
    }

    private void executeAction(PathAction action) {
        try {
            String comment = commentField.getValue();
            ProcessStepResult result =
                    processEngine.executeTransition(train.getId(), action, comment);
            train.setProcessState(result.newState());
            commentField.clear();

            refreshState();
            refreshActions();
            refreshHistory();

            if (onTransitionExecuted != null) {
                onTransitionExecuted.accept(train);
            }
        } catch (IllegalStateException ex) {
            Notification.show(ex.getMessage(), 4000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private boolean isImAction(PathAction action) {
        String name = action.name();
        for (String prefix : IM_ACTION_PREFIXES) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private void applyRaStyle(Button btn) {
        btn.getStyle()
                .set("color", "rgb(45, 212, 191)")
                .set("border", "1px solid rgba(45, 212, 191, 0.3)")
                .set("background", "rgba(45, 212, 191, 0.08)");
    }

    private void applyImStyle(Button btn) {
        btn.getStyle()
                .set("color", "rgb(245, 158, 11)")
                .set("border", "1px solid rgba(245, 158, 11, 0.3)")
                .set("background", "rgba(245, 158, 11, 0.08)");
    }

    private StatusBadge createStateBadge(PathProcessState state) {
        if (state == null) {
            return new StatusBadge("--", StatusBadge.StatusType.NEUTRAL);
        }
        String label = state.name();
        return switch (state) {
            case NEW -> new StatusBadge(label, StatusBadge.StatusType.NEUTRAL);
            case CREATED, MODIFIED -> new StatusBadge(label, StatusBadge.StatusType.INFO);
            case RECEIPT_CONFIRMED -> new StatusBadge(label, StatusBadge.StatusType.INFO);
            case DRAFT_OFFERED, FINAL_OFFERED ->
                    new StatusBadge(label, StatusBadge.StatusType.WARNING);
            case BOOKED -> new StatusBadge(label, StatusBadge.StatusType.SUCCESS);
            case WITHDRAWN, CANCELED -> new StatusBadge(label, StatusBadge.StatusType.DANGER);
            case NO_ALTERNATIVE -> new StatusBadge(label, StatusBadge.StatusType.DANGER);
            case REVISION_REQUESTED -> new StatusBadge(label, StatusBadge.StatusType.WARNING);
            case MODIFICATION_REQUESTED -> new StatusBadge(label, StatusBadge.StatusType.WARNING);
            case ALTERATION_ANNOUNCED, ALTERATION_OFFERED ->
                    new StatusBadge(label, StatusBadge.StatusType.WARNING);
            case SUPERSEDED -> new StatusBadge(label, StatusBadge.StatusType.NEUTRAL);
        };
    }
}
