package com.ordermgmt.railway.ui.component.order;

import java.util.List;
import java.util.function.BiFunction;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.shared.Registration;

import com.ordermgmt.railway.domain.order.model.CoverageType;
import com.ordermgmt.railway.domain.order.model.OrderPosition;
import com.ordermgmt.railway.domain.order.model.ResourceCatalogItem;
import com.ordermgmt.railway.domain.order.model.ResourceType;
import com.ordermgmt.railway.domain.order.repository.ResourceCatalogItemRepository;
import com.ordermgmt.railway.domain.order.service.ResourceNeedService;

/** Dialog for adding a resource need to an order position. */
public class ResourceDialog extends Dialog {

    private static final String CAT_VEHICLE_TYPE = "VEHICLE_TYPE";
    private static final String CAT_PERSONNEL_QUAL = "PERSONNEL_QUAL";

    private final OrderPosition position;
    private final ResourceNeedService resourceNeedService;
    private final ResourceCatalogItemRepository catalogItemRepository;
    private final BiFunction<String, Object[], String> translator;

    public ResourceDialog(
            OrderPosition position,
            ResourceNeedService resourceNeedService,
            ResourceCatalogItemRepository catalogItemRepository,
            BiFunction<String, Object[], String> translator) {
        this.position = position;
        this.resourceNeedService = resourceNeedService;
        this.catalogItemRepository = catalogItemRepository;
        this.translator = translator;

        setHeaderTitle(tr("resource.add"));
        setWidth("500px");

        buildForm();
    }

    private void buildForm() {
        Select<ResourceType> typeSelect = new Select<>();
        typeSelect.setLabel(tr("resource.type"));
        typeSelect.setItems(ResourceType.values());
        typeSelect.setItemLabelGenerator(t -> tr("resource.type." + t.name()));
        typeSelect.setValue(ResourceType.CAPACITY);
        typeSelect.setWidthFull();

        Select<CoverageType> coverageSelect = new Select<>();
        coverageSelect.setLabel(tr("resource.coverage"));
        coverageSelect.setItems(CoverageType.values());
        coverageSelect.setItemLabelGenerator(c -> tr("resource.coverage." + c.name()));
        coverageSelect.setValue(CoverageType.EXTERNAL);
        coverageSelect.setWidthFull();

        ComboBox<ResourceCatalogItem> catalogCombo = new ComboBox<>(tr("resource.catalogItem"));
        catalogCombo.setItemLabelGenerator(item -> item.getCode() + " — " + item.getName());
        catalogCombo.setWidthFull();
        catalogCombo.setClearButtonVisible(true);

        // Update catalog items when type changes
        typeSelect.addValueChangeListener(e -> updateCatalogItems(e.getValue(), catalogCombo));
        updateCatalogItems(typeSelect.getValue(), catalogCombo);

        IntegerField quantity = new IntegerField(tr("resource.quantity"));
        quantity.setMin(1);
        quantity.setValue(1);
        quantity.setStepButtonsVisible(true);
        quantity.setWidthFull();

        TextField description = new TextField(tr("resource.description"));
        description.setWidthFull();

        Div form = new Div(typeSelect, coverageSelect, catalogCombo, quantity, description);
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
                    ResourceType type = typeSelect.getValue();
                    CoverageType coverage = coverageSelect.getValue();
                    if (type == null || coverage == null) {
                        return;
                    }
                    int qty = quantity.getValue() != null ? quantity.getValue() : 1;
                    ResourceCatalogItem catalogItem = catalogCombo.getValue();

                    try {
                        resourceNeedService.addResource(
                                position.getId(),
                                type,
                                catalogItem != null ? catalogItem.getId() : null,
                                qty,
                                coverage,
                                description.getValue());

                        Notification.show("OK", 2000, Notification.Position.BOTTOM_END)
                                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                        close();
                        fireEvent(new SaveEvent(this));
                    } catch (Exception ex) {
                        Notification.show(ex.getMessage(), 4000, Notification.Position.BOTTOM_END)
                                .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    }
                });

        getFooter().add(cancel, save);
    }

    private void updateCatalogItems(ResourceType type, ComboBox<ResourceCatalogItem> combo) {
        if (type == null) {
            combo.setItems(List.of());
            return;
        }
        String category =
                switch (type) {
                    case VEHICLE -> CAT_VEHICLE_TYPE;
                    case PERSONNEL -> CAT_PERSONNEL_QUAL;
                    case CAPACITY -> null;
                };
        if (category != null) {
            combo.setItems(catalogItemRepository.findByCategoryAndActiveTrue(category));
            combo.setVisible(true);
        } else {
            combo.setItems(List.of());
            combo.setVisible(false);
        }
    }

    /** Fired after a resource need is successfully saved. */
    public static class SaveEvent extends ComponentEvent<ResourceDialog> {
        public SaveEvent(ResourceDialog source) {
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
