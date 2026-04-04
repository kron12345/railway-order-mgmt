package com.ordermgmt.railway.ui.view.vehicleplanning;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import com.ordermgmt.railway.domain.pathmanager.model.PmReferenceTrain;
import com.ordermgmt.railway.domain.pathmanager.model.PmTimetableYear;
import com.ordermgmt.railway.domain.pathmanager.repository.PmTimetableYearRepository;
import com.ordermgmt.railway.domain.vehicleplanning.model.Conflict;
import com.ordermgmt.railway.domain.vehicleplanning.model.VehicleType;
import com.ordermgmt.railway.domain.vehicleplanning.model.VpRotationSet;
import com.ordermgmt.railway.domain.vehicleplanning.model.VpVehicle;
import com.ordermgmt.railway.domain.vehicleplanning.service.ConflictDetectionService;
import com.ordermgmt.railway.domain.vehicleplanning.service.VehiclePlanningService;
import com.ordermgmt.railway.ui.component.vehicleplanning.ConflictPanel;
import com.ordermgmt.railway.ui.component.vehicleplanning.GanttChart;
import com.ordermgmt.railway.ui.layout.MainLayout;

/**
 * Vehicle rotation planning view with a full-width Gantt chart (tltv Gantt Flow), FPJ filter, duty
 * management, and conflict detection.
 */
@Route(value = "vehicleplanning", layout = MainLayout.class)
@PageTitle("Vehicle Planning")
@jakarta.annotation.security.RolesAllowed({"ADMIN", "DISPATCHER"})
public class VehiclePlanningView extends VerticalLayout {

    private final VehiclePlanningService planningService;
    private final ConflictDetectionService conflictService;
    private final PmTimetableYearRepository timetableYearRepo;

    private final ComboBox<VpRotationSet> rotationSelect;
    private final ComboBox<PmTimetableYear> fpjSelect;
    private final GanttChart ganttChart;
    private final ConflictPanel conflictPanel;

    public VehiclePlanningView(
            VehiclePlanningService planningService,
            ConflictDetectionService conflictService,
            PmTimetableYearRepository timetableYearRepo) {
        this.planningService = planningService;
        this.conflictService = conflictService;
        this.timetableYearRepo = timetableYearRepo;

        setSizeFull();
        setPadding(false);
        setSpacing(false);

        // --- Header toolbar ---
        H2 title = new H2(getTranslation("vp.title"));
        title.getStyle().set("margin", "0");

        rotationSelect = buildRotationSelect();
        fpjSelect = buildFpjSelect();

        Button addDutyBtn = new Button(getTranslation("vp.addDuty"), VaadinIcon.PLUS.create());
        addDutyBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addDutyBtn.addClickListener(e -> openAddDutyDialog());

        Button writeLinkBtn =
                new Button(getTranslation("vp.writeLinks"), VaadinIcon.CONNECT.create());
        writeLinkBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        writeLinkBtn.setTooltipText(getTranslation("vp.writeLinks.help"));
        writeLinkBtn.addClickListener(e -> handleWriteLinks());

        Button refreshConflictsBtn =
                new Button(getTranslation("vp.conflicts"), VaadinIcon.REFRESH.create());
        refreshConflictsBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshConflictsBtn.addClickListener(e -> refreshConflicts());

        HorizontalLayout header =
                new HorizontalLayout(
                        title,
                        rotationSelect,
                        fpjSelect,
                        addDutyBtn,
                        writeLinkBtn,
                        refreshConflictsBtn);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.BASELINE);
        header.setWidthFull();
        header.getStyle()
                .set("padding", "var(--lumo-space-s) var(--lumo-space-m)")
                .set("border-bottom", "1px solid var(--lumo-contrast-10pct)");

        // --- Gantt chart (full width) ---
        ganttChart = new GanttChart();
        ganttChart.setMoveHandler(this::handleStepMoved);

        Div ganttWrapper = new Div(ganttChart);
        ganttWrapper.setSizeFull();
        ganttWrapper.getStyle().set("flex-grow", "1").set("overflow", "auto");

        // --- Conflict panel (bottom) ---
        conflictPanel = new ConflictPanel();

        add(header, ganttWrapper, conflictPanel);
        expand(ganttWrapper);

