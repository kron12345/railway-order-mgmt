package com.ordermgmt.railway.ui.view.settings;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.BiFunction;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;

import com.ordermgmt.railway.domain.order.model.ResourceCatalogItem;
import com.ordermgmt.railway.domain.order.repository.ResourceCatalogItemRepository;
import com.ordermgmt.railway.domain.order.service.ResourceCatalogImportService;

/** Settings tab for managing the resource catalog (vehicle types, personnel qualifications). */
public class CatalogTab extends Div {

    private static final String CAT_VEHICLE_TYPE = "VEHICLE_TYPE";
    private static final String CAT_PERSONNEL_QUAL = "PERSONNEL_QUAL";

    private final ResourceCatalogItemRepository catalogRepo;
    private final ResourceCatalogImportService importService;
    private final Grid<ResourceCatalogItem> grid = new Grid<>(ResourceCatalogItem.class, false);
    private final BiFunction<String, Object[], String> t;
    private String categoryFilter = null;

    public CatalogTab(
            ResourceCatalogItemRepository catalogRepo,
            ResourceCatalogImportService importService,
            BiFunction<String, Object[], String> translator) {
        this.catalogRepo = catalogRepo;
        this.importService = importService;
        this.t = translator;
        setWidthFull();

        add(createHeader());
        add(createFilterBar());
        add(createGridWrapper());

        refresh();
    }

    private HorizontalLayout createHeader() {
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.getStyle().set("margin-bottom", "var(--lumo-space-s)");

        H3 title = new H3(tr("catalog.title"));
        title.getStyle()
                .set("color", "var(--rom-text-primary)")
                .set("margin", "0")
                .set("font-size", "var(--lumo-font-size-m)");

        HorizontalLayout buttons = new HorizontalLayout();
        buttons.setSpacing(true);

        Button addBtn = new Button(tr("catalog.add"), VaadinIcon.PLUS.create());
        addBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
        addBtn.getStyle()
                .set("background", "var(--rom-accent)")
                .set("color", "var(--rom-bg-primary)");
        addBtn.addClickListener(e -> openCatalogDialog(null));

        Button importBtn = new Button(tr("settings.import.upload"), VaadinIcon.UPLOAD.create());
        importBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
        importBtn
                .getStyle()
                .set("color", "var(--rom-text-secondary)")
                .set("border", "1px solid var(--rom-border)");
        importBtn.addClickListener(e -> openImportDialog());

        buttons.add(addBtn, importBtn);
        header.add(title, buttons);
        return header;
    }

    private Div createFilterBar() {
        Div bar = new Div();
        bar.getStyle()
                .set("display", "flex")
                .set("gap", "var(--lumo-space-s)")
                .set("margin-bottom", "var(--lumo-space-s)")
                .set("align-items", "end");

        ComboBox<String> filter = new ComboBox<>(tr("catalog.category"));
        filter.setItems(CAT_VEHICLE_TYPE, CAT_PERSONNEL_QUAL);
        filter.setItemLabelGenerator(this::categoryLabel);
        filter.setClearButtonVisible(true);
        filter.setWidth("220px");
        filter.addValueChangeListener(
                e -> {
                    categoryFilter = e.getValue();
                    refresh();
                });

        bar.add(filter);
        return bar;
    }

