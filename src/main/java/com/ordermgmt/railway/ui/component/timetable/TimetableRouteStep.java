package com.ordermgmt.railway.ui.component.timetable;

import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.createCard;
import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.helperSpan;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import org.springframework.data.domain.PageRequest;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.timepicker.TimePicker;

import com.ordermgmt.railway.domain.infrastructure.model.OperationalPoint;
import com.ordermgmt.railway.domain.infrastructure.repository.OperationalPointRepository;
import com.ordermgmt.railway.domain.timetable.model.TimetableActivityOption;
import com.ordermgmt.railway.domain.timetable.model.TimetableRouteResult;
import com.ordermgmt.railway.domain.timetable.model.TimetableRowData;
import com.ordermgmt.railway.domain.timetable.service.TimetableRoutingService;

/**
 * Route-definition step for the timetable builder.
 *
 * <p>Allows the user to select origin, destination, and optional via points, then calculates a
 * route with estimated travel times. The result is displayed on an interactive map. The via-point
 * editors live in {@link ViaPointList}; the point-list preview + summary in {@link
 * TimetableRoutePreview}.
 */
public class TimetableRouteStep extends Div {

    // ── Data records ───────────────────────────────────────────────────

    /** Callback payload delivered after a successful route calculation. */
    public record RouteCalculationResult(TimetableRouteResult route, List<TimetableRowData> rows) {}

    /** Prefill data for a single via point. */
    public record ViaData(OperationalPoint point, boolean halt, String activityCode) {}

    // ── Route Form ──────────────────────────────────────────────────────

    /** Max background OPs pushed to the map per viewport — capped so a wide zoom stays cheap. */
    private static final int MAX_BACKGROUND_OPS = 600;

    private final OperationalPointRepository opRepo;
    private final TimetableRoutingService routingService;

    private final ComboBox<OperationalPoint> fromField = new ComboBox<>();
    private final ComboBox<OperationalPoint> toField = new ComboBox<>();
    private final ViaPointList viaPointList;
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
            OperationalPointRepository opRepo,
            List<TimetableActivityOption> activityOptions,
            TimetableRoutingService timetableRoutingService) {
        this.opRepo = opRepo;
        this.routingService = timetableRoutingService;
        this.viaPointList = new ViaPointList(opRepo, activityOptions, this);
        configureRouteInputs();
    }

    /** Builds and returns the SplitLayout that forms this step's content. */
    public Component createContent() {
        Button addViaBtn = new Button(t("timetable.route.addVia"), VaadinIcon.PLUS.create());
        addViaBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
        addViaBtn.addClickListener(e -> viaPointList.addEditor(null, false, null));

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
                        viaPointList,
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
        viaPointList.clear();
        if (vias != null) {
            vias.forEach(
                    via -> viaPointList.addEditor(via.point(), via.halt(), via.activityCode()));
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

    public List<ViaData> getViaValues() {
        return viaPointList.getValues();
    }

    public void setOnRouteCalculated(Consumer<RouteCalculationResult> callback) {
        this.onRouteCalculated = callback;
    }

    public void setOnRouteDirty(Runnable callback) {
        this.onRouteDirty = callback;
    }

    /** Registers a callback for when an operational point is clicked on the map. */
    public void addOpSelectedListener(Consumer<String> callback) {
        routeMap.addOpSelectedListener(callback);
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
        viaPointList.reverse();
    }

    private void markCalcButtonSuccess() {
        if (calcButton == null) {
            return;
        }
        calcButton.setText(t("timetable.route.calculated"));
        calcButton.setIcon(VaadinIcon.CHECK.create());
        calcButton
                .getStyle()
                .set("background", "var(--rom-status-success, #22c55e)")
                .set("color", "#fff");
    }

    private void resetCalcButtonAppearance() {
        if (calcButton == null) {
            return;
        }
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
        if (uopid == null) {
            return null;
        }
        return opRepo.findByUopid(uopid).orElse(null);
    }

    // ── Route input configuration ──────────────────────────────────────

    private void configureRouteInputs() {
        configureOpCombo(fromField, t("position.from"), t("position.from.help"));
        configureOpCombo(toField, t("position.to"), t("position.to.help"));
        departureAnchorField.setLabel(t("timetable.route.departureAnchor"));
        departureAnchorField.setHelperText(t("timetable.route.departureAnchor.help"));
        arrivalAnchorField.setLabel(t("timetable.route.arrivalAnchor"));
        arrivalAnchorField.setHelperText(t("timetable.route.arrivalAnchor.help"));
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

        // Register map listeners ONCE here — createContent() re-runs on every step switch, so
        // registering there would stack duplicate viewport fetches and click handlers per visit.
        routeMap.addBoundsChangedListener(
                (south, west, north, east, zoom) ->
                        routeMap.setBackgroundOperationalPoints(
                                opRepo.findByLatitudeBetweenAndLongitudeBetween(
                                        south,
                                        north,
                                        west,
                                        east,
                                        PageRequest.of(0, MAX_BACKGROUND_OPS))));
        routeMap.addOpSelectedListener(
                uopid -> {
                    OperationalPoint operationalPoint = findOpByUopid(uopid);
                    if (operationalPoint == null) {
                        return;
                    }
                    if (fromField.getValue() == null) {
                        fromField.setValue(operationalPoint);
                    } else if (toField.getValue() == null) {
                        toField.setValue(operationalPoint);
                    } else {
                        viaPointList.addEditor(operationalPoint, false, null);
                    }
                });
    }

    private void configureOpCombo(ComboBox<OperationalPoint> combo, String label, String helper) {
        combo.setLabel(label);
        com.ordermgmt.railway.ui.component.OperationalPointComboBox.bindLazySearch(combo, opRepo);
        combo.setItemLabelGenerator(TimetableFormatUtils::opLabel);
        combo.setWidthFull();
        combo.setClearButtonVisible(true);
        combo.setHelperText(helper);
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
        ViaPointList.ViaValidation via = viaPointList.validatePoints();
        if (via.errorKey() != null) {
            routeError.setText(t(via.errorKey()));
            return null;
        }
        List<OperationalPoint> waypoints = new ArrayList<>();
        waypoints.add(fromField.getValue());
        waypoints.addAll(via.points());
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
            viaPointList.applyPreferences(rows);
            routeMap.setRoute(currentRoute.points());
            routeSummary.setText(TimetableRoutePreview.summary(rows, currentRoute, this));
            TimetableRoutePreview.render(routePreview, rows, this);
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

    // ── Formatting helpers ─────────────────────────────────────────────

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

    private String t(String key, Object... params) {
        return getTranslation(key, params);
    }
}
