package com.ordermgmt.railway.ui.component.order;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import com.ordermgmt.railway.ui.component.ValidityCalendar;

/**
 * Editor for an expression's Verkehrstage (operating days). The calendar is bounded by the train's
 * window and marks days already taken by sibling expressions. Picking an occupied day reassigns it:
 * on save the user confirms, the day is removed from the sibling and recorded there as a change.
 */
class ExpressionVerkehrstageDialog extends Dialog {

    ExpressionVerkehrstageDialog(
            String title,
            LocalDate min,
            LocalDate max,
            List<LocalDate> current,
            Map<LocalDate, String> occupied,
            BiFunction<String, Object[], String> t,
            Consumer<List<LocalDate>> onSave) {
        setHeaderTitle(title);
        setWidth("720px");

        ValidityCalendar calendar = new ValidityCalendar(min, max);
        calendar.setCompact(true);
        calendar.setOccupiedDates(occupied);
        calendar.setSelectedDates(current);

        Span hint = new Span(t.apply("verkehrstage.dialog.hint", new Object[0]));
        hint.getStyle()
                .set("font-size", "12px")
                .set("color", "var(--rom-text-secondary)")
                .set("margin-bottom", "8px");

        VerticalLayout layout = new VerticalLayout(hint, calendar);
        layout.setPadding(false);
        layout.setSpacing(false);
        add(layout);

        Button cancel = new Button(t.apply("common.cancel", new Object[0]), e -> close());
        Button save = new Button(t.apply("common.save", new Object[0]));
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.addClickListener(e -> save(calendar, occupied, t, onSave));
        getFooter().add(cancel, save);
    }

    private void save(
            ValidityCalendar calendar,
            Map<LocalDate, String> occupied,
            BiFunction<String, Object[], String> translator,
            Consumer<List<LocalDate>> onSave) {
        List<LocalDate> selectedDays = calendar.getSelectedDates();
        if (selectedDays.isEmpty()) {
            Notification.show(
                            translator.apply("verkehrstage.empty", new Object[0]),
                            3000,
                            Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        Set<LocalDate> claimedDays =
                selectedDays.stream().filter(occupied::containsKey).collect(Collectors.toSet());
        if (claimedDays.isEmpty()) {
            onSave.accept(selectedDays);
            close();
            return;
        }

        confirmReassignment(claimedDays, selectedDays, occupied, translator, onSave);
    }

    private void confirmReassignment(
            Set<LocalDate> claimedDays,
            List<LocalDate> selectedDays,
            Map<LocalDate, String> occupied,
            BiFunction<String, Object[], String> translator,
            Consumer<List<LocalDate>> onSave) {
        String owners =
                claimedDays.stream()
                        .map(occupied::get)
                        .distinct()
                        .collect(Collectors.joining(", "));
        ConfirmDialog confirm = new ConfirmDialog();
        confirm.setHeader(translator.apply("verkehrstage.reassign.title", new Object[0]));
        confirm.setText(
                translator.apply(
                        "verkehrstage.reassign.text", new Object[] {claimedDays.size(), owners}));
        confirm.setCancelable(true);
        confirm.setCancelText(translator.apply("common.cancel", new Object[0]));
        confirm.setConfirmText(translator.apply("verkehrstage.reassign.confirm", new Object[0]));
        confirm.addConfirmListener(
                event -> {
                    onSave.accept(selectedDays);
                    close();
                });
        confirm.open();
    }
}
