package com.ordermgmt.railway.ui.component.order;

import java.util.function.BiFunction;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.shared.Registration;

import com.ordermgmt.railway.domain.order.model.PurchasePosition;
import com.ordermgmt.railway.domain.order.model.ResourceNeed;
import com.ordermgmt.railway.domain.order.model.ResourceType;
import com.ordermgmt.railway.domain.order.service.PurchaseOrderService;

/** Dialog for creating a purchase position for a resource need. */
public class PurchaseDialog extends Dialog {

    private final ResourceNeed resourceNeed;
    private final PurchaseOrderService purchaseOrderService;
    private final BiFunction<String, Object[], String> translator;

    public PurchaseDialog(
            ResourceNeed resourceNeed,
            PurchaseOrderService purchaseOrderService,
            BiFunction<String, Object[], String> translator) {
        this.resourceNeed = resourceNeed;
        this.purchaseOrderService = purchaseOrderService;
        this.translator = translator;

        setHeaderTitle(tr("purchase.add"));
        setWidth("500px");

        buildForm();
    }

    private void buildForm() {
        // Resource info (read-only)
        Span resourceInfo =
                new Span(
                        tr("resource.type." + resourceNeed.getResourceType().name())
                                + " — "
                                + tr("resource.coverage." + resourceNeed.getCoverageType().name()));
        resourceInfo
                .getStyle()
                .set("font-size", "12px")
                .set("font-weight", "600")
                .set("color", "var(--rom-text-primary)")
                .set("display", "block")
                .set("margin-bottom", "var(--lumo-space-xs)");

        TextField description = new TextField(tr("purchase.description"));
        description.setWidthFull();

        boolean isCapacity = resourceNeed.getResourceType() == ResourceType.CAPACITY;
        Checkbox viaTtt = new Checkbox(tr("purchase.triggerTtt"));
        viaTtt.setVisible(isCapacity);
        viaTtt.setValue(isCapacity);

        Div form = new Div(resourceInfo, description, viaTtt);
        form.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "var(--lumo-space-s)");
        add(form);

        Button cancel = new Button(tr("common.cancel"));
        cancel.addClickListener(e -> close());

        Button save = new Button(tr("common.save"));
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.getStyle()
                .set("background", "var(--rom-accent)")
                .set("color", "var(--rom-bg-primary)");
        save.addClickListener(
                e -> {
                    try {
                        PurchasePosition pp =
                                purchaseOrderService.createPurchasePosition(
                                        resourceNeed.getId(),
                                        description.getValue(),
                                        resourceNeed.getOrderPosition().getValidity());

                        if (isCapacity && viaTtt.getValue()) {
                            close();
                            TttOrderDialog tttDialog = new TttOrderDialog(pp, translator);
                            tttDialog.addSubmitListener(
                                    evt -> {
                                        purchaseOrderService.triggerTttOrder(
                                                evt.getPurchasePositionId(),
                                                evt.getTttAttributesJson());
                                        Notification.show(
                                                        "OK",
                                                        2000,
                                                        Notification.Position.BOTTOM_END)
                                                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                                        fireEvent(new SaveEvent(this));
                                    });
                            tttDialog.open();
                        } else {
                            Notification.show("OK", 2000, Notification.Position.BOTTOM_END)
                                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                            close();
                            fireEvent(new SaveEvent(this));
                        }
                    } catch (Exception ex) {
                        Notification.show(ex.getMessage(), 4000, Notification.Position.BOTTOM_END)
                                .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    }
                });

        getFooter().add(cancel, save);
    }

    /** Fired after a purchase position is successfully saved. */
    public static class SaveEvent extends ComponentEvent<PurchaseDialog> {
        public SaveEvent(PurchaseDialog source) {
            super(source, false);
        }
    }

    public Registration addSaveListener(ComponentEventListener<SaveEvent> listener) {
        return addListener(SaveEvent.class, listener);
    }

    private String tr(String key) {
        return translator.apply(key, new Object[0]);
    }
}
