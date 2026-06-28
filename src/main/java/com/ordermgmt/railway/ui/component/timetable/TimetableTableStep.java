package com.ordermgmt.railway.ui.component.timetable;

import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.activityLabel;
import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.createCard;
import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.helperSpan;

import java.time.LocalDate;
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
import com.ordermgmt.railway.domain.infrastructure.repository.OperationalPointRepository;
import com.ordermgmt.railway.domain.timetable.model.TimetableActivityOption;
import com.ordermgmt.railway.domain.timetable.model.TimetableRowData;
import com.ordermgmt.railway.domain.timetable.service.TimetableEditingService;
import com.ordermgmt.railway.ui.component.ValidityCalendar;

/**
 * Table step of the timetable builder: grid, per-row editor, validity calendar. Cell rendering and
 * tooltips live in {@link TimetableRowCells}, estimate gap-filling + speed checks in {@link
 * TimetableEstimateCalculator}, and quick-add row construction in {@link TimetableStopBuilder};
 * this class wires them onto the grid and owns the row list + selection state.
 */
public class TimetableTableStep extends Div {

    private static final int ADD_STOP_ERROR_DURATION_MS = 4000;
    private static final int EDITOR_APPLIED_NOTIFICATION_DURATION_MS = 1800;
    private static final int VALIDATION_ERROR_DURATION_MS = 4500;

    private final List<TimetableActivityOption> activityOptions;
    private final TimetableEditingService editingService;
    private final OperationalPointRepository opRepo;

    private final Grid<TimetableRowData> rowGrid = new Grid<>(TimetableRowData.class, false);
    private final TimetableRowEditorPanel editorPanel;
    private final List<TimetableRowData> timetableRows = new ArrayList<>();
    private final TimetableRowCells cells;
    private TimetableRowData selectedRow;
    private AddStopForm addStopForm;

