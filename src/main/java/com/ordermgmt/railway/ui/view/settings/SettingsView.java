package com.ordermgmt.railway.ui.view.settings;

import java.io.InputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import jakarta.annotation.security.RolesAllowed;

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
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import com.ordermgmt.railway.domain.infrastructure.model.ImportLog;
import com.ordermgmt.railway.domain.infrastructure.repository.OperationalPointRepository;
import com.ordermgmt.railway.domain.infrastructure.repository.PredefinedTagRepository;
import com.ordermgmt.railway.domain.infrastructure.service.PredefinedTagImportService;
import com.ordermgmt.railway.domain.infrastructure.service.RinfImportService;
import com.ordermgmt.railway.domain.order.repository.ResourceCatalogItemRepository;
import com.ordermgmt.railway.domain.order.service.ResourceCatalogImportService;
import com.ordermgmt.railway.ui.layout.MainLayout;

/** Admin settings with tabs: Infrastructure, Topology, Tags. */
@Route(value = "settings", layout = MainLayout.class)
@PageTitle("Settings")
@RolesAllowed("ADMIN")
public class SettingsView extends VerticalLayout {

    private final RinfImportService importService;
    private final OperationalPointRepository opRepo;
    private final PredefinedTagRepository tagRepo;
    private final PredefinedTagImportService tagImportService;
    private final ResourceCatalogItemRepository catalogItemRepository;
    private final ResourceCatalogImportService catalogImportService;

    private final Div tabContent = new Div();

    private static final DateTimeFormatter DT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss").withZone(ZoneId.systemDefault());

    /** ISO country codes used for RINF import selection. */
    private static final String COUNTRY_SWITZERLAND = "CHE";

    private static final String COUNTRY_GERMANY = "DEU";

    /** RINF import type identifiers. */
    private static final String IMPORT_TYPE_OPERATIONAL_POINTS = "OP";

    private static final String IMPORT_TYPE_SECTIONS_OF_LINE = "SOL";

    public SettingsView(
            RinfImportService importService,
            OperationalPointRepository opRepo,
            PredefinedTagRepository tagRepo,
            PredefinedTagImportService tagImportService,
            ResourceCatalogItemRepository catalogItemRepository,
            ResourceCatalogImportService catalogImportService) {
        this.importService = importService;
        this.opRepo = opRepo;
        this.tagRepo = tagRepo;
        this.tagImportService = tagImportService;
        this.catalogItemRepository = catalogItemRepository;
        this.catalogImportService = catalogImportService;

        setPadding(false);
        setWidthFull();
        setSizeFull();
        getStyle()
                .set("background", "var(--rom-bg-primary)")
                .set("padding", "var(--lumo-space-xs) var(--lumo-space-m)")
                .set("overflow-x", "hidden")
                .set("box-sizing", "border-box");

        H2 title = new H2(getTranslation("settings.title"));
        title.getStyle()
                .set("color", "var(--rom-text-primary)")
                .set("font-weight", "600")
                .set("margin", "0 0 var(--lumo-space-s) 0");
        add(title);

        Tab infraTab =
                new Tab(
                        VaadinIcon.DATABASE.create(),
                        new Span(getTranslation("settings.infrastructure")));
        Tab topoTab =
                new Tab(
                        VaadinIcon.MAP_MARKER.create(),
                        new Span(getTranslation("settings.topology")));
        Tab tagsTab = new Tab(VaadinIcon.TAGS.create(), new Span(getTranslation("settings.tags")));
        Tab catalogTab =
                new Tab(VaadinIcon.LIST.create(), new Span(getTranslation("catalog.title")));

        Tabs tabs = new Tabs(infraTab, topoTab, tagsTab, catalogTab);
        tabs.setWidthFull();
        tabs.addSelectedChangeListener(
                e -> {
                    if (e.getSelectedTab() == infraTab) showInfraTab();
                    else if (e.getSelectedTab() == topoTab) showTopoTab();
                    else if (e.getSelectedTab() == tagsTab) showTagsTab();
                    else if (e.getSelectedTab() == catalogTab) showCatalogTab();
                });
        add(tabs);

        tabContent.setWidthFull();
        tabContent
                .getStyle()
                .set("background", "var(--rom-bg-card)")
                .set("border", "1px solid var(--rom-border)")
                .set("border-radius", "0 0 6px 6px")
                .set("padding", "var(--lumo-space-m) var(--lumo-space-l)")
                .set("box-sizing", "border-box")
                .set("flex", "1")
                .set("overflow", "auto");
        add(tabContent);

        showInfraTab();
    }

