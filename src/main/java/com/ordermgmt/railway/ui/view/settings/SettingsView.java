package com.ordermgmt.railway.ui.view.settings;

import java.io.InputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import jakarta.annotation.security.PermitAll;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import com.ordermgmt.railway.domain.infrastructure.model.ImportLog;
import com.ordermgmt.railway.domain.infrastructure.service.RinfImportService;
import com.ordermgmt.railway.ui.layout.MainLayout;

@Route(value = "settings", layout = MainLayout.class)
@PageTitle("Settings")
@PermitAll
public class SettingsView extends VerticalLayout {

    private final RinfImportService importService;
    private final Grid<ImportLog> historyGrid = new Grid<>(ImportLog.class, false);
    private Span opsCountCh;
    private Span opsCountDe;
    private Span solsCountCh;
    private Span solsCountDe;

    private static final DateTimeFormatter DT = DateTimeFormatter
            .ofPattern("dd.MM.yyyy HH:mm:ss").withZone(ZoneId.systemDefault());

    public SettingsView(RinfImportService importService) {
        this.importService = importService;
        setPadding(false);
        setWidthFull();
        getStyle()
                .set("background", "var(--rom-bg-primary)")
                .set("padding", "var(--lumo-space-xs) var(--lumo-space-m)")
                .set("overflow-x", "hidden")
                .set("box-sizing", "border-box");

        add(createHeader());
        add(createStatsPanel());
        add(createImportPanel());
        add(createHistoryPanel());
        refreshStats();
        refreshHistory();
    }

    private H2 createHeader() {
        H2 title = new H2(getTranslation("settings.title"));
        title.getStyle()
                .set("color", "var(--rom-text-primary)")
                .set("font-weight", "600")
                .set("margin", "0 0 var(--lumo-space-s) 0");
        return title;
    }

    private Div createStatsPanel() {
        Div panel = card();
        H3 title = sectionTitle(getTranslation("settings.stats"));
        panel.add(title);

        HorizontalLayout stats = new HorizontalLayout();
        stats.setWidthFull();
        stats.getStyle().set("gap", "24px").set("flex-wrap", "wrap");

        opsCountCh = statBox("CH " + getTranslation("settings.stats.ops"), "0");
        opsCountDe = statBox("DE " + getTranslation("settings.stats.ops"), "0");
        solsCountCh = statBox("CH " + getTranslation("settings.stats.sols"), "0");
        solsCountDe = statBox("DE " + getTranslation("settings.stats.sols"), "0");

        stats.add(opsCountCh.getParent().get(), opsCountDe.getParent().get(),
                solsCountCh.getParent().get(), solsCountDe.getParent().get());
        panel.add(stats);
        return panel;
    }