    private Div createGridWrapper() {
        grid.addColumn(ResourceCatalogItem::getCode)
                .setHeader(tr("catalog.code"))
                .setSortable(true)
                .setWidth("100px");

        grid.addColumn(ResourceCatalogItem::getName)
                .setHeader(tr("catalog.name"))
                .setSortable(true)
                .setFlexGrow(2);

        grid.addComponentColumn(
                        item -> {
                            Span badge = new Span(categoryLabel(item.getCategory()));
                            String color =
                                    CAT_VEHICLE_TYPE.equals(item.getCategory())
                                            ? "var(--rom-status-active)"
                                            : "var(--rom-status-info)";
                            badge.getStyle()
                                    .set("font-family", "'JetBrains Mono', monospace")
                                    .set("font-size", "10px")
                                    .set("font-weight", "600")
                                    .set("padding", "2px 6px")
                                    .set("border-radius", "3px")
                                    .set("color", color)
                                    .set(
                                            "background",
                                            "color-mix(in srgb, " + color + " 12%, transparent)");
                            return badge;
                        })
                .setHeader(tr("catalog.category"))
                .setWidth("140px");

        grid.addComponentColumn(
                        item -> {
                            Span status = new Span(item.isActive() ? "OK" : "--");
                            status.getStyle()
                                    .set(
                                            "color",
                                            item.isActive()
                                                    ? "var(--rom-status-active)"
                                                    : "var(--rom-text-muted)");
                            return status;
                        })
                .setHeader(tr("catalog.active"))
                .setWidth("60px");

        grid.addColumn(ResourceCatalogItem::getSortOrder)
                .setHeader(tr("catalog.sortOrder"))
                .setWidth("60px");

        grid.addComponentColumn(
                        item -> {
                            Button edit = new Button(VaadinIcon.EDIT.create());
                            edit.addThemeVariants(
                                    ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
                            edit.getStyle().set("color", "var(--rom-text-muted)");
                            edit.addClickListener(e -> openCatalogDialog(item));

                            Button del = new Button(VaadinIcon.TRASH.create());
                            del.addThemeVariants(
                                    ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
                            del.getStyle().set("color", "var(--rom-status-danger)");
                            del.addClickListener(
                                    e -> {
                                        catalogRepo.delete(item);
                                        refresh();
                                    });

                            HorizontalLayout actions = new HorizontalLayout(edit, del);
                            actions.setSpacing(false);
                            return actions;
                        })
                .setHeader("")
                .setWidth("80px");

        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_COMPACT);
        grid.setAllRowsVisible(true);

        Div wrap = new Div(grid);
        wrap.setWidthFull();
        wrap.getStyle()
                .set("border", "1px solid var(--rom-border)")
                .set("border-radius", "6px")
                .set("overflow", "hidden");
        return wrap;
    }

    private void openCatalogDialog(ResourceCatalogItem existing) {
        boolean isNew = existing == null;
        ResourceCatalogItem item = isNew ? new ResourceCatalogItem() : existing;

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(isNew ? tr("catalog.add") : tr("common.edit"));
        dialog.setWidth("500px");

        TextField code = new TextField(tr("catalog.code"));
        code.setWidthFull();
        code.setValue(item.getCode() != null ? item.getCode() : "");

        TextField name = new TextField(tr("catalog.name"));
        name.setWidthFull();
        name.setValue(item.getName() != null ? item.getName() : "");

        ComboBox<String> category = new ComboBox<>(tr("catalog.category"));
        category.setItems(CAT_VEHICLE_TYPE, CAT_PERSONNEL_QUAL);
        category.setItemLabelGenerator(this::categoryLabel);
        category.setValue(item.getCategory());
        category.setWidthFull();

        IntegerField sortOrder = new IntegerField(tr("catalog.sortOrder"));
        sortOrder.setValue(item.getSortOrder());
        sortOrder.setWidthFull();

        Checkbox active = new Checkbox(tr("catalog.active"));
        active.setValue(item.isActive());

        Div form = new Div(code, name, category, sortOrder, active);
        form.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "var(--lumo-space-s)");
        dialog.add(form);

        Button cancel = new Button(tr("common.cancel"));
        cancel.addClickListener(e -> dialog.close());

        Button save = new Button(tr("common.save"));
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.getStyle()
                .set("background", "var(--rom-accent)")
                .set("color", "var(--rom-bg-primary)");
        save.addClickListener(
                e -> {
                    if (code.getValue().isBlank()
                            || name.getValue().isBlank()
                            || category.getValue() == null) {
                        if (code.getValue().isBlank()) code.setInvalid(true);
                        if (name.getValue().isBlank()) name.setInvalid(true);
                        return;
                    }
                    item.setCode(code.getValue().trim());
                    item.setName(name.getValue().trim());
                    item.setCategory(category.getValue());
                    item.setSortOrder(sortOrder.getValue() != null ? sortOrder.getValue() : 0);
                    item.setActive(active.getValue());
                    catalogRepo.save(item);
                    Notification.show("OK", 2000, Notification.Position.BOTTOM_END)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    dialog.close();
                    refresh();
                });

        dialog.getFooter().add(cancel, save);
        dialog.open();
    }

    private void openImportDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(tr("settings.import.upload"));
        dialog.setWidth("500px");

        Span desc = new Span("CSV: code,name,category,active,sort_order");
        desc.getStyle()
                .set("font-size", "11px")
                .set("color", "var(--rom-text-muted)")
                .set("display", "block")
                .set("margin-bottom", "var(--lumo-space-s)");

        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes(".csv");
        upload.setMaxFiles(1);
        upload.addSucceededListener(
                event -> {
                    try (InputStream is = buffer.getInputStream()) {
                        byte[] csvBytes = is.readAllBytes();
                        int count = importService.importCsv(csvBytes);
                        Notification.show(
                                        tr("settings.import.success", String.valueOf(count)),
                                        4000,
                                        Notification.Position.BOTTOM_END)
                                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                        dialog.close();
                        refresh();
                    } catch (IOException | IllegalArgumentException ex) {
                        Notification.show(
                                        tr("settings.import.error") + ": " + ex.getMessage(),
                                        5000,
                                        Notification.Position.BOTTOM_END)
                                .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    }
                });

        dialog.add(desc, upload);

        Button cancel = new Button(tr("common.cancel"));
        cancel.addClickListener(e -> dialog.close());
        dialog.getFooter().add(cancel);
        dialog.open();
    }

    private void refresh() {
        if (categoryFilter != null) {
            grid.setItems(catalogRepo.findByCategoryOrderBySortOrderAsc(categoryFilter));
        } else {
            grid.setItems(catalogRepo.findAll());
        }
    }

    private String categoryLabel(String cat) {
        if (cat == null) return "";
        return switch (cat) {
            case CAT_VEHICLE_TYPE -> tr("catalog.vehicleTypes");
            case CAT_PERSONNEL_QUAL -> tr("catalog.personnelQual");
            default -> cat;
        };
    }

    private String tr(String key) {
        return t.apply(key, new Object[0]);
    }

    private String tr(String key, String param) {
        return t.apply(key, new Object[] {param});
    }
}
