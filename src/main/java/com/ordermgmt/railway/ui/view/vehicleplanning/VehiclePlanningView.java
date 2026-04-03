package com.ordermgmt.railway.ui.view.vehicleplanning;

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
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import com.ordermgmt.railway.domain.pathmanager.model.PmReferenceTrain;
import com.ordermgmt.railway.domain.pathmanager.model.PmTimetableYear;
import com.ordermgmt.railway.domain.pathmanager.repository.PmTimetableYearRepository;
import com.ordermgmt.railway.domain.vehicleplanning.model.Conflict;
import com.ordermgmt.railway.domain.vehicleplanning.model.VpRotationSet;
import com.ordermgmt.railway.domain.vehicleplanning.model.VpVehicle;
import com.ordermgmt.railway.domain.vehicleplanning.service.ConflictDetectionService;
import com.ordermgmt.railway.domain.vehicleplanning.service.VehiclePlanningService;
import com.ordermgmt.railway.ui.component.vehicleplanning.ConflictPanel;
import com.ordermgmt.railway.ui.component.vehicleplanning.GanttChart;
import com.ordermgmt.railway.ui.component.vehicleplanning.TrainPalette;
import com.ordermgmt.railway.ui.layout.MainLayout;

/** Vehicle rotation planning view with Gantt chart, train palette, and conflict detection. */
@Route(value = "vehicleplanning", layout = MainLayout.class)
@PageTitle("Vehicle Planning")
@jakarta.annotation.security.PermitAll
public class VehiclePlanningView extends VerticalLayout {

    private final VehiclePlanningService planningService;
    private final ConflictDetectionService conflictService;
    private final PmTimetableYearRepository timetableYearRepo;

    private final ComboBox<VpRotationSet> rotationSelect;
    private final Select<Integer> daySelect;
    private final TrainPalette trainPalette;
    private final GanttChart ganttChart;
    private final ConflictPanel conflictPanel;

    private PmTimetableYear currentYear;

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

        // Header
        H2 title = new H2(getTranslation("vp.title"));
        title.getStyle().set("margin", "0");

        rotationSelect = new ComboBox<>(getTranslation("vp.rotationSet"));
        rotationSelect.setItemLabelGenerator(VpRotationSet::getName);
        rotationSelect.setWidth("250px");
        rotationSelect.addValueChangeListener(e -> onRotationSelected(e.getValue()));

        daySelect = new Select<>();
        daySelect.setLabel(getTranslation("vp.dayOfWeek"));
        daySelect.setItems(1, 2, 3, 4, 5, 6, 7);
        daySelect.setItemLabelGenerator(this::dayLabel);
        daySelect.setValue(1);
        daySelect.addValueChangeListener(e -> refreshGantt());

        Button newRotationBtn =
                new Button(getTranslation("vp.newRotation"), VaadinIcon.PLUS.create());
        newRotationBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        newRotationBtn.addClickListener(e -> openNewRotationDialog());

        Button writeLinkBtn =
                new Button(getTranslation("vp.writeLinks"), VaadinIcon.CONNECT.create());
        writeLinkBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        writeLinkBtn.setTooltipText(getTranslation("vp.writeLinks.help"));
        writeLinkBtn.addClickListener(
                e -> {
                    VpRotationSet selected = rotationSelect.getValue();
                    if (selected != null) {
                        int count =
                                planningService.writeVehicleLinksToPathManager(selected.getId());
                        Notification.show(
                                getTranslation("vp.writeLinks.success") + " (" + count + ")",
                                3000,
                                Notification.Position.BOTTOM_END);
                    }
                });