    private Div createImportPanel() {
        Div panel = card();
        H3 title = sectionTitle(getTranslation("settings.infrastructure"));
        Span desc = new Span(getTranslation("settings.infrastructure.desc"));
        desc.getStyle().set("color", "var(--rom-text-muted)").set("font-size", "12px")
                .set("display", "block").set("margin-bottom", "var(--lumo-space-m)");
        panel.add(title, desc);

        // Import row: Country + Type + Upload
        ComboBox<String> countrySelect = new ComboBox<>(getTranslation("settings.import.country"));
        countrySelect.setItems("CHE", "DEU");
        countrySelect.setItemLabelGenerator(c -> "CHE".equals(c) ? "Schweiz (CH)" : "Deutschland (DE)");
        countrySelect.setValue("CHE");
        countrySelect.setWidth("200px");

        ComboBox<String> typeSelect = new ComboBox<>("Typ");
        typeSelect.setItems("OP", "SOL");
        typeSelect.setItemLabelGenerator(t -> "OP".equals(t)
                ? getTranslation("settings.import.ops")
                : getTranslation("settings.import.sols"));
        typeSelect.setValue("OP");
        typeSelect.setWidth("200px");

        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes(".csv");
        upload.setMaxFiles(1);
        upload.setDropLabel(new Span(getTranslation("settings.import.upload")));
        upload.setUploadButton(
                new Button(getTranslation("settings.import.upload"), VaadinIcon.UPLOAD.create()));

        upload.addSucceededListener(event -> {
            String country = countrySelect.getValue();
            String type = typeSelect.getValue();
            InputStream is = buffer.getInputStream();

            ImportLog result;
            if ("OP".equals(type)) {
                result = importService.importOperationalPoints(is, country);
            } else {
                result = importService.importSectionsOfLine(is, country);
            }

            if ("SUCCESS".equals(result.getStatus())) {
                Notification.show(
                        getTranslation("settings.import.success", String.valueOf(result.getRecordCount())),
                        5000, Notification.Position.BOTTOM_END)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } else {
                Notification.show(
                        getTranslation("settings.import.error") + ": " + result.getMessage(),
                        5000, Notification.Position.BOTTOM_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
            refreshStats();
            refreshHistory();
        });

        HorizontalLayout importRow = new HorizontalLayout(countrySelect, typeSelect, upload);
        importRow.setAlignItems(FlexComponent.Alignment.END);
        importRow.getStyle().set("flex-wrap", "wrap").set("gap", "12px");
        panel.add(importRow);
        return panel;
    }

    private Div createHistoryPanel() {
        Div panel = card();
        H3 title = sectionTitle(getTranslation("settings.import.history"));
        panel.add(title);

        historyGrid.addColumn(ImportLog::getSource).setHeader("Source").setWidth("100px");
        historyGrid.addColumn(ImportLog::getCountry).setHeader("Land").setWidth("60px");
        historyGrid.addColumn(ImportLog::getRecordCount).setHeader("Records").setWidth("80px");
        historyGrid.addColumn(l -> l.getStartedAt() != null ? DT.format(l.getStartedAt()) : "—")
                .setHeader("Gestartet").setWidth("160px");
        historyGrid.addComponentColumn(l -> {
            String color = switch (l.getStatus()) {
                case "SUCCESS" -> "var(--rom-status-active)";
                case "ERROR" -> "var(--rom-status-danger)";
                default -> "var(--rom-status-info)";
            };
            Span badge = new Span(l.getStatus());
            badge.getStyle()
                    .set("font-family", "'JetBrains Mono', monospace")
                    .set("font-size", "10px").set("font-weight", "600")
                    .set("color", color)
                    .set("background", "color-mix(in srgb, " + color + " 12%, transparent)")
                    .set("padding", "2px 6px").set("border-radius", "3px");
            return badge;
        }).setHeader("Status").setWidth("100px");
        historyGrid.addColumn(ImportLog::getMessage).setHeader("Details").setFlexGrow(1);

        historyGrid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_COMPACT);
        historyGrid.setAllRowsVisible(true);
        panel.add(historyGrid);
        return panel;
    }

    private void refreshStats() {
        opsCountCh.setText(String.valueOf(importService.countOps("CHE")));
        opsCountDe.setText(String.valueOf(importService.countOps("DEU")));
        solsCountCh.setText(String.valueOf(importService.countSols("CHE")));
        solsCountDe.setText(String.valueOf(importService.countSols("DEU")));
    }

    private void refreshHistory() {
        historyGrid.setItems(importService.getImportHistory());
    }

    private Div card() {
        Div div = new Div();
        div.setWidthFull();
        div.getStyle()
                .set("background", "var(--rom-bg-card)")
                .set("border", "1px solid var(--rom-border)")
                .set("border-radius", "6px")
                .set("padding", "var(--lumo-space-m) var(--lumo-space-l)")
                .set("margin-bottom", "var(--lumo-space-s)")
                .set("box-sizing", "border-box");
        return div;
    }

    private H3 sectionTitle(String text) {
        H3 h = new H3(text);
        h.getStyle()
                .set("color", "var(--rom-text-primary)")
                .set("margin", "0 0 var(--lumo-space-s) 0")
                .set("font-size", "var(--lumo-font-size-m)");
        return h;
    }

    private Span statBox(String label, String value) {
        Div box = new Div();
        box.getStyle()
                .set("background", "var(--rom-bg-primary)")
                .set("border", "1px solid var(--rom-border)")
                .set("border-radius", "6px")
                .set("padding", "12px 16px")
                .set("min-width", "140px");

        Span val = new Span(value);
        val.getStyle()
                .set("font-family", "'JetBrains Mono', monospace")
                .set("font-size", "20px")
                .set("font-weight", "700")
                .set("color", "var(--rom-accent)")
                .set("display", "block");

        Span lbl = new Span(label);
        lbl.getStyle()
                .set("font-size", "11px")
                .set("color", "var(--rom-text-muted)")
                .set("display", "block");

        box.add(val, lbl);
        return val;
    }
}
