package com.ordermgmt.railway.ui.component.vehicleplanning;

import java.util.List;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;

import com.ordermgmt.railway.domain.vehicleplanning.model.Conflict;

/** Bottom panel displaying detected scheduling conflicts for the current rotation set. */
public class ConflictPanel extends Div {

    private static final String[] DAY_LABELS = {
        "", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"
    };

    private final Div conflictList;
    private final Span statusLabel;

    public ConflictPanel() {
        getStyle()
                .set("border-top", "1px solid var(--lumo-contrast-10pct)")
                .set("background", "var(--lumo-contrast-5pct)")
                .set("padding", "var(--lumo-space-s)")
                .set("max-height", "200px")
                .set("overflow-y", "auto");

        statusLabel = new Span();
        statusLabel
                .getStyle()
                .set("font-weight", "600")
                .set("font-size", "var(--lumo-font-size-s)")
                .set("display", "block")
                .set("margin-bottom", "var(--lumo-space-xs)");

        conflictList = new Div();
        add(statusLabel, conflictList);
        showEmpty();
    }

    /** Refreshes the panel with the given list of conflicts. */
    public void setConflicts(List<Conflict> conflicts) {
        conflictList.removeAll();

        if (conflicts == null || conflicts.isEmpty()) {
            showEmpty();
            return;
        }

        statusLabel.setText(conflicts.size() + " conflict(s) detected");
        statusLabel.getStyle().set("color", "var(--lumo-error-text-color)");

        for (Conflict c : conflicts) {
            conflictList.add(buildConflictRow(c));
        }
    }

    private void showEmpty() {
        statusLabel.setText("No conflicts");
        statusLabel.getStyle().set("color", "var(--lumo-success-text-color)");
        conflictList.removeAll();
    }

    private Div buildConflictRow(Conflict conflict) {
        Div row = new Div();
        row.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("gap", "var(--lumo-space-xs)")
                .set("padding", "var(--lumo-space-xs) 0")
                .set("font-size", "var(--lumo-font-size-s)")
                .set("border-bottom", "1px solid var(--lumo-contrast-5pct)");

        Icon icon;
        if (conflict.severity() == Conflict.Severity.ERROR) {
            icon = VaadinIcon.EXCLAMATION_CIRCLE.create();
            icon.getStyle().set("color", "var(--lumo-error-text-color)");
        } else {
            icon = VaadinIcon.WARNING.create();
            icon.getStyle().set("color", "var(--lumo-warning-text-color, orange)");
        }
        icon.setSize("16px");

        String dayLabel =
                conflict.dayOfWeek() >= 1 && conflict.dayOfWeek() <= 7
                        ? DAY_LABELS[conflict.dayOfWeek()]
                        : "?";

        Span vehicle = new Span(conflict.vehicleLabel());
        vehicle.getStyle().set("font-weight", "500");

        Span day = new Span("[" + dayLabel + "]");
        day.getStyle().set("color", "var(--lumo-secondary-text-color)");

        Span desc = new Span(conflict.description());

        row.add(icon, vehicle, day, desc);
        return row;
    }
}
