package com.ordermgmt.railway.ui.component.order;

import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.shared.Registration;

import com.ordermgmt.railway.domain.order.model.PurchasePosition;

/**
 * Dialog for entering TTT-specific order attributes before triggering a CAPACITY purchase via TTT.
 * Collects mandatory fields (debit code, contact, brake sequence) and optional advanced attributes
 * organized in collapsible detail sections.
 */
public class TttOrderDialog extends Dialog {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final List<String> TRAIN_TYPES =
            List.of(
                    "01 - Personenzug",
                    "02 - Güterzug",
                    "03 - Lokfahrt",
                    "04 - Dienstzug",
                    "05 - Lösch-/Rettungszug",
                    "06 - Güter-/Personenzug");

    private static final List<String> TRAFFIC_TYPES =
            List.of(
                    "S - S-Bahn",
                    "RE - RegioExpress",
                    "IR - InterRegio",
                    "IC - InterCity",
                    "EC - EuroCity",
                    "EN - EuroNight",
                    "FG - Fernverkehr Güter",
                    "GZ - Güterzug",
                    "AT - Autoverlad",
                    "ARZ - AutoReisezug",
                    "CNL - CityNightLine",
                    "LRZ - Lösch-/Rettungszug",
                    "DZ - Dienstzug");

    private static final List<String> BRAKE_SEQUENCES =
            List.of(
                    "N180", "N150", "R150", "R135", "R125", "R115", "R105", "A115", "A105", "A95",
                    "A85", "A80", "A75", "A70", "A65", "A60", "A50", "D115", "D105", "D95", "D85",
                    "D80", "D75", "D70", "D65", "D60", "D50");

    private final UUID purchasePositionId;
    private final BiFunction<String, Object[], String> translator;

    /* Required fields */
    private final TextField debitCode = new TextField();
    private final ComboBox<String> trainType = new ComboBox<>();
    private final ComboBox<String> trafficTypeCode = new ComboBox<>();
    private final TextField contactName = new TextField();
    private final EmailField contactEmail = new EmailField();
    private final ComboBox<String> trainAndBrakeSequence = new ComboBox<>();

    /* Advanced fields (created by TttOrderDetailSections) */
    private final TttOrderDetailSections detailSections;

    public TttOrderDialog(
            PurchasePosition purchasePosition, BiFunction<String, Object[], String> translator) {
        this.purchasePositionId = purchasePosition.getId();
        this.translator = translator;

        String positionName = purchasePosition.getPositionNumber();
        setHeaderTitle(tr("ttt.order.title") + " \u2014 " + positionName);
        setWidth("700px");

        Div content = new Div();
        content.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "var(--lumo-space-m)");

        content.add(buildTrainInfoSection(purchasePosition));
        content.add(buildRequiredFields());

        detailSections = new TttOrderDetailSections(translator);
        content.add(detailSections.build());

        add(content);

        Button cancel = new Button(tr("common.cancel"));
        cancel.addClickListener(e -> close());

        Button submit = new Button(tr("ttt.order.submit") + " \u2192");
        submit.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        submit.getStyle()
                .set("background", "var(--rom-accent)")
                .set("color", "var(--rom-bg-primary)");
        submit.addClickListener(e -> onSubmit());