        HorizontalLayout header =
                new HorizontalLayout(
                        title, rotationSelect, daySelect, newRotationBtn, writeLinkBtn);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.BASELINE);
        header.setWidthFull();
        header.getStyle()
                .set("padding", "var(--lumo-space-s) var(--lumo-space-m)")
                .set("border-bottom", "1px solid var(--lumo-contrast-10pct)");

        // Main area: split palette / gantt
        trainPalette = new TrainPalette();
        ganttChart = new GanttChart();
        ganttChart.setDropHandler(this::handleEntryDrop);

        SplitLayout splitLayout = new SplitLayout(trainPalette, ganttChart);
        splitLayout.setSplitterPosition(20);
        splitLayout.setSizeFull();

        // Conflict panel
        conflictPanel = new ConflictPanel();

        Div mainContent = new Div(splitLayout);
        mainContent.getStyle().set("flex-grow", "1").set("overflow", "hidden");
        mainContent.setSizeFull();

        add(header, mainContent, conflictPanel);
        expand(mainContent);

        loadTimetableYear();
    }

    private void loadTimetableYear() {
        List<PmTimetableYear> years = timetableYearRepo.findAll();
        if (!years.isEmpty()) {
            currentYear = years.getFirst();
            loadRotationSets();
            loadTrains();
        }
    }

    private void loadRotationSets() {
        if (currentYear == null) return;
        List<VpRotationSet> sets = planningService.getRotationSets(currentYear.getId());
        rotationSelect.setItems(sets);
        if (!sets.isEmpty()) {
            rotationSelect.setValue(sets.getFirst());
        }
    }

    private void loadTrains() {
        if (currentYear == null) return;
        List<PmReferenceTrain> trains = planningService.getAvailableTrains(currentYear.getYear());
        trainPalette.setTrains(trains);
    }

    private void onRotationSelected(VpRotationSet rs) {
        if (rs == null) return;
        refreshGantt();
        refreshConflicts();
    }

    private void refreshGantt() {
        VpRotationSet rs = rotationSelect.getValue();
        Integer day = daySelect.getValue();
        if (rs == null || day == null) return;

        List<VpVehicle> vehicles = planningService.getVehicles(rs.getId());
        ganttChart.refresh(vehicles, day);
    }

    private void refreshConflicts() {
        VpRotationSet rs = rotationSelect.getValue();
        if (rs == null) return;

        List<Conflict> conflicts = conflictService.detectConflicts(rs.getId());
        conflictPanel.setConflicts(conflicts);
    }

    private void handleEntryDrop(UUID entryId, UUID targetVehicleId, int dayOfWeek) {
        try {
            planningService.moveEntry(entryId, targetVehicleId, dayOfWeek);
            refreshGantt();
            refreshConflicts();
        } catch (Exception ex) {
            Notification.show(
                    getTranslation("vp.error.move") + ": " + ex.getMessage(),
                    3000,
                    Notification.Position.MIDDLE);
        }
    }

    private void openNewRotationDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(getTranslation("vp.newRotation"));

        TextField nameField = new TextField(getTranslation("vp.rotation.name"));
        nameField.setWidthFull();
        nameField.setRequired(true);

        TextField descField = new TextField(getTranslation("vp.rotation.description"));
        descField.setWidthFull();

        VerticalLayout content = new VerticalLayout(nameField, descField);
        content.setPadding(false);
        dialog.add(content);

        Button saveBtn = new Button(getTranslation("common.save"));
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveBtn.addClickListener(
                e -> {
                    String name = nameField.getValue();
                    if (name == null || name.isBlank()) {
                        nameField.setInvalid(true);
                        return;
                    }
                    if (currentYear == null) return;

                    planningService.createRotationSet(
                            name, descField.getValue(), currentYear.getYear());
                    dialog.close();
                    loadRotationSets();
                    Notification.show(
                            getTranslation("vp.rotation.created"),
                            2000,
                            Notification.Position.BOTTOM_START);
                });

        Button cancelBtn = new Button(getTranslation("common.cancel"), e -> dialog.close());

        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();
    }

    private String dayLabel(Integer dow) {
        return switch (dow) {
            case 1 -> getTranslation("vp.day.mon");
            case 2 -> getTranslation("vp.day.tue");
            case 3 -> getTranslation("vp.day.wed");
            case 4 -> getTranslation("vp.day.thu");
            case 5 -> getTranslation("vp.day.fri");
            case 6 -> getTranslation("vp.day.sat");
            case 7 -> getTranslation("vp.day.sun");
            default -> String.valueOf(dow);
        };
    }
}
