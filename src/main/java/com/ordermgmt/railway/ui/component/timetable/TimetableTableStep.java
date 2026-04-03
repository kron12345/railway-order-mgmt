package com.ordermgmt.railway.ui.component.timetable;

import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.activityLabel;
import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.arrivalConstraintLabel;
import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.createCard;
import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.departureConstraintLabel;
import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.formatTime;
import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.helperSpan;
import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.parseTime;
import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.timeOrDash;
import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.timingQualifierCode;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;

import com.ordermgmt.railway.domain.infrastructure.model.OperationalPoint;
import com.ordermgmt.railway.domain.timetable.model.TimetableActivityOption;
import com.ordermgmt.railway.domain.timetable.model.TimetableRowData;
import com.ordermgmt.railway.domain.timetable.service.TimetableEditingService;
import com.ordermgmt.railway.ui.component.ValidityCalendar;

/** Table step of the timetable builder: grid, per-row editor, validity calendar. */
public class TimetableTableStep extends Div {

    private final List<TimetableActivityOption> activityOptions;
    private final TimetableEditingService editingService;
    private final List<OperationalPoint> availableOperationalPoints;

    private final Grid<TimetableRowData> rowGrid = new Grid<>(TimetableRowData.class, false);
    private final TimetableRowEditorPanel editorPanel;
    private final List<TimetableRowData> timetableRows = new ArrayList<>();
    private TimetableRowData selectedRow;
    private AddStopForm addStopForm;

    public TimetableTableStep(
            List<TimetableActivityOption> activityOptions,
            TimetableEditingService editingService,
            List<OperationalPoint> availableOperationalPoints) {
        this.activityOptions = activityOptions;
        this.editingService = editingService;
        this.availableOperationalPoints = availableOperationalPoints;
        this.editorPanel = new TimetableRowEditorPanel(activityOptions, this::applyEditorChanges);
        configureGrid();
    }

    /** Builds the full table-step layout (validity card + split grid/editor). */
    public Component createContent(
            LocalDate orderFrom,
            LocalDate orderTo,
            ValidityCalendar validityCalendar,
            String routeSummaryText) {

        Div wrapper = new Div();
        wrapper.setWidthFull();
        wrapper.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "8px")
                .set("height", "100%")
                .set("min-height", "0");

        validityCalendar.setCompact(true);
        int selectedCount = validityCalendar.getSelectedDates().size();
        String validitySummary =
                t("position.validity")
                        + " \u2014 "
                        + selectedCount
                        + " "
                        + t("timetable.archive.days", selectedCount);
        Details validityDetails = new Details();
        validityDetails.setSummaryText(validitySummary);
        Div calendarWrapper = new Div(validityCalendar);
        calendarWrapper.getStyle().set("max-height", "200px").set("overflow-y", "auto");
        validityDetails.add(
                helperSpan(t("position.validity.help", orderFrom, orderTo)), calendarWrapper);
        validityDetails.setWidthFull();
        validityDetails
                .getStyle()
                .set("background", "var(--rom-bg-card)")
                .set("border", "1px solid var(--rom-border)")
                .set("border-radius", "6px");
        validityDetails.setOpened(selectedCount == 0);
        wrapper.add(validityDetails);

        addStopForm =
                new AddStopForm(availableOperationalPoints, activityOptions, this::handleAddStop);

        Div gridAndForm = new Div(rowGrid, addStopForm);
        gridAndForm.setWidthFull();
        gridAndForm
                .getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("flex", "1")
                .set("min-height", "0");
        rowGrid.getStyle().set("flex", "1").set("min-height", "0");

        Div gridCard = createCard(t("timetable.table.title"), helperSpan(routeSummaryText));
        gridCard.add(gridAndForm);
        gridCard.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("height", "100%")
                .set("min-height", "0");

        Div editorCard = createCard(t("timetable.editor.title"), editorPanel);
        editorCard.getStyle().set("overflow-y", "auto");

