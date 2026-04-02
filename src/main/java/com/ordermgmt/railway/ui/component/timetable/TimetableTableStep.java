package com.ordermgmt.railway.ui.component.timetable;

import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.activityLabel;
import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.arrivalConstraintLabel;
import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.createCard;
import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.departureConstraintLabel;
import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.distanceLabel;
import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.helperSpan;
import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.nvl;
import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.parseTime;
import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.roleLabel;
import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.timeOrDash;

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
                .setAutoWidth(true)
                .setFlexGrow(0);
        rowGrid.addColumn(row -> roleLabel(row.getRoutePointRole(), this))
                .setHeader(t("timetable.table.role"))
                .setAutoWidth(true);
        rowGrid.addColumn(TimetableRowData::getName)
                .setHeader(t("timetable.table.point"))
                .setAutoWidth(true)
                .setFlexGrow(1);
        rowGrid.addColumn(row -> nvl(row.getFromName()))
                .setHeader(t("timetable.table.from"))
                .setAutoWidth(true);
        rowGrid.addColumn(row -> nvl(row.getToName()))
                .setHeader(t("timetable.table.to"))
                .setAutoWidth(true);
        rowGrid.addColumn(row -> distanceLabel(row.getSegmentLengthMeters()))
                .setHeader(t("timetable.table.segment"))
                .setAutoWidth(true);
        rowGrid.addColumn(row -> distanceLabel(row.getDistanceFromStartMeters()))
                .setHeader(t("timetable.table.distance"))
                .setAutoWidth(true);
        rowGrid.addColumn(row -> timeOrDash(row.getEstimatedArrival()))
                .setHeader(t("timetable.table.estimatedArrival"))
                .setAutoWidth(true);
        rowGrid.addColumn(row -> arrivalConstraintLabel(row))
                .setHeader(t("timetable.table.arrival"))
                .setAutoWidth(true);
        rowGrid.addColumn(row -> timeOrDash(row.getEstimatedDeparture()))
                .setHeader(t("timetable.table.estimatedDeparture"))
                .setAutoWidth(true);
        rowGrid.addColumn(row -> departureConstraintLabel(row))
                .setHeader(t("timetable.table.departure"))
                .setAutoWidth(true);
        rowGrid.addColumn(
                        row ->
                                Boolean.TRUE.equals(row.getHalt())
                                        ? t("common.yes")
                                        : t("common.no"))
                .setHeader(t("timetable.table.halt"))
                .setAutoWidth(true);
        rowGrid.addColumn(row -> activityLabel(row, activityOptions))
                .setHeader(t("timetable.table.activity"))
                .setAutoWidth(true);

        // Actions column: + (insert) and trash (soft-delete)
        rowGrid.addComponentColumn(this::createRowActions)
                .setHeader("")
                .setAutoWidth(true)
                .setFlexGrow(0);

        rowGrid.asSingleSelect()
                .addValueChangeListener(
                        event -> {
                            selectedRow = event.getValue();
                            editorPanel.populate(
                                    selectedRow, isOrigin(selectedRow), isDestination(selectedRow));
                        });
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
