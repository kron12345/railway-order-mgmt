package com.ordermgmt.railway.ui.component.timetable;

import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.firstNonBlank;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import com.ordermgmt.railway.domain.infrastructure.model.OperationalPoint;
import com.ordermgmt.railway.domain.infrastructure.repository.OperationalPointRepository;
import com.ordermgmt.railway.domain.timetable.model.RoutePointRole;
import com.ordermgmt.railway.domain.timetable.model.TimeConstraintMode;
import com.ordermgmt.railway.domain.timetable.model.TimetableActivityOption;
import com.ordermgmt.railway.domain.timetable.model.TimetableRowData;
import com.ordermgmt.railway.ui.component.OperationalPointComboBox;

/**
 * The editable list of via points in {@link TimetableRouteStep}: each row is an operational-point
 * combo + optional halt with a required activity. <em>Is</em> the layout that holds the editors, so
 * the route step adds it directly. Owns add/remove/renumber/reverse, value extraction, validation,
 * and the application of halt/activity preferences onto computed rows — extracted so the route step
 * stays focused on the form, calculation and map.
 */
class ViaPointList extends VerticalLayout {

    /** Validation outcome: the ordered via points, or an i18n error key when invalid. */
    record ViaValidation(List<OperationalPoint> points, String errorKey) {}

    private final OperationalPointRepository opRepo;
    private final List<TimetableActivityOption> activityOptions;
    private final Component i18n;
    private final List<ViaPointEditor> editors = new ArrayList<>();

    ViaPointList(
            OperationalPointRepository opRepo,
            List<TimetableActivityOption> activityOptions,
            Component i18n) {
        this.opRepo = opRepo;
        this.activityOptions = activityOptions;
        this.i18n = i18n;
        setPadding(false);
        setSpacing(true);
        setWidthFull();
    }

    /** Adds a via-point editor, optionally prefilled with point/halt/activity. */
    void addEditor(OperationalPoint point, boolean halt, String activityCode) {
        ViaPointEditor editor = new ViaPointEditor();
        OperationalPointComboBox.bindLazySearch(editor.pointField, opRepo);
        editor.pointField.setItemLabelGenerator(TimetableFormatUtils::opLabel);
        editor.pointField.setWidthFull();
        editor.pointField.setClearButtonVisible(true);
        editor.pointField.setValue(point);
        editor.haltField.setLabel(t("timetable.route.viaStop"));
        editor.haltField.setValue(halt);
        editor.haltField.addValueChangeListener(e -> editor.updateActivityVisibility());
        editor.activityField.setItems(activityOptions);
        editor.activityField.setItemLabelGenerator(this::activityOptionLabel);
        editor.activityField.setWidthFull();
        editor.activityField.setLabel(t("timetable.editor.activity"));
        editor.activityField.setValue(findActivityOption(activityCode).orElse(null));
        editor.removeButton.setText(t("common.delete"));
        editor.removeButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        editor.removeButton.getStyle().set("color", "var(--rom-status-danger)");
        editor.removeButton.addClickListener(
                e -> {
                    editors.remove(editor);
                    remove(editor.container);
                    renumber();
                });
        HorizontalLayout actions = new HorizontalLayout(editor.haltField, editor.removeButton);
        actions.setWidthFull();
        actions.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        actions.setAlignItems(FlexComponent.Alignment.CENTER);
        editor.container
                .getStyle()
                .set("border", "1px solid var(--rom-border)")
                .set("border-radius", "6px")
                .set("padding", "10px")
                .set("background", "rgba(148,163,184,0.04)")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "10px");
        editor.container.add(editor.label, editor.pointField, actions, editor.activityField);
        editor.updateActivityVisibility();
        editors.add(editor);
        add(editor.container);
        renumber();
    }

    /** Removes all editors. */
    void clear() {
        editors.clear();
        removeAll();
    }

    /** Reverses the via order in place (used when the route's origin/destination are swapped). */
    void reverse() {
        List<TimetableRouteStep.ViaData> reversed = new ArrayList<>();
        for (int i = editors.size() - 1; i >= 0; i--) {
            reversed.add(toViaData(editors.get(i)));
        }
        clear();
        for (TimetableRouteStep.ViaData via : reversed) {
            addEditor(via.point(), via.halt(), via.activityCode());
        }
    }

    /** Current via points as data records (point + halt + activity code). */
    List<TimetableRouteStep.ViaData> getValues() {
        List<TimetableRouteStep.ViaData> result = new ArrayList<>();
        for (ViaPointEditor editor : editors) {
            result.add(toViaData(editor));
        }
        return result;
    }

    /**
     * Validates the via points: each must have a point set, and a halt must have an activity.
     * Returns the ordered points on success, or the failing i18n error key.
     */
    ViaValidation validatePoints() {
        List<OperationalPoint> points = new ArrayList<>();
        for (ViaPointEditor editor : editors) {
            if (editor.pointField.getValue() == null) {
                return new ViaValidation(null, "timetable.route.viaRequired");
            }
            if (Boolean.TRUE.equals(editor.haltField.getValue())
                    && editor.activityField.getValue() == null) {
                return new ViaValidation(null, "timetable.route.viaActivityRequired");
            }
            points.add(editor.pointField.getValue());
        }
        return new ViaValidation(points, null);
    }

    /**
     * Applies each halting via editor's preferences onto the matching VIA rows: marks them as halts
     * with the chosen activity and pins exact arrival/departure (except at the route endpoints).
     */
    void applyPreferences(List<TimetableRowData> rows) {
        List<TimetableRowData> viaRows =
                rows.stream().filter(r -> r.getRoutePointRole() == RoutePointRole.VIA).toList();
        for (int i = 0; i < Math.min(editors.size(), viaRows.size()); i++) {
            ViaPointEditor editor = editors.get(i);
            TimetableRowData row = viaRows.get(i);
            if (!Boolean.TRUE.equals(editor.haltField.getValue())) {
                continue;
            }
            row.setHalt(true);
            row.setActivityCode(
                    editor.activityField.getValue() != null
                            ? editor.activityField.getValue().code()
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

    private TimetableRouteStep.ViaData toViaData(ViaPointEditor editor) {
        return new TimetableRouteStep.ViaData(
                editor.pointField.getValue(),
                Boolean.TRUE.equals(editor.haltField.getValue()),
                editor.activityField.getValue() != null
                        ? editor.activityField.getValue().code()
                        : null);
    }

    private void renumber() {
        for (int i = 0; i < editors.size(); i++) {
            editors.get(i).label.setText(t("timetable.route.viaPoint", i + 1));
        }
    }

    private String activityOptionLabel(TimetableActivityOption o) {
        return o.code() + " · " + o.label();
    }

    private Optional<TimetableActivityOption> findActivityOption(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return activityOptions.stream().filter(option -> code.equals(option.code())).findFirst();
    }

    private String t(String key, Object... params) {
        return i18n.getTranslation(key, params);
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
