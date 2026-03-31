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
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;

import com.ordermgmt.railway.domain.infrastructure.model.PredefinedTag;
import com.ordermgmt.railway.domain.infrastructure.repository.PredefinedTagRepository;
import com.ordermgmt.railway.domain.infrastructure.service.PredefinedTagImportService;

/** Manage predefined tags for orders and positions. */
public class TagsTab extends Div {

    private final PredefinedTagRepository tagRepo;
    private final PredefinedTagImportService importService;
    private final Grid<PredefinedTag> grid = new Grid<>(PredefinedTag.class, false);
    private final BiFunction<String, Object[], String> t;

    public TagsTab(PredefinedTagRepository tagRepo,
                   PredefinedTagImportService importService,
                   BiFunction<String, Object[], String> translator) {
        this.tagRepo = tagRepo;
        this.importService = importService;
        this.t = translator;
        setWidthFull();

        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.getStyle().set("margin-bottom", "var(--lumo-space-s)");

        H3 title = new H3(tr("settings.tags"));
        title.getStyle()
                .set("color", "var(--rom-text-primary)")
                .set("margin", "0")
                .set("font-size", "var(--lumo-font-size-m)");

        Button addBtn = new Button(tr("settings.tags.new"), VaadinIcon.PLUS.create());
        addBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
        addBtn.getStyle()
                .set("background", "var(--rom-accent)")
                .set("color", "var(--rom-bg-primary)");
        addBtn.addClickListener(e -> openTagDialog(null));

        header.add(title, addBtn);
        add(header, createImportSection());

        grid.addColumn(PredefinedTag::getName)
                .setHeader(tr("settings.tags.name")).setSortable(true).setFlexGrow(2);

        grid.addComponentColumn(tag -> {
            Span badge = new Span(categoryLabel(tag.getCategory()));
            String color = switch (tag.getCategory()) {
                case "ORDER" -> "var(--rom-accent)";
                case "POSITION" -> "var(--rom-status-info)";
                default -> "var(--rom-text-muted)";
            };
            badge.getStyle()
                    .set("font-family", "'JetBrains Mono', monospace")
                    .set("font-size", "10px")
                    .set("font-weight", "600")
                    .set("padding", "2px 6px")
                    .set("border-radius", "3px")
                    .set("color", color)
                    .set("background", "color-mix(in srgb, " + color + " 12%, transparent)");
            return badge;
        }).setHeader(tr("settings.tags.category")).setWidth("120px");

        grid.addComponentColumn(tag -> {
            if (tag.getColor() == null) return new Span("—");
            Div dot = new Div();
            dot.getStyle()
                    .set("width", "16px").set("height", "16px")
                    .set("border-radius", "4px")
                    .set("background", tag.getColor());
            return dot;
        }).setHeader(tr("settings.tags.color")).setWidth("60px");

        grid.addColumn(PredefinedTag::getSortOrder)
                .setHeader("#").setWidth("50px");

        grid.addComponentColumn(tag -> {
            Span status = new Span(tag.isActive() ? "✓" : "—");
            status.getStyle().set("color",
                    tag.isActive() ? "var(--rom-status-active)" : "var(--rom-text-muted)");
            return status;
        }).setHeader(tr("settings.tags.active")).setWidth("60px");

        grid.addComponentColumn(tag -> {
            Button edit = new Button(VaadinIcon.EDIT.create());
            edit.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            edit.getStyle().set("color", "var(--rom-text-muted)");
            edit.addClickListener(e -> openTagDialog(tag));

            Button del = new Button(VaadinIcon.TRASH.create());
            del.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            del.getStyle().set("color", "var(--rom-status-danger)");
            del.addClickListener(e -> {
                tagRepo.delete(tag);
                refresh();
            });

            HorizontalLayout actions = new HorizontalLayout(edit, del);
            actions.setSpacing(false);
            return actions;
        }).setHeader("").setWidth("80px");

        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_COMPACT);
        grid.setAllRowsVisible(true);

        Div wrap = new Div(grid);
        wrap.setWidthFull();
        wrap.getStyle()
                .set("border", "1px solid var(--rom-border)")
                .set("border-radius", "6px")
                .set("overflow", "hidden");
        add(wrap);

