package com.ordermgmt.railway.ui.component.order;

import java.util.function.BiFunction;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;

import com.ordermgmt.railway.domain.order.model.PurchasePosition;
import com.ordermgmt.railway.domain.order.service.PurchaseOrderService;

/**
 * Factories for the "order / sync this purchase" buttons in {@link ResourcePanel}. There are two
 * ordering channels: TTT path requests (CAPACITY needs, via {@link TttOrderDialog}) and the mock
 * R²P channel for non-capacity external needs (e.g. a Lokführer). Both are styled the same way so
 * the channels read consistently; {@code onChanged} is run after a successful action to refresh the
 * UI.
 */
final class PurchaseOrderButtons {

    private PurchaseOrderButtons() {}

    /** TTT path-request order for a CAPACITY purchase — opens the attribute dialog. */
    static Button ttt(
            PurchasePosition pp,
            String otn,
            String routeLabel,
            PurchaseOrderService service,
            BiFunction<String, Object[], String> translator,
            Runnable onChanged) {
        Button btn =
                styled(
                        translator.apply("purchase.triggerTtt", new Object[0]),
                        "var(--rom-status-warning)",
                        "rgba(255,184,0,0.08)");
        btn.addClickListener(
                e -> {
                    TttOrderDialog dialog =
                            new TttOrderDialog(
                                    pp.getId(),
                                    pp.getPositionNumber(),
                                    otn,
                                    routeLabel,
                                    translator);
                    dialog.addSubmitListener(
                            evt ->
                                    run(
                                            () ->
                                                    service.triggerTttOrder(
                                                            evt.getPurchasePositionId(),
                                                            evt.getTttAttributesJson()),
                                            "TTT",
                                            onChanged));
                    dialog.open();
                });
        return btn;
    }

    /** Mock R²P order for a non-capacity external purchase — sets status BESTELLT. */
    static Button r2p(
            PurchasePosition pp,
            PurchaseOrderService service,
            BiFunction<String, Object[], String> translator,
            Runnable onChanged) {
        Button btn =
                styled(
                        translator.apply("purchase.r2p.order", new Object[0]),
                        "var(--rom-status-info)",
                        "rgba(68,138,255,0.08)");
        btn.addClickListener(e -> run(() -> service.triggerR2pOrder(pp.getId()), "R²P", onChanged));
        return btn;
    }

    /** Re-sync the TTT status of an already-ordered CAPACITY purchase from the path manager. */
    static Button sync(
            PurchasePosition pp,
            PurchaseOrderService service,
            BiFunction<String, Object[], String> translator,
            Runnable onChanged) {
        Button btn = new Button(VaadinIcon.REFRESH.create());
        btn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        btn.getStyle()
                .set("color", "var(--rom-text-muted)")
                .set("min-width", "24px")
                .set("padding", "0");
        btn.setTooltipText(translator.apply("purchase.synced", new Object[0]));
        btn.addClickListener(
                e -> {
                    service.syncTttStatus(pp.getId());
                    onChanged.run();
                });
        return btn;
    }

    private static void run(Runnable action, String okLabel, Runnable onChanged) {
        try {
            action.run();
            onChanged.run();
            Notification.show(okLabel, 2000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (RuntimeException ex) {
            Notification.show(ex.getMessage(), 4000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private static Button styled(String text, String color, String bg) {
        Button btn = new Button(text);
        btn.addThemeVariants(ButtonVariant.LUMO_SMALL);
        btn.getStyle()
                .set("font-size", "10px")
                .set("color", color)
                .set("border", "1px solid " + color)
                .set("background", bg)
                .set("padding", "1px 6px");
        return btn;
    }
}
