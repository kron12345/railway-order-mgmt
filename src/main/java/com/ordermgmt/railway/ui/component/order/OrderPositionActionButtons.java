package com.ordermgmt.railway.ui.component.order;

import java.util.function.BiFunction;
import java.util.function.Consumer;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import com.ordermgmt.railway.domain.order.model.OrderPosition;

/**
 * Builds the action button row for an {@link OrderPositionRow}: calendar toggle, view, history, and
 * the edit/delete buttons (the last two only when editable). Each behaviour is supplied as a
 * callback so this stays a stateless factory; the row owns the state the callbacks act on.
 */
final class OrderPositionActionButtons {

    private OrderPositionActionButtons() {}

    static HorizontalLayout build(
            BiFunction<String, Object[], String> t,
            OrderPosition position,
            boolean editable,
            long purchaseCount,
            Runnable onToggleCalendar,
            Runnable onView,
            Runnable onHistory,
            Consumer<OrderPosition> onEdit,
            Consumer<OrderPosition> onDelete) {
        HorizontalLayout actionRow =
                new HorizontalLayout(
                        calendarButton(t, purchaseCount, onToggleCalendar),
                        viewButton(t, onView),
                        historyButton(t, onHistory),
                        editButton(t, position, editable, onEdit),
                        deleteButton(t, position, editable, onDelete));
        actionRow.setSpacing(true);
        actionRow.setAlignItems(FlexComponent.Alignment.START);
        return actionRow;
    }

    private static Button calendarButton(
            BiFunction<String, Object[], String> t, long purchaseCount, Runnable onToggleCalendar) {
        String calendarLabel = t.apply("position.action.calendar", new Object[0]);
        Button button =
                new Button(
                        purchaseCount > 0 ? purchaseCount + " " + calendarLabel : calendarLabel,
                        VaadinIcon.CALENDAR.create());
        button.addThemeVariants(ButtonVariant.LUMO_SMALL);
        button.addClassName("op-action");
        if (purchaseCount > 0) {
            button.addClassName("op-action--primary");
        }
        button.addClickListener(event -> onToggleCalendar.run());
        return button;
    }

    private static Button viewButton(BiFunction<String, Object[], String> t, Runnable onView) {
        Button button =
                new Button(t.apply("position.action.view", new Object[0]), VaadinIcon.EYE.create());
        button.addThemeVariants(ButtonVariant.LUMO_SMALL);
        button.addClassNames("op-action", "op-action--info");
        button.addClickListener(event -> onView.run());
        return button;
    }

    private static Button historyButton(
            BiFunction<String, Object[], String> t, Runnable onHistory) {
        Button button =
                new Button(t.apply("audit.button", new Object[0]), VaadinIcon.CLOCK.create());
        button.addThemeVariants(ButtonVariant.LUMO_SMALL);
        button.addClassName("op-action");
        button.addClickListener(event -> onHistory.run());
        return button;
    }

    private static Button editButton(
            BiFunction<String, Object[], String> t,
            OrderPosition position,
            boolean editable,
            Consumer<OrderPosition> onEdit) {
        Button button = new Button(t.apply("common.edit", new Object[0]), VaadinIcon.EDIT.create());
        button.addThemeVariants(ButtonVariant.LUMO_SMALL);
        button.addClassName("op-action");
        button.addClickListener(event -> onEdit.accept(position));
        button.setVisible(editable);
        return button;
    }

    private static Button deleteButton(
            BiFunction<String, Object[], String> t,
            OrderPosition position,
            boolean editable,
            Consumer<OrderPosition> onDelete) {
        Button button = new Button(VaadinIcon.TRASH.create());
        button.addThemeVariants(ButtonVariant.LUMO_SMALL);
        button.addClassNames("op-action", "op-action--icon", "op-action--danger");
        button.setTooltipText(t.apply("common.delete", new Object[0]));
        button.addClickListener(event -> onDelete.accept(position));
        button.setVisible(editable);
        return button;
    }
}
