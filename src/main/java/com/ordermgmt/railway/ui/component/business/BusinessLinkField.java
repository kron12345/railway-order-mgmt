package com.ordermgmt.railway.ui.component.business;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;

import com.ordermgmt.railway.domain.business.model.Business;
import com.ordermgmt.railway.domain.business.service.BusinessService;

/**
 * Reusable "link to business(es)" field for the order-position editors: a multi-select of existing
 * businesses plus a "+ new business" button that creates one inline and pre-selects it. After the
 * position is saved, {@link #applyTo(UUID)} reconciles the selection against the currently-linked
 * businesses (delta link/unlink) so it is safe on both create and edit. All mutation goes through
 * {@link BusinessService} (the owning side of the m:n link).
 */
public class BusinessLinkField extends Div {

    private final BusinessService businessService;
    private final Function<String, String> tr;
    private final MultiSelectComboBox<Business> combo = new MultiSelectComboBox<>();
    private List<Business> items;

    public BusinessLinkField(BusinessService businessService, Function<String, String> tr) {
        this.businessService = businessService;
        this.tr = tr;
        this.items = businessService.listAll();

        setWidthFull();
        combo.setLabel(tr.apply("position.businesses"));
        combo.setItems(items);
        combo.setItemLabelGenerator(b -> b.getTitle() == null ? "—" : b.getTitle());
        combo.setWidthFull();
        combo.setHelperText(tr.apply("position.businesses.help"));

        Button addBtn = new Button(tr.apply("business.create.inline"), VaadinIcon.PLUS.create());
        addBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        addBtn.addClickListener(e -> openCreateDialog());

        var row = new HorizontalLayout(combo, addBtn);
        row.setWidthFull();
        row.setDefaultVerticalComponentAlignment(
                com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.END);
        row.expand(combo);
        add(row);
    }

    /** Pre-select the businesses currently linked to the edited position (matched by id). */
    public void preset(List<Business> linked) {
        Set<UUID> ids = linked.stream().map(Business::getId).collect(Collectors.toSet());
        combo.setValue(
                items.stream().filter(b -> ids.contains(b.getId())).collect(Collectors.toSet()));
    }

    /**
     * Reconciles the current selection with the persisted links for {@code orderPositionId}: links
     * newly-selected businesses and unlinks de-selected ones. Best-effort per link (each is its own
     * transaction); failures are swallowed so one bad link doesn't abort the save.
     */
    public void applyTo(UUID orderPositionId) {
        Set<UUID> selected =
                combo.getValue().stream().map(Business::getId).collect(Collectors.toSet());
        try {
            businessService.setOrderPositionLinks(orderPositionId, selected);
        } catch (RuntimeException ex) {
            // best-effort: linking must not abort the position save
        }
    }

    private void openCreateDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(tr.apply("business.create.inline"));
        TextField title = new TextField(tr.apply("business.title"));
        title.setWidthFull();
        title.setRequired(true);
        TextArea description = new TextArea(tr.apply("business.description"));
        description.setWidthFull();
        dialog.add(new Div(title, description));

        Button cancel = new Button(tr.apply("common.cancel"), e -> dialog.close());
        Button create = new Button(tr.apply("common.create"));
        create.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        create.addClickListener(
                e -> {
                    if (title.getValue() == null || title.getValue().isBlank()) {
                        title.setInvalid(true);
                        return;
                    }
                    Business created =
                            businessService.create(
                                    title.getValue().trim(),
                                    description.getValue(),
                                    List.of(),
                                    List.of());
                    items = businessService.listAll();
                    combo.setItems(items);
                    Set<Business> selection = new HashSet<>(combo.getValue());
                    items.stream()
                            .filter(b -> b.getId().equals(created.getId()))
                            .findFirst()
                            .ifPresent(selection::add);
                    combo.setValue(selection);
                    dialog.close();
                });
        dialog.getFooter().add(cancel, create);
        dialog.open();
    }
}
