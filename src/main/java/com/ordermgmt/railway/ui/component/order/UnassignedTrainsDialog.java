package com.ordermgmt.railway.ui.component.order;

import java.util.UUID;
import java.util.function.BiFunction;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import com.ordermgmt.railway.domain.pathmanager.model.PmReferenceTrain;
import com.ordermgmt.railway.domain.pathmanager.service.PathManagerService;

/** Lists RailOpt reference trains not yet captured under an order and lets the user attach one. */
class UnassignedTrainsDialog extends Dialog {

    private final PathManagerService pathManagerService;
    private final UUID orderId;
    private final BiFunction<String, Object[], String> t;
    private final Runnable onCaptured;

    UnassignedTrainsDialog(
            PathManagerService pathManagerService,
            UUID orderId,
            BiFunction<String, Object[], String> t,
            Runnable onCaptured) {
        this.pathManagerService = pathManagerService;
        this.orderId = orderId;
        this.t = t;
        this.onCaptured = onCaptured;

        setHeaderTitle(t.apply("position.fromPm.title", new Object[0]));

        VerticalLayout list = new VerticalLayout();
        list.setPadding(false);
        list.setSpacing(true);
        list.setWidth("440px");

        var trains = pathManagerService.findUnassignedTrains();
        if (trains.isEmpty()) {
            Span empty = new Span(t.apply("position.fromPm.empty", new Object[0]));
            empty.getStyle().set("color", "var(--rom-text-muted)").set("font-size", "13px");
            list.add(empty);
        } else {
            for (PmReferenceTrain train : trains) {
                Span info = new Span(label(train));
                info.getStyle().set("font-size", "13px");
                Button take =
                        new Button(
                                t.apply("position.fromPm.capture", new Object[0]),
                                VaadinIcon.DOWNLOAD.create());
                take.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
                take.addClickListener(e -> capture(train));

                HorizontalLayout row = new HorizontalLayout(info, take);
                row.setWidthFull();
                row.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
                row.setAlignItems(FlexComponent.Alignment.CENTER);
                list.add(row);
            }
        }

        add(list);
        getFooter().add(new Button(t.apply("common.cancel", new Object[0]), e -> close()));
    }

    private void capture(PmReferenceTrain train) {
        try {
            pathManagerService.captureUnassignedTrainAsPosition(train.getId(), orderId);
            Notification.show(
                            t.apply("position.fromPm.captured", new Object[0]),
                            2500,
                            Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            close();
            onCaptured.run();
        } catch (RuntimeException ex) {
            Notification.show(
                            t.apply("common.errorGeneric", new Object[0]),
                            3000,
                            Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private String label(PmReferenceTrain train) {
        StringBuilder label = new StringBuilder();
        label.append(
                train.getOperationalTrainNumber() != null
                        ? "OTN " + train.getOperationalTrainNumber()
                        : train.getTridCore());
        if (train.getCalendarStart() != null && train.getCalendarEnd() != null) {
            label.append("  ·  ")
                    .append(train.getCalendarStart())
                    .append(" – ")
                    .append(train.getCalendarEnd());
        }
        return label.toString();
    }
}
