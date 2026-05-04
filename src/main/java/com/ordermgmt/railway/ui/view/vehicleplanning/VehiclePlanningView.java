package com.ordermgmt.railway.ui.view.vehicleplanning;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import com.ordermgmt.railway.domain.pathmanager.model.PmTimetableYear;
import com.ordermgmt.railway.domain.pathmanager.repository.PmTimetableYearRepository;
import com.ordermgmt.railway.domain.rollingstock.model.RollingStockItem;
import com.ordermgmt.railway.domain.rollingstock.service.RollingStockService;
import com.ordermgmt.railway.domain.vehicleplanning.model.CouplingPosition;
import com.ordermgmt.railway.domain.vehicleplanning.model.TrainDisplayInfo;
import com.ordermgmt.railway.domain.vehicleplanning.model.VehicleType;
import com.ordermgmt.railway.domain.vehicleplanning.model.VpRotationEntry;
import com.ordermgmt.railway.domain.vehicleplanning.model.VpRotationSet;
import com.ordermgmt.railway.domain.vehicleplanning.model.VpVehicle;
import com.ordermgmt.railway.domain.vehicleplanning.service.VehiclePlanningService;
import com.ordermgmt.railway.ui.component.vehicleplanning.RotationLaneComponent;
import com.ordermgmt.railway.ui.layout.MainLayout;

/**
 * Simplified vehicle rotation planning view. Shows each vehicle as a horizontal train chain with
 * Von/Für (0044/0045) arrows. Supports coupling/decoupling visualization.
 */
@Route(value = "vehicleplanning", layout = MainLayout.class)
@PageTitle("Vehicle Planning")
@jakarta.annotation.security.RolesAllowed({"ADMIN", "DISPATCHER"})
public class VehiclePlanningView extends VerticalLayout {

    private final VehiclePlanningService planningService;
    private final RollingStockService rollingStockService;
    private final PmTimetableYearRepository timetableYearRepo;

    private final ComboBox<PmTimetableYear> fpjSelect;
    private final ComboBox<VpRotationSet> rotationSelect;
    private final VerticalLayout lanesContainer;
    private final Div couplingSummary;

    // Cached data for the current view
    private Map<UUID, TrainDisplayInfo> displayInfoMap = Map.of();

    public VehiclePlanningView(
            VehiclePlanningService planningService,
            RollingStockService rollingStockService,
            PmTimetableYearRepository timetableYearRepo) {
        this.planningService = planningService;
        this.rollingStockService = rollingStockService;
        this.timetableYearRepo = timetableYearRepo;

        setSizeFull();
        setPadding(false);
        setSpacing(false);

        // --- Header ---
        H2 title = new H2(getTranslation("vp.title"));
        title.getStyle().set("margin", "0");

        fpjSelect = buildFpjSelect();
        rotationSelect = buildRotationSelect();

        Button newSetBtn = new Button(VaadinIcon.PLUS_CIRCLE_O.create());
        newSetBtn.setTooltipText(getTranslation("vp.newRotationSet"));
        newSetBtn.addClickListener(e -> openNewRotationSetDialog());

        Button addVehicleBtn =
                new Button(getTranslation("vp.addDuty"), VaadinIcon.PLUS.create());
        addVehicleBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addVehicleBtn.addClickListener(e -> openAddVehicleDialog());

        Button writeLinksBtn =
                new Button(getTranslation("vp.writeLinks"), VaadinIcon.CONNECT.create());
        writeLinksBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        writeLinksBtn.setTooltipText(getTranslation("vp.writeLinks.help"));
        writeLinksBtn.addClickListener(e -> handleWriteLinks());

        HorizontalLayout header = new HorizontalLayout(
                title, fpjSelect, rotationSelect, newSetBtn, addVehicleBtn, writeLinksBtn);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.BASELINE);
        header.setWidthFull();
        header.getStyle()
                .set("padding", "var(--lumo-space-s) var(--lumo-space-m)")
                .set("border-bottom", "1px solid var(--lumo-contrast-10pct)")
                .set("flex-wrap", "wrap")
                .set("gap", "var(--lumo-space-s)");

