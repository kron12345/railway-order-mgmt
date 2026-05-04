package com.ordermgmt.railway.ui.component.business;

import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;

/**
 * Bloomberg-style card for managing business documents. Pure UI component: callers wire
 * the row supplier, upload sink, and remove sink — so it works equally for a saved
 * business (calls service) or a draft (mutates an in-memory list).
 */
public class BusinessDocsCard extends Div {

    private static final DateTimeFormatter DATE_TIME_FMT =
            DateTimeFormatter.ofPattern("dd.MM.yy HH:mm");

    /** Row shown in the grid. {@code id} must be stable so remove can find it. */
    public record DocRow(UUID id, String filename, String contentType, Instant createdAt) {}

    @FunctionalInterface
    public interface UploadFn {
        void accept(String filename, String contentType, byte[] data);
    }

    private final Function<String, String> tr;
    private final Supplier<List<DocRow>> rowsSupplier;
    private final Grid<DocRow> grid = new Grid<>();

    public BusinessDocsCard(Function<String, String> tr,
                            Supplier<List<DocRow>> rowsSupplier,
                            UploadFn onAdd,
                            Consumer<UUID> onRemove) {
        this.tr = tr;
        this.rowsSupplier = rowsSupplier;

        addClassName("biz-card");
        addClassName("biz-card--flex");

        add(buildHeader(onAdd));
        configureGrid(onRemove);
        add(grid);
        refresh();
    }

    public void refresh() {
        grid.setItems(rowsSupplier.get());
    }

    private HorizontalLayout buildHeader(UploadFn onAdd) {
        var headerRow = new HorizontalLayout();
        headerRow.setWidthFull();
        headerRow.setPadding(false);
        headerRow.setSpacing(false);
        headerRow.setAlignItems(FlexComponent.Alignment.CENTER);
        headerRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        var header = new Span(tr.apply("business.documents").toUpperCase());
        header.addClassName("biz-section-title");

        var receiver = new MemoryBuffer();
        var upload = new Upload(receiver);
        upload.setMaxFileSize(10 * 1024 * 1024);
        var uploadBtn = new Button(tr.apply("business.uploadDocument"),
                VaadinIcon.CLOUD_UPLOAD.create());
        uploadBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        upload.setUploadButton(uploadBtn);
        upload.setDropAllowed(false);

        upload.addSucceededListener(event -> {
            try (InputStream is = receiver.getInputStream()) {
                byte[] data = is.readAllBytes();
                onAdd.accept(receiver.getFileName(),
                        event.getMIMEType() != null ? event.getMIMEType()
                                : "application/octet-stream",
                        data);
                refresh();
                Notification.show(tr.apply("business.documentUploaded"), 1500,
                        Notification.Position.BOTTOM_END)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                Notification.show("Fehler: " + ex.getMessage(), 3000,
                        Notification.Position.BOTTOM_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        headerRow.add(header, upload);
        return headerRow;
    }

    private void configureGrid(Consumer<UUID> onRemove) {
        grid.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_NO_BORDER,
                GridVariant.LUMO_ROW_STRIPES);
        grid.setSizeFull();

        grid.addColumn(d -> d.filename() != null ? d.filename() : "")
                .setHeader(tr.apply("business.documentName")).setFlexGrow(2);
        grid.addColumn(d -> d.contentType() != null ? d.contentType() : "")
                .setHeader(tr.apply("business.documentType")).setWidth("220px").setFlexGrow(0);
        grid.addColumn(d -> {
            var ts = d.createdAt();
            if (ts == null) return "—";
            LocalDateTime ldt = ts.atZone(ZoneId.systemDefault()).toLocalDateTime();
            return ldt.format(DATE_TIME_FMT);
        }).setHeader(tr.apply("business.documentUploadDate"))
                .setWidth("160px").setFlexGrow(0);

        grid.addComponentColumn(d -> {
            var btn = new Button(VaadinIcon.CLOSE_SMALL.create(),
                    e -> confirmRemove(d, onRemove));
            btn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY,
                    ButtonVariant.LUMO_ICON);
            return btn;
        }).setHeader("").setWidth("44px").setFlexGrow(0);
    }

    private void confirmRemove(DocRow row, Consumer<UUID> onRemove) {
        var dialog = new ConfirmDialog();
        dialog.setHeader(tr.apply("business.deleteDocumentQuestion"));
        dialog.setCancelable(true);
        dialog.setConfirmText(tr.apply("common.delete"));
        dialog.setCancelText(tr.apply("common.cancel"));
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(e -> {
            onRemove.accept(row.id());
            refresh();
            Notification.show(tr.apply("business.documentRemoved"), 1500,
                    Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });
        dialog.open();
    }
}