    public TimetableTableStep(
            List<TimetableActivityOption> activityOptions,
            TimetableEditingService editingService,
            OperationalPointRepository opRepo) {
        this.activityOptions = activityOptions;
        this.editingService = editingService;
        this.opRepo = opRepo;
        this.editorPanel = new TimetableRowEditorPanel(activityOptions, this::applyEditorChanges);
        this.cells = new TimetableRowCells(editingService, activityOptions, this, timetableRows);
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
                        + " — "
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

        addStopForm = new AddStopForm(opRepo, activityOptions, this::handleAddStop);

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
                    if (TimetableEstimateCalculator.hasUnrealisticSpeed(timetableRows, row)) {
                        return "row-warning";
                    }
                    return null;
                });

        // Sequence: tiny, fixed, no resize/tooltip needed
        rowGrid.addColumn(TimetableRowData::getSequence)
                .setHeader("#")
                .setWidth("38px")
                .setFlexGrow(0);

        // Name (point): TTT-aware renderer — origin/destination/halt/manually-added rows get the
        // accent badge since their UOPID + Name will be exported in the Path Request.
        rowGrid.addComponentColumn(cells::renderPointCell)
                .setHeader(t("timetable.table.point"))
                .setFlexGrow(1)
                .setResizable(true)
                .setTooltipGenerator(cells::pointTooltip);

        // Arrival: ComponentRenderer so user-entered (TTT-exportable) values can be styled
        // bold + accent — at a glance the user sees what gets sent in the Path Request.
        rowGrid.addComponentColumn(row -> cells.renderTimeCell(row, true))
                .setHeader(t("timetable.table.arrival"))
                .setAutoWidth(true)
                .setFlexGrow(0)
                .setResizable(true)
                .setTooltipGenerator(cells::arrivalTooltip);

        // Dwell: TTT-aware so user-entered DwellTime is visually distinct from a missing/zero dwell
        rowGrid.addComponentColumn(cells::renderDwellCell)
                .setHeader(t("timetable.editor.dwell"))
                .setAutoWidth(true)
                .setFlexGrow(0)
                .setResizable(true)
                .setTooltipGenerator(cells::dwellTooltip);

        // Departure: same TTT-aware renderer as arrival
        rowGrid.addComponentColumn(row -> cells.renderTimeCell(row, false))
                .setHeader(t("timetable.table.departure"))
                .setAutoWidth(true)
                .setFlexGrow(0)
                .setResizable(true)
                .setTooltipGenerator(cells::departureTooltip);

        rowGrid.addColumn(row -> Boolean.TRUE.equals(row.getHalt()) ? "✓" : "—")
                .setHeader(t("timetable.table.halt"))
                .setWidth("42px")
                .setFlexGrow(0);

        // Activity: auto-width since codes vary in length; tooltip carries the full label
        rowGrid.addColumn(row -> activityLabel(row, activityOptions))
                .setHeader(t("timetable.table.activity"))
                .setAutoWidth(true)
                .setFlexGrow(0)
                .setResizable(true)
                .setTooltipGenerator(cells::activityTooltip);

        // Actions column: + (insert) and trash (soft-delete)
        rowGrid.addComponentColumn(this::createRowActions)
                .setHeader("")
                .setWidth("64px")
                .setFlexGrow(0);

        // Excel-like: double-click anywhere on the header recalculates auto-width columns to fit
        // their current content. Single-click drag on the resizer (built-in) is unaffected.
        rowGrid.getElement()
                .executeJs(
                        "this.addEventListener('dblclick', e => {"
                                + "  const slot = e.target && e.target.assignedSlot;"
                                + "  if (!slot) return;"
                                + "  const name = slot.name || '';"
                                + "  if (name.startsWith('header')) { this.recalculateColumnWidths(); }"
                                + "});");

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
                    int rowIndex = timetableRows.indexOf(row);
                    if (rowIndex >= 0 && addStopForm != null) {
                        addStopForm.show(rowIndex, row.getName());
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
                    int rowIndex = timetableRows.indexOf(row);
                    editingService.softDeleteStop(timetableRows, rowIndex);
                    rowGrid.getDataProvider().refreshAll();
                });

        HorizontalLayout layout = new HorizontalLayout(insertBtn, deleteBtn);
        layout.setSpacing(false);
        layout.setPadding(false);
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        return layout;
    }

    // ── Add stop handler ──────────────────────────────────────────────

    private void handleAddStop(
            int insertAfterIndex,
            OperationalPoint point,
            String activityCode,
            AddStopForm.StopTimes times) {
        // The quick-add form has no day-offset field, so a departure before the arrival is always a
        // data-entry error (not a valid midnight crossing). Reject it instead of producing an
        // invalid stop.
        if (times.arrivalPrimary() != null
                && times.departurePrimary() != null
                && times.departurePrimary().isBefore(times.arrivalPrimary())) {
            Notification.show(
                            t("timetable.addstop.departureBeforeArrival"),
                            ADD_STOP_ERROR_DURATION_MS,
                            Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        TimetableRowData newRow = TimetableStopBuilder.buildStopRow(point, activityCode, times);
        // The new row brings its own anchors; the service propagates neighbours.
        editingService.insertStop(
                timetableRows, insertAfterIndex + 1, newRow, editorPanel.getPropagationMode());

        var violations = editingService.validate(timetableRows);
        if (!violations.isEmpty()) {
            Notification.show(violations.get(0), ADD_STOP_ERROR_DURATION_MS, Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }

        rowGrid.setItems(timetableRows);
        rowGrid.asSingleSelect().setValue(newRow);
        selectedRow = newRow;
        editorPanel.populate(newRow, isOrigin(newRow), isDestination(newRow));
    }

    // ── Editor sync ───────────────────────────────────────────────────

    private void applyEditorChanges() {
        if (syncSelectedRowFromEditor(true)) {
            Notification.show(
                            t("timetable.editor.applied"),
                            EDITOR_APPLIED_NOTIFICATION_DURATION_MS,
                            Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        }
    }

    private boolean syncSelectedRowFromEditor(boolean showNotifications) {
        if (selectedRow == null) {
            return true;
        }
        int idx = timetableRows.indexOf(selectedRow);
        if (idx < 0) {
            return true;
        }
        // Snapshot anchors BEFORE applying the user's edits, so propagate()
        // can detect direction (arrival changed → backward, departure → forward)
        // independent of which mode the user is in.
        TimetableEditingService.TimeSnapshot before = editingService.snapshot(selectedRow);

        if (!editorPanel.syncToRow(
                selectedRow,
                isOrigin(selectedRow),
                isDestination(selectedRow),
                showNotifications)) {
            return false;
        }

        // Enforce halt rules (Regel 3 + 4 + 5):
        //   halt=false → strip all constraint fields
        //   halt + dwell + 1 side → mirror dwell into other side, same mode
        //   halt + 2 sides → drop dwell (it's implied by the delta)
        // applyHaltRules supersedes the old deriveDepartureFromDwell + reconcileDwell pair.
        editingService.applyHaltRules(selectedRow);

        // Distance-weighted speed interpolation refreshes estimated* on intermediate rows
        // between user-entered anchors. propagate() then handles delta shifts.
        editingService.interpolateBetweenAnchors(timetableRows);

        editingService.propagate(timetableRows, idx, before, editorPanel.getPropagationMode());

        var violations = editingService.validate(timetableRows);
        if (!violations.isEmpty() && showNotifications) {
            Notification.show(violations.get(0), VALIDATION_ERROR_DURATION_MS, Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }

        // Fill missing estimates if the user just entered a first anchor
        TimetableEstimateCalculator.recalculateIfNeeded(timetableRows);

        rowGrid.getDataProvider().refreshAll();
        editorPanel.populate(selectedRow, isOrigin(selectedRow), isDestination(selectedRow));
        return true;
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
