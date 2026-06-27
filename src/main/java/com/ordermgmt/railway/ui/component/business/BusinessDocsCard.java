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
 * Bloomberg-style card for managing business documents. Pure UI component: callers wire the row
 * supplier, upload sink, and remove sink — so it works equally for a saved business (calls service)
 * or a draft (mutates an in-memory list).
 */
public class BusinessDocsCard extends Div {

    private static final int MAX_UPLOAD_SIZE_BYTES = 10 * 1024 * 1024;
    private static final int SUCCESS_NOTIFICATION_DURATION_MS = 1500;
    private static final int ERROR_NOTIFICATION_DURATION_MS = 3000;
    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";
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

    public BusinessDocsCard(
            Function<String, String> tr,
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

        var uploadBuffer = new MemoryBuffer();
        var upload = new Upload(uploadBuffer);
        upload.setMaxFileSize(MAX_UPLOAD_SIZE_BYTES);
        var uploadBtn =
                new Button(tr.apply("business.uploadDocument"), VaadinIcon.CLOUD_UPLOAD.create());
        uploadBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        upload.setUploadButton(uploadBtn);
        upload.setDropAllowed(false);

        upload.addSucceededListener(
                event -> {
                    try (InputStream inputStream = uploadBuffer.getInputStream()) {
                        byte[] data = inputStream.readAllBytes();
                        onAdd.accept(
                                uploadBuffer.getFileName(),
                                contentTypeOrDefault(event.getMIMEType()),
                                data);
                        refresh();
                        showSuccess("business.documentUploaded");
                    } catch (Exception ex) {
                        Notification.show(
                                        "Fehler: " + ex.getMessage(),
                                        ERROR_NOTIFICATION_DURATION_MS,
                                        Notification.Position.BOTTOM_END)
                                .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    }
                });

        headerRow.add(header, upload);
        return headerRow;
    }

    private void configureGrid(Consumer<UUID> onRemove) {
        grid.addThemeVariants(
                GridVariant.LUMO_COMPACT, GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_ROW_STRIPES);
        grid.setSizeFull();

        grid.addColumn(documentRow -> documentRow.filename() != null ? documentRow.filename() : "")
                .setHeader(tr.apply("business.documentName"))
                .setFlexGrow(2);
        grid.addColumn(
                        documentRow ->
                                documentRow.contentType() != null ? documentRow.contentType() : "")
                .setHeader(tr.apply("business.documentType"))
                .setWidth("220px")
                .setFlexGrow(0);
        grid.addColumn(documentRow -> formatUploadDate(documentRow.createdAt()))
                .setHeader(tr.apply("business.documentUploadDate"))
                .setWidth("160px")
                .setFlexGrow(0);

        grid.addComponentColumn(
                        documentRow -> buildRemoveButton(documentRow, onRemove))
                .setHeader("")
                .setWidth("44px")
                .setFlexGrow(0);
    }

    private Button buildRemoveButton(DocRow documentRow, Consumer<UUID> onRemove) {
        var button =
                new Button(
                        VaadinIcon.CLOSE_SMALL.create(),
                        e -> confirmRemove(documentRow, onRemove));
        button.addThemeVariants(
                ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        return button;
    }

    private void confirmRemove(DocRow row, Consumer<UUID> onRemove) {
        var dialog = new ConfirmDialog();
        dialog.setHeader(tr.apply("business.deleteDocumentQuestion"));
        dialog.setCancelable(true);
        dialog.setConfirmText(tr.apply("common.delete"));
        dialog.setCancelText(tr.apply("common.cancel"));
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(
                e -> {
                    onRemove.accept(row.id());
                    refresh();
                    showSuccess("business.documentRemoved");
                });
        dialog.open();
    }

    private void showSuccess(String translationKey) {
        Notification.show(
                        tr.apply(translationKey),
                        SUCCESS_NOTIFICATION_DURATION_MS,
                        Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private static String contentTypeOrDefault(String contentType) {
        return contentType != null ? contentType : DEFAULT_CONTENT_TYPE;
    }

    private static String formatUploadDate(Instant createdAt) {
        if (createdAt == null) {
            return "—";
        }
        LocalDateTime localDateTime = createdAt.atZone(ZoneId.systemDefault()).toLocalDateTime();
        return localDateTime.format(DATE_TIME_FMT);
    }
}