        // Initial load
        loadTimetableYears();
    }

    private ComboBox<VpRotationSet> buildRotationSelect() {
        ComboBox<VpRotationSet> combo = new ComboBox<>(getTranslation("vp.rotationSet"));
        combo.setItemLabelGenerator(VpRotationSet::getName);
        combo.setWidth("250px");
        combo.addValueChangeListener(e -> onRotationSelected(e.getValue()));
        return combo;
    }

    private ComboBox<PmTimetableYear> buildFpjSelect() {
        ComboBox<PmTimetableYear> combo = new ComboBox<>(getTranslation("vp.fpj"));
        combo.setItemLabelGenerator(this::fpjLabel);
        combo.setWidth("200px");
        combo.addValueChangeListener(e -> onFpjChanged(e.getValue()));
        return combo;
    }

    private String fpjLabel(PmTimetableYear year) {
        if (year == null) {
            return "";
        }
        return year.getLabel() != null
                ? year.getLabel() + " (" + year.getYear() + ")"
                : String.valueOf(year.getYear());
    }

    // --- Data loading ---

    private void loadTimetableYears() {
        List<PmTimetableYear> years = timetableYearRepo.findAll();
        fpjSelect.setItems(years);
        if (!years.isEmpty()) {
            fpjSelect.setValue(years.getFirst());
        }
    }

    private void onFpjChanged(PmTimetableYear year) {
        if (year == null) {
            return;
        }
        loadRotationSets(year);
    }

    private void loadRotationSets(PmTimetableYear year) {
        List<VpRotationSet> sets = planningService.getRotationSets(year.getId());
        rotationSelect.setItems(sets);
        if (!sets.isEmpty()) {
            rotationSelect.setValue(sets.getFirst());
        } else {
            rotationSelect.clear();
            ganttChart.clearAll();
        }
    }

    private void onRotationSelected(VpRotationSet rs) {
        if (rs == null) {
            ganttChart.clearAll();
            return;
        }
        refreshGantt();
        refreshConflicts();
    }

    private void refreshGantt() {
        VpRotationSet rs = rotationSelect.getValue();
        PmTimetableYear fpj = fpjSelect.getValue();
        if (rs == null || fpj == null) {
            return;
        }

        List<PmReferenceTrain> unassigned =
                planningService.getUnassignedTrains(rs.getId(), fpj.getYear());
        List<VpVehicle> vehicles = planningService.getVehicles(rs.getId());

        // Use the FPJ start date or today as the base for the Gantt timeline
        LocalDate baseDay = fpj.getStartDate() != null ? fpj.getStartDate() : LocalDate.now();
        LocalDateTime baseDate = baseDay.atTime(LocalTime.MIN);

        ganttChart.loadData(unassigned, vehicles, baseDate);
    }

    private void refreshConflicts() {
        VpRotationSet rs = rotationSelect.getValue();
        if (rs == null) {
            return;
        }
        List<Conflict> conflicts = conflictService.detectConflicts(rs.getId());
        conflictPanel.setConflicts(conflicts);
    }

    // --- DnD move handling ---

    private void handleStepMoved(String subStepUid, String newOwnerUid) {
        try {
            if (newOwnerUid.startsWith(GanttChart.DUTY_PREFIX)) {
                UUID targetVehicleId =
                        UUID.fromString(newOwnerUid.substring(GanttChart.DUTY_PREFIX.length()));
                handleMoveToVehicle(subStepUid, targetVehicleId);
            } else if (newOwnerUid.startsWith(GanttChart.SHELF_PREFIX)) {
                handleMoveToShelf(subStepUid);
            }
            refreshGantt();
            refreshConflicts();
        } catch (Exception ex) {
            Notification.show(
                    getTranslation("vp.error.move") + ": " + ex.getMessage(),
                    3000,
                    Notification.Position.MIDDLE);
            refreshGantt();
        }
    }

    private void handleMoveToVehicle(String subStepUid, UUID targetVehicleId) {
        if (subStepUid.startsWith(GanttChart.TRAIN_PREFIX)) {
            // Unassigned train dragged to a duty
            UUID trainId = UUID.fromString(subStepUid.substring(GanttChart.TRAIN_PREFIX.length()));
            planningService.addTrainToVehicle(
                    targetVehicleId,
                    trainId,
                    1,
                    com.ordermgmt.railway.domain.vehicleplanning.model.CouplingPosition.FULL);
        } else if (subStepUid.startsWith(GanttChart.ENTRY_PREFIX)) {
            // Existing entry moved to a different duty
            UUID entryId = UUID.fromString(subStepUid.substring(GanttChart.ENTRY_PREFIX.length()));
            planningService.moveEntry(entryId, targetVehicleId, 1);
        }
    }

    private void handleMoveToShelf(String subStepUid) {
        if (subStepUid.startsWith(GanttChart.ENTRY_PREFIX)) {
            // Entry moved back to shelf = remove assignment
            UUID entryId = UUID.fromString(subStepUid.substring(GanttChart.ENTRY_PREFIX.length()));
            planningService.removeEntry(entryId);
        }
        // train-to-shelf moves don't require action (already unassigned)
    }

    // --- Actions ---

    private void handleWriteLinks() {
        VpRotationSet selected = rotationSelect.getValue();
        if (selected != null) {
            int count = planningService.writeVehicleLinksToPathManager(selected.getId());
            Notification.show(
                    getTranslation("vp.writeLinks.success") + " (" + count + ")",
                    3000,
                    Notification.Position.BOTTOM_END);
        }
    }

    private void openAddDutyDialog() {
        VpRotationSet rs = rotationSelect.getValue();
        if (rs == null) {
            Notification.show(getTranslation("vp.rotationSet"), 2000, Notification.Position.MIDDLE);
            return;
        }

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(getTranslation("vp.addDuty"));

        TextField labelField = new TextField(getTranslation("vp.vehicle.label"));
        labelField.setWidthFull();
        labelField.setRequired(true);

        ComboBox<VehicleType> typeField = new ComboBox<>(getTranslation("vp.vehicle.type"));
        typeField.setItems(VehicleType.values());
        typeField.setItemLabelGenerator(vt -> getTranslation("vp.vehicle.type." + vt.name()));
        typeField.setValue(VehicleType.MULTIPLE_UNIT);
        typeField.setWidthFull();

        VerticalLayout content = new VerticalLayout(labelField, typeField);
        content.setPadding(false);
        dialog.add(content);

        Button saveBtn = new Button(getTranslation("common.save"));
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveBtn.addClickListener(
                e -> {
                    String label = labelField.getValue();
                    if (label == null || label.isBlank()) {
                        labelField.setInvalid(true);
                        return;
                    }
                    planningService.addVehicle(rs.getId(), label, typeField.getValue());
                    dialog.close();
                    refreshGantt();
                    Notification.show(
                            getTranslation("vp.duty.created"),
                            2000,
                            Notification.Position.BOTTOM_START);
                });

        Button cancelBtn = new Button(getTranslation("common.cancel"), e -> dialog.close());
        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();
    }
}
