package com.ordermgmt.railway.ui.component.order;

import java.util.List;
import java.util.function.BiFunction;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;

/**
 * Builds the collapsible "advanced" detail sections for the TTT order dialog. Each section groups
 * related optional TTT attributes (traction, calendar, NSP, references, extended contact, special
 * transport).
 */
class TttOrderDetailSections {

    private static final List<String> TRACTION_MODES =
            List.of("11 (Kopf Pos.1)", "21 (Mitte Pos.1)", "33 (Schub Pos.3)");

    private static final List<String> HYBRID_POWER_UNITS =
            List.of("1 Elektrisch", "2 Diesel", "3 Batterie", "4 Wasserstoff");

    private static final List<String> REASON_OF_REFERENCE =
            List.of(
                    "1000 - Gleiches Angebot wie PathRequest",
                    "1001 - Gleiches Angebot wie Path/ReferenceTrain",
                    "1002 - Vollstaendiger Ersatz",
                    "1003 - Teilweiser Ersatz",
                    "1013 - Modification durch RA");

    private static final List<String> GAUGING_PROFILES = List.of("C0", "C1", "C2", "C3", "C4");

    private final BiFunction<String, Object[], String> translator;

    /* Traction */
    private final TextField locoTypeNumber = new TextField();
    private final ComboBox<String> tractionMode = new ComboBox<>();
    private final ComboBox<String> hybridPowerUnit = new ComboBox<>();

    /* Calendar */
    private final TextField calendarBitmapDays = new TextField();
    private final IntegerField offsetToReference = new IntegerField();

    /* NSP */
    private final IntegerField maxCommercialSpeed = new IntegerField();
    private final TextField cataloguePath = new TextField();
    private final IntegerField numberOfFormationGroup = new IntegerField();
    private final TextField loadingGauge = new TextField();

    /* References */
    private final ComboBox<String> reasonOfReference = new ComboBox<>();
    private final TextField referencedPathId = new TextField();
    private final TextField caseReferenceId = new TextField();

    /* Extended contact */
    private final TextField contactPhone = new TextField();
    private final TextField contactAddress = new TextField();
    private final TextField distributionList = new TextField();

    /* Special transport */
    private final TextField dangerousGoodsUn = new TextField();
    private final TextField exceptionalGaugingCode = new TextField();
    private final ComboBox<String> plannedGaugingProfile = new ComboBox<>();

    TttOrderDetailSections(BiFunction<String, Object[], String> translator) {
        this.translator = translator;
    }

    /** Builds a collapsible Details component containing all advanced attribute sections. */
    Component build() {
        Div wrapper = new Div();
        wrapper.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "var(--lumo-space-s)");

        wrapper.add(buildTractionSection());
        wrapper.add(buildCalendarSection());
        wrapper.add(buildNspSection());
        wrapper.add(buildReferencesSection());
        wrapper.add(buildExtendedContactSection());
        wrapper.add(buildSpecialTransportSection());

