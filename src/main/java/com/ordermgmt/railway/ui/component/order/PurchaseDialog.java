package com.ordermgmt.railway.ui.component.order;

import java.util.UUID;
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
import com.ordermgmt.railway.domain.order.service.PurchaseOrderService;

/** Dialog for creating a purchase position for a resource need. */
public class PurchaseDialog extends Dialog {

    private static final String SUCCESS_MESSAGE = "OK";

    private final UUID resourceNeedId;
    private final boolean isCapacity;
    private final String positionValidity;
    private final String positionOtn;
    private final String positionRouteLabel;
    private final PurchaseOrderService purchaseOrderService;
    private final BiFunction<String, Object[], String> translator;

    /**
     * Creates a new PurchaseDialog.
     *
     * @param resourceNeedId the resource need UUID
     * @param resourceTypeLabel the translated resource type label
     * @param coverageTypeLabel the translated coverage type label
     * @param isCapacity whether this is a CAPACITY resource type
     * @param positionValidity the validity JSON from the order position (nullable)
     * @param positionOtn the operational train number from the order position (nullable)
     * @param positionRouteLabel the route label, e.g. "Bern -> Zurich" (nullable)
     * @param purchaseOrderService the service for creating purchase positions
     * @param translator i18n translation function
     */
    public PurchaseDialog(
            UUID resourceNeedId,
            String resourceTypeLabel,
            String coverageTypeLabel,
            boolean isCapacity,
            String positionValidity,
            String positionOtn,
            String positionRouteLabel,
            PurchaseOrderService purchaseOrderService,
            BiFunction<String, Object[], String> translator) {
        this.resourceNeedId = resourceNeedId;
        this.isCapacity = isCapacity;
        this.positionValidity = positionValidity;
        this.positionOtn = positionOtn;
        this.positionRouteLabel = positionRouteLabel;
        this.purchaseOrderService = purchaseOrderService;
        this.translator = translator;

        setHeaderTitle(tr("purchase.add"));
        setWidth("500px");

        buildForm(resourceTypeLabel, coverageTypeLabel);
    }

    private void buildForm(String resourceTypeLabel, String coverageTypeLabel) {
        // Resource info (read-only)
        Span resourceInfo = new Span(resourceTypeLabel + " \u2014 " + coverageTypeLabel);
        resourceInfo
                .getStyle()
                .set("font-size", "12px")
                .set("font-weight", "600")
                .set("color", "var(--rom-text-primary)")
                .set("display", "block")
                .set("margin-bottom", "var(--lumo-space-xs)");

        TextField description = new TextField(tr("purchase.description"));
        description.setWidthFull();

        Checkbox viaTtt = new Checkbox(tr("purchase.triggerTtt"));
        viaTtt.setVisible(isCapacity);
        viaTtt.setValue(isCapacity);

        Div form = new Div(resourceInfo, description, viaTtt);
        form.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "var(--lumo-space-s)");
        add(form);

        getFooter().add(createCancelButton(), createSaveButton(description, viaTtt));
    }

    private Button createCancelButton() {
        Button cancel = new Button(tr("common.cancel"));
        cancel.addClickListener(event -> close());
        return cancel;
    }

    private Button createSaveButton(TextField description, Checkbox viaTtt) {
        Button save = new Button(tr("common.save"));
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.getStyle()
                .set("background", "var(--rom-accent)")
                .set("color", "var(--rom-bg-primary)");
        save.addClickListener(event -> savePurchase(description.getValue(), viaTtt.getValue()));
        return save;
    }

    private void savePurchase(String description, boolean triggerTtt) {
        try {
            PurchasePosition purchasePosition =
                    purchaseOrderService.createPurchasePosition(
                            resourceNeedId, description, positionValidity);
            if (isCapacity && triggerTtt) {
                openTttDialog(purchasePosition);
                return;
            }
            completeSave();
        } catch (Exception ex) {
            Notification.show(ex.getMessage(), 4000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void openTttDialog(PurchasePosition purchasePosition) {
        close();
        TttOrderDialog dialog =
                new TttOrderDialog(
                        purchasePosition.getId(),
                        purchasePosition.getPositionNumber(),
                        positionOtn,
                        positionRouteLabel,
                        translator);
        dialog.addSubmitListener(
                event -> {
                    purchaseOrderService.triggerTttOrder(
                            event.getPurchasePositionId(), event.getTttAttributesJson());
                    showSuccess();
                    fireEvent(new SaveEvent(this));
                });
        dialog.open();
    }

    private void completeSave() {
        showSuccess();
        close();
        fireEvent(new SaveEvent(this));
    }

    private void showSuccess() {
        Notification.show(SUCCESS_MESSAGE, 2000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
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