    private void showInfraTab() {
        tabContent.removeAll();
        buildStatsSection();
        buildImportSection();
        buildHistoryGrid();
    }

    private void buildStatsSection() {
        H3 statsTitle = sectionTitle(getTranslation("settings.stats"));
        tabContent.add(statsTitle);

        Div stats = new Div();
        stats.getStyle()
                .set("display", "flex")
                .set("gap", "16px")
                .set("flex-wrap", "wrap")
                .set("margin-bottom", "var(--lumo-space-m)");
        stats.add(statBox("CH OPs", String.valueOf(importService.countOps(COUNTRY_SWITZERLAND))));
        stats.add(statBox("DE OPs", String.valueOf(importService.countOps(COUNTRY_GERMANY))));
        stats.add(statBox("CH SoLs", String.valueOf(importService.countSols(COUNTRY_SWITZERLAND))));
        stats.add(statBox("DE SoLs", String.valueOf(importService.countSols(COUNTRY_GERMANY))));
        tabContent.add(stats);
    }

    private void buildImportSection() {
        H3 importTitle = sectionTitle(getTranslation("settings.infrastructure"));
        Span desc = new Span(getTranslation("settings.infrastructure.desc"));
        desc.getStyle()
                .set("color", "var(--rom-text-muted)")
                .set("font-size", "11px")
                .set("display", "block")
                .set("margin-bottom", "var(--lumo-space-s)");
        tabContent.add(importTitle, desc);

        ComboBox<String> countrySelect = new ComboBox<>(getTranslation("settings.import.country"));
        countrySelect.setItems(COUNTRY_SWITZERLAND, COUNTRY_GERMANY);
        countrySelect.setItemLabelGenerator(
                c -> COUNTRY_SWITZERLAND.equals(c) ? "Schweiz (CH)" : "Deutschland (DE)");
        countrySelect.setValue(COUNTRY_SWITZERLAND);
        countrySelect.setWidth("180px");

        ComboBox<String> typeSelect = new ComboBox<>(getTranslation("settings.import.type"));
        typeSelect.setItems(IMPORT_TYPE_OPERATIONAL_POINTS, IMPORT_TYPE_SECTIONS_OF_LINE);
        typeSelect.setItemLabelGenerator(
                tp ->
                        IMPORT_TYPE_OPERATIONAL_POINTS.equals(tp)
                                ? getTranslation("settings.import.ops")
                                : getTranslation("settings.import.sols"));
        typeSelect.setValue(IMPORT_TYPE_OPERATIONAL_POINTS);
        typeSelect.setWidth("180px");

        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes(".csv");
        upload.setMaxFiles(1);
        upload.setMaxFileSize(50 * 1024 * 1024);
        upload.setUploadButton(
                new Button(getTranslation("settings.import.upload"), VaadinIcon.UPLOAD.create()));
        upload.addSucceededListener(
                event -> {
                    InputStream is = buffer.getInputStream();
                    ImportLog result =
                            IMPORT_TYPE_OPERATIONAL_POINTS.equals(typeSelect.getValue())
                                    ? importService.importOperationalPoints(
                                            is, countrySelect.getValue())
                                    : importService.importSectionsOfLine(
                                            is, countrySelect.getValue());
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
                                        getTranslation("settings.import.error")
                                                + ": "
                                                + result.getMessage(),
                                        5000,
                                        Notification.Position.BOTTOM_END)
                                .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    }
                    showInfraTab(); // refresh stats
                });

        Div importRow = new Div(countrySelect, typeSelect, upload);
        importRow
                .getStyle()
                .set("display", "flex")
                .set("gap", "var(--lumo-space-s)")
                .set("flex-wrap", "wrap")
                .set("align-items", "end")
                .set("margin-bottom", "var(--lumo-space-m)");
        tabContent.add(importRow);
    }

    private void buildHistoryGrid() {
        H3 histTitle = sectionTitle(getTranslation("settings.import.history"));
        tabContent.add(histTitle);

        Grid<ImportLog> historyGrid = new Grid<>(ImportLog.class, false);
        historyGrid.addColumn(ImportLog::getSource).setHeader("Source").setWidth("90px");
        historyGrid.addColumn(ImportLog::getCountry).setHeader("Land").setWidth("50px");
        historyGrid.addColumn(ImportLog::getRecordCount).setHeader("Records").setWidth("70px");
        historyGrid
                .addColumn(l -> l.getStartedAt() != null ? DT.format(l.getStartedAt()) : "\u2014")
                .setHeader("Gestartet")
                .setWidth("150px");
        historyGrid
                .addComponentColumn(
                        l -> {
                            String color =
                                    switch (l.getStatus()) {
                                        case "SUCCESS" -> "var(--rom-status-active)";
                                        case "ERROR" -> "var(--rom-status-danger)";
                                        default -> "var(--rom-status-info)";
                                    };
                            Span badge = new Span(l.getStatus());
                            badge.getStyle()
                                    .set("font-family", "'JetBrains Mono', monospace")
                                    .set("font-size", "10px")
                                    .set("font-weight", "600")
                                    .set("color", color)
                                    .set(
                                            "background",
                                            "color-mix(in srgb, " + color + " 12%, transparent)")
                                    .set("padding", "2px 6px")
                                    .set("border-radius", "3px");
                            return badge;
                        })
                .setHeader("Status")
                .setWidth("90px");
        historyGrid.addColumn(ImportLog::getMessage).setHeader("Details").setFlexGrow(1);
        historyGrid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_COMPACT);
        historyGrid.setItems(importService.getImportHistory());
        historyGrid.setAllRowsVisible(true);
        tabContent.add(historyGrid);
    }

    private void showTopoTab() {
        tabContent.removeAll();
        tabContent.add(new TopologyTab(opRepo, this::getTranslation));
    }

    private void showTagsTab() {
        tabContent.removeAll();
        tabContent.add(new TagsTab(tagRepo, tagImportService, this::getTranslation));
    }

    private void showCatalogTab() {
        tabContent.removeAll();
        tabContent.add(
                new CatalogTab(catalogItemRepository, catalogImportService, this::getTranslation));
    }

    private H3 sectionTitle(String text) {
        H3 h = new H3(text);
        h.getStyle()
                .set("color", "var(--rom-text-primary)")
                .set("margin", "0 0 var(--lumo-space-xs) 0")
                .set("font-size", "var(--lumo-font-size-m)");
        return h;
    }

    private Div statBox(String label, String value) {
        Div box = new Div();
        box.getStyle()
                .set("background", "var(--rom-bg-primary)")
                .set("border", "1px solid var(--rom-border)")
                .set("border-radius", "6px")
                .set("padding", "10px 14px")
                .set("min-width", "120px");

        Span val = new Span(value);
        val.getStyle()
                .set("font-family", "'JetBrains Mono', monospace")
                .set("font-size", "18px")
                .set("font-weight", "700")
                .set("color", "var(--rom-accent)")
                .set("display", "block");

        Span lbl = new Span(label);
        lbl.getStyle()
                .set("font-size", "10px")
                .set("color", "var(--rom-text-muted)")
                .set("display", "block");

        box.add(val, lbl);
        return box;
    }
}
