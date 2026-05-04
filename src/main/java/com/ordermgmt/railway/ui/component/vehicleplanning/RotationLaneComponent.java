package com.ordermgmt.railway.ui.component.vehicleplanning;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import com.ordermgmt.railway.domain.vehicleplanning.model.CouplingPosition;
import com.ordermgmt.railway.domain.vehicleplanning.model.TrainDisplayInfo;
import com.ordermgmt.railway.domain.vehicleplanning.model.VpRotationEntry;
import com.ordermgmt.railway.domain.vehicleplanning.model.VpVehicle;

/**
 * Renders a single vehicle's rotation as a horizontal chain of train cards connected by Von/Für
 * arrows. Coupled trains (shared with another vehicle) are highlighted in orange.
 */
public class RotationLaneComponent extends Div {

    private Runnable addTrainHandler;
    private Consumer<UUID> removeEntryHandler;
    private Runnable deleteVehicleHandler;

    public RotationLaneComponent(
            VpVehicle vehicle,
            Map<UUID, TrainDisplayInfo> trainInfoMap,
            Set<UUID> coupledTrainIds) {

        getStyle()
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("padding", "var(--lumo-space-m)")
                .set("margin-bottom", "var(--lumo-space-s)")
                .set("background", "var(--lumo-base-color)");

        // --- Header ---
        Span title = new Span(vehicle.getLabel());
        title.getStyle()
                .set("font-weight", "bold")
                .set("font-size", "var(--lumo-font-size-l)");

        Span typeBadge = createBadge(couplingLabel(vehicle), "var(--lumo-contrast-10pct)");

        Button addBtn = new Button(VaadinIcon.PLUS.create());
        addBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        addBtn.setTooltipText("Zug hinzufügen");
        addBtn.addClickListener(e -> {
            if (addTrainHandler != null) addTrainHandler.run();
        });

        Button deleteBtn = new Button(VaadinIcon.TRASH.create());
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY,
                ButtonVariant.LUMO_ERROR);
        deleteBtn.setTooltipText("Fahrzeug entfernen");
        deleteBtn.addClickListener(e -> {
            if (deleteVehicleHandler != null) deleteVehicleHandler.run();
        });

        HorizontalLayout header = new HorizontalLayout(title, typeBadge, addBtn, deleteBtn);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.getStyle().set("margin-bottom", "var(--lumo-space-s)");
        add(header);

        // --- Train chain ---
        HorizontalLayout chain = new HorizontalLayout();
        chain.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        chain.getStyle().set("flex-wrap", "wrap").set("gap", "var(--lumo-space-xs)");
        chain.setPadding(false);

        List<VpRotationEntry> entries =
                vehicle.getEntries().stream()
                        .sorted(Comparator.comparingInt(VpRotationEntry::getSequenceInDay))
                        .toList();

        for (int i = 0; i < entries.size(); i++) {
            VpRotationEntry entry = entries.get(i);
            TrainDisplayInfo info = trainInfoMap.get(entry.getReferenceTrain().getId());
            if (info == null) continue;

            boolean coupled = coupledTrainIds.contains(entry.getReferenceTrain().getId());
            chain.add(createTrainCard(entry, info, coupled));

            if (i < entries.size() - 1) {
                chain.add(createVonFuerArrow());
            }
        }

        if (entries.isEmpty()) {
            Span empty = new Span("Keine Züge — klicke + um Züge hinzuzufügen");
            empty.getStyle().set("color", "var(--lumo-secondary-text-color)")
                    .set("font-style", "italic");
            chain.add(empty);
        }