        // --- Vehicle lanes ---
        lanesContainer = new VerticalLayout();
        lanesContainer.setPadding(true);
        lanesContainer.setSpacing(false);

        // --- Coupling summary ---
        couplingSummary = new Div();
        couplingSummary.getStyle().set("padding", "0 var(--lumo-space-m) var(--lumo-space-m)");

        add(header, lanesContainer, couplingSummary);
        expand(lanesContainer);

        loadTimetableYears();
    }

    // --- Builders ---

    private ComboBox<PmTimetableYear> buildFpjSelect() {
        ComboBox<PmTimetableYear> combo = new ComboBox<>(getTranslation("vp.fpj"));
        combo.setItemLabelGenerator(y -> y.getLabel() != null
                ? y.getLabel() + " (" + y.getYear() + ")" : String.valueOf(y.getYear()));
        combo.setWidth("200px");
        combo.addValueChangeListener(e -> onFpjChanged(e.getValue()));
        return combo;
    }

    private ComboBox<VpRotationSet> buildRotationSelect() {
        ComboBox<VpRotationSet> combo = new ComboBox<>(getTranslation("vp.rotationSet"));
        combo.setItemLabelGenerator(VpRotationSet::getName);
        combo.setWidth("250px");
        combo.addValueChangeListener(e -> refreshLanes());
        return combo;
    }

    // --- Data loading ---

    private void loadTimetableYears() {
        List<PmTimetableYear> years = timetableYearRepo.findAll();
        fpjSelect.setItems(years);
        if (!years.isEmpty()) fpjSelect.setValue(years.getFirst());
    }

    private void onFpjChanged(PmTimetableYear year) {
        if (year == null) return;
        List<VpRotationSet> sets = planningService.getRotationSets(year.getId());
        rotationSelect.setItems(sets);
        if (!sets.isEmpty()) {
            rotationSelect.setValue(sets.getFirst());
        } else {
            rotationSelect.clear();
            lanesContainer.removeAll();
            couplingSummary.removeAll();
        }
    }

    private void refreshLanes() {
        lanesContainer.removeAll();
        couplingSummary.removeAll();

        VpRotationSet rs = rotationSelect.getValue();
        PmTimetableYear fpj = fpjSelect.getValue();
        if (rs == null || fpj == null) return;

        displayInfoMap = planningService.getDisplayInfoMap(fpj.getYear());
        List<VpVehicle> vehicles = planningService.getVehicles(rs.getId());
        Set<UUID> coupledTrainIds = findCoupledTrainIds(vehicles);

        for (VpVehicle vehicle : vehicles) {
            RotationLaneComponent lane =
                    new RotationLaneComponent(vehicle, displayInfoMap, coupledTrainIds);
            lane.setAddTrainHandler(() -> openAddTrainDialog(vehicle));
            lane.setRemoveEntryHandler(entryId -> {
                planningService.removeEntry(entryId);
                refreshLanes();
            });
            lane.setDeleteVehicleHandler(() -> {
                planningService.deleteVehicle(vehicle.getId());
                refreshLanes();
            });
            lanesContainer.add(lane);
        }

        if (vehicles.isEmpty()) {
            Span hint = new Span(getTranslation("vp.selectRotationSetFirst"));
            hint.getStyle().set("color", "var(--lumo-secondary-text-color)");
            lanesContainer.add(hint);
        }

        renderCouplingSummary(vehicles, coupledTrainIds);
    }

    // --- Coupling detection ---

    private Set<UUID> findCoupledTrainIds(List<VpVehicle> vehicles) {
        Map<UUID, Integer> trainCount = new HashMap<>();
        for (VpVehicle v : vehicles) {
            for (VpRotationEntry e : v.getEntries()) {
                trainCount.merge(e.getReferenceTrain().getId(), 1, Integer::sum);
            }
        }
        Set<UUID> coupled = new HashSet<>();
        trainCount.forEach((id, count) -> {
            if (count > 1) coupled.add(id);
        });
        return coupled;
    }

    private void renderCouplingSummary(List<VpVehicle> vehicles, Set<UUID> coupledTrainIds) {
        if (coupledTrainIds.isEmpty()) return;

        Div header = new Div();
        header.setText(getTranslation("vp.couplings"));
        header.getStyle()
                .set("font-weight", "bold")
                .set("margin-bottom", "var(--lumo-space-xs)")
                .set("margin-top", "var(--lumo-space-m)");
        couplingSummary.add(header);

        for (UUID trainId : coupledTrainIds) {
            TrainDisplayInfo info = displayInfoMap.get(trainId);
            if (info == null) continue;

            Div block = new Div();
            block.getStyle()
                    .set("border-left", "3px solid var(--lumo-warning-color, #FF9800)")
                    .set("padding", "var(--lumo-space-xs) var(--lumo-space-s)")
                    .set("margin-bottom", "var(--lumo-space-s)")
                    .set("background", "var(--lumo-contrast-5pct)")
                    .set("border-radius", "0 var(--lumo-border-radius-s) var(--lumo-border-radius-s) 0");

            // Title: shared train
            Div title = new Div();
            title.setText("Zug " + info.otn() + ": " + info.route()
                    + " (" + info.timeRange() + ")");
            title.getStyle().set("font-weight", "bold");
            block.add(title);

            // Composition: which vehicles ride this train
            List<String> composition = new ArrayList<>();
            for (VpVehicle v : vehicles) {
                for (VpRotationEntry e : v.getEntries()) {
                    if (e.getReferenceTrain().getId().equals(trainId)) {
                        composition.add(v.getLabel() + " (" + posLabel(e.getCouplingType()) + ")");
                    }
                }
            }
            Div compLine = new Div();
            compLine.setText("Zusammensetzung: " + String.join(" + ", composition));
            compLine.getStyle().set("font-size", "var(--lumo-font-size-s)");
            block.add(compLine);

            // Detect coupling event (Kuppeln): vehicles come from different trains
            detectCouplingEvent(vehicles, trainId, info, block);
            // Detect decoupling event (Entkuppeln): vehicles go to different trains
            detectDecouplingEvent(vehicles, trainId, info, block);

            couplingSummary.add(block);
        }
    }

    private void detectCouplingEvent(
            List<VpVehicle> vehicles, UUID sharedTrainId, TrainDisplayInfo sharedInfo, Div block) {
        List<String> origins = new ArrayList<>();
        for (VpVehicle v : vehicles) {
            var sorted = sortedEntries(v);
            for (int i = 0; i < sorted.size(); i++) {
                if (sorted.get(i).getReferenceTrain().getId().equals(sharedTrainId) && i > 0) {
                    TrainDisplayInfo prev = displayInfoMap.get(
                            sorted.get(i - 1).getReferenceTrain().getId());
                    if (prev != null) {
                        origins.add(v.getLabel() + " kommt von Zug " + prev.otn()
                                + " (" + prev.toLocation() + ")");
                    }
                }
            }
        }
        if (!origins.isEmpty()) {
            Div event = new Div();
            event.setText("\u2192 Kuppeln in " + sharedInfo.fromLocation()
                    + ": " + String.join(", ", origins));
            event.getStyle()
                    .set("font-size", "var(--lumo-font-size-s)")
                    .set("color", "var(--lumo-success-color)");
            block.add(event);
        }
    }

    private void detectDecouplingEvent(
            List<VpVehicle> vehicles, UUID sharedTrainId, TrainDisplayInfo sharedInfo, Div block) {
        List<String> destinations = new ArrayList<>();
        for (VpVehicle v : vehicles) {
            var sorted = sortedEntries(v);
            for (int i = 0; i < sorted.size(); i++) {
                if (sorted.get(i).getReferenceTrain().getId().equals(sharedTrainId)
                        && i < sorted.size() - 1) {
                    TrainDisplayInfo next = displayInfoMap.get(
                            sorted.get(i + 1).getReferenceTrain().getId());
                    if (next != null) {
                        destinations.add(v.getLabel() + " weiter als Zug " + next.otn()
                                + " (" + next.route() + ")");
                    }
                }
            }
        }
        if (destinations.size() > 1) {
            Div event = new Div();
            event.setText("\u2192 Entkuppeln in " + sharedInfo.toLocation()
                    + ": " + String.join(", ", destinations));
            event.getStyle()
                    .set("font-size", "var(--lumo-font-size-s)")
                    .set("color", "var(--lumo-error-color)");
            block.add(event);
        }
    }

    private List<VpRotationEntry> sortedEntries(VpVehicle vehicle) {
        return vehicle.getEntries().stream()
                .sorted(java.util.Comparator.comparingInt(VpRotationEntry::getSequenceInDay))
                .toList();
    }

    private String posLabel(CouplingPosition pos) {
        if (pos == null) return "Voll";
        return switch (pos) {
            case FULL -> "Vollst\u00e4ndig";
            case FRONT -> "Vorne";
            case REAR -> "Hinten";
        };
    }

    // --- Dialogs ---

    private void openAddTrainDialog(VpVehicle vehicle) {
        PmTimetableYear fpj = fpjSelect.getValue();
        VpRotationSet rs = rotationSelect.getValue();
        if (fpj == null || rs == null) return;

        List<TrainDisplayInfo> allTrains = new ArrayList<>(displayInfoMap.values());
        allTrains.sort(java.util.Comparator.comparing(TrainDisplayInfo::otn));

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Zug zu " + vehicle.getLabel() + " hinzuf\u00fcgen");
        dialog.setWidth("550px");

        ComboBox<TrainDisplayInfo> trainCombo = new ComboBox<>("Zug");
        trainCombo.setItems(allTrains);
        trainCombo.setItemLabelGenerator(TrainDisplayInfo::comboLabel);
        trainCombo.setWidthFull();

        RadioButtonGroup<CouplingPosition> couplingGroup = new RadioButtonGroup<>("Kupplung");
        couplingGroup.setItems(CouplingPosition.values());
        couplingGroup.setItemLabelGenerator(cp -> switch (cp) {
            case FULL -> "Vollst\u00e4ndig (einzeln)";
            case FRONT -> "Vorne (Fl\u00fcgel vorne)";
            case REAR -> "Hinten (Fl\u00fcgel hinten)";
        });
        couplingGroup.setValue(CouplingPosition.FULL);

        // Join/Leave location selection (optional, for partial routes)
        ComboBox<String> joinAt = new ComboBox<>("Einstieg ab Betriebspunkt");
        joinAt.setHelperText("Leer = ab Start des Zuges");
        joinAt.setWidthFull();
        joinAt.setClearButtonVisible(true);

        ComboBox<String> leaveAt = new ComboBox<>("Ausstieg bis Betriebspunkt");
        leaveAt.setHelperText("Leer = bis Ende des Zuges");
        leaveAt.setWidthFull();
        leaveAt.setClearButtonVisible(true);

        // Load locations when train is selected
        trainCombo.addValueChangeListener(e -> {
            TrainDisplayInfo sel = e.getValue();
            if (sel != null) {
                List<String> locations = planningService.getTrainLocationNames(sel.trainId());
                joinAt.setItems(locations);
                leaveAt.setItems(locations);
            } else {
                joinAt.setItems();
                leaveAt.setItems();
            }
        });

        VerticalLayout content = new VerticalLayout(
                trainCombo, couplingGroup, joinAt, leaveAt);
        content.setPadding(false);
        dialog.add(content);

        Button saveBtn = new Button(getTranslation("common.save"));
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveBtn.addClickListener(e -> {
            TrainDisplayInfo selected = trainCombo.getValue();
            if (selected == null) {
                trainCombo.setInvalid(true);
                return;
            }
            planningService.addTrainToVehicle(
                    vehicle.getId(), selected.trainId(), 1,
                    couplingGroup.getValue(),
                    joinAt.getValue(), leaveAt.getValue());
            dialog.close();
            refreshLanes();
        });
        Button cancelBtn = new Button(getTranslation("common.cancel"), e -> dialog.close());
        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();
    }

    private void openAddVehicleDialog() {
        VpRotationSet rs = rotationSelect.getValue();
        if (rs == null) {
            Notification.show(getTranslation("vp.selectRotationSetFirst"), 2000,
                    Notification.Position.MIDDLE);
            return;
        }

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(getTranslation("vp.addDuty"));

        // Rolling stock catalog selection
        ComboBox<RollingStockItem> rsCombo = new ComboBox<>(getTranslation("rs.title"));
        rsCombo.setItems(rollingStockService.findActive());
        rsCombo.setItemLabelGenerator(item -> item.getDesignation()
                + (item.getEvn() != null ? " [" + item.getEvn() + "]" : "")
                + " (" + getTranslation("rs.category." + item.getVehicleCategory().name()) + ")");
        rsCombo.setWidthFull();
        rsCombo.setRequired(true);

        TextField labelField = new TextField(getTranslation("vp.vehicle.label"));
        labelField.setWidthFull();
        labelField.setHelperText("Wird automatisch aus Rollmaterial gesetzt");

        // Auto-fill label from selected rolling stock
        rsCombo.addValueChangeListener(e -> {
            RollingStockItem item = e.getValue();
            if (item != null) {
                labelField.setValue(item.getDesignation());
            }
        });

        dialog.add(new VerticalLayout(rsCombo, labelField));

        Button saveBtn = new Button(getTranslation("common.save"));
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveBtn.addClickListener(e -> {
            RollingStockItem selectedRs = rsCombo.getValue();
            if (selectedRs == null) {
                rsCombo.setInvalid(true);
                return;
            }
            String label = labelField.getValue();
            if (label == null || label.isBlank()) {
                label = selectedRs.getDesignation();
            }
            VehicleType vt = mapCategory(selectedRs.getVehicleCategory());
            VpVehicle vehicle = planningService.addVehicle(rs.getId(), label, vt);
            vehicle.setRollingStock(selectedRs);
            planningService.saveVehicle(vehicle);
            dialog.close();
            refreshLanes();
        });
        Button cancelBtn = new Button(getTranslation("common.cancel"), e -> dialog.close());
        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();
    }

    private VehicleType mapCategory(
            com.ordermgmt.railway.domain.rollingstock.model.VehicleCategory cat) {
        return switch (cat) {
            case LOCOMOTIVE -> VehicleType.LOCOMOTIVE;
            case EMU, DMU, CONTROL_CAR -> VehicleType.MULTIPLE_UNIT;
            case COACH, FREIGHT_WAGON -> VehicleType.COACH_SET;
        };
    }

    private void openNewRotationSetDialog() {
        PmTimetableYear fpj = fpjSelect.getValue();
        if (fpj == null) {
            Notification.show(getTranslation("vp.selectFpjFirst"), 2000,
                    Notification.Position.MIDDLE);
            return;
        }

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(getTranslation("vp.newRotationSet"));

        TextField nameField = new TextField(getTranslation("vp.rotationSet.name"));
        nameField.setWidthFull();
        nameField.setRequired(true);

        TextField descField = new TextField(getTranslation("vp.rotationSet.description"));
        descField.setWidthFull();

        dialog.add(new VerticalLayout(nameField, descField));

        Button saveBtn = new Button(getTranslation("common.save"));
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveBtn.addClickListener(e -> {
            if (nameField.getValue() == null || nameField.getValue().isBlank()) {
                nameField.setInvalid(true);
                return;
            }
            planningService.createRotationSet(
                    nameField.getValue(), descField.getValue(), fpj.getYear());
            dialog.close();
            onFpjChanged(fpj);
            Notification.show(getTranslation("vp.rotationSet.created"), 2000,
                    Notification.Position.BOTTOM_START);
        });
        Button cancelBtn = new Button(getTranslation("common.cancel"), e -> dialog.close());
        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();
    }

    private void handleWriteLinks() {
        VpRotationSet rs = rotationSelect.getValue();
        if (rs == null) return;
        int count = planningService.writeVehicleLinksToPathManager(rs.getId());
        Notification.show(getTranslation("vp.writeLinks.success") + " (" + count + ")",
                3000, Notification.Position.BOTTOM_END);
    }
}