        getFooter().add(cancel, submit);
    }

    private Div buildTrainInfoSection(PurchasePosition pp) {
        Div section = new Div();
        section.getStyle()
                .set("background", "var(--rom-bg-secondary)")
                .set("border", "1px solid var(--rom-border)")
                .set("border-radius", "4px")
                .set("padding", "8px 12px");

        Span title = new Span(tr("ttt.order.trainInfo"));
        title.getStyle()
                .set("font-weight", "600")
                .set("font-size", "11px")
                .set("color", "var(--rom-text-secondary)")
                .set("display", "block")
                .set("margin-bottom", "4px");
        section.add(title);

        var position = pp.getOrderPosition();
        StringBuilder info = new StringBuilder();
        if (position.getOperationalTrainNumber() != null) {
            info.append("OTN: ").append(position.getOperationalTrainNumber()).append("  ");
        }
        if (position.getFromLocation() != null && position.getToLocation() != null) {
            info.append(position.getFromLocation())
                    .append(" \u2192 ")
                    .append(position.getToLocation());
        }

        Span infoSpan = new Span(info.toString().trim());
        infoSpan.getStyle()
                .set("font-family", "'JetBrains Mono', monospace")
                .set("font-size", "11px")
                .set("color", "var(--rom-text-primary)");
        section.add(infoSpan);

        return section;
    }

    private FormLayout buildRequiredFields() {
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));

        Span sectionLabel = new Span(tr("ttt.order.required"));
        sectionLabel
                .getStyle()
                .set("font-weight", "600")
                .set("font-size", "12px")
                .set("color", "var(--rom-text-primary)");
        form.add(sectionLabel, 2);

        debitCode.setLabel(tr("ttt.order.debitCode"));
        debitCode.setHelperText(tr("ttt.order.debitCode.help"));
        debitCode.setPlaceholder("z.B. 12345");
        debitCode.setRequired(true);
        form.add(debitCode);

        trainType.setLabel(tr("ttt.order.trainType"));
        trainType.setItems(TRAIN_TYPES);
        trainType.setRequired(true);
        trainType.setHelperText(tr("ttt.order.trainType.help"));
        form.add(trainType);

        trafficTypeCode.setLabel(tr("ttt.order.trafficType"));
        trafficTypeCode.setItems(TRAFFIC_TYPES);
        trafficTypeCode.setRequired(true);
        trafficTypeCode.setHelperText(tr("ttt.order.trafficType.help"));
        form.add(trafficTypeCode);

        contactName.setLabel(tr("ttt.order.contactName"));
        contactName.setRequired(true);
        form.add(contactName);

        contactEmail.setLabel(tr("ttt.order.contactEmail"));
        contactEmail.setRequired(true);
        form.add(contactEmail);

        trainAndBrakeSequence.setLabel(tr("ttt.order.brakeSequence"));
        trainAndBrakeSequence.setHelperText(tr("ttt.order.brakeSequence.help"));
        trainAndBrakeSequence.setItems(BRAKE_SEQUENCES);
        trainAndBrakeSequence.setRequired(true);
        form.add(trainAndBrakeSequence);

        return form;
    }

    private void onSubmit() {
        if (!validateRequired()) {
            return;
        }

        try {
            String json = buildJson();
            fireEvent(new SubmitEvent(this, purchasePositionId, json));
            close();
        } catch (Exception ex) {
            Notification.show(ex.getMessage(), 4000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private boolean validateRequired() {
        boolean valid = true;
        if (debitCode.isEmpty()) {
            debitCode.setInvalid(true);
            valid = false;
        }
        if (trainType.isEmpty()) {
            trainType.setInvalid(true);
            valid = false;
        }
        if (trafficTypeCode.isEmpty()) {
            trafficTypeCode.setInvalid(true);
            valid = false;
        }
        if (contactName.isEmpty()) {
            contactName.setInvalid(true);
            valid = false;
        }
        if (contactEmail.isEmpty() || contactEmail.isInvalid()) {
            contactEmail.setInvalid(true);
            valid = false;
        }
        if (trainAndBrakeSequence.isEmpty()) {
            trainAndBrakeSequence.setInvalid(true);
            valid = false;
        }
        return valid;
    }

    private String buildJson() {
        ObjectNode root = MAPPER.createObjectNode();

        // Required
        root.put("debitCode", debitCode.getValue());
        root.put("trainType", trainType.getValue());
        root.put("trafficTypeCode", trafficTypeCode.getValue());
        root.put("contactName", contactName.getValue());
        root.put("contactEmail", contactEmail.getValue());
        root.put("trainAndBrakeSequence", trainAndBrakeSequence.getValue());

        // Delegate advanced fields to detail sections
        detailSections.writeToJson(root);

        return root.toString();
    }

    /** Fired when the user confirms the TTT order with all attributes. */
    public static class SubmitEvent extends ComponentEvent<TttOrderDialog> {

        private final UUID purchasePositionId;
        private final String tttAttributesJson;

        public SubmitEvent(TttOrderDialog source, UUID purchasePositionId, String json) {
            super(source, false);
            this.purchasePositionId = purchasePositionId;
            this.tttAttributesJson = json;
        }

        public UUID getPurchasePositionId() {
            return purchasePositionId;
        }

        public String getTttAttributesJson() {
            return tttAttributesJson;
        }
    }

    public Registration addSubmitListener(ComponentEventListener<SubmitEvent> listener) {
        return addListener(SubmitEvent.class, listener);
    }

    private String tr(String key) {
        return translator.apply(key, new Object[0]);
    }
}
