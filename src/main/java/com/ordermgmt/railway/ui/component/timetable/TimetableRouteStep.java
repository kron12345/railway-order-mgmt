package com.ordermgmt.railway.ui.component.timetable;

import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.createCard;
import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.distanceLabel;
import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.firstNonBlank;
import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.helperSpan;
import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.timeOrDash;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.timepicker.TimePicker;

import com.ordermgmt.railway.domain.infrastructure.model.OperationalPoint;
import com.ordermgmt.railway.domain.timetable.model.RoutePointRole;
import com.ordermgmt.railway.domain.timetable.model.TimeConstraintMode;
import com.ordermgmt.railway.domain.timetable.model.TimetableActivityOption;
import com.ordermgmt.railway.domain.timetable.model.TimetableRouteResult;
import com.ordermgmt.railway.domain.timetable.model.TimetableRowData;
import com.ordermgmt.railway.domain.timetable.service.TimetableRoutingService;

/**
 * Route-definition step for the timetable builder.
 *
 * <p>Allows the user to select origin, destination, and optional via points, then calculates a
 * route with estimated travel times. The result is displayed on an interactive map.
 */
public class TimetableRouteStep extends Div {

    // ── Data records ───────────────────────────────────────────────────

    /** Callback payload delivered after a successful route calculation. */
    public record RouteCalculationResult(TimetableRouteResult route, List<TimetableRowData> rows) {}

    /** Prefill data for a single via point. */
    public record ViaData(OperationalPoint point, boolean halt, String activityCode) {}

    // ── Route Form ──────────────────────────────────────────────────────

    private final List<OperationalPoint> availableOps;
    private final List<TimetableActivityOption> activityOptions;
    private final TimetableRoutingService routingService;

    private final ComboBox<OperationalPoint> fromField = new ComboBox<>();
    private final ComboBox<OperationalPoint> toField = new ComboBox<>();
    private final VerticalLayout viaList = new VerticalLayout();
    private final List<ViaPointEditor> viaEditors = new ArrayList<>();
    private final TimePicker departureAnchorField = buildTimePicker();
    private final TimePicker arrivalAnchorField = buildTimePicker();
    private final Span routeSummary = new Span();
    private final Span routeError = new Span();
    private final TimetableMap routeMap = new TimetableMap();

    private final Div routePreview = new Div();
    private Button calcButton;
    private TimetableRouteResult currentRoute = new TimetableRouteResult(List.of(), 0D);
    private Consumer<RouteCalculationResult> onRouteCalculated;
    private Runnable onRouteDirty;

    public TimetableRouteStep(
            List<OperationalPoint> availableOperationalPoints,
            List<TimetableActivityOption> activityOptions,
            TimetableRoutingService timetableRoutingService) {
        this.availableOps = availableOperationalPoints;
        this.activityOptions = activityOptions;
        this.routingService = timetableRoutingService;
        configureRouteInputs();
    }

    /** Builds and returns the SplitLayout that forms this step's content. */
    public Component createContent() {
        Button addViaBtn = new Button(t("timetable.route.addVia"), VaadinIcon.PLUS.create());
        addViaBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
        addViaBtn.addClickListener(e -> addViaEditor(null, false, null));

        Button reverseBtn = new Button(VaadinIcon.EXCHANGE.create());
        reverseBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY_INLINE);
        reverseBtn.getStyle().set("color", "var(--rom-text-secondary)").set("cursor", "pointer");
        reverseBtn.getElement().setAttribute("title", t("timetable.route.reverse"));
        reverseBtn.addClickListener(e -> reverseRoute());

