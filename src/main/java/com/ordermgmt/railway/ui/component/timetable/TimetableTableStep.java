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

        // Sequence: tiny, fixed, no resize/tooltip needed
        rowGrid.addColumn(TimetableRowData::getSequence)
                .setHeader("#")
                .setWidth("38px")
                .setFlexGrow(0);

        // Name (point): TTT-aware renderer — origin/destination/halt/manually-added rows get the
        // accent badge since their UOPID + Name will be exported in the Path Request.
        rowGrid.addComponentColumn(this::renderPointCell)
                .setHeader(t("timetable.table.point"))
                .setFlexGrow(1)
                .setResizable(true)
                .setTooltipGenerator(this::pointTooltip);

        // Arrival: ComponentRenderer so user-entered (TTT-exportable) values can be styled
        // bold + accent \u2014 at a glance the user sees what gets sent in the Path Request.
        rowGrid.addComponentColumn(row -> renderTimeCell(row, true))
                .setHeader(t("timetable.table.arrival"))
                .setAutoWidth(true)
                .setFlexGrow(0)
                .setResizable(true)
                .setTooltipGenerator(this::arrivalTooltip);

        // Dwell: TTT-aware so user-entered DwellTime is visually distinct from a missing/zero dwell
        rowGrid.addComponentColumn(this::renderDwellCell)
                .setHeader(t("timetable.editor.dwell"))
                .setAutoWidth(true)
                .setFlexGrow(0)
                .setResizable(true)
                .setTooltipGenerator(this::dwellTooltip);

        // Departure: same TTT-aware renderer as arrival
        rowGrid.addComponentColumn(row -> renderTimeCell(row, false))
                .setHeader(t("timetable.table.departure"))
                .setAutoWidth(true)
                .setFlexGrow(0)
                .setResizable(true)
                .setTooltipGenerator(this::departureTooltip);

        rowGrid.addColumn(row -> Boolean.TRUE.equals(row.getHalt()) ? "\u2713" : "\u2014")
                .setHeader(t("timetable.table.halt"))
                .setWidth("42px")
                .setFlexGrow(0);

        // Activity: auto-width since codes vary in length; tooltip carries the full label
        rowGrid.addColumn(row -> activityLabel(row, activityOptions))
                .setHeader(t("timetable.table.activity"))
                .setAutoWidth(true)
                .setFlexGrow(0)
                .setResizable(true)
                .setTooltipGenerator(this::activityTooltip);

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

    /**
     * Combined arrival: constraint if set, else estimate, with qualifier tag and TTT-export marker
     * (\u25cf) prefix when the row's arrival was user-entered (i.e. will be sent in the Path
     * Request as a TTT Timing entry).
     */
    private String combinedArrivalLabel(TimetableRowData row) {
        String marker = editingService.hasUserEnteredArrival(row) ? "\u25cf " : "";
        String offsetSuffix = formatOffsetSuffix(row.getArrivalOffset());
        String constraint = arrivalConstraintLabel(row);
        if (!"\u2014".equals(constraint)) {
            String qualifier = timingQualifierCode(row.getArrivalMode(), true);
            return marker
                    + constraint
                    + (qualifier != null ? " [" + qualifier + "]" : "")
                    + offsetSuffix;
        }
        return timeOrDash(row.getEstimatedArrival()) + offsetSuffix;
    }

    /** Combined departure with the same marker + offset rules as arrival. */
    private String combinedDepartureLabel(TimetableRowData row) {
        String marker = editingService.hasUserEnteredDeparture(row) ? "\u25cf " : "";
        String offsetSuffix = formatOffsetSuffix(row.getDepartureOffset());
        String constraint = departureConstraintLabel(row);
        if (!"\u2014".equals(constraint)) {
            String qualifier = timingQualifierCode(row.getDepartureMode(), false);
            return marker
                    + constraint
                    + (qualifier != null ? " [" + qualifier + "]" : "")
                    + offsetSuffix;
        }
        return timeOrDash(row.getEstimatedDeparture()) + offsetSuffix;
    }

    /** "+1d" / "-1d" suffix for non-zero day offsets, empty for same-day. */
    private String formatOffsetSuffix(Integer offset) {
        if (offset == null || offset == 0) return "";
        return offset > 0 ? " +" + offset + "d" : " " + offset + "d";
    }

    /**
     * Cell renderer for the arrival/departure columns. User-entered (TTT-exportable) values are
     * displayed in bold accent color, with a small "TTT" badge prefix — making contractual times
     * visually distinct from machine-derived estimates.
     */
    private com.vaadin.flow.component.html.Span renderTimeCell(
            TimetableRowData row, boolean isArrival) {
        // Endpoints have no opposite-side time concept by definition — Origin has no arrival
        // (the train doesn't arrive there from anywhere on this run), Destination has no
        // departure. Return an empty span instead of "—" so the cell is visually clean.
        if (isArrival && isOrigin(row)) {
            return new com.vaadin.flow.component.html.Span();
        }
        if (!isArrival && isDestination(row)) {
            return new com.vaadin.flow.component.html.Span();
        }
        boolean userEntered =
                isArrival
                        ? editingService.hasUserEnteredArrival(row)
                        : editingService.hasUserEnteredDeparture(row);
        String text = isArrival ? combinedArrivalLabel(row) : combinedDepartureLabel(row);
        // Strip the leading "● " marker from text since the badge replaces it visually.
        if (text.startsWith("● ")) text = text.substring(2);
        return tttCell(text, userEntered);
    }

    /**
     * Cell renderer for the operational point name. Marked TTT when the row will be sent in the
     * Path Request (origin, destination, halt, or manually-added pass-through).
     */
    private com.vaadin.flow.component.html.Span renderPointCell(TimetableRowData row) {
        return tttCell(
                row.getName() == null ? "" : row.getName(), editingService.isExportedToTtt(row));
    }

    /**
     * Cell renderer for the dwell column. Marked TTT when the user explicitly entered a dwell value
     * — that DwellTime travels in the Path Request as part of TimingAtLocation.
     */
    private com.vaadin.flow.component.html.Span renderDwellCell(TimetableRowData row) {
        return tttCell(dwellLabel(row), Boolean.TRUE.equals(row.getUserEnteredDwell()));
    }

    /** Common TTT-or-plain cell builder — keeps the badge style consistent across columns. */
    private com.vaadin.flow.component.html.Span tttCell(String text, boolean ttt) {
        com.vaadin.flow.component.html.Span span = new com.vaadin.flow.component.html.Span();
        if (ttt && text != null && !text.isBlank()) {
            com.vaadin.flow.component.html.Span badge =
                    new com.vaadin.flow.component.html.Span("TTT");
            badge.getStyle()
                    .set("font-size", "9px")
                    .set("font-weight", "700")
                    .set("background", "var(--rom-accent)")
                    .set("color", "var(--rom-bg-base, #1a1a1a)")
                    .set("padding", "1px 5px")
                    .set("border-radius", "3px")
                    .set("margin-right", "6px")
                    .set("letter-spacing", "0.04em")
                    .set("vertical-align", "middle");
            com.vaadin.flow.component.html.Span value =
                    new com.vaadin.flow.component.html.Span(text);
            value.getStyle().set("font-weight", "600").set("color", "var(--rom-accent)");
            span.add(badge, value);
        } else {
            span.setText(text == null ? "" : text);
            if (text != null && !text.isBlank()) {
                span.getStyle().set("color", "var(--rom-text-muted, var(--rom-text-primary))");
            }
        }
        return span;
    }

    /** Dwell minutes, only shown if > 0. */
    private String dwellLabel(TimetableRowData row) {
        Integer dwell = row.getDwellMinutes();
        return dwell != null && dwell > 0 ? dwell + "'" : "";
    }

    // ── Tooltip generators ───────────────────────────────────────────

    private String pointTooltip(TimetableRowData row) {
        StringBuilder sb = new StringBuilder();
        if (row.getName() != null) sb.append(row.getName());
        if (row.getUopid() != null && !row.getUopid().isBlank()) {
            sb.append(" (").append(row.getCountry() == null ? "" : row.getCountry() + " ");
            sb.append(row.getUopid()).append(")");
        }
        if (row.getRoutePointRole() != null) {
            sb.append("\n").append(row.getRoutePointRole().name());
        }
        return sb.toString();
    }

    private String arrivalTooltip(TimetableRowData row) {
        return timeSideTooltip(
                row,
                true,
                row.getArrivalMode(),
                row.getArrivalExact(),
                row.getArrivalEarliest(),
                row.getArrivalLatest(),
                row.getCommercialArrival(),
                row.getEstimatedArrival());
    }

    private String departureTooltip(TimetableRowData row) {
        return timeSideTooltip(
                row,
                false,
                row.getDepartureMode(),
                row.getDepartureExact(),
                row.getDepartureEarliest(),
                row.getDepartureLatest(),
                row.getCommercialDeparture(),
                row.getEstimatedDeparture());
    }

    private String timeSideTooltip(
            TimetableRowData row,
            boolean arrival,
            com.ordermgmt.railway.domain.timetable.model.TimeConstraintMode mode,
            String exact,
            String earliest,
            String latest,
            String commercial,
            String estimated) {
        if (mode == null) {
            mode = com.ordermgmt.railway.domain.timetable.model.TimeConstraintMode.NONE;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(t(arrival ? "timetable.editor.arrival" : "timetable.editor.departure"));
        sb.append(" — ").append(t("timetable.timeMode." + mode.name())).append("\n");
        switch (mode) {
            case EXACT -> sb.append(arrival ? "ALA: " : "ALD: ").append(timeOrDash(exact));
            case WINDOW ->
                    sb.append(arrival ? "ELA: " : "ELD: ")
                            .append(timeOrDash(earliest))
                            .append("\n")
                            .append(arrival ? "LLA: " : "LLD: ")
                            .append(timeOrDash(latest));
            case AFTER ->
                    sb.append("≥ ")
                            .append(arrival ? "ELA: " : "ELD: ")
                            .append(timeOrDash(earliest));
            case BEFORE ->
                    sb.append("≤ ").append(arrival ? "LLA: " : "LLD: ").append(timeOrDash(latest));
            case COMMERCIAL ->
                    sb.append(arrival ? "PLA: " : "PLD: ").append(timeOrDash(commercial));
            case NONE -> sb.append("—");
        }
        if (estimated != null && !estimated.isBlank()) {
            sb.append("\n")
                    .append(
                            t(
                                    arrival
                                            ? "timetable.editor.estimatedArrival"
                                            : "timetable.editor.estimatedDeparture"))
                    .append(": ")
                    .append(estimated);
        }
        return sb.toString();
    }

    private String dwellTooltip(TimetableRowData row) {
        Integer dwell = row.getDwellMinutes();
        if (dwell == null || dwell <= 0) {
            return Boolean.TRUE.equals(row.getHalt()) ? t("timetable.editor.halt") : "—";
        }
        return t("timetable.editor.dwell") + ": " + dwell + " min";
    }

    private String activityTooltip(TimetableRowData row) {
        if (row.getActivityCodes() != null && !row.getActivityCodes().isEmpty()) {
            return row.getActivityCodes().stream()
                    .map(
                            code ->
                                    com.ordermgmt.railway.ui.component.timetable
                                            .TimetableFormatUtils.findActivityOption(
                                                    code, activityOptions)
                                            .map(opt -> opt.code() + " — " + opt.label())
                                            .orElse(code))
                    .reduce((left, right) -> left + "\n" + right)
                    .orElse("—");
        }
        return com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.findActivityOption(
                        row.getActivityCode(), activityOptions)
                .map(opt -> opt.code() + " — " + opt.label())
                .orElseGet(
                        () ->
                                Boolean.TRUE.equals(row.getHalt())
                                        ? t("timetable.stop.activityRequired")
                                        : "—");
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
                            4000,
                            Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        TimetableRowData newRow = buildStopRow(point, activityCode, times);
        // The new row brings its own anchors; the service propagates neighbours.
        editingService.insertStop(
                timetableRows, insertAfterIndex + 1, newRow, editorPanel.getPropagationMode());

        var violations = editingService.validate(timetableRows);
        if (!violations.isEmpty()) {
            Notification.show(violations.get(0), 4000, Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }

        rowGrid.setItems(timetableRows);
        rowGrid.asSingleSelect().setValue(newRow);
        selectedRow = newRow;
        editorPanel.populate(newRow, isOrigin(newRow), isDestination(newRow));
    }

    /** Build a row from the form's collected times, populating the right fields per mode. */
    private TimetableRowData buildStopRow(
            OperationalPoint point, String activityCode, AddStopForm.StopTimes times) {
        TimetableRowData row = new TimetableRowData();
        row.setUopid(point.getUopid());
        row.setName(point.getName());
        row.setCountry(point.getCountry());
        row.setRoutePointRole(com.ordermgmt.railway.domain.timetable.model.RoutePointRole.VIA);
        row.setJourneyLocationType("INTERMEDIATE");
        row.setHalt(true);
        row.setActivityCode(activityCode);
        row.setActivityCodes(List.of(activityCode));
        row.setManuallyAdded(true);
        applyArrival(row, times);
        applyDeparture(row, times);
        // dwell derived from arrival → departure of the new row, no hardcoded default
        var arr = times.arrivalPrimary();
        var dep = times.departurePrimary();
        if (arr != null && dep != null) {
            long dwell = java.time.Duration.between(arr, dep).toMinutes();
            // handleAddStop already rejects departure-before-arrival; guard defensively so a stop
            // can never receive a negative dwell.
            if (dwell >= 0) {
                row.setDwellMinutes((int) dwell);
                row.setUserEnteredDwell(true);
            }
        }
        return row;
    }

    private void applyArrival(TimetableRowData row, AddStopForm.StopTimes t) {
        java.time.format.DateTimeFormatter f =
                java.time.format.DateTimeFormatter.ofPattern("HH:mm");
        row.setArrivalMode(t.arrivalMode());
        row.setEstimatedArrival(t.arrivalPrimary().format(f));
        switch (t.arrivalMode()) {
            case EXACT -> {
                row.setArrivalExact(t.arrivalPrimary().format(f));
                row.setUserEnteredArrivalExact(true);
            }
            case WINDOW -> {
                row.setArrivalEarliest(t.arrivalPrimary().format(f));
                row.setArrivalLatest(t.arrivalSecondary().format(f));
                row.setUserEnteredArrivalEarliest(true);
                row.setUserEnteredArrivalLatest(true);
            }
            case AFTER -> {
                row.setArrivalEarliest(t.arrivalPrimary().format(f));
                row.setUserEnteredArrivalEarliest(true);
            }
            case BEFORE -> {
                row.setArrivalLatest(t.arrivalPrimary().format(f));
                row.setUserEnteredArrivalLatest(true);
            }
            case COMMERCIAL -> {
                row.setCommercialArrival(t.arrivalPrimary().format(f));
                row.setUserEnteredCommercialArrival(true);
            }
            case NONE -> {
                /* estimated already set */
            }
        }
    }

    private void applyDeparture(TimetableRowData row, AddStopForm.StopTimes t) {
        java.time.format.DateTimeFormatter f =
                java.time.format.DateTimeFormatter.ofPattern("HH:mm");
        row.setDepartureMode(t.departureMode());
        row.setEstimatedDeparture(t.departurePrimary().format(f));
        switch (t.departureMode()) {
            case EXACT -> {
                row.setDepartureExact(t.departurePrimary().format(f));
                row.setUserEnteredDepartureExact(true);
            }
            case WINDOW -> {
                row.setDepartureEarliest(t.departureSecondary().format(f));
                row.setDepartureLatest(t.departurePrimary().format(f));
                row.setUserEnteredDepartureEarliest(true);
                row.setUserEnteredDepartureLatest(true);
            }
            case AFTER -> {
                row.setDepartureEarliest(t.departurePrimary().format(f));
                row.setUserEnteredDepartureEarliest(true);
            }
            case BEFORE -> {
                row.setDepartureLatest(t.departurePrimary().format(f));
                row.setUserEnteredDepartureLatest(true);
            }
            case COMMERCIAL -> {
                row.setCommercialDeparture(t.departurePrimary().format(f));
                row.setUserEnteredCommercialDeparture(true);
            }
            case NONE -> {
                /* estimated already set */
            }
        }
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
            Notification.show(violations.get(0), 4500, Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }

        // Fill missing estimates if the user just entered a first anchor
        recalculateEstimatesIfNeeded();

        rowGrid.getDataProvider().refreshAll();
        editorPanel.populate(selectedRow, isOrigin(selectedRow), isDestination(selectedRow));
        return true;
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