        add(chain);
    }

    // --- Card rendering ---

    private Div createTrainCard(VpRotationEntry entry, TrainDisplayInfo info, boolean coupled) {
        Div card = new Div();
        String borderColor = coupled ? "var(--lumo-warning-color, #FF9800)" : "var(--lumo-primary-color)";
        card.getStyle()
                .set("border", "2px solid " + borderColor)
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "var(--lumo-space-s)")
                .set("min-width", "140px")
                .set("background", "var(--lumo-base-color)")
                .set("color", "var(--lumo-body-text-color)")
                .set("position", "relative");

        // OTN header
        Div otn = new Div();
        otn.setText(info.otn());
        otn.getStyle().set("font-weight", "bold").set("padding-right", "var(--lumo-space-l)");

        // Route: Von → Nach (or partial route)
        Div route = new Div();
        route.setText(info.route());
        route.getStyle().set("font-size", "var(--lumo-font-size-s)");

        // Partial route indicators (join/leave at specific location)
        boolean hasPartial = entry.getJoinAtLocation() != null
                || entry.getLeaveAtLocation() != null;
        if (hasPartial) {
            Div partial = new Div();
            StringBuilder pb = new StringBuilder();
            if (entry.getJoinAtLocation() != null) {
                pb.append("ab ").append(entry.getJoinAtLocation());
            }
            if (entry.getLeaveAtLocation() != null) {
                if (!pb.isEmpty()) pb.append(" | ");
                pb.append("bis ").append(entry.getLeaveAtLocation());
            }
            partial.setText(pb.toString());
            partial.getStyle()
                    .set("font-size", "var(--lumo-font-size-xxs)")
                    .set("color", "var(--lumo-primary-color)")
                    .set("font-weight", "bold");
            card.add(partial);
        }

        // Time range
        Div time = new Div();
        time.setText(info.timeRange());
        time.getStyle()
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("color", "var(--lumo-secondary-text-color)");

        // Coupling badge
        String posLabel = positionLabel(entry.getCouplingType());
        Span badge = createBadge(posLabel, "var(--lumo-contrast-10pct)");

        // Remove button
        Button removeBtn = new Button("×");
        removeBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY_INLINE);
        removeBtn.getStyle()
                .set("position", "absolute")
                .set("top", "2px")
                .set("right", "2px")
                .set("min-width", "24px")
                .set("height", "24px")
                .set("padding", "0");
        removeBtn.addClickListener(e -> {
            if (removeEntryHandler != null) removeEntryHandler.accept(entry.getId());
        });

        card.add(otn, route, time, badge, removeBtn);
        return card;
    }

    private Div createVonFuerArrow() {
        Div arrow = new Div();
        arrow.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("align-items", "center")
                .set("padding", "0 var(--lumo-space-xs)")
                .set("color", "var(--lumo-secondary-text-color)");

        Span arrowIcon = new Span("\u2192");
        arrowIcon.getStyle().set("font-size", "var(--lumo-font-size-xl)");

        Span codes = new Span("0045/0044");
        codes.getStyle().set("font-size", "var(--lumo-font-size-xxs)");

        arrow.add(arrowIcon, codes);
        return arrow;
    }

    // --- Helpers ---

    private Span createBadge(String text, String bgColor) {
        Span badge = new Span(text);
        badge.getStyle()
                .set("background", bgColor)
                .set("border-radius", "var(--lumo-border-radius-s)")
                .set("padding", "1px var(--lumo-space-xs)")
                .set("font-size", "var(--lumo-font-size-xxs)");
        return badge;
    }

    private String couplingLabel(VpVehicle vehicle) {
        if (vehicle.getVehicleType() == null) return "";
        return switch (vehicle.getVehicleType()) {
            case MULTIPLE_UNIT -> "Triebzug";
            case LOCOMOTIVE -> "Lokomotive";
            case COACH_SET -> "Wagengarnitur";
        };
    }

    private String positionLabel(CouplingPosition pos) {
        if (pos == null) return "Voll";
        return switch (pos) {
            case FULL -> "Voll";
            case FRONT -> "Vorne";
            case REAR -> "Hinten";
        };
    }

    public void setAddTrainHandler(Runnable handler) {
        this.addTrainHandler = handler;
    }

    public void setRemoveEntryHandler(Consumer<UUID> handler) {
        this.removeEntryHandler = handler;
    }

    public void setDeleteVehicleHandler(Runnable handler) {
        this.deleteVehicleHandler = handler;
    }
}
