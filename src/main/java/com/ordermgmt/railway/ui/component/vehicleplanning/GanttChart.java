package com.ordermgmt.railway.ui.component.vehicleplanning;

import java.util.List;
import java.util.UUID;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dnd.DragSource;
import com.vaadin.flow.component.dnd.DropTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

import com.ordermgmt.railway.domain.vehicleplanning.model.VpRotationEntry;
import com.ordermgmt.railway.domain.vehicleplanning.model.VpVehicle;

/**
 * Gantt-style chart showing vehicles as rows and time blocks for their assigned trains. Supports
 * drag-and-drop for moving train entries between vehicles/days.
 */
public class GanttChart extends Div {

    private static final int HOURS = 24;
    private static final int PIXELS_PER_HOUR = 60;
    private static final int ROW_HEIGHT = 48;
    private static final int RULER_HEIGHT = 32;

    /**
     * Placeholder width per block in pixels. Will be replaced with proper time-based positioning
     * once departure/arrival times are available from the timetable data.
     */
    private static final int BLOCK_WIDTH_PX = 100;

    /**
     * Placeholder horizontal offset between consecutive blocks in pixels. Will be replaced with
     * proper time-based positioning once departure/arrival times are available.
     */
    private static final int BLOCK_OFFSET_PX = 120;

    /** Prefix for drag data originating from existing rotation entries in the Gantt chart. */
    public static final String DRAG_PREFIX_ENTRY = "ENTRY:";

    private final Div rulerRow;
    private final Div vehicleRows;

    private DropHandler dropHandler;

    /** Callback for when a draggable item is dropped onto a vehicle row. */
    @FunctionalInterface
    public interface DropHandler {
        void onDrop(String dragPayload, UUID targetVehicleId, int dayOfWeek);
    }

    public GanttChart() {
        getStyle()
                .set("position", "relative")
                .set("overflow-x", "auto")
                .set("overflow-y", "auto")
                .set("background", "var(--lumo-base-color)")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "var(--lumo-border-radius-m)");

        rulerRow = new Div();
        rulerRow.getStyle()
                .set("display", "flex")
                .set("position", "sticky")
                .set("top", "0")
                .set("z-index", "10")
                .set("background", "var(--lumo-contrast-5pct)")
                .set("border-bottom", "1px solid var(--lumo-contrast-10pct)")
                .set("height", RULER_HEIGHT + "px")
                .set("min-width", (HOURS * PIXELS_PER_HOUR) + "px");
        buildRuler();

        vehicleRows = new Div();
        vehicleRows.getStyle().set("min-width", (HOURS * PIXELS_PER_HOUR) + "px");

        add(rulerRow, vehicleRows);
    }

    public void setDropHandler(DropHandler handler) {
        this.dropHandler = handler;
    }

    /** Rebuilds the chart for the given vehicles and selected day of week. */
    public void refresh(List<VpVehicle> vehicles, int dayOfWeek) {
        vehicleRows.removeAll();
        for (VpVehicle vehicle : vehicles) {
            vehicleRows.add(buildVehicleRow(vehicle, dayOfWeek));
        }
    }

    private void buildRuler() {
        for (int h = 0; h < HOURS; h++) {
            Span tick = new Span(String.format("%02d:00", h));
            tick.getStyle()
                    .set("width", PIXELS_PER_HOUR + "px")
                    .set("flex-shrink", "0")
                    .set("text-align", "center")
                    .set("font-size", "var(--lumo-font-size-xs)")
                    .set("color", "var(--lumo-secondary-text-color)")
                    .set("line-height", RULER_HEIGHT + "px")
                    .set("border-right", "1px solid var(--lumo-contrast-10pct)");
            rulerRow.add(tick);
        }
    }

    private Component buildVehicleRow(VpVehicle vehicle, int dayOfWeek) {
        Div row = new Div();
        row.getStyle()
                .set("position", "relative")
                .set("height", ROW_HEIGHT + "px")
                .set("border-bottom", "1px solid var(--lumo-contrast-10pct)")
                .set("min-width", (HOURS * PIXELS_PER_HOUR) + "px");

        // Make the row a drop target
        DropTarget<Div> dropTarget = DropTarget.create(row);
        dropTarget.setDropEffect(com.vaadin.flow.component.dnd.DropEffect.MOVE);
        dropTarget.addDropListener(
                event -> {
                    if (dropHandler == null) {
                        return;
                    }
                    event.getDragData()
                            .ifPresent(
                                    data -> {
                                        if (data instanceof String payload) {
                                            dropHandler.onDrop(payload, vehicle.getId(), dayOfWeek);
                                        }
                                    });
                });

        // Render train blocks for this day
        List<VpRotationEntry> entries =
                vehicle.getEntries().stream()
                        .filter(e -> e.getDayOfWeek() == dayOfWeek)
                        .sorted(
                                java.util.Comparator.comparingInt(
                                        VpRotationEntry::getSequenceInDay))
                        .toList();

        for (VpRotationEntry entry : entries) {
            row.add(buildTrainBlock(entry));
        }

        return row;
    }

    private Component buildTrainBlock(VpRotationEntry entry) {
        String otn = entry.getReferenceTrain().getOperationalTrainNumber();
        String label = otn != null ? otn : entry.getReferenceTrain().getTridCore();

        Div block = new Div();
        block.setText(label);
        block.getStyle()
                .set("position", "absolute")
                .set("top", "4px")
                .set("height", (ROW_HEIGHT - 8) + "px")
                .set("background", "var(--lumo-primary-color)")
                .set("color", "var(--lumo-primary-contrast-color)")
                .set("border-radius", "var(--lumo-border-radius-s)")
                .set("padding", "0 var(--lumo-space-xs)")
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("line-height", (ROW_HEIGHT - 8) + "px")
                .set("white-space", "nowrap")
                .set("overflow", "hidden")
                .set("text-overflow", "ellipsis")
                .set("cursor", "grab")
                .set("min-width", "40px");

        // Position based on sequence (placeholder until proper time-based positioning)
        int left = entry.getSequenceInDay() * BLOCK_OFFSET_PX;
        block.getStyle().set("left", left + "px").set("width", BLOCK_WIDTH_PX + "px");

        // Make draggable with typed prefix to distinguish from train palette drops
        DragSource<Div> dragSource = DragSource.create(block);
        dragSource.setDragData(DRAG_PREFIX_ENTRY + entry.getId());
        dragSource.setEffectAllowed(com.vaadin.flow.component.dnd.EffectAllowed.MOVE);

        return block;
    }
}
