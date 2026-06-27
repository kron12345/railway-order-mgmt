package com.ordermgmt.railway.ui.component.order;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.select.SelectVariant;

import com.ordermgmt.railway.domain.order.model.PositionStatus;
import com.ordermgmt.railway.domain.order.service.OrderService;

/**
 * Bulk-action bar for the position panel: collects the selected positions and applies one internal
 * (Bearbeitungs-)status to all of them in a single transaction. Hidden until something is selected.
 */
class PositionBulkBar extends HorizontalLayout {

    private final OrderService orderService;
    private final BiFunction<String, Object[], String> t;
    private final Runnable onApplied;
    private final Set<UUID> selected = new LinkedHashSet<>();
    private final Span countLabel = new Span();
    private final Select<PositionStatus> statusSelect = new Select<>();

    PositionBulkBar(
            OrderService orderService, BiFunction<String, Object[], String> t, Runnable onApplied) {
        this.orderService = orderService;
        this.t = t;
        this.onApplied = onApplied;

        statusSelect.setItems(PositionStatus.values());
        statusSelect.setItemLabelGenerator(
                s -> t.apply("position.status." + s.name(), new Object[0]));
        statusSelect.setPlaceholder(t.apply("bulk.status.placeholder", new Object[0]));
        statusSelect.addThemeVariants(SelectVariant.LUMO_SMALL);

        Button apply = new Button(t.apply("bulk.apply", new Object[0]), VaadinIcon.CHECK.create());
        apply.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
        apply.addClickListener(e -> applyStatus());

        add(countLabel, statusSelect, apply);
        setAlignItems(FlexComponent.Alignment.CENTER);
        setSpacing(true);
        getStyle()
                .set("margin", "4px 0 8px 0")
                .set("padding", "6px 10px")
                .set("background", "var(--rom-bg-card)")
                .set("border", "1px solid var(--rom-border)")
                .set("border-radius", "6px");
        setVisible(false);
    }

    /** Clears the selection and hides the bar (called at the start of each refresh). */
    void reset() {
        selected.clear();
        update();
    }

    /** Toggles one position's membership; shows/hides the bar accordingly. */
    void toggle(UUID positionId, boolean isSelected) {
        if (isSelected) {
            selected.add(positionId);
        } else {
            selected.remove(positionId);
        }
        update();
    }

    private void update() {
        boolean hasSelection = !selected.isEmpty();
        setVisible(hasSelection);
        if (!hasSelection) {
            statusSelect.clear(); // don't carry a stale status into the next selection
        }
        countLabel.setText(t.apply("bulk.selected", new Object[] {selected.size()}));
    }

    private void applyStatus() {
        PositionStatus status = statusSelect.getValue();
        if (status == null || selected.isEmpty()) {
            return;
        }
        int updatedCount =
                orderService.setPositionInternalStatusBulk(new HashSet<>(selected), status);
        Notification.show(
                        t.apply("bulk.done", new Object[] {updatedCount}),
                        2500,
                        Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        selected.clear();
        onApplied.run();
    }
}