        FormLayout routeForm = new FormLayout();
        routeForm.setWidthFull();
        routeForm.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("640px", 2));
        routeForm.add(fromField, reverseBtn, toField, departureAnchorField, arrivalAnchorField);

        Div viaHeader = new Div();
        viaHeader
                .getStyle()
                .set("display", "flex")
                .set("justify-content", "space-between")
                .set("align-items", "center")
                .set("margin-top", "8px");
        Span viaTitle = new Span(t("timetable.route.via"));
        viaTitle.getStyle().set("font-weight", "600").set("color", "var(--rom-text-primary)");
        viaHeader.add(viaTitle, addViaBtn);

        calcButton = new Button(t("timetable.route.calculate"), VaadinIcon.MAP_MARKER.create());
        calcButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        calcButton
                .getStyle()
                .set("background", "var(--rom-accent)")
                .set("color", "var(--rom-bg-primary)");
        calcButton.addClickListener(e -> calculateRoute());
        calcButton.addClickShortcut(Key.ENTER);

        // Clear field-level validation on value change
        fromField.addValueChangeListener(
                e -> {
                    fromField.setInvalid(false);
                    resetCalcButtonAppearance();
                });
        toField.addValueChangeListener(
                e -> {
                    toField.setInvalid(false);
                    resetCalcButtonAppearance();
                });
        departureAnchorField.addValueChangeListener(e -> resetCalcButtonAppearance());
        arrivalAnchorField.addValueChangeListener(e -> resetCalcButtonAppearance());

        routePreview.setVisible(false);
        routePreview.setWidthFull();

        VerticalLayout left = new VerticalLayout();
        left.setPadding(false);
        left.setSpacing(true);
        left.setWidthFull();
        left.add(
                createCard(
                        t("timetable.route.title"),
                        routeForm,
                        helperSpan(t("timetable.route.anchor.help")),
                        viaHeader,
                        viaList,
                        routeSummary,
                        routePreview,
                        routeError,
                        calcButton));

        Div mapCard = new Div();
        mapCard.setSizeFull();
        mapCard.getStyle()
                .set("background", "var(--rom-bg-card)")
                .set("border", "1px solid var(--rom-border)")
                .set("border-radius", "6px")
                .set("padding", "12px")
                .set("box-sizing", "border-box")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("min-height", "0");
        Span mapLabel = new Span(t("timetable.route.map"));
        mapLabel.getStyle().set("font-weight", "600").set("margin-bottom", "10px");
        routeMap.getElement().getStyle().set("flex", "1").set("min-height", "0");
        mapCard.add(mapLabel, routeMap);

        // Show all operational points as background markers on the map
        routeMap.setAllOperationalPoints(availableOps);

        // Map click on OP fills from/to/via fields sequentially
        routeMap.addOpSelectedListener(
                uopid -> {
                    OperationalPoint op = findOpByUopid(uopid);
                    if (op == null) return;
                    if (fromField.getValue() == null) {
                        fromField.setValue(op);
                    } else if (toField.getValue() == null) {
                        toField.setValue(op);
                    } else {
                        addViaEditor(op, false, null);
                    }
                });

        SplitLayout split = new SplitLayout(left, mapCard);
        split.setWidthFull();
        split.setSizeFull();
        split.setSplitterPosition(25);
        return split;
    }

    // ── Public accessors ────────────────────────────────────────────────

    /** Replaces the current route and updates the map display. */
    public void setRoute(TimetableRouteResult route) {
        this.currentRoute = route;
        routeMap.setRoute(route.points());
    }

    public TimetableRouteResult getCurrentRoute() {
        return currentRoute;
    }

    /** Calculates route with explicit anchors. Returns null if validation fails. */
    public List<TimetableRowData> calculateRoute(LocalTime depAnchor, LocalTime arrAnchor) {
        return doCalculateRoute(depAnchor, arrAnchor, false);
    }

    /** Triggers route calculation using the current field values. */
    public void calculateRoute() {
        doCalculateRoute(departureAnchorField.getValue(), arrivalAnchorField.getValue(), true);
    }

    public void prefillFrom(OperationalPoint from, OperationalPoint to, List<ViaData> vias) {
        fromField.setValue(from);
        toField.setValue(to);
        clearViaEditors();
        if (vias != null) {
            vias.forEach(v -> addViaEditor(v.point(), v.halt(), v.activityCode()));
        }
    }

    public OperationalPoint getFromValue() {
        return fromField.getValue();
    }

    public OperationalPoint getToValue() {
        return toField.getValue();
    }

    public LocalTime getDepartureAnchor() {
        return departureAnchorField.getValue();
    }

    public LocalTime getArrivalAnchor() {
        return arrivalAnchorField.getValue();
    }

    public void setDepartureAnchor(LocalTime t) {
        departureAnchorField.setValue(t);
    }

    public void setArrivalAnchor(LocalTime t) {
        arrivalAnchorField.setValue(t);
    }

    public Span getRouteSummary() {
        return routeSummary;
    }

    public Span getRouteError() {
        return routeError;
    }

    public void setOnRouteCalculated(Consumer<RouteCalculationResult> callback) {
        this.onRouteCalculated = callback;
    }

    public void setOnRouteDirty(Runnable callback) {
        this.onRouteDirty = callback;
    }

    // ── Route manipulation ─────────────────────────────────────────────

    private void reverseRoute() {
        OperationalPoint from = fromField.getValue();
        OperationalPoint to = toField.getValue();
        fromField.setValue(to);
        toField.setValue(from);
        LocalTime dep = departureAnchorField.getValue();
        LocalTime arr = arrivalAnchorField.getValue();
        departureAnchorField.setValue(arr);
        arrivalAnchorField.setValue(dep);
        // Reverse via editors order
        List<ViaData> reversedVias = new ArrayList<>();
        for (int i = viaEditors.size() - 1; i >= 0; i--) {
            ViaPointEditor ed = viaEditors.get(i);
            reversedVias.add(
                    new ViaData(
                            ed.pointField.getValue(),
                            Boolean.TRUE.equals(ed.haltField.getValue()),
                            ed.activityField.getValue() != null
                                    ? ed.activityField.getValue().code()
                                    : null));
        }
        clearViaEditors();
        for (ViaData via : reversedVias) {
            addViaEditor(via.point(), via.halt(), via.activityCode());
        }
    }

    public List<ViaData> getViaValues() {
        List<ViaData> result = new ArrayList<>();
        for (ViaPointEditor ed : viaEditors) {
            result.add(
                    new ViaData(
                            ed.pointField.getValue(),
                            Boolean.TRUE.equals(ed.haltField.getValue()),
                            ed.activityField.getValue() != null
                                    ? ed.activityField.getValue().code()
                                    : null));
        }
        return result;
    }

    /** Registers a callback for when an operational point is clicked on the map. */
    public void addOpSelectedListener(Consumer<String> callback) {
        routeMap.addOpSelectedListener(callback);
    }

    private void markCalcButtonSuccess() {
        if (calcButton == null) return;
        calcButton.setText(t("timetable.route.calculated"));
        calcButton.setIcon(VaadinIcon.CHECK.create());
        calcButton
                .getStyle()
                .set("background", "var(--rom-status-success, #22c55e)")
                .set("color", "#fff");
    }

    private void resetCalcButtonAppearance() {
        if (calcButton == null) return;
        calcButton.setText(t("timetable.route.calculate"));
        calcButton.setIcon(VaadinIcon.MAP_MARKER.create());
        calcButton
                .getStyle()
                .set("background", "var(--rom-accent)")
                .set("color", "var(--rom-bg-primary)");
        if (onRouteDirty != null) {
            onRouteDirty.run();
        }
    }

    private OperationalPoint findOpByUopid(String uopid) {
        if (uopid == null) return null;
        return availableOps.stream()
                .filter(op -> uopid.equals(op.getUopid()))
                .findFirst()
                .orElse(null);
    }

    // ── Route input configuration ──────────────────────────────────────

    private void configureRouteInputs() {
        configureOpCombo(fromField, t("position.from"), t("position.from.help"));
        configureOpCombo(toField, t("position.to"), t("position.to.help"));
        departureAnchorField.setLabel(t("timetable.route.departureAnchor"));
        departureAnchorField.setHelperText(t("timetable.route.departureAnchor.help"));
        arrivalAnchorField.setLabel(t("timetable.route.arrivalAnchor"));
        arrivalAnchorField.setHelperText(t("timetable.route.arrivalAnchor.help"));
        viaList.setPadding(false);
        viaList.setSpacing(true);
        viaList.setWidthFull();
        routeSummary
                .getStyle()
                .set("display", "block")
                .set("font-size", "12px")
                .set("font-family", "'JetBrains Mono', monospace")
                .set("color", "var(--rom-text-secondary)");
        routeError
                .getStyle()
                .set("display", "block")
                .set("font-size", "12px")
                .set("font-weight", "600")
                .set("color", "var(--rom-status-danger)");
        routeMap.getElement().getStyle().set("height", "100%");
    }

    private void configureOpCombo(ComboBox<OperationalPoint> combo, String label, String helper) {
        combo.setLabel(label);
        combo.setItems(availableOps);
        combo.setItemLabelGenerator(TimetableFormatUtils::opLabel);
        combo.setWidthFull();
        combo.setClearButtonVisible(true);
        combo.setHelperText(helper);
    }

    // ── Via point editors ──────────────────────────────────────────────

    private void addViaEditor(OperationalPoint point, boolean halt, String activityCode) {
        ViaPointEditor ed = new ViaPointEditor();
        ed.pointField.setItems(availableOps);
        ed.pointField.setItemLabelGenerator(TimetableFormatUtils::opLabel);
        ed.pointField.setWidthFull();
        ed.pointField.setClearButtonVisible(true);
        ed.pointField.setValue(point);
        ed.haltField.setLabel(t("timetable.route.viaStop"));
        ed.haltField.setValue(halt);
        ed.haltField.addValueChangeListener(e -> ed.updateActivityVisibility());
        ed.activityField.setItems(activityOptions);
        ed.activityField.setItemLabelGenerator(this::activityOptionLabel);
        ed.activityField.setWidthFull();
        ed.activityField.setLabel(t("timetable.editor.activity"));
        ed.activityField.setValue(findActivityOption(activityCode).orElse(null));
        ed.removeButton.setText(t("common.delete"));
        ed.removeButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        ed.removeButton.getStyle().set("color", "var(--rom-status-danger)");
        ed.removeButton.addClickListener(
                e -> {
                    viaEditors.remove(ed);
                    viaList.remove(ed.container);
                    renumberViaEditors();
                });
        HorizontalLayout actions = new HorizontalLayout(ed.haltField, ed.removeButton);
        actions.setWidthFull();
        actions.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        actions.setAlignItems(FlexComponent.Alignment.CENTER);
        ed.container
                .getStyle()
                .set("border", "1px solid var(--rom-border)")
                .set("border-radius", "6px")
                .set("padding", "10px")
                .set("background", "rgba(148,163,184,0.04)")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "10px");
        ed.container.add(ed.label, ed.pointField, actions, ed.activityField);
        ed.updateActivityVisibility();
        viaEditors.add(ed);
        viaList.add(ed.container);
        renumberViaEditors();
    }

    private void clearViaEditors() {
        viaEditors.clear();
        viaList.removeAll();
    }

    private void renumberViaEditors() {
        for (int i = 0; i < viaEditors.size(); i++) {
            viaEditors.get(i).label.setText(t("timetable.route.viaPoint", i + 1));
        }
    }

    // ── Route calculation ──────────────────────────────────────────────

    /**
     * Validates route inputs (origin, destination, anchors, via points). Sets error messages on
     * fields and returns null if validation fails, or the ordered waypoint list on success.
     */
    private List<OperationalPoint> validateRouteInputs(LocalTime depAnchor, LocalTime arrAnchor) {
        routeError.setText("");
        fromField.setInvalid(fromField.getValue() == null);
        toField.setInvalid(toField.getValue() == null);
        if (fromField.getValue() == null) {
            fromField.setErrorMessage(t("timetable.route.pointsRequired"));
            fromField.focus();
            routeError.setText(t("timetable.route.pointsRequired"));
            return null;
        }
        if (toField.getValue() == null) {
            toField.setErrorMessage(t("timetable.route.pointsRequired"));
            toField.focus();
            routeError.setText(t("timetable.route.pointsRequired"));
            return null;
        }
        if (fromField.getValue().getUopid().equals(toField.getValue().getUopid())) {
            routeError.setText(t("timetable.route.samePoint"));
            return null;
        }
        if (depAnchor != null && arrAnchor != null) {
            routeError.setText(t("timetable.route.anchorExclusive"));
            return null;
        }
        List<OperationalPoint> waypoints = new ArrayList<>();
        waypoints.add(fromField.getValue());
        for (ViaPointEditor viaEd : viaEditors) {
            if (viaEd.pointField.getValue() == null) {
                routeError.setText(t("timetable.route.viaRequired"));
                return null;
            }
            if (Boolean.TRUE.equals(viaEd.haltField.getValue())
                    && viaEd.activityField.getValue() == null) {
                routeError.setText(t("timetable.route.viaActivityRequired"));
                return null;
            }
            waypoints.add(viaEd.pointField.getValue());
        }
        waypoints.add(toField.getValue());
        return waypoints;
    }

    /**
     * Validates inputs and executes the route calculation. On success, updates the map, summary,
     * and notifies the callback if requested.
     */
    private List<TimetableRowData> doCalculateRoute(
            LocalTime depAnchor, LocalTime arrAnchor, boolean notifyCallback) {
        List<OperationalPoint> waypoints = validateRouteInputs(depAnchor, arrAnchor);
        if (waypoints == null) {
            return null;
        }
        return executeRouteCalculation(waypoints, depAnchor, arrAnchor, notifyCallback);
    }

    /** Performs the actual route calculation against the routing service and updates the UI. */
    private List<TimetableRowData> executeRouteCalculation(
            List<OperationalPoint> waypoints,
            LocalTime depAnchor,
            LocalTime arrAnchor,
            boolean notifyCallback) {
        try {
            currentRoute = routingService.calculateRoute(waypoints);
            List<TimetableRowData> rows =
                    routingService.estimateRows(currentRoute, depAnchor, arrAnchor);
            applyViaPreferences(rows);
            routeMap.setRoute(currentRoute.points());
            routeSummary.setText(routeSummaryText(rows, currentRoute));
            buildRoutePreview(rows);
            routeError.setText("");
            markCalcButtonSuccess();
            if (notifyCallback && onRouteCalculated != null) {
                onRouteCalculated.accept(new RouteCalculationResult(currentRoute, rows));
            }
            return rows;
        } catch (RuntimeException ex) {
            routeMap.clearRoute();
            routeSummary.setText(t("timetable.route.empty"));
            routeError.setText(t("timetable.route.unresolvedSegment", ex.getMessage()));
            return null;
        }
    }

    private void applyViaPreferences(List<TimetableRowData> rows) {
        List<TimetableRowData> viaRows =
                rows.stream().filter(r -> r.getRoutePointRole() == RoutePointRole.VIA).toList();
        for (int i = 0; i < Math.min(viaEditors.size(), viaRows.size()); i++) {
            ViaPointEditor ed = viaEditors.get(i);
            TimetableRowData row = viaRows.get(i);
            if (!Boolean.TRUE.equals(ed.haltField.getValue())) continue;
            row.setHalt(true);
            row.setActivityCode(
                    ed.activityField.getValue() != null
                            ? ed.activityField.getValue().code()
                            : null);
            if (row != rows.getFirst()) {
                row.setArrivalMode(TimeConstraintMode.EXACT);
                row.setArrivalExact(
                        firstNonBlank(row.getArrivalExact(), row.getEstimatedArrival()));
            }
            if (row != rows.getLast()) {
                row.setDepartureMode(TimeConstraintMode.EXACT);
                row.setDepartureExact(
                        firstNonBlank(row.getDepartureExact(), row.getEstimatedDeparture()));
            }
        }
    }

    // ── Route preview ──────────────────────────────────────────────────

    private void buildRoutePreview(List<TimetableRowData> rows) {
        routePreview.removeAll();
        if (rows == null || rows.isEmpty()) {
            routePreview.setVisible(false);
            return;
        }
        routePreview.setVisible(true);
        routePreview
                .getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "0")
                .set("padding", "12px 0");
        Span header = new Span(t("timetable.route.preview"));
        header.getStyle()
                .set("font-weight", "600")
                .set("font-size", "11px")
                .set("color", "var(--rom-text-primary)")
                .set("margin-bottom", "6px");
        routePreview.add(header);
        for (TimetableRowData row : rows) {
            Div line = new Div();
            line.getStyle()
                    .set("display", "flex")
                    .set("align-items", "center")
                    .set("gap", "8px")
                    .set("padding", "2px 0")
                    .set("font-size", "11px");
            Span dot = new Span("\u25cf");
            dot.getStyle().set("color", getDotColor(row)).set("font-size", "8px");
            Span name = new Span(row.getName());
            name.getStyle().set("color", "var(--rom-text-primary)");
            Span km = new Span(distanceLabel(row.getDistanceFromStartMeters()));
            km.getStyle()
                    .set("color", "var(--rom-text-muted)")
                    .set("font-family", "'JetBrains Mono', monospace")
                    .set("font-size", "10px");
            line.add(dot, name, km);
            routePreview.add(line);
        }
    }

    private String getDotColor(TimetableRowData row) {
        if (row.getRoutePointRole() == null) {
            return "var(--rom-text-muted)";
        }
        return switch (row.getRoutePointRole()) {
            case ORIGIN, DESTINATION -> "var(--rom-accent)";
            case VIA -> "var(--rom-status-info, #3b82f6)";
            default -> "var(--rom-text-muted)";
        };
    }

    // ── Formatting helpers ─────────────────────────────────────────────

    private String activityOptionLabel(TimetableActivityOption o) {
        return o.code() + " \u00b7 " + o.label();
    }

    private String routeSummaryText(List<TimetableRowData> rows, TimetableRouteResult route) {
        if (rows == null || rows.isEmpty()) return t("timetable.route.empty");
        return t(
                "timetable.route.summary",
                rows.size(),
                distanceLabel(route.totalLengthMeters()),
                timeOrDash(rows.getFirst().getEstimatedDeparture()),
                timeOrDash(rows.getLast().getEstimatedArrival()));
    }

    private TimePicker buildTimePicker() {
        TimePicker picker = new TimePicker();
        picker.setStep(Duration.ofMinutes(1));
        picker.setAllowedCharPattern("[0-9:]");
        picker.setPlaceholder("HH:mm");
        picker.setLocale(resolveLocale());
        picker.setWidthFull();
        return picker;
    }

    private Locale resolveLocale() {
        return getLocale() != null ? getLocale() : Locale.GERMANY;
    }

    private Optional<TimetableActivityOption> findActivityOption(String code) {
        if (code == null || code.isBlank()) return Optional.empty();
        return activityOptions.stream().filter(o -> code.equals(o.code())).findFirst();
    }

    private String t(String key, Object... params) {
        return getTranslation(key, params);
    }

    private static final class ViaPointEditor {
        final Div container = new Div();
        final Span label = new Span();
        final ComboBox<OperationalPoint> pointField = new ComboBox<>();
        final Checkbox haltField = new Checkbox();
        final ComboBox<TimetableActivityOption> activityField = new ComboBox<>();
        final Button removeButton = new Button();

        void updateActivityVisibility() {
            activityField.setVisible(Boolean.TRUE.equals(haltField.getValue()));
        }
    }
}
