package com.ordermgmt.railway.ui.component.order;

import java.util.function.BiFunction;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.select.SelectVariant;

import com.ordermgmt.railway.domain.order.model.Order;
import com.ordermgmt.railway.domain.order.model.PositionStatus;
import com.ordermgmt.railway.domain.order.service.OrderService;

/**
 * Internal "Bearbeitungs-Status" bar for the order detail: an editable select for users who may
 * mutate, a read-only badge otherwise, plus a lock hint while the order is "in Bearbeitung". The
 * status itself stays changeable even when the content is locked, so the order can be returned via
 * "überarbeiten". Persisting goes through {@link OrderService#setInternalStatus}, which enforces
 * SOB §5.7 (a Kostenträger is required for FREIGEGEBEN); violations surface as a friendly
 * notification.
 */
public class OrderInternalStatusBar extends HorizontalLayout {

    private final BiFunction<String, Object[], String> tr;

    public OrderInternalStatusBar(
            Order order,
            OrderService orderService,
            boolean canMutate,
            BiFunction<String, Object[], String> translator,
            Runnable onChanged) {
        this.tr = translator;
        addClassName("order-detail__internal-status");
        setAlignItems(FlexComponent.Alignment.CENTER);
        setSpacing(true);
        setPadding(false);

        Span label = new Span(t("order.internalStatus"));
        label.addClassName("order-detail__internal-status-label");
        add(label);

        if (canMutate) {
            Select<PositionStatus> select = new Select<>();
            select.setItems(PositionStatus.values());
            select.setItemLabelGenerator(s -> s == null ? "—" : t("position.status." + s.name()));
            select.setEmptySelectionAllowed(true);
            select.setEmptySelectionCaption("—");
            select.setValue(order.getInternalStatus());
            select.addThemeVariants(SelectVariant.LUMO_SMALL);
            select.addValueChangeListener(
                    e -> {
                        if (e.isFromClient()) {
                            changeStatus(order, orderService, e.getValue(), onChanged);
                        }
                    });
            add(select);
        } else {
            add(badge(order.getInternalStatus()));
        }

        // SOB §5.7: only the Auftraggeber (non-mutator) is locked out while "in Bearbeitung"; die
        // Planung keeps working, so the "gesperrt" hint is shown to non-mutators only.
        if (!canMutate && order.getInternalStatus() == PositionStatus.IN_BEARBEITUNG) {
            Span lock =
                    new Span(VaadinIcon.LOCK.create(), new Span(t("order.locked.inBearbeitung")));
            lock.addClassName("order-detail__lock-hint");
            add(lock);
        }
    }

    private Span badge(PositionStatus status) {
        Span badge = new Span(status == null ? "—" : t("position.status." + status.name()));
        badge.addClassName("order-internal-status-badge");
        if (status != null) {
            badge.addClassName("order-internal-status-badge--" + status.name().toLowerCase());
        }
        return badge;
    }

    private void changeStatus(
            Order order, OrderService orderService, PositionStatus newStatus, Runnable onChanged) {
        if (newStatus == order.getInternalStatus()) {
            return;
        }
        try {
            orderService.setInternalStatus(order.getId(), newStatus);
            Notification.show(
                            t(
                                    "order.internalStatus.changed",
                                    newStatus == null
                                            ? "—"
                                            : t("position.status." + newStatus.name())),
                            2000,
                            Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (OrderService.CostCenterRequiredException ex) {
            Notification.show(t("order.costCenter.required"), 3500, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } catch (RuntimeException ex) {
            Notification.show(t("order.phase.denied"), 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
        onChanged.run();
    }

    private String t(String key) {
        return tr.apply(key, new Object[0]);
    }

    private String t(String key, Object... params) {
        return tr.apply(key, params);
    }
}
