package com.ordermgmt.railway.ui.component.timetable;

import static com.ordermgmt.railway.ui.component.timetable.TimetableFormatUtils.activityOptionLabel;

import java.util.List;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import com.ordermgmt.railway.domain.infrastructure.model.OperationalPoint;
import com.ordermgmt.railway.domain.timetable.model.TimetableActivityOption;

/**
 * Inline form for adding a new stop to the timetable grid. Displayed below the grid when the user
 * clicks "+" on a row.
 */
class AddStopForm extends Div {

    private final ComboBox<OperationalPoint> pointCombo = new ComboBox<>();
    private final ComboBox<TimetableActivityOption> activityCombo = new ComboBox<>();
    private final Button addButton = new Button();
    private final Button cancelButton = new Button();

    /** Index after which the new stop will be inserted. */
    private int insertAfterIndex = -1;

    /**
     * @param operationalPoints selectable operational points
     * @param activityOptions selectable TTT activities
     * @param onAdd callback receiving (insertAfterIndex, selectedPoint, activityCode)
     */
    AddStopForm(
            List<OperationalPoint> operationalPoints,
            List<TimetableActivityOption> activityOptions,
            AddStopCallback onAdd) {

        setWidthFull();
        setVisible(false);
        getStyle()
                .set("background", "var(--rom-bg-card)")
                .set("border", "1px solid var(--rom-accent)")
                .set("border-radius", "6px")
                .set("padding", "12px 16px")
                .set("box-sizing", "border-box")
                .set("margin-top", "6px");

        Span title = new Span();
        title.getStyle()
                .set("font-weight", "600")
                .set("font-size", "13px")
                .set("color", "var(--rom-text-primary)")
                .set("display", "block")
                .set("margin-bottom", "8px");

        pointCombo.setItems(operationalPoints);
        pointCombo.setItemLabelGenerator(
                op -> op.getCountry() + " \u00b7 " + op.getName() + " (" + op.getUopid() + ")");
        pointCombo.setWidthFull();
        pointCombo.setClearButtonVisible(true);
        pointCombo.setAllowCustomValue(false);

        activityCombo.setItems(activityOptions);
        activityCombo.setItemLabelGenerator(opt -> activityOptionLabel(opt));
        activityCombo.setWidthFull();
        activityCombo.setRequired(true);
        activityCombo.setClearButtonVisible(true);

        addButton.setIcon(VaadinIcon.CHECK.create());
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        addButton
                .getStyle()
                .set("background", "var(--rom-accent)")
                .set("color", "var(--rom-bg-primary)");
        addButton.addClickListener(
                e -> {
                    if (pointCombo.getValue() == null || activityCombo.getValue() == null) {
                        return;
                    }
                    onAdd.accept(
                            insertAfterIndex,
                            pointCombo.getValue(),
                            activityCombo.getValue().code());
                    hide();
                });

        cancelButton.setIcon(VaadinIcon.CLOSE_SMALL.create());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        cancelButton.addClickListener(e -> hide());

        HorizontalLayout fields = new HorizontalLayout(pointCombo, activityCombo);
        fields.setWidthFull();
        fields.expand(pointCombo);

        HorizontalLayout actions = new HorizontalLayout(addButton, cancelButton);
        actions.setAlignItems(FlexComponent.Alignment.CENTER);

        // We set i18n labels lazily in show() since getTranslation needs attach
        add(title, fields, actions);
    }

    /** Shows the form for inserting a stop after the given row index. */
    void show(int afterIndex, String rowName) {
        this.insertAfterIndex = afterIndex;
        pointCombo.setValue(null);
        activityCombo.setValue(null);

        // Set labels using i18n (component must be attached)
        pointCombo.setLabel(getTranslation("timetable.stop.selectPoint"));
        activityCombo.setLabel(getTranslation("timetable.editor.activity"));
        addButton.setText(getTranslation("timetable.stop.add"));
        cancelButton.setText(getTranslation("common.cancel"));

        Span title = (Span) getComponentAt(0);
        title.setText(
                getTranslation("timetable.stop.add")
                        + " — "
                        + getTranslation("timetable.editor.context", rowName, "..."));

        setVisible(true);
        pointCombo.focus();
    }

    void hide() {
        setVisible(false);
        insertAfterIndex = -1;
    }

    /** Callback for when a stop is confirmed. */
    @FunctionalInterface
    interface AddStopCallback {
        void accept(int insertAfterIndex, OperationalPoint point, String activityCode);
    }
}
