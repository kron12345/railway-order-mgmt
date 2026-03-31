package com.ordermgmt.railway.ui.view.settings;

import java.io.InputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import jakarta.annotation.security.PermitAll;

import com.vaadin.flow.component.button.Button;
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

/** Administrative view for importing infrastructure data and reviewing import history. */
@Route(value = "settings", layout = MainLayout.class)
@PageTitle("Settings")
@PermitAll // TODO: restrict to @RolesAllowed("ADMIN") once Keycloak role mapping verified
public class SettingsView extends VerticalLayout {

    private final RinfImportService importService;
    private final Grid<ImportLog> historyGrid = new Grid<>(ImportLog.class, false);
    private Span opsCountCh;
    private Span opsCountDe;
    private Span solsCountCh;
    private Span solsCountDe;

    private static final DateTimeFormatter DT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss").withZone(ZoneId.systemDefault());

    public SettingsView(RinfImportService importService) {
        this.importService = importService;
        setPadding(false);
        setWidthFull();
        getStyle()
                .set("background", "var(--rom-bg-primary)")
                .set("padding", "var(--lumo-space-xs) var(--lumo-space-m)")
                .set("overflow-x", "hidden")
                .set("box-sizing", "border-box");

        configureHistoryGrid();
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

        StatCard opsChCard = createStatCard("CH " + getTranslation("settings.stats.ops"), "0");
        StatCard opsDeCard = createStatCard("DE " + getTranslation("settings.stats.ops"), "0");
        StatCard solsChCard = createStatCard("CH " + getTranslation("settings.stats.sols"), "0");
        StatCard solsDeCard = createStatCard("DE " + getTranslation("settings.stats.sols"), "0");

        opsCountCh = opsChCard.valueSpan();
        opsCountDe = opsDeCard.valueSpan();
        solsCountCh = solsChCard.valueSpan();
        solsCountDe = solsDeCard.valueSpan();

        stats.add(
                opsChCard.container(),
                opsDeCard.container(),
                solsChCard.container(),
                solsDeCard.container());
        panel.add(stats);
        return panel;
    }

    private Div createImportPanel() {
        Div panel = card();
        H3 title = sectionTitle(getTranslation("settings.infrastructure"));
        Span desc = new Span(getTranslation("settings.infrastructure.desc"));
        desc.getStyle()
                .set("color", "var(--rom-text-muted)")
                .set("font-size", "12px")
                .set("display", "block")
                .set("margin-bottom", "var(--lumo-space-m)");
        panel.add(title, desc);

        ComboBox<String> countrySelect = createCountrySelect();
        ComboBox<String> typeSelect = createImportTypeSelect();
        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = createUpload(buffer, countrySelect, typeSelect);

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
        panel.add(historyGrid);
        return panel;
    }

    private ComboBox<String> createCountrySelect() {
        ComboBox<String> countrySelect = new ComboBox<>(getTranslation("settings.import.country"));
        countrySelect.setItems("CHE", "DEU");
        countrySelect.setItemLabelGenerator(
                country ->
                        "CHE".equals(country)
                                ? getTranslation("settings.country.che")
                                : getTranslation("settings.country.deu"));
        countrySelect.setValue("CHE");
        countrySelect.setWidth("200px");
        return countrySelect;
    }

    private ComboBox<String> createImportTypeSelect() {
        ComboBox<String> typeSelect = new ComboBox<>(getTranslation("settings.import.type"));
        typeSelect.setItems("OP", "SOL");
        typeSelect.setItemLabelGenerator(
                type ->
                        "OP".equals(type)
                                ? getTranslation("settings.import.ops")
                                : getTranslation("settings.import.sols"));
        typeSelect.setValue("OP");
        typeSelect.setWidth("200px");
        return typeSelect;
    }

    private Upload createUpload(
            MemoryBuffer buffer, ComboBox<String> countrySelect, ComboBox<String> typeSelect) {
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes(".csv");
        upload.setMaxFiles(1);
        upload.setMaxFileSize(50 * 1024 * 1024);
        upload.setDropLabel(new Span(getTranslation("settings.import.upload")));
        upload.setUploadButton(
                new Button(getTranslation("settings.import.upload"), VaadinIcon.UPLOAD.create()));
        upload.addSucceededListener(
                event -> handleImport(countrySelect.getValue(), typeSelect.getValue(), buffer));
        return upload;
    }

    private void handleImport(String country, String type, MemoryBuffer buffer) {
        InputStream inputStream = buffer.getInputStream();
        ImportLog result = importFile(type, inputStream, country);

        if ("SUCCESS".equals(result.getStatus())) {
            Notification.show(
                            getTranslation(
                                    "settings.import.success",
                                    String.valueOf(result.getRecordCount())),
                            5000,
                            Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } else {
            Notification.show(
                            getTranslation("settings.import.error") + ": " + result.getMessage(),
                            5000,
                            Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
        refreshStats();
        refreshHistory();
    }

    private ImportLog importFile(String type, InputStream inputStream, String country) {
        if ("OP".equals(type)) {
            return importService.importOperationalPoints(inputStream, country);
        }
        return importService.importSectionsOfLine(inputStream, country);
    }

    private void configureHistoryGrid() {
        historyGrid
                .addColumn(ImportLog::getSource)
                .setHeader(getTranslation("settings.history.source"))
                .setWidth("100px");
        historyGrid
                .addColumn(ImportLog::getCountry)
                .setHeader(getTranslation("settings.history.country"))
                .setWidth("60px");
        historyGrid
                .addColumn(ImportLog::getRecordCount)
                .setHeader(getTranslation("settings.history.records"))
                .setWidth("80px");
        historyGrid
                .addColumn(this::formatStartedAt)
                .setHeader(getTranslation("settings.history.started"))
                .setWidth("160px");
        historyGrid
                .addComponentColumn(this::createHistoryStatusBadge)
                .setHeader(getTranslation("settings.history.status"))
                .setWidth("100px");
        historyGrid
                .addColumn(ImportLog::getMessage)
                .setHeader(getTranslation("settings.history.details"))
                .setFlexGrow(1);
        historyGrid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_COMPACT);
        historyGrid.setAllRowsVisible(true);
    }

    private String formatStartedAt(ImportLog importLog) {
        return importLog.getStartedAt() != null ? DT.format(importLog.getStartedAt()) : "—";
    }

    private Span createHistoryStatusBadge(ImportLog importLog) {
        String color =
                switch (importLog.getStatus()) {
                    case "SUCCESS" -> "var(--rom-status-active)";
                    case "ERROR" -> "var(--rom-status-danger)";
                    default -> "var(--rom-status-info)";
                };
        Span badge = new Span(importLog.getStatus());
        badge.getStyle()
                .set("font-family", "'JetBrains Mono', monospace")
                .set("font-size", "10px")
                .set("font-weight", "600")
                .set("color", color)
                .set("background", "color-mix(in srgb, " + color + " 12%, transparent)")
                .set("padding", "2px 6px")
                .set("border-radius", "3px");
        return badge;
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

    private StatCard createStatCard(String label, String value) {
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
        return new StatCard(box, val);
    }

    private record StatCard(Div container, Span valueSpan) {}
}