        Details details = new Details(tr("ttt.order.advanced"), wrapper);
        details.setOpened(false);
        details.setWidthFull();
        return details;
    }

    /** Writes all non-empty advanced fields into the given JSON object node. */
    void writeToJson(ObjectNode root) {
        putIfNotEmpty(root, "locoTypeNumber", locoTypeNumber.getValue());
        putIfNotEmpty(root, "tractionMode", tractionMode.getValue());
        putIfNotEmpty(root, "hybridPowerUnit", hybridPowerUnit.getValue());
        putIfNotEmpty(root, "calendarBitmapDays", calendarBitmapDays.getValue());
        putIfNotNull(root, "offsetToReference", offsetToReference.getValue());
        putIfNotNull(root, "maxCommercialSpeed", maxCommercialSpeed.getValue());
        putIfNotEmpty(root, "cataloguePath", cataloguePath.getValue());
        putIfNotNull(root, "numberOfFormationGroup", numberOfFormationGroup.getValue());
        putIfNotEmpty(root, "loadingGauge", loadingGauge.getValue());
        putIfNotEmpty(root, "reasonOfReference", reasonOfReference.getValue());
        putIfNotEmpty(root, "referencedPathId", referencedPathId.getValue());
        putIfNotEmpty(root, "caseReferenceId", caseReferenceId.getValue());
        putIfNotEmpty(root, "contactPhone", contactPhone.getValue());
        putIfNotEmpty(root, "contactAddress", contactAddress.getValue());
        putIfNotEmpty(root, "distributionList", distributionList.getValue());
        putIfNotEmpty(root, "dangerousGoodsUn", dangerousGoodsUn.getValue());
        putIfNotEmpty(root, "exceptionalGaugingCode", exceptionalGaugingCode.getValue());
        putIfNotEmpty(root, "plannedGaugingProfile", plannedGaugingProfile.getValue());
    }

    // --- Section builders ---

    private Component buildTractionSection() {
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));

        locoTypeNumber.setLabel(tr("ttt.order.locoType"));
        locoTypeNumber.setPlaceholder("z.B. RABe 502");
        form.add(locoTypeNumber);

        tractionMode.setLabel(tr("ttt.order.tractionMode"));
        tractionMode.setItems(TRACTION_MODES);
        form.add(tractionMode);

        hybridPowerUnit.setLabel(tr("ttt.order.hybridPower"));
        hybridPowerUnit.setItems(HYBRID_POWER_UNITS);
        form.add(hybridPowerUnit);

        return wrapSection(tr("ttt.order.traction"), form);
    }

    private Component buildCalendarSection() {
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));

        calendarBitmapDays.setLabel(tr("ttt.order.bitmapDays"));
        calendarBitmapDays.setHelperText(tr("ttt.order.bitmapDays.help"));
        calendarBitmapDays.setPlaceholder("1111100 (Mo-Fr)");
        form.add(calendarBitmapDays);

        offsetToReference.setLabel(tr("ttt.order.offset"));
        offsetToReference.setValue(0);
        form.add(offsetToReference);

        return wrapSection(tr("ttt.order.calendar"), form);
    }

    private Component buildNspSection() {
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));

        maxCommercialSpeed.setLabel(tr("ttt.order.maxSpeed"));
        maxCommercialSpeed.setPlaceholder("140");
        form.add(maxCommercialSpeed);

        cataloguePath.setLabel(tr("ttt.order.catalogPath"));
        form.add(cataloguePath);

        numberOfFormationGroup.setLabel(tr("ttt.order.formationGroups"));
        numberOfFormationGroup.setValue(1);
        form.add(numberOfFormationGroup);

        loadingGauge.setLabel(tr("ttt.order.loadingGauge"));
        form.add(loadingGauge);

        return wrapSection(tr("ttt.order.nsp"), form);
    }

    private Component buildReferencesSection() {
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));

        reasonOfReference.setLabel(tr("ttt.order.reasonOfRef"));
        reasonOfReference.setItems(REASON_OF_REFERENCE);
        form.add(reasonOfReference, 2);

        referencedPathId.setLabel(tr("ttt.order.refPath"));
        form.add(referencedPathId);

        caseReferenceId.setLabel(tr("ttt.order.caseRef"));
        form.add(caseReferenceId);

        return wrapSection(tr("ttt.order.references"), form);
    }

    private Component buildExtendedContactSection() {
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));

        contactPhone.setLabel(tr("ttt.order.phone"));
        form.add(contactPhone);

        contactAddress.setLabel(tr("ttt.order.address"));
        form.add(contactAddress);

        distributionList.setLabel(tr("ttt.order.distributionList"));
        distributionList.setHelperText(tr("ttt.order.distributionList.help"));
        form.add(distributionList, 2);

        return wrapSection(tr("ttt.order.contactExtended"), form);
    }

    private Component buildSpecialTransportSection() {
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));

        dangerousGoodsUn.setLabel(tr("ttt.order.dangerousGoods"));
        dangerousGoodsUn.setPlaceholder("UN-Nummer");
        form.add(dangerousGoodsUn);

        exceptionalGaugingCode.setLabel(tr("ttt.order.exceptionalGauging"));
        form.add(exceptionalGaugingCode);

        plannedGaugingProfile.setLabel(tr("ttt.order.gaugingProfile"));
        plannedGaugingProfile.setItems(GAUGING_PROFILES);
        form.add(plannedGaugingProfile);

        return wrapSection(tr("ttt.order.special"), form);
    }

    // --- Helpers ---

    private Div wrapSection(String label, Component content) {
        Div wrapper = new Div();
        wrapper.getStyle()
                .set("border", "1px solid var(--rom-border)")
                .set("border-radius", "4px")
                .set("padding", "8px 12px");

        Span sectionLabel = new Span(label);
        sectionLabel
                .getStyle()
                .set("font-weight", "600")
                .set("font-size", "11px")
                .set("color", "var(--rom-text-secondary)")
                .set("display", "block")
                .set("margin-bottom", "4px");
        wrapper.add(sectionLabel, content);
        return wrapper;
    }

    private void putIfNotEmpty(ObjectNode node, String key, String value) {
        if (value != null && !value.isBlank()) {
            node.put(key, value.trim());
        }
    }

    private void putIfNotNull(ObjectNode node, String key, Integer value) {
        if (value != null) {
            node.put(key, value);
        }
    }

    private String tr(String key) {
        return translator.apply(key, new Object[0]);
    }
}