        refresh();
    }

    private void openTagDialog(PredefinedTag existing) {
        boolean isNew = existing == null;
        PredefinedTag tag = isNew ? new PredefinedTag() : existing;

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(isNew ? tr("settings.tags.new") : tr("settings.tags.edit"));
        dialog.setWidth("500px");

        TextField name = new TextField(tr("settings.tags.name"));
        name.setWidthFull();
        name.setValue(tag.getName() != null ? tag.getName() : "");

        ComboBox<String> category = new ComboBox<>(tr("settings.tags.category"));
        category.setItems("ORDER", "POSITION", "GENERAL");
        category.setItemLabelGenerator(this::categoryLabel);
        category.setValue(tag.getCategory());
        category.setWidthFull();

        TextField color = new TextField(tr("settings.tags.color"));
        color.setWidthFull();
        color.setValue(tag.getColor() != null ? tag.getColor() : "#FFB800");
        color.setHelperText("Hex-Farbe, z.B. #FFB800");

        IntegerField sortOrder = new IntegerField("#");
        sortOrder.setValue(tag.getSortOrder());
        sortOrder.setWidthFull();

        Checkbox active = new Checkbox(tr("settings.tags.active"));
        active.setValue(tag.isActive());

        Div form = new Div(name, category, color, sortOrder, active);
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
        save.addClickListener(e -> {
            if (name.getValue().isBlank()) {
                name.setInvalid(true);
                return;
            }
            tag.setName(name.getValue().trim());
            tag.setCategory(category.getValue());
            tag.setColor(color.getValue().isBlank() ? null : color.getValue().trim());
            tag.setSortOrder(sortOrder.getValue() != null ? sortOrder.getValue() : 0);
            tag.setActive(active.getValue());
            tagRepo.save(tag);
            Notification.show("✓", 2000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            dialog.close();
            refresh();
        });

        dialog.getFooter().add(cancel, save);
        dialog.open();
    }

    private void refresh() {
        grid.setItems(tagRepo.findAllByOrderByCategoryAscSortOrderAsc());
    }

    private Div createImportSection() {
        Div section = new Div();
        section.setWidthFull();
        section.getStyle()
                .set("display", "flex")
                .set("justify-content", "space-between")
                .set("align-items", "end")
                .set("gap", "var(--lumo-space-s)")
                .set("flex-wrap", "wrap")
                .set("padding", "0 0 var(--lumo-space-s) 0");

        Div text = new Div();
        text.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "4px");

        Span title = new Span(tr("settings.tags.import"));
        title.getStyle()
                .set("font-weight", "600")
                .set("font-size", "12px")
                .set("color", "var(--rom-text-primary)");

        Span desc = new Span(tr("settings.tags.import.desc"));
        desc.getStyle()
                .set("font-size", "11px")
                .set("color", "var(--rom-text-muted)");

        Span sample = new Span(tr("settings.tags.import.sample", "data/seeds/predefined-tags.csv"));
        sample.getStyle()
                .set("font-family", "'JetBrains Mono', monospace")
                .set("font-size", "10px")
                .set("color", "var(--rom-text-muted)");
        text.add(title, desc, sample);

        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes(".csv");
        upload.setMaxFiles(1);
        upload.setUploadButton(new Button(tr("settings.tags.import.upload"),
                VaadinIcon.UPLOAD.create()));
        upload.addSucceededListener(event -> importCsv(buffer));

        section.add(text, upload);
        return section;
    }

    private void importCsv(MemoryBuffer buffer) {
        try (InputStream inputStream = buffer.getInputStream()) {
            int count = importService.importCsv(inputStream);
            Notification.show(
                            tr("settings.tags.import.success", String.valueOf(count)),
                            4000,
                            Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            refresh();
        } catch (IOException | IllegalArgumentException e) {
            Notification.show(
                            tr("settings.tags.import.error") + ": " + e.getMessage(),
                            5000,
                            Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private String categoryLabel(String cat) {
        return switch (cat) {
            case "ORDER" -> tr("settings.tags.cat.order");
            case "POSITION" -> tr("settings.tags.cat.position");
            case "GENERAL" -> tr("settings.tags.cat.general");
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