        SplitLayout tableLayout = new SplitLayout(gridCard, editorCard);
        tableLayout.setWidthFull();
        tableLayout.getStyle().set("flex", "1").set("min-height", "0");
        tableLayout.setSplitterPosition(65);

        wrapper.add(tableLayout);
        return wrapper;
    }

    /** Replaces the grid rows and selects the first row. */
    public void setRows(List<TimetableRowData> rows) {
        timetableRows.clear();
        timetableRows.addAll(rows);
        rowGrid.setItems(timetableRows);
        if (!timetableRows.isEmpty()) {
            rowGrid.asSingleSelect().setValue(timetableRows.getFirst());
            selectedRow = timetableRows.getFirst();
            editorPanel.populate(selectedRow, isOrigin(selectedRow), isDestination(selectedRow));
        } else {
            selectedRow = null;
            editorPanel.setVisible(false);
        }
    }

    /** Returns the current list of timetable rows. */
    public List<TimetableRowData> getRows() {
        return timetableRows;
    }

    /** Syncs the currently selected row from the editor. Returns false on validation failure. */
    public boolean syncCurrentEditor() {
        return syncSelectedRowFromEditor(false);
    }

    /** Returns the currently selected row, or {@code null}. */
    public TimetableRowData getSelectedRow() {
        return selectedRow;
    }

    // ── Grid configuration ────────────────────────────────────────────

    private void configureGrid() {
        rowGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COLUMN_BORDERS);
        rowGrid.setWidthFull();
        rowGrid.setSelectionMode(Grid.SelectionMode.SINGLE);

        // Strike-through styling for soft-deleted rows, warning for unrealistic speed
        rowGrid.setClassNameGenerator(
                row -> {
                    if (Boolean.TRUE.equals(row.getDeleted())) {
                        return "row-deleted";
                    }
                    if (hasUnrealisticSpeed(row)) {
                        return "row-warning";
                    }
                    return null;
                });

        rowGrid.addColumn(TimetableRowData::getSequence)
                .setHeader("#")
                .setWidth("30px")
                .setFlexGrow(0);
        rowGrid.addColumn(TimetableRowData::getName)
                .setHeader(t("timetable.table.point"))
                .setFlexGrow(1);
        rowGrid.addColumn(this::combinedArrivalLabel)
                .setHeader(t("timetable.table.arrival"))
                .setWidth("90px")
                .setFlexGrow(0);
        rowGrid.addColumn(this::dwellLabel)
                .setHeader(t("timetable.editor.dwell"))
                .setWidth("45px")
                .setFlexGrow(0);
        rowGrid.addColumn(this::combinedDepartureLabel)
                .setHeader(t("timetable.table.departure"))
                .setWidth("90px")
                .setFlexGrow(0);
        rowGrid.addColumn(row -> Boolean.TRUE.equals(row.getHalt()) ? "\u2713" : "\u2014")
                .setHeader(t("timetable.table.halt"))
                .setWidth("35px")
                .setFlexGrow(0);
        rowGrid.addColumn(row -> activityLabel(row, activityOptions))
                .setHeader(t("timetable.table.activity"))
                .setWidth("80px")
                .setFlexGrow(0);

        // Actions column: + (insert) and trash (soft-delete)
        rowGrid.addComponentColumn(this::createRowActions)
                .setHeader("")
                .setWidth("60px")
                .setFlexGrow(0);

        rowGrid.asSingleSelect()
                .addValueChangeListener(
                        event -> {
                            selectedRow = event.getValue();
                            editorPanel.populate(
                                    selectedRow, isOrigin(selectedRow), isDestination(selectedRow));
                        });
    }

    /** Combined arrival: constraint if set, else estimate, with qualifier tag. */
    private String combinedArrivalLabel(TimetableRowData row) {
        String constraint = arrivalConstraintLabel(row);
        if (!"\u2014".equals(constraint)) {
            String qualifier = timingQualifierCode(row.getArrivalMode(), true);
            return constraint + (qualifier != null ? " [" + qualifier + "]" : "");
        }
        return timeOrDash(row.getEstimatedArrival());
    }

    /** Combined departure: constraint if set, else estimate, with qualifier tag. */
    private String combinedDepartureLabel(TimetableRowData row) {
        String constraint = departureConstraintLabel(row);
        if (!"\u2014".equals(constraint)) {
            String qualifier = timingQualifierCode(row.getDepartureMode(), false);
            return constraint + (qualifier != null ? " [" + qualifier + "]" : "");
        }
        return timeOrDash(row.getEstimatedDeparture());
    }

    /** Dwell minutes, only shown if > 0. */
    private String dwellLabel(TimetableRowData row) {
        Integer dwell = row.getDwellMinutes();
        return dwell != null && dwell > 0 ? dwell + "'" : "";
    }

    private HorizontalLayout createRowActions(TimetableRowData row) {
        Button insertBtn = new Button(VaadinIcon.PLUS.create());
        insertBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY_INLINE);
        insertBtn.getStyle().set("color", "var(--rom-accent)").set("min-width", "28px");
        insertBtn.getElement().setAttribute("title", t("timetable.stop.add"));
        insertBtn.addClickListener(
                e -> {
                    int idx = timetableRows.indexOf(row);
                    if (idx >= 0 && addStopForm != null) {
                        addStopForm.show(idx, row.getName());
                    }
                });

        boolean isDeleted = Boolean.TRUE.equals(row.getDeleted());
        boolean isEndpoint = isOrigin(row) || isDestination(row);

        Button deleteBtn =
                new Button(
                        isDeleted ? VaadinIcon.ARROW_BACKWARD.create() : VaadinIcon.TRASH.create());
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY_INLINE);
        deleteBtn
                .getElement()
                .setAttribute(
                        "title", isDeleted ? t("timetable.stop.undo") : t("timetable.stop.remove"));
        deleteBtn.getStyle().set("min-width", "28px");
        if (isDeleted) {
            deleteBtn.getStyle().set("color", "var(--rom-accent)");
        } else {
            deleteBtn.getStyle().set("color", "var(--rom-status-error, #ef4444)");
        }
        deleteBtn.setVisible(!isEndpoint);
        deleteBtn.addClickListener(
                e -> {
                    int idx = timetableRows.indexOf(row);
                    editingService.softDeleteStop(timetableRows, idx);
                    rowGrid.getDataProvider().refreshAll();
                });

        HorizontalLayout layout = new HorizontalLayout(insertBtn, deleteBtn);
        layout.setSpacing(false);
        layout.setPadding(false);
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        return layout;
    }

    // ── Add stop handler ──────────────────────────────────────────────

    private void handleAddStop(int insertAfterIndex, OperationalPoint point, String activityCode) {
        editingService.insertStop(timetableRows, insertAfterIndex + 1, point, activityCode);
        recalculateEstimatesIfNeeded();
        rowGrid.setItems(timetableRows);
        TimetableRowData newRow = timetableRows.get(insertAfterIndex + 1);
        rowGrid.asSingleSelect().setValue(newRow);
        selectedRow = newRow;
        editorPanel.populate(newRow, isOrigin(newRow), isDestination(newRow));
    }

    // ── Editor sync ───────────────────────────────────────────────────

    private void applyEditorChanges() {
        if (syncSelectedRowFromEditor(true)) {
            Notification.show(t("timetable.editor.applied"), 1800, Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        }
    }

    private boolean syncSelectedRowFromEditor(boolean showNotifications) {
        if (selectedRow == null) {
            return true;
        }
        if (!editorPanel.syncToRow(
                selectedRow,
                isOrigin(selectedRow),
                isDestination(selectedRow),
                showNotifications)) {
            return false;
        }

        // Phase 3: Propagate time changes
        propagateIfNeeded(selectedRow, true);
        propagateIfNeeded(selectedRow, false);

        // Phase 4: Fill in missing estimates when first time is entered
        recalculateEstimatesIfNeeded();

        rowGrid.getDataProvider().refreshAll();
        editorPanel.populate(selectedRow, isOrigin(selectedRow), isDestination(selectedRow));
        return true;
    }

    private void propagateIfNeeded(TimetableRowData row, boolean isArrival) {
        int idx = timetableRows.indexOf(row);
        if (idx < 0) {
            return;
        }
        String currentExact = isArrival ? row.getArrivalExact() : row.getDepartureExact();
        String estimated = isArrival ? row.getEstimatedArrival() : row.getEstimatedDeparture();
        LocalTime exactTime = parseTime(currentExact);
        LocalTime estTime = parseTime(estimated);

        if (exactTime != null && estTime != null && !exactTime.equals(estTime)) {
            editingService.propagateTimeChange(
                    timetableRows, idx, isArrival, exactTime, editorPanel.getPropagationMode());
        }
    }

    // ── Estimate gap filling ─────────────────────────────────────────────

    /** Assumed travel speed (m/s) equivalent to 70 km/h. */
    private static final double ASSUMED_SPEED_MPS = 70_000D / 3_600D;

    /**
     * Checks if any rows after the first row with a departure time are missing estimates, and fills
     * them based on segment distances and an assumed speed of 70 km/h.
     */
    private void recalculateEstimatesIfNeeded() {
        TimetableRowData anchorRow = null;
        int anchorIdx = -1;

        // Find the first row that has any departure time set
        for (int i = 0; i < timetableRows.size(); i++) {
            TimetableRowData row = timetableRows.get(i);
            String dep = resolveEffectiveDeparture(row);
            if (dep != null && !dep.isBlank()) {
                anchorRow = row;
                anchorIdx = i;
                break;
            }
        }
        if (anchorRow == null || anchorIdx >= timetableRows.size() - 1) {
            return;
        }

        // Check if any following rows lack estimated departure
        boolean hasGaps = false;
        for (int i = anchorIdx + 1; i < timetableRows.size(); i++) {
            if (timetableRows.get(i).getEstimatedArrival() == null
                    || timetableRows.get(i).getEstimatedArrival().isBlank()) {
                hasGaps = true;
                break;
            }
        }

        if (hasGaps) {
            fillEstimatesFromAnchor(anchorRow, anchorIdx);
        }
    }

    /**
     * Forward-fills estimated arrival/departure times from the given anchor row using segment
     * distances and the assumed speed.
     */
    private void fillEstimatesFromAnchor(TimetableRowData anchorRow, int anchorIdx) {
        String depStr = resolveEffectiveDeparture(anchorRow);
        LocalTime cursor = parseTime(depStr);
        if (cursor == null) {
            return;
        }
        // Ensure the anchor row itself has the estimated departure set
        if (anchorRow.getEstimatedDeparture() == null
                || anchorRow.getEstimatedDeparture().isBlank()) {
            anchorRow.setEstimatedDeparture(formatTime(cursor));
        }

        for (int i = anchorIdx + 1; i < timetableRows.size(); i++) {
            TimetableRowData row = timetableRows.get(i);
            double segmentMeters =
                    row.getSegmentLengthMeters() != null ? row.getSegmentLengthMeters() : 0D;
            long travelSec = Math.round(segmentMeters / ASSUMED_SPEED_MPS);
            cursor = cursor.plusSeconds(travelSec);

            // Only fill if currently empty
            if (row.getEstimatedArrival() == null || row.getEstimatedArrival().isBlank()) {
                row.setEstimatedArrival(formatTime(cursor));
            } else {
                cursor = parseTime(row.getEstimatedArrival());
                if (cursor == null) {
                    return;
                }
            }

            // Add dwell time for halts
            if (Boolean.TRUE.equals(row.getHalt()) && row.getDwellMinutes() != null) {
                cursor = cursor.plusMinutes(row.getDwellMinutes());
            }

            if (i < timetableRows.size() - 1) {
                if (row.getEstimatedDeparture() == null || row.getEstimatedDeparture().isBlank()) {
                    row.setEstimatedDeparture(formatTime(cursor));
                } else {
                    cursor = parseTime(row.getEstimatedDeparture());
                    if (cursor == null) {
                        return;
                    }
                }
            }
        }
    }

    /**
     * Returns the effective departure time string for a row: prefers exact/constraint time, falls
     * back to estimated.
     */
    private String resolveEffectiveDeparture(TimetableRowData row) {
        // Check all departure time sources: exact, window earliest, commercial, estimated
        if (row.getDepartureExact() != null && !row.getDepartureExact().isBlank()) {
            return row.getDepartureExact();
        }
        if (row.getDepartureEarliest() != null && !row.getDepartureEarliest().isBlank()) {
            return row.getDepartureEarliest();
        }
        if (row.getCommercialDeparture() != null && !row.getCommercialDeparture().isBlank()) {
            return row.getCommercialDeparture();
        }
        if (row.getEstimatedDeparture() != null && !row.getEstimatedDeparture().isBlank()) {
            return row.getEstimatedDeparture();
        }
        // Fallback: check arrival times (user may have set arrival but not departure)
        if (row.getArrivalExact() != null && !row.getArrivalExact().isBlank()) {
            return row.getArrivalExact();
        }
        if (row.getArrivalEarliest() != null && !row.getArrivalEarliest().isBlank()) {
            return row.getArrivalEarliest();
        }
        if (row.getEstimatedArrival() != null && !row.getEstimatedArrival().isBlank()) {
            return row.getEstimatedArrival();
        }
        return null;
    }

    // ── Speed plausibility ─────────────────────────────────────────────

    /** Maximum plausible speed (km/h) for a rail segment; rows exceeding this get a warning. */
    private static final double MAX_SPEED_KMH = 200.0;

    /**
     * Checks whether the implied speed between the previous row's departure and this row's arrival
     * exceeds {@link #MAX_SPEED_KMH}, indicating a likely data error.
     */
    private boolean hasUnrealisticSpeed(TimetableRowData row) {
        double impliedSpeed = calculateImpliedSpeedKmh(row);
        return impliedSpeed > MAX_SPEED_KMH;
    }

    /**
     * Calculates the implied speed (km/h) for reaching this row from the previous row. Returns 0 if
     * the calculation is not possible (missing data or first row).
     */
    private double calculateImpliedSpeedKmh(TimetableRowData row) {
        if (row.getSegmentLengthMeters() == null || row.getSegmentLengthMeters() <= 0) {
            return 0;
        }
        LocalTime arrival = parseTime(row.getEstimatedArrival());
        if (arrival == null) {
            return 0;
        }
        int idx = timetableRows.indexOf(row);
        if (idx <= 0) {
            return 0;
        }
        TimetableRowData previousRow = timetableRows.get(idx - 1);
        LocalTime previousDeparture = parseTime(previousRow.getEstimatedDeparture());
        if (previousDeparture == null) {
            return 0;
        }
        long travelTimeSeconds = Duration.between(previousDeparture, arrival).getSeconds();
        if (travelTimeSeconds <= 0) {
            return 0;
        }
        double distanceKm = row.getSegmentLengthMeters() / 1000.0;
        double travelTimeHours = travelTimeSeconds / 3600.0;
        return distanceKm / travelTimeHours;
    }

    // ── UI helpers ────────────────────────────────────────────────────

    private boolean isOrigin(TimetableRowData row) {
        return row != null && !timetableRows.isEmpty() && row == timetableRows.getFirst();
    }

    private boolean isDestination(TimetableRowData row) {
        return row != null && !timetableRows.isEmpty() && row == timetableRows.getLast();
    }

    private String t(String key, Object... params) {
        return getTranslation(key, params);
    }
}
