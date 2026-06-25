package com.ordermgmt.railway.ui.component.order;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;

import com.ordermgmt.railway.domain.order.model.Weekdays;
import com.ordermgmt.railway.domain.order.service.OrderService;

/** Dialog to create a new expression (Ausprägung = OTN × Verkehrstage) under a train identity. */
class ExpressionDialog extends Dialog {

    ExpressionDialog(
            BiFunction<String, Object[], String> t,
            Function<OrderService.ExpressionDraft, Boolean> onSave) {
        setHeaderTitle(t.apply("expression.add.title", new Object[0]));

        TextField name = new TextField(t.apply("expression.field.name", new Object[0]));
        name.setWidthFull();
        TextField otn = new TextField(t.apply("expression.field.otn", new Object[0]));
        TextField from = new TextField(t.apply("expression.field.from", new Object[0]));
        TextField to = new TextField(t.apply("expression.field.to", new Object[0]));
        DatePicker start = new DatePicker(t.apply("expression.field.start", new Object[0]));
        DatePicker end = new DatePicker(t.apply("expression.field.end", new Object[0]));

        CheckboxGroup<DayOfWeek> days = new CheckboxGroup<>();
        days.setLabel(t.apply("expression.field.weekdays", new Object[0]));
        days.setItems(DayOfWeek.values());
        Locale loc = UI.getCurrent() != null ? UI.getCurrent().getLocale() : Locale.GERMAN;
        days.setItemLabelGenerator(d -> d.getDisplayName(TextStyle.SHORT, loc));

        VerticalLayout layout = new VerticalLayout(name, otn, days, from, to, start, end);
        layout.setPadding(false);
        layout.setSpacing(false);
        add(layout);

        Button cancel = new Button(t.apply("common.cancel", new Object[0]), e -> close());
        Button save = new Button(t.apply("common.save", new Object[0]));
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.addClickListener(
                e -> {
                    LocalDateTime s =
                            start.getValue() != null ? start.getValue().atStartOfDay() : null;
                    LocalDateTime en =
                            end.getValue() != null ? end.getValue().atTime(23, 59) : null;
                    String nameValue = name.getValue();
                    if (nameValue == null || nameValue.isBlank() || days.getValue().isEmpty()) {
                        warn(t.apply("expression.validation", new Object[0]));
                        return;
                    }
                    if (s != null && en != null && s.isAfter(en)) {
                        warn(t.apply("expression.invalidDates", new Object[0]));
                        return;
                    }
                    OrderService.ExpressionDraft draft =
                            new OrderService.ExpressionDraft(
                                    nameValue,
                                    otn.getValue(),
                                    Weekdays.format(new TreeSet<>(days.getValue())),
                                    from.getValue(),
                                    to.getValue(),
                                    s,
                                    en);
                    // Close only when the handler accepts the draft, so a rejected save keeps
                    // input.
                    if (Boolean.TRUE.equals(onSave.apply(draft))) {
                        close();
                    }
                });
        getFooter().add(cancel, save);
    }

    private static void warn(String message) {
        Notification.show(message, 3000, Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}
